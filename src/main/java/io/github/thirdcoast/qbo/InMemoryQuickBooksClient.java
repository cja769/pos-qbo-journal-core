package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.JournalEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class InMemoryQuickBooksClient implements QuickBooksClient {
    private final Map<String, StoredEntry> entries = new ConcurrentHashMap<>();
    private final AtomicLong ids = new AtomicLong();
    public Optional<String> findByIdempotencyKey(String key) { return Optional.ofNullable(entries.get(key)).map(StoredEntry::id); }
    public String createJournalEntry(JournalEntry entry) {
        var stored = entries.computeIfAbsent(entry.idempotencyKey(), k -> new StoredEntry(Long.toString(ids.incrementAndGet()), entry));
        return stored.id();
    }
    public Collection<StoredEntry> entries() { return List.copyOf(entries.values()); }
    public record StoredEntry(String id, JournalEntry entry) {}
}
