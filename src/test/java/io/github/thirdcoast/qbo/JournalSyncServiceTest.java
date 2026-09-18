package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
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
        var service = new JournalSyncService(client, new JournalEntryFactory(), new AccountMapping(accounts, Map.of()));
        assertTrue(service.sync(s).created());
        assertFalse(service.sync(s).created());
        var entry = client.entries().iterator().next().entry();
        var debits = entry.lines().stream().filter(l -> l.postingType() == JournalLine.PostingType.DEBIT).map(l -> l.amount().amount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var credits = entry.lines().stream().filter(l -> l.postingType() == JournalLine.PostingType.CREDIT).map(l -> l.amount().amount()).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        assertEquals(0, debits.compareTo(credits));
    }

    @Test void rejectsUnbalancedSummary() {
        var s = new DailySalesSummary("x", "test", "one", LocalDate.now(), Money.usd("10.00"), Money.usd("0.00"),
                Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"),
                Money.usd("0.00"), Map.of("cash", Money.usd("9.00")));
        assertThrows(IllegalArgumentException.class, () -> SummaryValidator.validate(s));
    }
}
