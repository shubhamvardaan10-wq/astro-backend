package com.astro.ai.model;

public record UsageStats(
    int promptTokens,
    int completionTokens,
    int totalTokens,
    double estimatedCostUsd
) {
    public static UsageStats of(int promptTokens, int completionTokens, double costUsd) {
        return new UsageStats(promptTokens, completionTokens, promptTokens + completionTokens, costUsd);
    }

    public static UsageStats zero() {
        return new UsageStats(0, 0, 0, 0.0);
    }
}
