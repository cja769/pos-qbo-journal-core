package io.github.thirdcoast.qbo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thirdcoast.qbo.model.AccountRole;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.thirdcoast.qbo.JsonHttp.text;

public final class LiveSyncJob {
    private final Function<String, String> config;
    private final DailySalesSummaryProvider provider;
    private final Consumer<String> report;
    private final ZoneId zone;
    private final Path directory;
    private final Clock clock = Clock.systemUTC();

    public LiveSyncJob(Function<String, String> config, DailySalesSummaryProvider provider,
                       ZoneId zone, Path directory, Consumer<String> report) {
        this.config = config;
        this.provider = provider;
        this.report = report;
        this.zone = zone;
        this.directory = directory;
    }

    public void run(LocalDate selectedDate) {
        var today = LocalDate.now(clock.withZone(zone));
        if (selectedDate != null && !selectedDate.isBefore(today)) {
            throw new IllegalArgumentException("Choose a completed business date");
        }
        var realm = config.apply("QBO_REALM_ID");
        if (realm != null && !realm.matches("[0-9]+")) throw new IllegalArgumentException("Invalid QuickBooks company ID");
        var mode = value(config, "SYNC_WRITE_MODE", "dry-run");
        if (!mode.equals("dry-run") && !mode.equals("post")) {
            throw new IllegalArgumentException("SYNC_WRITE_MODE must be dry-run or post");
        }
        var posting = mode.equals("post");
        var firstPostDate = posting ? LocalDate.parse(required(config, "SYNC_FIRST_POST_DATE")) : LocalDate.MIN;
        if (posting && realm == null) throw new IllegalArgumentException("Set QBO_REALM_ID before posting");
        if (posting && selectedDate != null && selectedDate.isBefore(firstPostDate)) {
            throw new IllegalArgumentException("Selected date precedes the posting start date");
        }
        var accounts = loadMapping(Path.of(required(config, "SYNC_ACCOUNT_MAPPING_FILE")), posting);
        var dates = selectedDate == null ? new ReconciliationPlanner().datesToVerify(today)
                : java.util.List.of(selectedDate);

        try (var ignored = SyncProcessLock.acquire(directory)) {
            var client = realm == null ? null : quickBooks(realm);
            if (client != null) {
                client.verifyCompany(required(config, "QBO_EXPECTED_COMPANY"));
            }
            var factory = new JournalEntryFactory();
            var service = posting ? new JournalSyncService(client, factory, accounts,
                    new FileSyncReceiptStore(directory.resolve("live-receipts")), clock) : null;
            int failures = 0;
            for (var date : dates) {
                if (posting && date.isBefore(firstPostDate)) continue;
                try {
                    var summary = provider.load(date);
                    summary.tenders().keySet().forEach(tender -> {
                        if (!accounts.tenderAccounts().containsKey(tender)) {
                            throw new IllegalStateException("No explicit QuickBooks mapping for tender " + tender);
                        }
                    });
                    var entry = factory.create(summary, accounts);
                    if (entry.lines().isEmpty()) {
                        if (client != null && client.findByIdempotencyKey(summary.idempotencyKey()).isPresent()) {
                            throw new IllegalStateException("Source is now empty for an existing journal");
                        }
                        report.accept(date + " no journal activity");
                    } else if (posting) {
                        var result = service.sync(summary);
                        report.accept(date + " " + result.action() + " QuickBooks journal " + result.quickBooksId());
                    } else {
                        var payload = IntuitJournalClient.payload(entry, SummaryFingerprint.sha256(summary, entry));
                        var existing = client == null ? "QuickBooks lookup skipped"
                                : client.findByIdempotencyKey(summary.idempotencyKey())
                                        .map(journal -> "existing journal " + journal.quickBooksId()).orElse("new journal");
                        report.accept(date + " dry-run " + existing + " " + payload);
                    }
                } catch (Exception e) {
                    failures++;
                    report.accept(date + " failed: " + e.getMessage());
                }
            }
            if (failures > 0) throw new IllegalStateException(failures + " sync date(s) failed");
        }
    }

    private IntuitJournalClient quickBooks(String realm) {
        var environment = value(config, "QBO_ENVIRONMENT", "sandbox");
        if (!environment.equals("sandbox") && !environment.equals("production")) {
            throw new IllegalArgumentException("QBO_ENVIRONMENT must be sandbox or production");
        }
        var http = new JsonHttp();
        var oauth = new IntuitOAuth(http, required(config, "QBO_CLIENT_ID"),
                required(config, "QBO_CLIENT_SECRET"), directory.resolve("qbo-" + realm + ".token"), clock);
        return new IntuitJournalClient(http, realm, environment.equals("sandbox"), oauth::authorization);
    }

    private static AccountMapping loadMapping(Path path, boolean posting) {
        try {
            var root = new ObjectMapper().readTree(path.toFile());
            var accounts = new EnumMap<AccountRole, String>(AccountRole.class);
            var configuredAccounts = root.path("accounts");
            if (!configuredAccounts.isObject()) throw new IllegalArgumentException("Missing accounts mapping");
            for (var role : AccountRole.values()) {
                if (posting || configuredAccounts.has(role.name())) {
                    accounts.put(role, accountId(configuredAccounts, role.name()));
                }
            }
            var tenders = new HashMap<String, String>();
            var configured = root.path("tenders");
            if (!configured.isObject()) throw new IllegalArgumentException("Missing tenders mapping");
            configured.fieldNames().forEachRemaining(name -> tenders.put(name, accountId(configured, name)));
            return new AccountMapping(accounts, tenders);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read the QuickBooks account mapping");
        }
    }

    private static String accountId(JsonNode mapping, String name) {
        var id = text(mapping, name);
        if (!id.matches("[0-9]+")) throw new IllegalArgumentException("Invalid account ID for " + name);
        return id;
    }

    public static String required(Function<String, String> config, String key) {
        var value = config.apply(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Set " + key);
        return value;
    }

    public static String value(Function<String, String> config, String key, String fallback) {
        var value = config.apply(key);
        return value == null ? fallback : value;
    }
}
