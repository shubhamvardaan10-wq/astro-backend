package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AstroCacheServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ListOperations<String, String> listOperations;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        listOperations = mock(ListOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    private AdvancedRequest createSampleRequest() {
        Map<String, Object> birth = new LinkedHashMap<>();
        birth.put("year", 1990);
        birth.put("month", 5);
        birth.put("day", 15);
        birth.put("hour", 14);
        birth.put("minute", 30);
        birth.put("latitude", 28.6139);
        birth.put("longitude", 77.2090);

        Map<String, Object> options = new LinkedHashMap<>();
        options.put("ayanamsa", "lahiri");
        options.put("nodes", "true");

        return new AdvancedRequest(
                birth,
                "2026-09-18T00:00:00Z",
                List.of("natal", "vimshottari"),
                options,
                null,
                "What does my career look like?",
                "career",
                "Test User",
                "male",
                "seed-123",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Nested
    @DisplayName("Disabled State (astro.redis.enabled=false)")
    class DisabledStateTests {

        private AstroCacheService service;

        @BeforeEach
        void init() {
            service = new AstroCacheService(redisTemplate, objectMapper, false);
        }

        @Test
        @DisplayName("isEnabled returns false")
        void isEnabledReturnsFalse() {
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("getCalculation gracefully returns empty without calling Redis")
        void getCalculationReturnsEmpty() {
            Optional<JsonNode> result = service.getCalculation("some-key");
            assertThat(result).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("putCalculation gracefully no-ops without calling Redis")
        void putCalculationNoOps() {
            JsonNode payload = objectMapper.createObjectNode().put("result", "ok");
            service.putCalculation("some-key", payload, Duration.ofMinutes(5));
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("saveChatMessage gracefully no-ops without calling Redis")
        void saveChatMessageNoOps() {
            service.saveChatMessage("sess-1", "user", "hello");
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("getChatHistory gracefully returns empty list without calling Redis")
        void getChatHistoryReturnsEmpty() {
            List<Map<String, String>> history = service.getChatHistory("sess-1");
            assertThat(history).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("computeCalculationKey works even when Redis is disabled")
        void computeCalculationKeyWorks() {
            AdvancedRequest request = createSampleRequest();
            String key = service.computeCalculationKey(request);
            assertThat(key).isNotNull().matches("^[a-f0-9]{64}$");
            verifyNoInteractions(redisTemplate);
        }
    }

    @Nested
    @DisplayName("Null RedisTemplate State")
    class NullRedisTemplateTests {

        private AstroCacheService service;

        @BeforeEach
        void init() {
            service = new AstroCacheService(null, objectMapper, true);
        }

        @Test
        @DisplayName("getCalculation returns empty without NPE")
        void getCalculationWithNullTemplate() {
            assertThat(service.getCalculation("any-key")).isEmpty();
        }

        @Test
        @DisplayName("putCalculation no-ops without NPE")
        void putCalculationWithNullTemplate() {
            JsonNode payload = objectMapper.createObjectNode().put("a", 1);
            service.putCalculation("any-key", payload, Duration.ofMinutes(1));
        }

        @Test
        @DisplayName("saveChatMessage no-ops without NPE")
        void saveChatMessageWithNullTemplate() {
            service.saveChatMessage("session-1", "user", "msg");
        }

        @Test
        @DisplayName("getChatHistory returns empty list without NPE")
        void getChatHistoryWithNullTemplate() {
            assertThat(service.getChatHistory("session-1")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Enabled State (astro.redis.enabled=true)")
    class EnabledStateTests {

        private AstroCacheService service;

        @BeforeEach
        void init() {
            service = new AstroCacheService(redisTemplate, objectMapper, true);
        }

        @Test
        @DisplayName("isEnabled returns true")
        void isEnabledReturnsTrue() {
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("getCalculation returns parsed JsonNode on cache hit")
        void getCalculationHit() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "calc-hash-1";
            when(valueOperations.get(redisKey)).thenReturn("{\"status\":\"ok\",\"planets\":[\"Sun\",\"Moon\"]}");

            Optional<JsonNode> result = service.getCalculation("calc-hash-1");

            assertThat(result).isPresent();
            assertThat(result.get().get("status").asText()).isEqualTo("ok");
            assertThat(result.get().get("planets").get(0).asText()).isEqualTo("Sun");
            verify(valueOperations).get(redisKey);
        }

        @Test
        @DisplayName("getCalculation handles key that already has prefix")
        void getCalculationAlreadyPrefixedKey() {
            String fullKey = AstroCacheService.CALC_KEY_PREFIX + "calc-hash-2";
            when(valueOperations.get(fullKey)).thenReturn("{\"result\":42}");

            Optional<JsonNode> result = service.getCalculation(fullKey);

            assertThat(result).isPresent();
            assertThat(result.get().get("result").asInt()).isEqualTo(42);
            verify(valueOperations).get(fullKey);
        }

        @Test
        @DisplayName("getCalculation returns empty on cache miss")
        void getCalculationMiss() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "missing-key";
            when(valueOperations.get(redisKey)).thenReturn(null);

            Optional<JsonNode> result = service.getCalculation("missing-key");

            assertThat(result).isEmpty();
            verify(valueOperations).get(redisKey);
        }

        @Test
        @DisplayName("getCalculation returns empty on blank cached value")
        void getCalculationBlankValue() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "blank-key";
            when(valueOperations.get(redisKey)).thenReturn("   ");

            Optional<JsonNode> result = service.getCalculation("blank-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCalculation returns empty on corrupt JSON without throwing")
        void getCalculationCorruptJson() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "corrupt-key";
            when(valueOperations.get(redisKey)).thenReturn("not-valid-json{[[");

            Optional<JsonNode> result = service.getCalculation("corrupt-key");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getCalculation returns empty for null or blank key without calling Redis")
        void getCalculationNullOrBlankKey() {
            assertThat(service.getCalculation(null)).isEmpty();
            assertThat(service.getCalculation("")).isEmpty();
            assertThat(service.getCalculation("   ")).isEmpty();
            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("putCalculation stores JSON with TTL")
        void putCalculationWithTtl() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "calc-hash-3";
            JsonNode data = objectMapper.createObjectNode().put("score", 95);
            Duration ttl = Duration.ofHours(2);

            service.putCalculation("calc-hash-3", data, ttl);

            verify(valueOperations).set(eq(redisKey), eq("{\"score\":95}"), eq(ttl));
        }

        @Test
        @DisplayName("putCalculation stores JSON without TTL when TTL is null, zero, or negative")
        void putCalculationWithoutTtl() {
            String redisKey = AstroCacheService.CALC_KEY_PREFIX + "calc-hash-4";
            JsonNode data = objectMapper.createObjectNode().put("score", 100);

            service.putCalculation("calc-hash-4", data, null);
            verify(valueOperations).set(eq(redisKey), eq("{\"score\":100}"));

            service.putCalculation("calc-hash-4", data, Duration.ZERO);
            verify(valueOperations, times(2)).set(eq(redisKey), eq("{\"score\":100}"));

            service.putCalculation("calc-hash-4", data, Duration.ofMinutes(-5));
            verify(valueOperations, times(3)).set(eq(redisKey), eq("{\"score\":100}"));
        }

        @Test
        @DisplayName("putCalculation ignores null or blank key or null value")
        void putCalculationInvalidArguments() {
            JsonNode data = objectMapper.createObjectNode().put("a", 1);

            service.putCalculation(null, data, Duration.ofMinutes(1));
            service.putCalculation("", data, Duration.ofMinutes(1));
            service.putCalculation("   ", data, Duration.ofMinutes(1));
            service.putCalculation("key", null, Duration.ofMinutes(1));

            verifyNoInteractions(valueOperations);
        }

        @Test
        @DisplayName("saveChatMessage pushes serialized message to Redis list")
        void saveChatMessageSuccess() {
            String chatKey = AstroCacheService.CHAT_KEY_PREFIX + "user-session-1";

            service.saveChatMessage("user-session-1", "user", "Hello astrologer");

            verify(listOperations).rightPush(eq(chatKey), eq("{\"role\":\"user\",\"content\":\"Hello astrologer\"}"));
        }

        @Test
        @DisplayName("saveChatMessage handles already prefixed sessionId and null fields")
        void saveChatMessagePrefixedAndNullFields() {
            String fullKey = AstroCacheService.CHAT_KEY_PREFIX + "user-session-2";

            service.saveChatMessage(fullKey, null, null);

            verify(listOperations).rightPush(eq(fullKey), eq("{\"role\":\"\",\"content\":\"\"}"));
        }

        @Test
        @DisplayName("saveChatMessage ignores null or blank sessionId")
        void saveChatMessageInvalidSessionId() {
            service.saveChatMessage(null, "user", "msg");
            service.saveChatMessage("", "user", "msg");
            service.saveChatMessage("   ", "user", "msg");

            verifyNoInteractions(listOperations);
        }

        @Test
        @DisplayName("getChatHistory returns ordered list of messages")
        void getChatHistorySuccess() {
            String chatKey = AstroCacheService.CHAT_KEY_PREFIX + "session-abc";
            List<String> raw = List.of(
                    "{\"role\":\"user\",\"content\":\"Hi\"}",
                    "{\"role\":\"assistant\",\"content\":\"Greetings!\"}"
            );
            when(listOperations.range(chatKey, 0, -1)).thenReturn(raw);

            List<Map<String, String>> history = service.getChatHistory("session-abc");

            assertThat(history).hasSize(2);
            assertThat(history.get(0)).containsEntry("role", "user").containsEntry("content", "Hi");
            assertThat(history.get(1)).containsEntry("role", "assistant").containsEntry("content", "Greetings!");
            verify(listOperations).range(chatKey, 0, -1);
        }

        @Test
        @DisplayName("getChatHistory returns empty list when Redis returns null or empty")
        void getChatHistoryEmpty() {
            String chatKey = AstroCacheService.CHAT_KEY_PREFIX + "empty-session";
            when(listOperations.range(chatKey, 0, -1)).thenReturn(null);
            assertThat(service.getChatHistory("empty-session")).isEmpty();

            when(listOperations.range(chatKey, 0, -1)).thenReturn(Collections.emptyList());
            assertThat(service.getChatHistory("empty-session")).isEmpty();
        }

        @Test
        @DisplayName("getChatHistory returns empty list for null or blank sessionId")
        void getChatHistoryInvalidSessionId() {
            assertThat(service.getChatHistory(null)).isEmpty();
            assertThat(service.getChatHistory("")).isEmpty();
            assertThat(service.getChatHistory("   ")).isEmpty();
            verifyNoInteractions(listOperations);
        }

        @Test
        @DisplayName("getChatHistory skips corrupted message JSON and keeps valid ones")
        void getChatHistoryCorruptElements() {
            String chatKey = AstroCacheService.CHAT_KEY_PREFIX + "corrupt-session";
            List<String> raw = List.of(
                    "{\"role\":\"user\",\"content\":\"valid message\"}",
                    "INVALID_JSON",
                    "{\"role\":\"assistant\",\"content\":\"another valid message\"}"
            );
            when(listOperations.range(chatKey, 0, -1)).thenReturn(raw);

            List<Map<String, String>> history = service.getChatHistory("corrupt-session");

            assertThat(history).hasSize(2);
            assertThat(history.get(0)).containsEntry("content", "valid message");
            assertThat(history.get(1)).containsEntry("content", "another valid message");
        }

        @Test
        @DisplayName("getChatHistory returns unmodifiable list")
        void getChatHistoryUnmodifiable() {
            String chatKey = AstroCacheService.CHAT_KEY_PREFIX + "unmod-session";
            when(listOperations.range(chatKey, 0, -1)).thenReturn(List.of("{\"role\":\"user\",\"content\":\"hi\"}"));

            List<Map<String, String>> history = service.getChatHistory("unmod-session");
            assertThatThrownBy(() -> history.add(Map.of()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Redis Connection Error Resilience")
    class ConnectionErrorResilienceTests {

        private AstroCacheService service;

        @BeforeEach
        void init() {
            service = new AstroCacheService(redisTemplate, objectMapper, true);
        }

        @Test
        @DisplayName("getCalculation catches Redis connection error and returns empty")
        void getCalculationConnectionError() {
            when(valueOperations.get(anyString()))
                    .thenThrow(new RedisConnectionFailureException("Connection refused"));

            Optional<JsonNode> result = service.getCalculation("calc-err");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("putCalculation catches Redis connection error and no-ops")
        void putCalculationConnectionError() {
            doThrow(new RedisConnectionFailureException("Connection timed out"))
                    .when(valueOperations).set(anyString(), anyString(), any(Duration.class));

            JsonNode node = objectMapper.createObjectNode().put("test", true);
            service.putCalculation("calc-err", node, Duration.ofMinutes(1));
            // No exception thrown
        }

        @Test
        @DisplayName("saveChatMessage catches Redis connection error and no-ops")
        void saveChatMessageConnectionError() {
            doThrow(new RedisConnectionFailureException("Cluster down"))
                    .when(listOperations).rightPush(anyString(), anyString());

            service.saveChatMessage("sess-err", "user", "hello");
            // No exception thrown
        }

        @Test
        @DisplayName("getChatHistory catches Redis connection error and returns empty list")
        void getChatHistoryConnectionError() {
            when(listOperations.range(anyString(), eq(0L), eq(-1L)))
                    .thenThrow(new RedisConnectionFailureException("Cannot connect to Redis"));

            List<Map<String, String>> history = service.getChatHistory("sess-err");

            assertThat(history).isEmpty();
        }
    }

    @Nested
    @DisplayName("computeCalculationKey Tests")
    class ComputeCalculationKeyTests {

        private AstroCacheService service;

        @BeforeEach
        void init() {
            service = new AstroCacheService(redisTemplate, objectMapper, true);
        }

        @Test
        @DisplayName("Computes deterministic 64-char hex SHA-256 hash")
        void deterministicHash() {
            AdvancedRequest request = createSampleRequest();
            String hash1 = service.computeCalculationKey(request);
            String hash2 = service.computeCalculationKey(request);

            assertThat(hash1).isNotNull().matches("^[a-f0-9]{64}$");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Produces identical hash regardless of Map key insertion order")
        void identicalHashRegardlessOfMapKeyOrder() {
            // Request 1: birth map insertion order A
            Map<String, Object> birth1 = new LinkedHashMap<>();
            birth1.put("year", 1985);
            birth1.put("month", 11);
            birth1.put("day", 20);
            birth1.put("latitude", 12.9716);
            birth1.put("longitude", 77.5946);

            // Request 2: birth map insertion order B (reversed)
            Map<String, Object> birth2 = new LinkedHashMap<>();
            birth2.put("longitude", 77.5946);
            birth2.put("latitude", 12.9716);
            birth2.put("day", 20);
            birth2.put("month", 11);
            birth2.put("year", 1985);

            AdvancedRequest req1 = new AdvancedRequest(
                    birth1, "2026-09-18T00:00:00Z", List.of("natal"),
                    Map.of("ayanamsa", "lahiri", "nodes", "true"),
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            // options map with reversed keys
            Map<String, Object> optionsReversed = new LinkedHashMap<>();
            optionsReversed.put("nodes", "true");
            optionsReversed.put("ayanamsa", "lahiri");

            AdvancedRequest req2 = new AdvancedRequest(
                    birth2, "2026-09-18T00:00:00Z", List.of("natal"),
                    optionsReversed,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
            );

            String hash1 = service.computeCalculationKey(req1);
            String hash2 = service.computeCalculationKey(req2);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Produces different hash when request parameters differ")
        void differentHashForDifferentParameters() {
            AdvancedRequest base = createSampleRequest();

            // Different asOf
            AdvancedRequest diffAsOf = new AdvancedRequest(
                    base.birth(), "2026-10-01T00:00:00Z", base.methods(), base.options(),
                    base.partner(), base.question(), base.topic(), base.name(), base.gender(),
                    base.seed(), base.location(), base.spread(), base.purpose(), base.lot(),
                    base.horizonDays(), base.count(), base.yearsAhead(), base.returnYear(),
                    base.dashaLevels(), base.divisionalChartFactor(), base.durationHours()
            );

            // Different methods
            AdvancedRequest diffMethods = new AdvancedRequest(
                    base.birth(), base.asOf(), List.of("western"), base.options(),
                    base.partner(), base.question(), base.topic(), base.name(), base.gender(),
                    base.seed(), base.location(), base.spread(), base.purpose(), base.lot(),
                    base.horizonDays(), base.count(), base.yearsAhead(), base.returnYear(),
                    base.dashaLevels(), base.divisionalChartFactor(), base.durationHours()
            );

            // Different topic
            AdvancedRequest diffTopic = new AdvancedRequest(
                    base.birth(), base.asOf(), base.methods(), base.options(),
                    base.partner(), base.question(), "health", base.name(), base.gender(),
                    base.seed(), base.location(), base.spread(), base.purpose(), base.lot(),
                    base.horizonDays(), base.count(), base.yearsAhead(), base.returnYear(),
                    base.dashaLevels(), base.divisionalChartFactor(), base.durationHours()
            );

            String baseHash = service.computeCalculationKey(base);
            String asOfHash = service.computeCalculationKey(diffAsOf);
            String methodsHash = service.computeCalculationKey(diffMethods);
            String topicHash = service.computeCalculationKey(diffTopic);

            assertThat(baseHash).isNotEqualTo(asOfHash);
            assertThat(baseHash).isNotEqualTo(methodsHash);
            assertThat(baseHash).isNotEqualTo(topicHash);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when request is null")
        void throwsOnNullRequest() {
            assertThatThrownBy(() -> service.computeCalculationKey(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("request must not be null");
        }
    }
}
