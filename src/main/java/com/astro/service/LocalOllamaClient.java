package com.astro.service;

import com.astro.util.LimitedHttpBody;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

@Component
public class LocalOllamaClient {
    private static final int MAX_RESPONSE = 256 * 1024;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String model;
    private final int timeoutSeconds;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER)
        .proxy(new ProxySelector() {
            public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
            public void connectFailed(URI uri, SocketAddress address, IOException error) { }
        }).build();

    public LocalOllamaClient(ObjectMapper mapper,
            @Value("${astro.ai.base-url:http://127.0.0.1:11434}") String baseUrl,
            @Value("${astro.ai.model:qwen2.5:7b}") String model,
            @Value("${astro.ai.timeout-seconds:90}") int timeoutSeconds) {
        this.mapper = mapper;
        this.baseUrl = baseUrl;
        this.model = model;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String model() { return model; }

    public void verifyLocalModel() {
        endpoint("/api/show");
        JsonNode response = post("/api/show", mapper.valueToTree(Map.of("model", model)), Math.min(timeoutSeconds, 10));
        if (response.hasNonNull("remote_host") || response.hasNonNull("remote_model")
                || !"gguf".equals(response.path("details").path("format").asText())) {
            throw unavailable("AI reports require an installed local GGUF model; cloud models are not allowed");
        }
    }

    public JsonNode generate(String system, String user, JsonNode schema) {
        return generate(system, user, schema, 1200, Duration.ofSeconds(timeoutSeconds));
    }

    public JsonNode generate(String system, String user, JsonNode schema, int maxTokens, Duration remaining) {
        if (maxTokens < 128 || maxTokens > 4096) throw new IllegalArgumentException("Invalid AI token budget");
        if (remaining.isNegative() || remaining.isZero()) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Detailed report exceeded its time limit");
        }
        int seconds = (int) Math.min(timeoutSeconds, Math.max(1, remaining.toSeconds()));
        var payload = mapper.createObjectNode();
        payload.put("model", model).put("stream", false).put("keep_alive", "2m");
        payload.set("format", schema);
        payload.set("messages", mapper.valueToTree(List.of(Map.of("role", "system", "content", system),
            Map.of("role", "user", "content", user))));
        payload.set("options", mapper.valueToTree(Map.of("temperature", 0, "seed", 0, "num_predict", maxTokens, "num_ctx", 8192)));
        JsonNode response = post("/api/chat", payload, seconds);
        if (!response.path("done").asBoolean() || "length".equals(response.path("done_reason").asText())
                || !"assistant".equals(response.path("message").path("role").asText())
                || !response.path("message").path("content").isTextual()) {
            throw invalidOutput();
        }
        try {
            JsonNode result = readJson(response.path("message").path("content").asText());
            if (result == null || !result.isObject()) throw invalidOutput();
            return result;
        } catch (IOException e) {
            throw invalidOutput();
        }
    }

    private URI endpoint(String path) {
        try {
            if (baseUrl == null) throw unavailable("Local AI address is not configured");
            URI base = URI.create(baseUrl);
            if (!"http".equals(base.getScheme()) || base.getHost() == null
                    || !Set.of("127.0.0.1", "localhost", "[::1]", "::1").contains(base.getHost())
                    || base.getRawUserInfo() != null || base.getRawQuery() != null || base.getRawFragment() != null
                    || !(base.getPath().isEmpty() || "/".equals(base.getPath()))
                    || base.getPort() < 1 || base.getPort() > 65535
                    || timeoutSeconds < 1 || timeoutSeconds > 180
                    || model == null || !model.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,90}(:[A-Za-z0-9._-]{1,40})?")
                    || model.toLowerCase(java.util.Locale.ROOT).contains("cloud")) {
                throw unavailable("Invalid local AI configuration; use a loopback HTTP address and a local model");
            }
            String host = "localhost".equals(base.getHost()) ? "127.0.0.1" : base.getHost();
            return new URI("http", null, host, base.getPort(), path, null, null);
        } catch (IllegalArgumentException | URISyntaxException e) {
            throw unavailable("Invalid local AI configuration");
        }
    }

    private JsonNode post(String path, JsonNode payload, int seconds) {
        URI uri = endpoint(path);
        CompletableFuture<HttpResponse<byte[]>> pending = null;
        try {
            byte[] body = mapper.writeValueAsBytes(payload);
            if (body.length > 48 * 1024) throw unavailable("AI context exceeds the configured limit");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(seconds))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
            pending = http.sendAsync(request, ignored -> new LimitedHttpBody(MAX_RESPONSE));
            HttpResponse<byte[]> response = pending.get(seconds, TimeUnit.SECONDS);
            if (response.statusCode() == 404) throw unavailable("Configured model is not installed in local Ollama");
            if (response.statusCode() != 200) throw unavailable("Local Ollama rejected the request");
            JsonNode json = readJson(new String(response.body(), java.nio.charset.StandardCharsets.UTF_8));
            if (json == null || !json.isObject() || json.has("error")) throw unavailable("Invalid response from local Ollama");
            return json;
        } catch (TimeoutException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Local AI exceeded its time limit");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Local AI request interrupted");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof HttpTimeoutException) {
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Local AI exceeded its time limit");
            }
            throw unavailable("Local Ollama is unavailable or returned an oversized response");
        } catch (IOException e) {
            throw unavailable("Local Ollama returned an unreadable response");
        } finally {
            if (pending != null && !pending.isDone()) pending.cancel(true);
        }
    }

    private JsonNode readJson(String content) throws IOException {
        return mapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .with(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).readTree(content);
    }

    private static ResponseStatusException unavailable(String reason) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, reason);
    }

    private static ResponseStatusException invalidOutput() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Local AI returned an incomplete or invalid report; retry the request");
    }

    @PreDestroy
    public void close() { http.shutdownNow(); }
}
