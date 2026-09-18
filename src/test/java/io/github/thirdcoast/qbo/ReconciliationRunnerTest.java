package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReconciliationRunnerTest {
    @TempDir Path temp;
    @Test void continuesAfterOneDateFails() {
        var accounts = new EnumMap<AccountRole,String>(AccountRole.class);
        for (var role : AccountRole.values()) accounts.put(role, role.name());
        var sync = new JournalSyncService(new InMemoryQuickBooksClient(), new JournalEntryFactory(), new AccountMapping(accounts, Map.of()),
                new FileSyncReceiptStore(temp.resolve("receipts")), Clock.systemUTC());
        DailySalesSummaryProvider provider = date -> {
            if (date.equals(LocalDate.of(2026,9,15))) throw new IllegalStateException("source unavailable");
            return new DailySalesSummary("test:one:" + date, "test", "one", date, Money.usd("10.00"), Money.usd("0.00"),
                    Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"), Money.usd("0.00"),
                    Money.usd("0.00"), Map.of("cash", Money.usd("10.00")));
        };
        var report = new ReconciliationRunner(new ReconciliationPlanner(), provider, sync, temp).run(LocalDate.of(2026,9,20));
        assertEquals(6, report.results().size());
        assertEquals(Set.of(LocalDate.of(2026,9,15)), report.failures().keySet());
    }
}
