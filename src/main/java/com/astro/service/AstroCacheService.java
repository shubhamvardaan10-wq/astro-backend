package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Multi-Tier Caching Service.
 * - L1: High-concurrency, bounded In-Memory Cache with LRU eviction and TTL.
 * - L2: Distributed Redis Cache (optional, active when enabled).
 * - Fallback in-memory chat session history for local/test environments.
 */
@Service
public class AstroCacheService {

    private static final Logger log = LoggerFactory.getLogger(AstroCacheService.class);

    public static final String CALC_KEY_PREFIX = "astro:calc:";
    public static final String CHAT_KEY_PREFIX = "astro:chat:";

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    // ── L1 Cache Structures ───────────────────────────────────────────────────
    private record L1Entry(JsonNode value, Instant expiresAt) {
        boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, L1Entry> l1Cache;
    private final boolean l1Enabled;
    private final int l1MaxSize;
    private final Duration l1DefaultTtl;

    // In-memory fallback for chat sessions if Redis is disabled
    private final Map<String, List<Map<String, String>>> localChatHistory = new ConcurrentHashMap<>();

    // Metrics telemetry
    private final AtomicLong l1Hits = new AtomicLong(0);
    private final AtomicLong l2Hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalMapper;
    private final boolean redisEnabled;

    @Autowired
    public AstroCacheService(
            @Autowired(required = false) @Nullable StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${astro.redis.enabled:false}") boolean redisEnabled,
            @Value("${astro.cache.l1-enabled:true}") boolean l1Enabled,
            @Value("${astro.cache.l1-max-size:10000}") int l1MaxSize,
            @Value("${astro.cache.l1-ttl-minutes:60}") int l1TtlMinutes
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.redisEnabled = redisEnabled;
        this.l1Enabled = l1Enabled;
        this.l1MaxSize = Math.max(100, l1MaxSize);
        this.l1DefaultTtl = Duration.ofMinutes(Math.max(1, l1TtlMinutes));
        this.canonicalMapper = createCanonicalMapper();

        // LinkedHashMap with access-order for LRU eviction bounded by l1MaxSize
        this.l1Cache = Collections.synchronizedMap(new LinkedHashMap<String, L1Entry>(128, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, L1Entry> eldest) {
                return size() > AstroCacheService.this.l1MaxSize;
            }
        });

        log.info("Initialized AstroCacheService with L1 In-Memory Cache (enabled={}, maxSize={}, ttl={}m) and L2 Redis (enabled={})",
                l1Enabled, this.l1MaxSize, l1TtlMinutes, redisEnabled);
    }

    public AstroCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, boolean enabled) {
        this(redisTemplate, objectMapper, enabled, enabled, 10000, 60);
    }

    public AstroCacheService(StringRedisTemplate redisTemplate, boolean enabled) {
        this(redisTemplate, new ObjectMapper(), enabled, enabled, 10000, 60);
    }

    public AstroCacheService() {
        this(null, new ObjectMapper(), false, false, 10000, 60);
    }

    public boolean isEnabled() {
        return redisEnabled;
    }

    public boolean isL1Enabled() {
        return l1Enabled;
    }

    public boolean isRedisAvailable() {
        return redisEnabled && redisTemplate != null;
    }

    private String calcKey(String key) {
        if (key == null) return null;
        return key.startsWith(CALC_KEY_PREFIX) ? key : CALC_KEY_PREFIX + key;
    }

    private String chatKey(String sessionId) {
        if (sessionId == null) return null;
        return sessionId.startsWith(CHAT_KEY_PREFIX) ? sessionId : CHAT_KEY_PREFIX + sessionId;
    }

    /**
     * Retrieves cached calculation result using multi-tier hierarchy:
     * 1. L1 In-Memory Cache (< 0.2ms)
     * 2. L2 Redis Cache (if enabled, ~1ms)
     */
    public Optional<JsonNode> getCalculation(String key) {
        if (key == null || key.isBlank()) {
            return Optional.empty();
        }

        // Tier 1: Check L1 In-Memory Cache
        if (l1Enabled) {
            L1Entry entry = l1Cache.get(key);
            if (entry != null) {
                if (!entry.isExpired()) {
                    l1Hits.incrementAndGet();
                    return Optional.of(entry.value());
                } else {
                    l1Cache.remove(key);
                }
            }
        }

        // Tier 2: Check L2 Redis Cache
        if (isRedisAvailable()) {
            try {
                String cached = redisTemplate.opsForValue().get(calcKey(key));
                if (cached != null && !cached.isBlank()) {
                    JsonNode node = objectMapper.readTree(cached);
                    if (node != null) {
                        l2Hits.incrementAndGet();
                        // Populate L1 cache for subsequent fast reads
                        if (l1Enabled) {
                            l1Cache.put(key, new L1Entry(node, Instant.now().plus(l1DefaultTtl)));
                        }
                        return Optional.of(node);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get calculation from Redis for key {}: {}", key, e.getMessage());
            }
        }

        misses.incrementAndGet();
        return Optional.empty();
    }

    /**
     * Caches calculation result across all active tiers (L1 In-Memory + L2 Redis).
     */
    public void putCalculation(String key, JsonNode value, Duration ttl) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }

        // Store into L1 Cache
        if (l1Enabled) {
            Duration effectiveTtl = (ttl != null && !ttl.isZero() && !ttl.isNegative()) ? ttl : l1DefaultTtl;
            l1Cache.put(key, new L1Entry(value, Instant.now().plus(effectiveTtl)));
        }

        // Store into L2 Redis Cache
        if (isRedisAvailable()) {
            try {
                String json = objectMapper.writeValueAsString(value);
                String redisKey = calcKey(key);
                if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
                    redisTemplate.opsForValue().set(redisKey, json, ttl);
                } else {
                    redisTemplate.opsForValue().set(redisKey, json);
                }
            } catch (Exception e) {
                log.warn("Failed to put calculation into Redis for key {}: {}", key, e.getMessage());
            }
        }
    }

    /**
     * Computes a deterministic SHA-256 hash over the canonical JSON representation
     * of the given AdvancedRequest.
     */
    public String computeCalculationKey(AdvancedRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        try {
            JsonNode tree = canonicalMapper.valueToTree(request);
            JsonNode canonicalTree = sortJsonNode(tree);
            String canonicalJson = canonicalMapper.writeValueAsString(canonicalTree);
            return sha256Hex(canonicalJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute calculation key", e);
        }
    }

    /**
     * Appends a chat message to the session's chat history list.
     * Stores in Redis if available, else in-memory localChatHistory.
     */
    public void saveChatMessage(String sessionId, String role, String content) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        Map<String, String> message = new LinkedHashMap<>();
        message.put("role", role != null ? role : "");
        message.put("content", content != null ? content : "");

        if (isRedisAvailable()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(chatKey(sessionId), json);
                return;
            } catch (Exception e) {
                log.warn("Failed to save chat message to Redis for session {}: {}", sessionId, e.getMessage());
            }
        }

        // Local in-memory fallback
        localChatHistory.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
    }

    /**
     * Retrieves chronological chat history for the given session.
     */
    public List<Map<String, String>> getChatHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return List.of();
        }

        if (isRedisAvailable()) {
            try {
                List<String> rawMessages = redisTemplate.opsForList().range(chatKey(sessionId), 0, -1);
                if (rawMessages != null && !rawMessages.isEmpty()) {
                    List<Map<String, String>> history = new ArrayList<>(rawMessages.size());
                    for (String raw : rawMessages) {
                        if (raw != null && !raw.isBlank()) {
                            try {
                                Map<String, String> msg = objectMapper.readValue(raw, MAP_TYPE);
                                history.add(msg);
                            } catch (Exception e) {
                                log.warn("Failed to deserialize chat message for session {}: {}", sessionId, e.getMessage());
                            }
                        }
                    }
                    return Collections.unmodifiableList(history);
                }
            } catch (Exception e) {
                log.warn("Failed to get chat history from Redis for session {}: {}", sessionId, e.getMessage());
            }
        }

        // Return local in-memory history fallback
        List<Map<String, String>> local = localChatHistory.get(sessionId);
        return local != null ? Collections.unmodifiableList(new ArrayList<>(local)) : List.of();
    }

    public Map<String, Object> getCacheStats() {
        return Map.of(
            "l1Size", l1Cache.size(),
            "l1Hits", l1Hits.get(),
            "l2Hits", l2Hits.get(),
            "misses", misses.get()
        );
    }

    private ObjectMapper createCanonicalMapper() {
        return com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();
    }

    private JsonNode sortJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            Collections.sort(fieldNames);
            ObjectNode sorted = JsonNodeFactory.instance.objectNode();
            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                if (child != null && !child.isNull()) {
                    JsonNode sortedChild = sortJsonNode(child);
                    if (sortedChild != null) {
                        sorted.set(fieldName, sortedChild);
                    }
                }
            }
            return sorted;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            ArrayNode sortedArray = JsonNodeFactory.instance.arrayNode();
            for (JsonNode child : arrayNode) {
                JsonNode sortedChild = sortJsonNode(child);
                if (sortedChild != null) {
                    sortedArray.add(sortedChild);
                }
            }
            return sortedArray;
        }
        return node;
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not found", e);
        }
    }
}
