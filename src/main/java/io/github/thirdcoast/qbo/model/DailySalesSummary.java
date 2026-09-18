package io.github.thirdcoast.qbo.model;

import java.time.LocalDate;
import java.util.Map;

public record DailySalesSummary(
        String idempotencyKey,
        String source,
        String locationId,
        LocalDate businessDate,
        Money grossSales,
        Money discounts,
        Money returns,
        Money salesTax,
        Money tips,
        Money giftCardSales,
        Money giftCardRedemptions,
        Money processingFees,
        Map<String, Money> tenders
) {
    public DailySalesSummary {
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("idempotencyKey is required");
        if (source == null || source.isBlank()) throw new IllegalArgumentException("source is required");
        if (locationId == null || locationId.isBlank()) throw new IllegalArgumentException("locationId is required");
        if (businessDate == null) throw new IllegalArgumentException("businessDate is required");
        if (tenders == null || tenders.isEmpty()) throw new IllegalArgumentException("at least one tender is required");
        tenders = Map.copyOf(tenders);
    }
}
