package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.Map;
import static io.github.thirdcoast.qbo.model.AccountRole.*;
import static org.junit.jupiter.api.Assertions.*;

class JournalSyncServiceTest {
    @Test void buildsBalancedIdempotentJournal() {
        var s = new DailySalesSummary("lightspeed:store-1:2026-09-18", "lightspeed", "store-1", LocalDate.of(2026,9,18),
                Money.usd("100.00"), Money.usd("5.00"), Money.usd("10.00"), Money.usd("7.01"), Money.usd("12.00"),
                Money.usd("20.00"), Money.usd("15.00"), Money.usd("3.00"), Map.of("card", Money.usd("109.01")));
        var accounts = new EnumMap<AccountRole,String>(AccountRole.class);
        for (var role : AccountRole.values()) accounts.put(role, role.name());
        var client = new InMemoryQuickBooksClient();
        var receipts = new InMemorySyncReceiptStore();
        var service = new JournalSyncService(client, new JournalEntryFactory(), new AccountMapping(accounts, Map.of()), receipts,
                Clock.fixed(Instant.parse("2026-09-19T06:00:00Z"), ZoneOffset.UTC));
        assertEquals(JournalSyncService.SyncAction.CREATED, service.sync(s).action());
        assertEquals(JournalSyncService.SyncAction.UNCHANGED, service.sync(s).action());
        var entry = client.entries().iterator().next().entry();
        var debits = entry.lines().stream().filter(l -> l.postingType() == JournalLine.PostingType.DEBIT).map(l -> l.amount().amount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var credits = entry.lines().stream().filter(l -> l.postingType() == JournalLine.PostingType.CREDIT).map(l -> l.amount().amount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertEquals(0, debits.compareTo(credits));
        assertEquals(2, receipts.find(s.idempotencyKey()).orElseThrow().revision());
    }

    @Test void updatesChangedDayAndRecreatesMissingRemoteEntry() {
        var accounts = new EnumMap<AccountRole,String>(AccountRole.class);
        for (var role : AccountRole.values()) accounts.put(role, role.name());
        var client = new InMemoryQuickBooksClient();
        var service = new JournalSyncService(client, new JournalEntryFactory(), new AccountMapping(accounts, Map.of()),
                new InMemorySyncReceiptStore(), Clock.systemUTC());
        var original = summary("100.00", "100.00");
        var changed = summary("110.00", "110.00");
        assertEquals(JournalSyncService.SyncAction.CREATED, service.sync(original).action());
        assertEquals(JournalSyncService.SyncAction.UPDATED, service.sync(changed).action());
        client.remove(original.idempotencyKey());
        assertEquals(JournalSyncService.SyncAction.CREATED, service.sync(changed).action());
    }

    @Test void rejectsUnbalancedSummary() {
        var s = new DailySalesSummary("x", "test", "one", LocalDate.now(), Money.usd("10.00"), Money.usd("0.00"),
                Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"),
                Money.usd("0.00"), Map.of("cash", Money.usd("9.00")));
        assertThrows(IllegalArgumentException.class, () -> SummaryValidator.validate(s));
    }

    private DailySalesSummary summary(String gross, String cash) {
        return new DailySalesSummary("test:one:2026-09-18", "test", "one", LocalDate.of(2026,9,18), Money.usd(gross),
                Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"),
                Money.usd("0.00"), Money.usd("0.00"), Map.of("cash", Money.usd(cash)));
    }
}
