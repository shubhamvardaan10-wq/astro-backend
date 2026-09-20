package com.astro.controller;

import com.astro.model.AdvancedRequest;
import com.astro.model.GeocodingRequest;
import com.astro.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/astro/v2")
public class AdvancedAstroController {

    private final AdvancedEngineService engine;
    private final AiReportService reports;
    private final GeocodingService geocoding;
    private final AstroCacheService cacheService;
    private final AstroKafkaService kafkaService;
    private final AstroSearchService searchService;

    @Autowired
    public AdvancedAstroController(
            AdvancedEngineService engine,
            AiReportService reports,
            GeocodingService geocoding,
            @Autowired(required = false) AstroCacheService cacheService,
            @Autowired(required = false) AstroKafkaService kafkaService,
            @Autowired(required = false) AstroSearchService searchService
    ) {
        this.engine = engine;
        this.reports = reports;
        this.geocoding = geocoding;
        this.cacheService = cacheService != null ? cacheService : new AstroCacheService();
        this.kafkaService = kafkaService != null ? kafkaService : new AstroKafkaService();
        this.searchService = searchService != null ? searchService : new AstroSearchService();
    }

    public AdvancedAstroController(AdvancedEngineService engine, AiReportService reports, GeocodingService geocoding) {
        this(engine, reports, geocoding, null, null, null);
    }

    @GetMapping("/methods")
    public JsonNode methods() {
        return engine.capabilities();
    }

    @PostMapping("/analyze")
    public JsonNode analyze(@Valid @RequestBody AdvancedRequest request) {
        String cacheKey = cacheService.computeCalculationKey(request);
        Optional<JsonNode> cached = cacheService.getCalculation(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        JsonNode computed = engine.analyze(request);
        if (computed != null && "complete".equals(computed.path("status").asText())) {
            cacheService.putCalculation(cacheKey, computed, Duration.ofHours(24));
        }
        return computed;
    }

    @PostMapping("/locations/search")
    public JsonNode locations(@Valid @RequestBody GeocodingRequest request) {
        return geocoding.search(request);
    }

    @PostMapping("/ai/report")
    public JsonNode aiReport(@Valid @RequestBody AdvancedRequest request) {
        kafkaService.publishTelemetry("report_requested_sync", Map.of(
            "topic", request.topic() != null ? request.topic() : "general"
        ));
        return reports.report(request);
    }

    @PostMapping("/ai/report/async")
    public ResponseEntity<Map<String, Object>> aiReportAsync(@Valid @RequestBody AdvancedRequest request) {
        String jobId = UUID.randomUUID().toString();
        kafkaService.publishReportRequest(jobId, request);
        kafkaService.publishTelemetry("report_requested_async", Map.of(
            "jobId", jobId,
            "topic", request.topic() != null ? request.topic() : "general"
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
            "jobId", jobId,
            "status", "QUEUED",
            "message", "Report generation task queued for asynchronous processing"
        ));
    }

    @PostMapping("/ai/chat")
    public JsonNode aiChat(@Valid @RequestBody AdvancedRequest request) {
        String sessionId = null;
        if (request.options() != null && request.options().containsKey("sessionId")) {
            Object s = request.options().get("sessionId");
            if (s != null) sessionId = s.toString();
        }
        JsonNode response = reports.chat(request);
        if (sessionId != null && !sessionId.isBlank()) {
            cacheService.saveChatMessage(sessionId, "user", request.question());
            if (response.hasNonNull("answer")) {
                cacheService.saveChatMessage(sessionId, "assistant", response.get("answer").asText());
            }
        }
        return response;
    }

    @GetMapping("/ai/chat/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> chatHistory(@PathVariable String sessionId) {
        List<Map<String, String>> history = cacheService.getChatHistory(sessionId);
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "count", history.size(),
            "history", history
        ));
    }

    @PostMapping("/ai/compatibility")
    public JsonNode aiCompatibility(@Valid @RequestBody AdvancedRequest request) {
        return reports.compatibility(request);
    }

    @PostMapping("/ai/cross-tradition")
    public JsonNode aiCrossTradition(@Valid @RequestBody AdvancedRequest request) {
        return reports.crossTradition(request);
    }

    @PostMapping("/ai/muhurta")
    public JsonNode aiMuhurta(@Valid @RequestBody AdvancedRequest request) {
        return reports.muhurta(request);
    }

    @GetMapping("/search/rules")
    public ResponseEntity<Map<String, Object>> searchRules(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        List<Map<String, Object>> results = searchService.searchRules(query, limit);
        return ResponseEntity.ok(Map.of(
            "query", query,
            "count", results.size(),
            "limit", limit,
            "elasticsearchEnabled", searchService.isEnabled(),
            "results", results
        ));
    }

    @PostMapping("/search/rules")
    public ResponseEntity<Map<String, Object>> indexRule(@RequestBody Map<String, String> body) {
        String id = body.get("id");
        String title = body.get("title");
        String tradition = body.getOrDefault("tradition", "Vedic");
        String content = body.get("content");
        String category = body.getOrDefault("category", "Custom");
        if (id == null || id.isBlank() || title == null || content == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "id, title, and content are required"));
        }
        searchService.indexRule(id, title, tradition, content, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", "indexed",
            "id", id,
            "title", title
        ));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> invalidRequest(Exception error) {
        String message = error instanceof IllegalArgumentException ? error.getMessage() : "Invalid analysis request";
        return ResponseEntity.badRequest().body(Map.of("error", message == null ? "Invalid analysis request" : message));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> calculationError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("error",
            error.getReason() == null ? "Calculation failed" : error.getReason()));
    }
}
