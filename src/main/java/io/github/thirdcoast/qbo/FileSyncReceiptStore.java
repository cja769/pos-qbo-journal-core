package io.github.thirdcoast.qbo;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

public final class FileSyncReceiptStore implements SyncReceiptStore {
    private final Path directory;
    public FileSyncReceiptStore(Path directory) { this.directory = directory; }

    public Optional<SyncReceipt> find(String key) {
        var path = pathFor(key);
        if (!Files.exists(path)) return Optional.empty();
        var p = new Properties();
        try (var in = Files.newInputStream(path)) {
            p.load(in);
            return Optional.of(new SyncReceipt(p.getProperty("idempotencyKey"), p.getProperty("quickBooksId"),
                    p.getProperty("sourceFingerprint"), Instant.parse(p.getProperty("verifiedAt")),
                    Long.parseLong(p.getProperty("revision")), JournalSyncService.SyncAction.valueOf(p.getProperty("action"))));
        } catch (IOException | RuntimeException e) { throw new IllegalStateException("Cannot read sync receipt " + path, e); }
    }

    public void save(SyncReceipt receipt) {
        try {
            Files.createDirectories(directory);
            var target = pathFor(receipt.idempotencyKey());
            var temp = Files.createTempFile(directory, target.getFileName().toString(), ".tmp");
            var p = new Properties();
            p.setProperty("idempotencyKey", receipt.idempotencyKey());
            p.setProperty("quickBooksId", receipt.quickBooksId());
            p.setProperty("sourceFingerprint", receipt.sourceFingerprint());
            p.setProperty("verifiedAt", receipt.verifiedAt().toString());
            p.setProperty("revision", Long.toString(receipt.revision()));
            p.setProperty("action", receipt.action().name());
            try (var out = new FileOutputStream(temp.toFile())) { p.store(out, "POS to QuickBooks sync receipt"); out.getFD().sync(); }
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            try (var channel = FileChannel.open(directory, StandardOpenOption.READ)) { channel.force(true); }
            catch (IOException ignored) { /* Directory fsync is unavailable on some filesystems. */ }
        } catch (IOException e) { throw new IllegalStateException("Cannot persist sync receipt", e); }
    }

    private Path pathFor(String key) { return directory.resolve(SummaryFingerprintKey.sha256(key) + ".properties"); }

    private static final class SummaryFingerprintKey {
        static String sha256(String value) {
            try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
            catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        }
    }
}
