package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.*;
import java.util.ArrayList;
import static io.github.thirdcoast.qbo.model.AccountRole.*;
import static io.github.thirdcoast.qbo.model.JournalLine.PostingType.*;

public final class JournalEntryFactory {
    public JournalEntry create(DailySalesSummary s, AccountMapping mapping) {
        SummaryValidator.validate(s);
        var lines = new ArrayList<JournalLine>();
        s.tenders().forEach((name, amount) -> add(lines, mapping.tenderAccount(name), DEBIT, amount, name + " payments"));
        add(lines, mapping.account(GIFT_CARD_LIABILITY), DEBIT, s.giftCardRedemptions(), "Gift cards redeemed");
        add(lines, mapping.account(DISCOUNTS), DEBIT, s.discounts(), "Discounts");
        add(lines, mapping.account(RETURNS), DEBIT, s.returns(), "Returns and refunds");
        add(lines, mapping.account(PROCESSING_FEES), DEBIT, s.processingFees(), "Processing fees");
        if (s.processingFees().amount().signum() != 0) {
            add(lines, mapping.account(TENDER_CLEARING), CREDIT, s.processingFees(), "Fees withheld from settlement");
        }
        add(lines, mapping.account(SALES_REVENUE), CREDIT, s.grossSales(), "Gross sales");
        add(lines, mapping.account(SALES_TAX_PAYABLE), CREDIT, s.salesTax(), "Sales tax collected");
        add(lines, mapping.account(TIPS_PAYABLE), CREDIT, s.tips(), "Tips collected");
        add(lines, mapping.account(GIFT_CARD_LIABILITY), CREDIT, s.giftCardSales(), "Gift cards sold");
        return new JournalEntry(s.idempotencyKey(), s.businessDate(),
                s.source() + " daily sales - " + s.locationId(), lines);
    }

    private void add(ArrayList<JournalLine> lines, String account, JournalLine.PostingType type, Money amount, String description) {
        if (amount.amount().signum() != 0) lines.add(new JournalLine(account, type, amount, description));
    }
}
