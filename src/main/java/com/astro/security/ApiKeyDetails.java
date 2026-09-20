package com.astro.security;

import java.time.Instant;

/**
 * Metadata record for validated API keys.
 */
public record ApiKeyDetails(
    String key,
    String ownerEmail,
    ApiKeyTier tier,
    boolean active,
    long createdAtEpochMs,
    long expiresAtEpochMs
) {
    public boolean isExpired() {
        return expiresAtEpochMs > 0 && Instant.now().toEpochMilli() > expiresAtEpochMs;
    }
}
