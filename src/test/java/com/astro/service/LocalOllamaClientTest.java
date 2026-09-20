package com.astro.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class LocalOllamaClientTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private HttpServer server;
    private ExecutorService executor;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        executor.close();
    }

    private LocalOllamaClient client() {
        return new LocalOllamaClient(mapper, baseUrl, "qwen2.5:7b", 3);
    }

    private void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    @Test
    void verifiesInstalledLocalModelAndUsesStructuredNonStreamingChat() throws Exception {
        AtomicReference<JsonNode> payload = new AtomicReference<>();
        server.createContext("/api/show", e -> reply(e, 200, "{\"details\":{\"format\":\"gguf\"}}"));
        server.createContext("/api/chat", e -> {
            payload.set(mapper.readTree(e.getRequestBody()));
            reply(e, 200, "{\"done\":true,\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"sections\\\":[]}\"}}");
        });
        LocalOllamaClient client = client();
        client.verifyLocalModel();
        assertThat(client.generate("system rules", "evidence", mapper.readTree("{\"type\":\"object\"}")).has("sections")).isTrue();
        assertThat(payload.get().path("stream").asBoolean()).isFalse();
        assertThat(payload.get().path("model").asText()).isEqualTo("qwen2.5:7b");
        assertThat(payload.get().path("format").path("type").asText()).isEqualTo("object");
        assertThat(payload.get().path("options").path("num_predict").asInt()).isLessThanOrEqualTo(1200);
    }

    @Test
    void refusesRemoteUrlsCloudTagsAndCloudAliases() {
        for (String url : new String[]{"https://example.com", "http://127.0.0.1@example.com", baseUrl + "/proxy"}) {
            assertThatThrownBy(() -> new LocalOllamaClient(mapper, url, "qwen2.5:7b", 3).verifyLocalModel())
                .isInstanceOf(ResponseStatusException.class);
        }
        assertThatThrownBy(() -> new LocalOllamaClient(mapper, baseUrl, "glm-5.1:cloud", 3).verifyLocalModel())
            .isInstanceOf(ResponseStatusException.class);
        server.createContext("/api/show", e -> reply(e, 200, "{\"details\":{\"format\":\"gguf\"},\"remote_host\":\"https://example.com\"}"));
        assertThatThrownBy(() -> client().verifyLocalModel()).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void doesNotFollowRedirectsOrAutoDownloadMissingModels() {
        server.createContext("/api/show", e -> {
            e.getResponseHeaders().add("Location", "https://example.com");
            reply(e, 302, "{}");
        });
        assertThatThrownBy(() -> client().verifyLocalModel()).isInstanceOf(ResponseStatusException.class);
        server.removeContext("/api/show");
        server.createContext("/api/show", e -> reply(e, 404, "{\"error\":\"private detail\"}"));
        assertThatThrownBy(() -> client().verifyLocalModel()).isInstanceOfSatisfying(ResponseStatusException.class,
            e -> { assertThat(e.getStatusCode().value()).isEqualTo(503); assertThat(e.getReason()).doesNotContain("private detail"); });
    }

    @Test
    void enforcesTotalGenerationDeadline() {
        server.createContext("/api/chat", e -> {
            try { Thread.sleep(1500); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            try { reply(e, 200, "{}"); } catch (IOException disconnected) { e.close(); }
        });
        LocalOllamaClient client = new LocalOllamaClient(mapper, baseUrl, "qwen2.5:7b", 1);
        assertThatThrownBy(() -> client.generate("rules", "evidence", mapper.createObjectNode()))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(504));
    }

    @Test
    void detailedGenerationUsesALargerButBoundedTokenBudget() throws Exception {
        AtomicReference<JsonNode> payload = new AtomicReference<>();
        server.createContext("/api/chat", e -> {
            payload.set(mapper.readTree(e.getRequestBody()));
            reply(e, 200, "{\"done\":true,\"message\":{\"role\":\"assistant\",\"content\":\"{}\"}}");
        });
        client().generate("rules", "evidence", mapper.createObjectNode(), 2048, java.time.Duration.ofSeconds(3));
        assertThat(payload.get().path("options").path("num_predict").asInt()).isEqualTo(2048);
        assertThatThrownBy(() -> client().generate("rules", "evidence", mapper.createObjectNode(), 5000, java.time.Duration.ofSeconds(3)))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> client().generate("rules", "evidence", mapper.createObjectNode(), 2048, java.time.Duration.ZERO))
            .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(504));
    }

    @Test
    void rejectsInvalidGeneratedJsonAndDuplicateFields() {
        for (String invalid : new String[]{"not-json", "{} {}", "{\"sections\":{},\"sections\":{}}", "null"}) {
            server.createContext("/api/chat", e -> reply(e, 200, mapper.writeValueAsString(java.util.Map.of(
                "done", true, "message", java.util.Map.of("role", "assistant", "content", invalid)))));
            try {
                assertThatThrownBy(() -> client().generate("rules", "evidence", mapper.createObjectNode()))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(502));
            } finally {
                server.removeContext("/api/chat");
            }
        }
    }

    @Test
    void rejectsOversizedOrMalformedResponses() {
        server.createContext("/api/chat", e -> reply(e, 200, "x".repeat(300_000)));
        assertThatThrownBy(() -> client().generate("rules", "evidence", mapper.createObjectNode()))
            .isInstanceOf(ResponseStatusException.class);
        server.removeContext("/api/chat");
        server.createContext("/api/chat", e -> reply(e, 200, "{\"done\":false,\"message\":{\"content\":\"not JSON\"}}"));
        assertThatThrownBy(() -> client().generate("rules", "evidence", mapper.createObjectNode()))
            .isInstanceOf(ResponseStatusException.class);
    }
}
