package io.github.thirdcoast.qbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import io.github.thirdcoast.qbo.model.JournalEntry;
import io.github.thirdcoast.qbo.model.JournalLine;
import io.github.thirdcoast.qbo.model.Money;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.thirdcoast.qbo.model.JournalLine.PostingType.*;
import static org.junit.jupiter.api.Assertions.*;

class IntuitJournalClientTest {
    @Test
    void verifiesCompanyCreatesFindsUpdatesAndDetectsChangedLines() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var stored = new AtomicReference<ObjectNode>();
        var lastUpdateToken = new AtomicReference<String>();
        var json = new ObjectMapper();
        server.createContext("/v3/company/123/", exchange -> {
            var name = exchange.getRequestURI().getPath();
            var response = json.createObjectNode();
            if (name.endsWith("/companyinfo/123")) {
                response.putObject("CompanyInfo").put("CompanyName", "Example Retail LLC");
            } else if (name.endsWith("/query")) {
                var query = response.putObject("QueryResponse");
                if (stored.get() != null) query.putArray("JournalEntry").add(stored.get());
            } else if (exchange.getRequestMethod().equals("POST")) {
                var journal = (ObjectNode) json.readTree(exchange.getRequestBody());
                lastUpdateToken.set(journal.path("SyncToken").asText(null));
                journal.put("Id", "71").put("SyncToken", stored.get() == null ? "0" : "1");
                stored.set(journal);
                response.set("JournalEntry", journal);
            } else {
                response.set("JournalEntry", stored.get());
            }
            var bytes = response.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        server.start();
        try {
            var client = new IntuitJournalClient(new JsonHttp(),
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()), "123", () -> "Bearer test");
            client.verifyCompany("Example Retail LLC");
            assertThrows(IllegalStateException.class, () -> client.verifyCompany("Example Pet Services LLC"));
            var original = sale("20");
            assertTrue(client.findByIdempotencyKey(original.idempotencyKey()).isEmpty());
            var journalId = client.createJournalEntry(original, "first");
            assertEquals("71", journalId);
            assertEquals("first", client.findByIdempotencyKey(original.idempotencyKey()).orElseThrow().sourceFingerprint());
            var changed = sale("22");
            client.updateJournalEntry(journalId, changed, "second");
            assertEquals("0", lastUpdateToken.get());
            assertEquals("second", client.findByIdempotencyKey(original.idempotencyKey()).orElseThrow().sourceFingerprint());
            ((ObjectNode) stored.get().path("Line").get(0)).put("Amount", 99);
            assertThrows(IllegalStateException.class, () -> client.findByIdempotencyKey(original.idempotencyKey()));
        } finally {
            server.stop(0);
        }
    }

    private JournalEntry sale(String dollars) {
        return new JournalEntry("lightspeed:outlet:2026-10-21", LocalDate.parse("2026-10-21"), "Sales",
                List.of(new JournalLine("101", DEBIT, Money.usd(dollars), "Card"),
                        new JournalLine("102", CREDIT, Money.usd(dollars), "Sales")));
    }
}
