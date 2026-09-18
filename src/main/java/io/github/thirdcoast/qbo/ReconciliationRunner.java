package io.github.thirdcoast.qbo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

public final class ReconciliationRunner {
    private final ReconciliationPlanner planner;
    private final DailySalesSummaryProvider provider;
    private final JournalSyncService syncService;
    private final Path dataDirectory;

    public ReconciliationRunner(ReconciliationPlanner planner, DailySalesSummaryProvider provider,
                                JournalSyncService syncService, Path dataDirectory) {
        this.planner = planner; this.provider = provider; this.syncService = syncService; this.dataDirectory = dataDirectory;
    }

    public Report run(LocalDate runDate) {
        var results = new LinkedHashMap<LocalDate, JournalSyncService.SyncResult>();
        var failures = new LinkedHashMap<LocalDate, String>();
        try (var ignored = SyncProcessLock.acquire(dataDirectory)) {
            for (var date : planner.datesToVerify(runDate)) {
                try { results.put(date, syncService.sync(provider.load(date))); }
                catch (Exception e) { failures.put(date, e.getClass().getSimpleName() + ": " + e.getMessage()); }
            }
        }
        return new Report(Collections.unmodifiableMap(results), Collections.unmodifiableMap(failures));
    }

    public record Report(Map<LocalDate, JournalSyncService.SyncResult> results, Map<LocalDate, String> failures) {
        public boolean successful() { return failures.isEmpty(); }
    }
}
