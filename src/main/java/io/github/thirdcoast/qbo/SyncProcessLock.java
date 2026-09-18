package io.github.thirdcoast.qbo;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;

public final class SyncProcessLock implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private SyncProcessLock(FileChannel channel, FileLock lock) { this.channel = channel; this.lock = lock; }
    public static SyncProcessLock acquire(Path dataDirectory) {
        try {
            Files.createDirectories(dataDirectory);
            var channel = FileChannel.open(dataDirectory.resolve("sync.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            var lock = channel.tryLock();
            if (lock == null) { channel.close(); throw new IllegalStateException("Another synchronization is already running"); }
            return new SyncProcessLock(channel, lock);
        } catch (IOException e) { throw new IllegalStateException("Cannot acquire synchronization lock", e); }
    }
    public void close() {
        try { lock.release(); channel.close(); }
        catch (IOException e) { throw new IllegalStateException("Cannot release synchronization lock", e); }
    }
}
