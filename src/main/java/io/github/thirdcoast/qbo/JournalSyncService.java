package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.DailySalesSummary;

public final class JournalSyncService {
    private final QuickBooksClient client;
    private final JournalEntryFactory factory;
    private final AccountMapping accounts;

    public JournalSyncService(QuickBooksClient client, JournalEntryFactory factory, AccountMapping accounts) {
        this.client = client; this.factory = factory; this.accounts = accounts;
    }
    public SyncResult sync(DailySalesSummary summary) {
        var existing = client.findByIdempotencyKey(summary.idempotencyKey());
        if (existing.isPresent()) return new SyncResult(existing.get(), false);
        return new SyncResult(client.createJournalEntry(factory.create(summary, accounts)), true);
    }
    public record SyncResult(String quickBooksId, boolean created) {}
}
