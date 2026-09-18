package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.DailySalesSummary;
import io.github.thirdcoast.qbo.model.Money;
import java.util.List;

public final class SummaryValidator {
    private SummaryValidator() {}

    public static void validate(DailySalesSummary s) {
        List.of(s.grossSales(), s.discounts(), s.returns(), s.salesTax(), s.tips(), s.giftCardSales(),
                s.giftCardRedemptions(), s.processingFees()).forEach(SummaryValidator::nonNegative);
        s.tenders().values().forEach(SummaryValidator::nonNegative);
        var currency = s.grossSales().currency();
        var tenders = s.tenders().values().stream().reduce(Money.zero(currency), Money::add);
        var expected = s.grossSales().subtract(s.discounts()).subtract(s.returns())
                .add(s.salesTax()).add(s.tips()).add(s.giftCardSales()).subtract(s.giftCardRedemptions());
        if (tenders.amount().compareTo(expected.amount()) != 0) {
            throw new IllegalArgumentException("Summary does not reconcile: tenders=" + tenders.amount() + ", expected=" + expected.amount());
        }
    }

    private static void nonNegative(Money money) {
        if (money.isNegative()) throw new IllegalArgumentException("Amounts must be non-negative");
    }
}
