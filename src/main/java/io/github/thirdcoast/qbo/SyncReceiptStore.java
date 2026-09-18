package io.github.thirdcoast.qbo;

import java.util.Optional;

public interface SyncReceiptStore {
    Optional<SyncReceipt> find(String idempotencyKey);
    void save(SyncReceipt receipt);
}
