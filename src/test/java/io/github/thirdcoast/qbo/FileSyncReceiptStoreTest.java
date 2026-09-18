package io.github.thirdcoast.qbo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class FileSyncReceiptStoreTest {
    @TempDir Path temp;
    @Test void atomicallyPersistsAndReplacesReceipt() {
        var store = new FileSyncReceiptStore(temp);
        store.save(new SyncReceipt("source:location:date", "123", "abc", Instant.parse("2026-09-18T06:00:00Z"), 1,
                JournalSyncService.SyncAction.CREATED));
        store.save(new SyncReceipt("source:location:date", "123", "def", Instant.parse("2026-09-19T06:00:00Z"), 2,
                JournalSyncService.SyncAction.UPDATED));
        var receipt = store.find("source:location:date").orElseThrow();
        assertEquals("def", receipt.sourceFingerprint());
        assertEquals(2, receipt.revision());
        assertEquals(1, assertDoesNotThrow(() -> java.nio.file.Files.list(temp).filter(p -> p.toString().endsWith(".properties")).count()));
    }
}
