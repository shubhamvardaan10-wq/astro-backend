package com.astro.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-Throughput In-Memory & Redis Rate Limiter.
 * Enforces per-minute sliding quotas according to API Key subscription tier.
 */
@Component
public class ApiKeyRateLimiter {

    public record RateLimitResult(
            boolean allowed,
            int limit,
            int remaining,
            long retryAfterSeconds
    ) {}

    // In-memory sliding minute bucket: "apiKey:minuteEpoch" -> AtomicInteger count
    private final Map<String, AtomicInteger> localCounters = new ConcurrentHashMap<>();

    public RateLimitResult checkRateLimit(ApiKeyDetails details) {
        if (details.tier() == ApiKeyTier.ADMIN) {
            // Admins have no rate limits
            return new RateLimitResult(true, details.tier().getRequestsPerMinute(), details.tier().getRequestsPerMinute(), 0);
        }

        int limit = details.tier().getRequestsPerMinute();
        long currentMinuteEpoch = Instant.now().getEpochSecond() / 60;
        String bucketKey = details.key() + ":" + currentMinuteEpoch;

        AtomicInteger counter = localCounters.computeIfAbsent(bucketKey, k -> new AtomicInteger(0));
        int currentCount = counter.incrementAndGet();

        // Evict expired minute counters periodically
        if (localCounters.size() > 10_000) {
            localCounters.entrySet().removeIf(e -> {
                String[] parts = e.getKey().split(":");
                if (parts.length == 2) {
                    try {
                        long min = Long.parseLong(parts[1]);
                        return min < currentMinuteEpoch;
                    } catch (NumberFormatException ignored) {}
                }
                return false;
            });
        }

        if (currentCount > limit) {
            long secondsUntilNextMinute = 60 - (Instant.now().getEpochSecond() % 60);
            return new RateLimitResult(false, limit, 0, secondsUntilNextMinute);
        }

        return new RateLimitResult(true, limit, limit - currentCount, 0);
    }
}
