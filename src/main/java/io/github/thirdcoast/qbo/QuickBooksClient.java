package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.JournalEntry;
import java.util.Optional;

public interface QuickBooksClient {
    Optional<PostedJournal> findByIdempotencyKey(String idempotencyKey);
    String createJournalEntry(JournalEntry entry, String sourceFingerprint);
    void updateJournalEntry(String quickBooksId, JournalEntry entry, String sourceFingerprint);

    record PostedJournal(String quickBooksId, String sourceFingerprint) {}
}
