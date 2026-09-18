package io.github.thirdcoast.qbo.model;

import java.time.LocalDate;
import java.util.List;

public record JournalEntry(String idempotencyKey, LocalDate transactionDate, String memo, List<JournalLine> lines) {
    public JournalEntry { lines = List.copyOf(lines); }
}
