package com.astro.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

/**
 * Embedded Reverse Proxy Controller for 2-Service Lean Architecture.
 * Routes /health, /calc/*, /ai/*, and /media/* directly through the Java API Gateway,
 * completely eliminating the need for a separate Nginx container.
 */
@RestController
public class UnifiedIngressProxyController {

    private static final Logger log = LoggerFactory.getLogger(UnifiedIngressProxyController.class);

    private final ObjectMapper mapper;
    private final String engineBaseUrl;
    private final HttpClient httpClient;

    public UnifiedIngressProxyController(
            ObjectMapper mapper,
            @Value("${astro.worker.remote-url:http://localhost:8081}") String workerUrl,
            @Value("${astro.engine.base-url:}") String engineUrl) {
        this.mapper = mapper;
        String resolved = (engineUrl != null && !engineUrl.isBlank()) ? engineUrl : workerUrl;
        this.engineBaseUrl = (resolved != null && !resolved.isBlank()) ? resolved.replaceAll("/+$", "") : "http://localhost:8081";
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(4))
                .build();
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "astro-api-gateway",
                "proxy", "astro-api-gateway",
                "architecture", "2-service-lean",
                "engineTarget", engineBaseUrl
        ));
    }

    @GetMapping(value = "/calc/capabilities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCalcCapabilities() {
        return proxyGet(engineBaseUrl + "/calc/capabilities");
    }

    @PostMapping(value = "/ai/rag/query", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> proxyAiRagQuery(@RequestBody String payload) {
        return proxyPost(engineBaseUrl + "/ai/rag/query", payload);
    }

    @GetMapping(value = "/media/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getMediaHealth() {
        return proxyGet(engineBaseUrl + "/media/health");
    }

    private ResponseEntity<String> proxyGet(String targetUri) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return ResponseEntity.status(resp.statusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.body());
        } catch (Exception e) {
            log.warn("Proxy GET to {} failed: {}", targetUri, e.getMessage());
            return ResponseEntity.status(502).body("{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private ResponseEntity<String> proxyPost(String targetUri, String jsonBody) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(targetUri))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return ResponseEntity.status(resp.statusCode()).contentType(MediaType.APPLICATION_JSON).body(resp.body());
        } catch (Exception e) {
            log.warn("Proxy POST to {} failed: {}", targetUri, e.getMessage());
            return ResponseEntity.status(502).body("{\"status\":\"DOWN\",\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
