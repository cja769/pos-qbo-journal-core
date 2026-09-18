package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.JournalEntry;
import java.util.Optional;

public interface QuickBooksClient {
    Optional<String> findByIdempotencyKey(String idempotencyKey);
    String createJournalEntry(JournalEntry entry);
}
