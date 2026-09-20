package com.astro.ai.cost;

import com.astro.ai.model.AiCostTelemetry;
import com.astro.ai.model.UsageStats;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiCostTracker {

    // Pricing per 1,000,000 tokens (USD)
    private record ModelPrice(double promptPerMillion, double completionPerMillion) {}

    private final Map<String, ModelPrice> pricingCatalog = Map.of(
        "gpt-4o", new ModelPrice(2.50, 10.00),
        "gpt-4o-mini", new ModelPrice(0.15, 0.60),
        "claude-3-5-sonnet", new ModelPrice(3.00, 15.00),
        "gemini-2-0-flash", new ModelPrice(0.10, 0.40),
        "ollama-local", new ModelPrice(0.00, 0.00),
        "mock-provider", new ModelPrice(0.00, 0.00)
    );

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalPromptTokens = new AtomicLong(0);
    private final AtomicLong totalCompletionTokens = new AtomicLong(0);

    private final Map<String, AtomicLong> requestsByModel = new ConcurrentHashMap<>();
    private final Map<String, Double> costByModel = new ConcurrentHashMap<>();

    public UsageStats record(String model, int promptTokens, int completionTokens) {
        totalRequests.incrementAndGet();
        totalPromptTokens.addAndGet(promptTokens);
        totalCompletionTokens.addAndGet(completionTokens);

        String normalizedModel = model != null ? model.toLowerCase() : "default";
        requestsByModel.computeIfAbsent(normalizedModel, k -> new AtomicLong(0)).incrementAndGet();

        double cost = calculateCost(normalizedModel, promptTokens, completionTokens);
        costByModel.merge(normalizedModel, cost, (oldVal, newVal) -> Math.round((oldVal + newVal) * 100000.0) / 100000.0);

        return new UsageStats(promptTokens, completionTokens, promptTokens + completionTokens, cost);
    }

    public double calculateCost(String model, int promptTokens, int completionTokens) {
        ModelPrice price = pricingCatalog.getOrDefault(model.toLowerCase(), new ModelPrice(0.50, 1.50));
        double promptCost = (promptTokens / 1_000_000.0) * price.promptPerMillion();
        double completionCost = (completionTokens / 1_000_000.0) * price.completionPerMillion();
        return Math.round((promptCost + completionCost) * 100000.0) / 100000.0;
    }

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) return 0;
        // Standard rule of thumb: ~4 characters per token in English / technical text
        return Math.max(1, (int) Math.ceil(text.length() / 4.0));
    }

    public AiCostTelemetry getTelemetry(long cacheHits, long cacheMisses, double cacheHitRatio, double p95LatencyMs) {
        AiCostTelemetry telemetry = new AiCostTelemetry();
        telemetry.setTotalRequests(totalRequests.get());
        telemetry.setTotalPromptTokens(totalPromptTokens.get());
        telemetry.setTotalCompletionTokens(totalCompletionTokens.get());
        telemetry.setTotalTokens(totalPromptTokens.get() + totalCompletionTokens.get());

        double totalCost = costByModel.values().stream().mapToDouble(Double::doubleValue).sum();
        telemetry.setTotalEstimatedCostUsd(Math.round(totalCost * 10000.0) / 10000.0);

        telemetry.setCacheHits(cacheHits);
        telemetry.setCacheMisses(cacheMisses);
        telemetry.setCacheHitRatio(cacheHitRatio);
        telemetry.setP95LatencyMs(p95LatencyMs);

        Map<String, Long> reqMap = new ConcurrentHashMap<>();
        requestsByModel.forEach((k, v) -> reqMap.put(k, v.get()));
        telemetry.setRequestsByModel(reqMap);
        telemetry.setCostByModel(new ConcurrentHashMap<>(costByModel));

        return telemetry;
    }
}
