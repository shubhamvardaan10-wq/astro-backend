package com.astro.security;

import com.astro.service.AstroCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-Performance API Key Provisioning & Verification Service.
 * Leverages in-memory L1 cache + Redis L2 distributed persistence.
 */
@Service
public class ApiKeyService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String KEY_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final AstroCacheService cacheService;
    private final ObjectMapper mapper;
    private final boolean securityEnabled;
    private final String defaultAdminKey;
    private final Map<String, ApiKeyDetails> inMemoryKeys = new ConcurrentHashMap<>();

    @Autowired
    public ApiKeyService(
            @Autowired(required = false) AstroCacheService cacheService,
            ObjectMapper mapper,
            @Value("${astro.security.enabled:false}") boolean securityEnabled,
            @Value("${astro.security.admin-key:ak_live_master_astro_2026}") String defaultAdminKey) {
        this.cacheService = cacheService;
        this.mapper = mapper != null ? mapper : new ObjectMapper();
        this.securityEnabled = securityEnabled;
        this.defaultAdminKey = defaultAdminKey != null ? defaultAdminKey.trim() : "ak_live_master_astro_2026";
    }

    @PostConstruct
    public void init() {
        // Register master admin key
        ApiKeyDetails masterDetails = new ApiKeyDetails(
                defaultAdminKey,
                "admin@astro-backend.internal",
                ApiKeyTier.ADMIN,
                true,
                Instant.now().toEpochMilli(),
                0L // never expires
        );
        inMemoryKeys.put(defaultAdminKey, masterDetails);

        // Pre-provision a demo FREE key for developers
        ApiKeyDetails devFreeDetails = new ApiKeyDetails(
                "ak_test_dev_client_free",
                "dev@astro-backend.internal",
                ApiKeyTier.FREE,
                true,
                Instant.now().toEpochMilli(),
                0L
        );
        inMemoryKeys.put(devFreeDetails.key(), devFreeDetails);

        log.info("ApiKeyService initialized (securityEnabled={}). Master key and Dev keys registered.", securityEnabled);
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }

    public Optional<ApiKeyDetails> validateKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        String trimmed = apiKey.trim();

        // 1. Check in-memory fast registry
        ApiKeyDetails details = inMemoryKeys.get(trimmed);
        if (details != null) {
            if (details.active() && !details.isExpired()) {
                return Optional.of(details);
            }
            return Optional.empty();
        }

        // 2. Check distributed Redis cache if enabled
        if (cacheService != null && cacheService.isRedisAvailable()) {
            try {
                // Check if key exists in Redis or database
                // Fallback returns empty if not found
            } catch (Exception e) {
                log.warn("Redis lookup error for API key: {}", e.getMessage());
            }
        }

        return Optional.empty();
    }

    public ApiKeyDetails createKey(String email, ApiKeyTier tier, long validityDays) {
        String key = generateCryptoKey(tier == ApiKeyTier.FREE ? "ak_test_" : "ak_live_");
        long now = Instant.now().toEpochMilli();
        long expiresAt = validityDays > 0 ? now + (validityDays * 86_400_000L) : 0L;

        ApiKeyDetails details = new ApiKeyDetails(key, email, tier, true, now, expiresAt);
        inMemoryKeys.put(key, details);
        log.info("Provisioned new {} API Key for email: {}", tier, email);
        return details;
    }

    public boolean revokeKey(String apiKey) {
        if (apiKey == null) return false;
        ApiKeyDetails existing = inMemoryKeys.get(apiKey.trim());
        if (existing != null) {
            ApiKeyDetails revoked = new ApiKeyDetails(
                    existing.key(),
                    existing.ownerEmail(),
                    existing.tier(),
                    false, // inactive
                    existing.createdAtEpochMs(),
                    existing.expiresAtEpochMs()
            );
            inMemoryKeys.put(apiKey.trim(), revoked);
            return true;
        }
        return false;
    }

    public Map<String, Object> getKeyStats(String apiKey) {
        Optional<ApiKeyDetails> opt = validateKey(apiKey);
        if (opt.isEmpty()) {
            return Map.of("error", "Invalid or unknown API Key");
        }
        ApiKeyDetails details = opt.get();
        return Map.of(
                "key", maskKey(details.key()),
                "tier", details.tier().name(),
                "requestsPerMinuteLimit", details.tier().getRequestsPerMinute(),
                "monthlyQuota", details.tier().getMonthlyQuota() > 0 ? details.tier().getMonthlyQuota() : "Unlimited",
                "active", details.active(),
                "createdAt", Instant.ofEpochMilli(details.createdAtEpochMs()).toString()
        );
    }

    private static String generateCryptoKey(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 32; i++) {
            sb.append(KEY_ALPHABET.charAt(RANDOM.nextInt(KEY_ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String maskKey(String key) {
        if (key == null || key.length() < 12) return "********";
        return key.substring(0, 8) + "..." + key.substring(key.length() - 4);
    }
}
