package io.github.thirdcoast.qbo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemorySyncReceiptStore implements SyncReceiptStore {
    private final Map<String, SyncReceipt> receipts = new ConcurrentHashMap<>();
    public Optional<SyncReceipt> find(String key) { return Optional.ofNullable(receipts.get(key)); }
    public void save(SyncReceipt receipt) { receipts.put(receipt.idempotencyKey(), receipt); }
}
