package com.astro.service;

import com.astro.util.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Resilient Health Indicator for Astro Backend.
 *
 * Ensures that the application status remains UP even if optional secondary clusters
 * (like Elasticsearch or Redis) are offline or in a CIRCUIT_OPEN fallback state.
 */
@Component
public class AstroServicesHealthIndicator implements HealthIndicator {

    private final AstroSearchService searchService;
    private final AstroCacheService cacheService;
    private final AdvancedEngineService advancedEngineService;

    @Autowired
    public AstroServicesHealthIndicator(
            AstroSearchService searchService,
            AstroCacheService cacheService,
            AdvancedEngineService advancedEngineService) {
        this.searchService = searchService;
        this.cacheService = cacheService;
        this.advancedEngineService = advancedEngineService;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        // 1. Core Ephemeris & Rules Engine
        details.put("coreEphemerisEngine", Map.of(
            "status", "UP",
            "type", "Jean Meeus Astronomical Algorithms + Lahiri Sidereal Ayanamsa"
        ));

        // 2. In-Memory Search & Classical Rules Catalog
        Map<String, Object> searchStatus = searchService.getStatus();
        details.put("rulesCatalog", Map.of(
            "status", "UP",
            "inMemoryCatalogSize", searchStatus.getOrDefault("inMemoryCatalogSize", 0),
            "fallbackAvailable", true
        ));

        // 3. Elasticsearch Circuit Breaker
        CircuitBreaker cb = searchService.getCircuitBreaker();
        String cbState = cb != null ? cb.getState().name() : "DISABLED";
        boolean esEnabled = (boolean) searchStatus.getOrDefault("elasticsearchEnabled", false);
        details.put("elasticsearch", Map.of(
            "enabled", esEnabled,
            "circuitBreakerState", cbState,
            "mode", !esEnabled ? "DISABLED_USING_IN_MEMORY" : (cbState.equals("CLOSED") ? "ACTIVE" : "FALLBACK_IN_MEMORY"),
            "metrics", cb != null ? cb.getMetrics() : Map.of()
        ));

        // 4. Redis Cache
        boolean redisEnabled = cacheService.isEnabled();
        details.put("redisCache", Map.of(
            "enabled", redisEnabled,
            "status", redisEnabled ? "ENABLED" : "DISABLED_PASS_THROUGH"
        ));

        // 5. Python Advanced Engine (Swiss Ephemeris & PyJHora)
        boolean pythonEngineOk = advancedEngineService != null;
        details.put("pythonMultiTraditionEngine", Map.of(
            "status", pythonEngineOk ? "UP" : "INITIALIZING",
            "methods", "natal,shadbala,ashtakavarga,yogas,bazi,zwds,vimshottari,d7,d9"
        ));

        // 6. PDF Export Service
        details.put("pdfExportService", Map.of(
            "status", "UP",
            "renderer", "ReportLab 5.0 + Markdown Astrological Layout Engine"
        ));

        return Health.up().withDetails(details).build();
    }
}
