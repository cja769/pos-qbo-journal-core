package io.github.thirdcoast.qbo;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.Instant;

import static io.github.thirdcoast.qbo.JsonHttp.*;

public final class IntuitOAuth {
    private static final URI TOKEN_ENDPOINT = URI.create("https://oauth.platform.intuit.com/oauth2/v1/tokens/bearer");
    private final JsonHttp http;
    private final String authorization;
    private final Path refreshFile;
    private final Clock clock;
    private String accessToken;
    private Instant refreshAfter = Instant.EPOCH;

    public IntuitOAuth(JsonHttp http, String clientId, String clientSecret, Path refreshFile, Clock clock) {
        this.http = http;
        this.authorization = basic(clientId + ":" + clientSecret);
        this.refreshFile = refreshFile;
        this.clock = clock;
    }

    public synchronized String authorization() {
        if (!clock.instant().isBefore(refreshAfter)) {
            var result = http.post(TOKEN_ENDPOINT, authorization, "application/x-www-form-urlencoded",
                    "grant_type=refresh_token&refresh_token=" + encode(readRefreshToken()));
            saveRefreshToken(text(result, "refresh_token"));
            accessToken = text(result, "access_token");
            int expiresIn = result.path("expires_in").asInt();
            if (expiresIn <= 60) throw new IllegalStateException("Unexpected Intuit access token lifetime");
            refreshAfter = clock.instant().plusSeconds(expiresIn - 60);
        }
        return "Bearer " + accessToken;
    }

    private String readRefreshToken() {
        try {
            var token = Files.readString(refreshFile).strip();
            if (token.isEmpty()) throw new IllegalStateException("Intuit refresh token file is empty");
            return token;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read Intuit refresh token file");
        }
    }

    private void saveRefreshToken(String token) {
        try {
            var destination = refreshFile.toAbsolutePath();
            var file = Files.createTempFile(destination.getParent(), ".intuit-", ".token",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            try {
                try (var channel = java.nio.channels.FileChannel.open(file, java.nio.file.StandardOpenOption.WRITE)) {
                    var bytes = java.nio.charset.StandardCharsets.UTF_8.encode(token);
                    while (bytes.hasRemaining()) channel.write(bytes);
                    channel.force(true);
                }
                Files.move(file, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot persist the rotated Intuit refresh token");
        }
    }
}
