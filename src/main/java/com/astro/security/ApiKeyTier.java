package com.astro.security;

/**
 * Commercial Subscription & Rate-Limiting Tiers.
 */
public enum ApiKeyTier {
    FREE(30, 1_000),           // 30 req/min, 1,000 req/month
    STARTER(120, 25_000),      // 120 req/min, 25,000 req/month
    PRO(600, 250_000),         // 600 req/min, 250,000 req/month
    ENTERPRISE(3_000, -1),     // 3,000 req/min, Unlimited
    ADMIN(10_000, -1);         // Internal Master Operations

    private final int requestsPerMinute;
    private final long monthlyQuota;

    ApiKeyTier(int requestsPerMinute, long monthlyQuota) {
        this.requestsPerMinute = requestsPerMinute;
        this.monthlyQuota = monthlyQuota;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public long getMonthlyQuota() {
        return monthlyQuota;
    }
}
