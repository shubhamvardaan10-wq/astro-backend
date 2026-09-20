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
import java.util.*;

@Service
public class AstroCacheService {

    private static final Logger log = LoggerFactory.getLogger(AstroCacheService.class);

    public static final String CALC_KEY_PREFIX = "astro:calc:";
    public static final String CHAT_KEY_PREFIX = "astro:chat:";

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<>() {};

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ObjectMapper canonicalMapper;
    private final boolean enabled;

    @Autowired
    public AstroCacheService(
            @Autowired(required = false) @Nullable StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${astro.redis.enabled:false}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.enabled = enabled;
        this.canonicalMapper = createCanonicalMapper();
    }

    public AstroCacheService(StringRedisTemplate redisTemplate, boolean enabled) {
        this(redisTemplate, new ObjectMapper(), enabled);
    }

    public AstroCacheService() {
        this(null, new ObjectMapper(), false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isAvailable() {
        return enabled && redisTemplate != null;
    }

    private String calcKey(String key) {
        if (key == null) {
            return null;
        }
        return key.startsWith(CALC_KEY_PREFIX) ? key : CALC_KEY_PREFIX + key;
    }

    private String chatKey(String sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionId.startsWith(CHAT_KEY_PREFIX) ? sessionId : CHAT_KEY_PREFIX + sessionId;
    }

    /**
     * Retrieves cached calculation result for the specified key.
     * Returns Optional.empty() if Redis is disabled, not configured, or if the key is missing.
     */
    public Optional<JsonNode> getCalculation(String key) {
        if (!isAvailable() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            String cached = redisTemplate.opsForValue().get(calcKey(key));
            if (cached == null || cached.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readTree(cached));
        } catch (Exception e) {
            log.warn("Failed to get calculation from Redis for key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Caches calculation result with the given TTL.
     * Gracefully no-ops if Redis is disabled or unavailable.
     */
    public void putCalculation(String key, JsonNode value, Duration ttl) {
        if (!isAvailable() || key == null || key.isBlank() || value == null) {
            return;
        }
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
     * Appends a chat message to the session's chat history list in Redis.
     * Gracefully no-ops if Redis is disabled or unavailable.
     */
    public void saveChatMessage(String sessionId, String role, String content) {
        if (!isAvailable() || sessionId == null || sessionId.isBlank()) {
            return;
        }
        try {
            Map<String, String> message = new LinkedHashMap<>();
            message.put("role", role != null ? role : "");
            message.put("content", content != null ? content : "");
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.opsForList().rightPush(chatKey(sessionId), json);
        } catch (Exception e) {
            log.warn("Failed to save chat message to Redis for session {}: {}", sessionId, e.getMessage());
        }
    }

    /**
     * Retrieves chronological chat history for the given session from Redis.
     * Returns an empty list if Redis is disabled, unavailable, or the session does not exist.
     */
    public List<Map<String, String>> getChatHistory(String sessionId) {
        if (!isAvailable() || sessionId == null || sessionId.isBlank()) {
            return List.of();
        }
        try {
            List<String> rawMessages = redisTemplate.opsForList().range(chatKey(sessionId), 0, -1);
            if (rawMessages == null || rawMessages.isEmpty()) {
                return List.of();
            }
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
        } catch (Exception e) {
            log.warn("Failed to get chat history from Redis for session {}: {}", sessionId, e.getMessage());
            return List.of();
        }
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
