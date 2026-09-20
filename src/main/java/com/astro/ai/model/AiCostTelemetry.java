package com.astro.ai.model;

import java.util.Map;

public class AiCostTelemetry {
    private long totalRequests;
    private long totalPromptTokens;
    private long totalCompletionTokens;
    private long totalTokens;
    private double totalEstimatedCostUsd;
    private long cacheHits;
    private long cacheMisses;
    private double cacheHitRatio;
    private double p95LatencyMs;
    private Map<String, Long> requestsByModel;
    private Map<String, Double> costByModel;

    public AiCostTelemetry() {}

    public long getTotalRequests() { return totalRequests; }
    public void setTotalRequests(long totalRequests) { this.totalRequests = totalRequests; }

    public long getTotalPromptTokens() { return totalPromptTokens; }
    public void setTotalPromptTokens(long totalPromptTokens) { this.totalPromptTokens = totalPromptTokens; }

    public long getTotalCompletionTokens() { return totalCompletionTokens; }
    public void setTotalCompletionTokens(long totalCompletionTokens) { this.totalCompletionTokens = totalCompletionTokens; }

    public long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(long totalTokens) { this.totalTokens = totalTokens; }

    public double getTotalEstimatedCostUsd() { return totalEstimatedCostUsd; }
    public void setTotalEstimatedCostUsd(double totalEstimatedCostUsd) { this.totalEstimatedCostUsd = totalEstimatedCostUsd; }

    public long getCacheHits() { return cacheHits; }
    public void setCacheHits(long cacheHits) { this.cacheHits = cacheHits; }

    public long getCacheMisses() { return cacheMisses; }
    public void setCacheMisses(long cacheMisses) { this.cacheMisses = cacheMisses; }

    public double getCacheHitRatio() { return cacheHitRatio; }
    public void setCacheHitRatio(double cacheHitRatio) { this.cacheHitRatio = cacheHitRatio; }

    public double getP95LatencyMs() { return p95LatencyMs; }
    public void setP95LatencyMs(double p95LatencyMs) { this.p95LatencyMs = p95LatencyMs; }

    public Map<String, Long> getRequestsByModel() { return requestsByModel; }
    public void setRequestsByModel(Map<String, Long> requestsByModel) { this.requestsByModel = requestsByModel; }

    public Map<String, Double> getCostByModel() { return costByModel; }
    public void setCostByModel(Map<String, Double> costByModel) { this.costByModel = costByModel; }
}
