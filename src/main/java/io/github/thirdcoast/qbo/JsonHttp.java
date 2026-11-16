package io.github.thirdcoast.qbo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public final class JsonHttp {
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();

    public JsonNode get(URI uri, String authorization) {
        return send(request(uri, authorization).GET().build());
    }

    public JsonNode post(URI uri, String authorization, ObjectNode payload) {
        return post(uri, authorization, "application/json", payload.toString());
    }

    public JsonNode post(URI uri, String authorization, String contentType, String payload) {
        return send(request(uri, authorization).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofString(payload)).build());
    }

    private HttpRequest.Builder request(URI uri, String authorization) {
        return HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json").header("Authorization", authorization);
    }

    private JsonNode send(HttpRequest request) {
        var host = request.uri().getHost();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(host + " returned HTTP " + response.statusCode());
            }
            return JSON.readTree(response.body());
        } catch (IOException e) {
            throw new IllegalStateException(host + " transport or response error");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(host + " request interrupted");
        }
    }

    public static ObjectNode object() { return JSON.createObjectNode(); }

    public static String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    public static String basic(String secret) {
        return "Basic " + Base64.getEncoder().encodeToString(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static String text(JsonNode parent, String field) {
        var value = parent.path(field);
        if ((!value.isTextual() && !value.isIntegralNumber()) || value.asText().isBlank()) {
            throw new IllegalStateException("Missing or invalid field " + field);
        }
        return value.asText();
    }

    public static JsonNode array(JsonNode parent, String field) {
        var value = parent.path(field);
        if (!value.isArray()) throw new IllegalStateException("Missing array " + field);
        return value;
    }
}
