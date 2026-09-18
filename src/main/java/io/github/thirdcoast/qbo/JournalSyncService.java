package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.DailySalesSummary;
import java.time.Clock;
import java.time.Instant;

public final class JournalSyncService {
    private final QuickBooksClient client;
    private final JournalEntryFactory factory;
    private final AccountMapping accounts;
    private final SyncReceiptStore receipts;
    private final Clock clock;

    public JournalSyncService(QuickBooksClient client, JournalEntryFactory factory, AccountMapping accounts,
                              SyncReceiptStore receipts, Clock clock) {
        this.client = client; this.factory = factory; this.accounts = accounts; this.receipts = receipts; this.clock = clock;
    }
    public SyncResult sync(DailySalesSummary summary) {
        var fingerprint = SummaryFingerprint.sha256(summary);
        var journal = factory.create(summary, accounts);
        var existing = client.findByIdempotencyKey(summary.idempotencyKey());
        if (existing.isEmpty()) {
            var id = client.createJournalEntry(journal, fingerprint);
            receipts.save(receipt(summary, id, fingerprint, SyncAction.CREATED));
            return new SyncResult(id, SyncAction.CREATED);
        }
        var posted = existing.get();
        if (fingerprint.equals(posted.sourceFingerprint())) {
            receipts.save(receipt(summary, posted.quickBooksId(), fingerprint, SyncAction.UNCHANGED));
            return new SyncResult(posted.quickBooksId(), SyncAction.UNCHANGED);
        }
        client.updateJournalEntry(posted.quickBooksId(), journal, fingerprint);
        receipts.save(receipt(summary, posted.quickBooksId(), fingerprint, SyncAction.UPDATED));
        return new SyncResult(posted.quickBooksId(), SyncAction.UPDATED);
    }

    private SyncReceipt receipt(DailySalesSummary summary, String id, String fingerprint, SyncAction action) {
        var revision = receipts.find(summary.idempotencyKey()).map(r -> r.revision() + 1).orElse(1L);
        return new SyncReceipt(summary.idempotencyKey(), id, fingerprint, Instant.now(clock), revision, action);
    }
    public enum SyncAction { CREATED, UPDATED, UNCHANGED }
    public record SyncResult(String quickBooksId, SyncAction action) {}
}
