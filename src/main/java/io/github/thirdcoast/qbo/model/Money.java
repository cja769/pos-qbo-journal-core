package io.github.thirdcoast.qbo.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null) throw new IllegalArgumentException("amount is required");
        if (currency == null || currency.isBlank()) throw new IllegalArgumentException("currency is required");
        amount = amount.setScale(2, RoundingMode.UNNECESSARY);
        currency = currency.toUpperCase();
    }

    public static Money usd(String amount) { return new Money(new BigDecimal(amount), "USD"); }
    public static Money zero(String currency) { return new Money(BigDecimal.ZERO.setScale(2), currency); }
    public Money add(Money other) { requireSameCurrency(other); return new Money(amount.add(other.amount), currency); }
    public Money subtract(Money other) { requireSameCurrency(other); return new Money(amount.subtract(other.amount), currency); }
    public boolean isNegative() { return amount.signum() < 0; }
    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) throw new IllegalArgumentException("currency mismatch");
    }
}
