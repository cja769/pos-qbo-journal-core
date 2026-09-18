package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.JournalEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryQuickBooksClient implements QuickBooksClient {
    private final Map<String, StoredEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    public Optional<PostedJournal> findByIdempotencyKey(String key) {
        return Optional.ofNullable(entries.get(key)).map(e -> new PostedJournal(e.id(), e.sourceFingerprint()));
    }
    public String createJournalEntry(JournalEntry entry, String sourceFingerprint) {
        var stored = entries.computeIfAbsent(entry.idempotencyKey(),
                k -> new StoredEntry(Long.toString(ids.incrementAndGet()), entry, sourceFingerprint));
        return stored.id();
    }
    public void updateJournalEntry(String quickBooksId, JournalEntry entry, String sourceFingerprint) {
        var current = entries.get(entry.idempotencyKey());
        if (current == null || !current.id().equals(quickBooksId)) throw new IllegalArgumentException("Journal entry not found: " + quickBooksId);
        entries.put(entry.idempotencyKey(), new StoredEntry(quickBooksId, entry, sourceFingerprint));
    }
    public void remove(String idempotencyKey) { entries.remove(idempotencyKey); }
    public Collection<StoredEntry> entries() { return List.copyOf(entries.values()); }
    public record StoredEntry(String id, JournalEntry entry, String sourceFingerprint) {}
}
