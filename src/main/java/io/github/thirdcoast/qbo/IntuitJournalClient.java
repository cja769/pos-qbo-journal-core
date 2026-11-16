package io.github.thirdcoast.qbo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.thirdcoast.qbo.model.JournalEntry;
import io.github.thirdcoast.qbo.model.JournalLine;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.function.Supplier;

import static io.github.thirdcoast.qbo.JsonHttp.*;

public final class IntuitJournalClient implements QuickBooksClient {
    private static final String NOTE_PREFIX = "pos-qbo-sync ";
    private final JsonHttp http;
    private final URI company;
    private final String realm;
    private final Supplier<String> authorization;

    public IntuitJournalClient(JsonHttp http, String realm, boolean sandbox, Supplier<String> authorization) {
        this(http, URI.create(sandbox ? "https://sandbox-quickbooks.api.intuit.com" : "https://quickbooks.api.intuit.com"),
                realm, authorization);
    }

    IntuitJournalClient(JsonHttp http, URI host, String realm, Supplier<String> authorization) {
        if (!realm.matches("[0-9]+")) throw new IllegalArgumentException("Invalid QuickBooks company ID");
        this.http = http;
        this.realm = realm;
        this.company = host.resolve("/v3/company/" + realm + "/");
        this.authorization = authorization;
    }

    public void verifyCompany(String expectedName) {
        var info = get("companyinfo/" + realm).path("CompanyInfo");
        if (!expectedName.equals(text(info, "CompanyName"))) {
            throw new IllegalStateException("QuickBooks company name does not match configuration");
        }
    }

    @Override
    public Optional<PostedJournal> findByIdempotencyKey(String key) {
        return find(key).map(journal -> new PostedJournal(text(journal, "Id"), fingerprint(journal, key)));
    }

    @Override
    public String createJournalEntry(JournalEntry entry, String fingerprint) {
        return save(entry, fingerprint, null, null);
    }

    @Override
    public void updateJournalEntry(String id, JournalEntry entry, String fingerprint) {
        if (!id.matches("[0-9]+")) throw new IllegalArgumentException("Invalid journal ID");
        var journal = get("journalentry/" + id).path("JournalEntry");
        fingerprint(journal, entry.idempotencyKey());
        if (!entry.transactionDate().toString().equals(text(journal, "TxnDate"))) {
            throw new IllegalStateException("QuickBooks journal date was changed manually");
        }
        save(entry, fingerprint, id, text(journal, "SyncToken"));
    }

    private Optional<JsonNode> find(String key) {
        var query = "select * from JournalEntry where DocNumber = '" + documentNumber(key) + "' MAXRESULTS 2";
        var response = get("query?query=" + encode(query)).path("QueryResponse");
        if (!response.isObject()) throw new IllegalStateException("Missing QuickBooks query response");
        var matches = response.path("JournalEntry");
        if (matches.isMissingNode()) return Optional.empty();
        if (!matches.isArray() || matches.size() != 1) {
            throw new IllegalStateException("Ambiguous QuickBooks journal lookup");
        }
        return Optional.of(matches.get(0));
    }

    private String save(JournalEntry entry, String fingerprint, String id, String syncToken) {
        var payload = payload(entry, fingerprint);
        if (id != null) {
            payload.put("Id", id);
            payload.put("SyncToken", syncToken);
        }
        var requestId = digest(entry.idempotencyKey() + ":" + id + ":" + syncToken + ":" + fingerprint).substring(0, 40);
        var returned = http.post(company.resolve("journalentry?requestid=" + requestId),
                authorization.get(), payload).path("JournalEntry");
        var savedId = text(returned, "Id");
        var verified = get("journalentry/" + savedId).path("JournalEntry");
        if (!fingerprint.equals(fingerprint(verified, entry.idempotencyKey()))
                || !entry.transactionDate().toString().equals(text(verified, "TxnDate"))
                || (id != null && !id.equals(savedId))) {
            throw new IllegalStateException("QuickBooks journal write did not verify");
        }
        return savedId;
    }

    static ObjectNode payload(JournalEntry entry, String fingerprint) {
        if (entry.lines().size() < 2) throw new IllegalArgumentException("A journal needs at least two lines");
        var json = object();
        json.put("DocNumber", documentNumber(entry.idempotencyKey()));
        json.put("TxnDate", entry.transactionDate().toString());
        var lines = json.putArray("Line");
        for (var line : entry.lines()) {
            if (!"USD".equals(line.amount().currency())) throw new IllegalArgumentException("Only USD is supported");
            var item = lines.addObject();
            item.put("Amount", line.amount().amount());
            item.put("Description", line.description());
            item.put("DetailType", "JournalEntryLineDetail");
            var detail = item.putObject("JournalEntryLineDetail");
            detail.put("PostingType", line.postingType() == JournalLine.PostingType.DEBIT ? "Debit" : "Credit");
            detail.putObject("AccountRef").put("value", line.accountId());
        }
        json.put("PrivateNote", NOTE_PREFIX + digest(entry.idempotencyKey()) + " " + fingerprint
                + " " + journalDigest(json) + "\n" + entry.memo());
        return json;
    }

    private static String fingerprint(JsonNode journal, String key) {
        var note = text(journal, "PrivateNote").split("\n", 2)[0].split(" ");
        if (!documentNumber(key).equals(text(journal, "DocNumber")) || note.length != 4
                || !(NOTE_PREFIX + digest(key)).equals(note[0] + " " + note[1])) {
            throw new IllegalStateException("QuickBooks journal lacks the expected sync identity");
        }
        if (!journalDigest(journal).equals(note[3])) {
            throw new IllegalStateException("QuickBooks journal was changed outside this sync");
        }
        return note[2];
    }

    private static String journalDigest(JsonNode journal) {
        var canonical = new StringBuilder(text(journal, "TxnDate"));
        var lines = new java.util.ArrayList<String>();
        for (var line : array(journal, "Line")) {
            var detail = line.path("JournalEntryLineDetail");
            lines.add(text(detail.path("AccountRef"), "value") + "|" + text(detail, "PostingType") + "|"
                    + line.path("Amount").decimalValue().stripTrailingZeros().toPlainString());
        }
        lines.stream().sorted().forEach(value -> canonical.append('\n').append(value));
        return digest(canonical.toString());
    }

    private JsonNode get(String path) { return http.get(company.resolve(path), authorization.get()); }

    private static String documentNumber(String key) { return "PS-" + digest(key).substring(0, 18); }

    private static String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
