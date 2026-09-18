package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.DailySalesSummary;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class SummaryFingerprint {
    private SummaryFingerprint() {}
    public static String sha256(DailySalesSummary s) {
        var canonical = new StringBuilder()
                .append(s.idempotencyKey()).append('|').append(s.businessDate()).append('|')
                .append(s.grossSales().amount()).append('|').append(s.discounts().amount()).append('|')
                .append(s.returns().amount()).append('|').append(s.salesTax().amount()).append('|')
                .append(s.tips().amount()).append('|').append(s.giftCardSales().amount()).append('|')
                .append(s.giftCardRedemptions().amount()).append('|').append(s.processingFees().amount()).append('|')
                .append(s.grossSales().currency());
        s.tenders().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .forEach(e -> canonical.append('|').append(e.getKey()).append('=').append(e.getValue().amount()));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
