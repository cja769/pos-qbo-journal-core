package io.github.thirdcoast.qbo.model;

public record JournalLine(String accountId, PostingType postingType, Money amount, String description) {
    public enum PostingType { DEBIT, CREDIT }
}
