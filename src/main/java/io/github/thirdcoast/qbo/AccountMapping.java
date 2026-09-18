package io.github.thirdcoast.qbo;

import io.github.thirdcoast.qbo.model.AccountRole;
import java.util.Map;

public record AccountMapping(Map<AccountRole, String> accounts, Map<String, String> tenderAccounts) {
    public AccountMapping {
        accounts = Map.copyOf(accounts);
        tenderAccounts = Map.copyOf(tenderAccounts);
    }
    public String account(AccountRole role) {
        var id = accounts.get(role);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("No account configured for " + role);
        return id;
    }
    public String tenderAccount(String tender) {
        var id = tenderAccounts.get(tender);
        if (id == null || id.isBlank()) id = accounts.get(AccountRole.TENDER_CLEARING);
        if (id == null || id.isBlank()) throw new IllegalArgumentException("No account configured for tender " + tender);
        return id;
    }
}
