package io.github.thirdcoast.qbo;

import java.time.Instant;

public record SyncReceipt(String idempotencyKey, String quickBooksId, String sourceFingerprint,
                          Instant verifiedAt, long revision, JournalSyncService.SyncAction action) {}
