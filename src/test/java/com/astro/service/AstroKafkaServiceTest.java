package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AstroKafkaServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        kafkaTemplate = mock(KafkaTemplate.class);
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
        options.put("reportLength", "detailed");

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
    @DisplayName("Disabled State (astro.kafka.enabled=false)")
    class DisabledStateTests {

        private AstroKafkaService service;

        @BeforeEach
        void init() {
            service = new AstroKafkaService(kafkaTemplate, objectMapper, false);
        }

        @Test
        @DisplayName("isEnabled returns false")
        void isEnabledReturnsFalse() {
            assertThat(service.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("isAvailable returns false")
        void isAvailableReturnsFalse() {
            assertThat(service.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("publishReportRequest gracefully no-ops without calling Kafka")
        void publishReportRequestNoOps() {
            AdvancedRequest request = createSampleRequest();
            service.publishReportRequest("job-123", request);
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("publishProgress gracefully no-ops without calling Kafka")
        void publishProgressNoOps() {
            service.publishProgress("job-123", 1, 12, "Career Overview");
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("publishTelemetry gracefully no-ops without calling Kafka")
        void publishTelemetryNoOps() {
            service.publishTelemetry("report_requested", Map.of("jobId", "job-123"));
            verifyNoInteractions(kafkaTemplate);
        }

        @Test
        @DisplayName("Default constructor initializes with disabled state")
        void defaultConstructorInitializesDisabled() {
            AstroKafkaService defaultService = new AstroKafkaService();
            assertThat(defaultService.isEnabled()).isFalse();
            assertThat(defaultService.isAvailable()).isFalse();
            assertThat(defaultService.getKafkaTemplate()).isNull();

            // Calling publish methods does not throw NPE
            assertThatCode(() -> {
                defaultService.publishReportRequest("job-0", null);
                defaultService.publishProgress("job-0", 0, 1, "test");
                defaultService.publishTelemetry("test", null);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Null KafkaTemplate State (astro.kafka.enabled=true, kafkaTemplate=null)")
    class NullKafkaTemplateTests {

        private AstroKafkaService service;

        @BeforeEach
        void init() {
            service = new AstroKafkaService(null, objectMapper, true);
        }

        @Test
        @DisplayName("isEnabled returns true when enabled flag is true")
        void isEnabledReturnsTrue() {
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isAvailable returns false when template is null")
        void isAvailableReturnsFalse() {
            assertThat(service.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("publishReportRequest gracefully no-ops without NPE")
        void publishReportRequestWithNullTemplate() {
            assertThatCode(() -> service.publishReportRequest("job-456", createSampleRequest()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishProgress gracefully no-ops without NPE")
        void publishProgressWithNullTemplate() {
            assertThatCode(() -> service.publishProgress("job-456", 2, 10, "Finance"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishTelemetry gracefully no-ops without NPE")
        void publishTelemetryWithNullTemplate() {
            assertThatCode(() -> service.publishTelemetry("telemetry_event", Map.of("k", "v")))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Enabled State (astro.kafka.enabled=true)")
    class EnabledStateTests {

        private AstroKafkaService service;

        @BeforeEach
        void init() {
            service = new AstroKafkaService(kafkaTemplate, objectMapper, true);
        }

        @Test
        @DisplayName("isEnabled returns true")
        void isEnabledReturnsTrue() {
            assertThat(service.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("isAvailable returns true when template is present")
        void isAvailableReturnsTrue() {
            assertThat(service.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("publishReportRequest sends JSON payload with jobId key to report requests topic")
        void publishReportRequestSuccess() throws Exception {
            AdvancedRequest request = createSampleRequest();
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            service.publishReportRequest("job-abc", request);

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(AstroKafkaService.DEFAULT_REPORT_REQUESTS_TOPIC);
            assertThat(keyCaptor.getValue()).isEqualTo("job-abc");

            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("jobId").asText()).isEqualTo("job-abc");
            assertThat(payload.get("timestamp")).isNotNull();
            assertThat(payload.get("request")).isNotNull();
            assertThat(payload.get("request").get("topic").asText()).isEqualTo("career");
            assertThat(payload.get("request").get("name").asText()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("publishReportRequest handles null request without error")
        void publishReportRequestHandlesNullRequest() throws Exception {
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            service.publishReportRequest("job-null-req", null);

            verify(kafkaTemplate).send(eq(AstroKafkaService.DEFAULT_REPORT_REQUESTS_TOPIC), eq("job-null-req"), payloadCaptor.capture());
            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("jobId").asText()).isEqualTo("job-null-req");
            assertThat(payload.get("request").isNull()).isTrue();
        }

        @Test
        @DisplayName("publishProgress sends JSON payload with chapter details and percentage")
        void publishProgressSuccess() throws Exception {
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            service.publishProgress("job-progress-1", 3, 12, "Health & Wellbeing");

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(AstroKafkaService.DEFAULT_PROGRESS_TOPIC);
            assertThat(keyCaptor.getValue()).isEqualTo("job-progress-1");

            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("jobId").asText()).isEqualTo("job-progress-1");
            assertThat(payload.get("chapterIndex").asInt()).isEqualTo(3);
            assertThat(payload.get("totalChapters").asInt()).isEqualTo(12);
            assertThat(payload.get("chapterTitle").asText()).isEqualTo("Health & Wellbeing");
            // (3 + 1) / 12 * 100 = 33.333...
            assertThat(payload.get("progressPercent").asDouble()).isGreaterThan(33.0).isLessThan(34.0);
            assertThat(payload.get("timestamp")).isNotNull();
        }

        @Test
        @DisplayName("publishProgress handles zero totalChapters safely")
        void publishProgressZeroTotalChapters() throws Exception {
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            service.publishProgress("job-zero", 0, 0, "No chapters");

            verify(kafkaTemplate).send(eq(AstroKafkaService.DEFAULT_PROGRESS_TOPIC), eq("job-zero"), payloadCaptor.capture());
            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("progressPercent").asDouble()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("publishTelemetry sends eventType key and details map to telemetry topic")
        void publishTelemetrySuccess() throws Exception {
            ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("durationMs", 1450);
            details.put("model", "qwen2.5:7b");
            details.put("status", "SUCCESS");

            service.publishTelemetry("ai_report_completed", details);

            verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), payloadCaptor.capture());
            assertThat(topicCaptor.getValue()).isEqualTo(AstroKafkaService.DEFAULT_TELEMETRY_TOPIC);
            assertThat(keyCaptor.getValue()).isEqualTo("ai_report_completed");

            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("eventType").asText()).isEqualTo("ai_report_completed");
            assertThat(payload.get("timestamp")).isNotNull();
            assertThat(payload.get("details").get("durationMs").asInt()).isEqualTo(1450);
            assertThat(payload.get("details").get("model").asText()).isEqualTo("qwen2.5:7b");
            assertThat(payload.get("details").get("status").asText()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("publishTelemetry handles null details gracefully")
        void publishTelemetryNullDetails() throws Exception {
            ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

            service.publishTelemetry("heartbeat", null);

            verify(kafkaTemplate).send(eq(AstroKafkaService.DEFAULT_TELEMETRY_TOPIC), eq("heartbeat"), payloadCaptor.capture());
            JsonNode payload = objectMapper.readTree(payloadCaptor.getValue());
            assertThat(payload.get("eventType").asText()).isEqualTo("heartbeat");
            assertThat(payload.get("details").isObject()).isTrue();
            assertThat(payload.get("details").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Allows custom topic configuration via setters")
        void customTopicsViaSetters() {
            service.setReportRequestsTopic("custom.report.requests");
            service.setProgressTopic("custom.report.progress");
            service.setTelemetryTopic("custom.telemetry");

            assertThat(service.getReportRequestsTopic()).isEqualTo("custom.report.requests");
            assertThat(service.getProgressTopic()).isEqualTo("custom.report.progress");
            assertThat(service.getTelemetryTopic()).isEqualTo("custom.telemetry");

            service.publishReportRequest("job-custom", createSampleRequest());
            service.publishProgress("job-custom", 1, 5, "Chap 1");
            service.publishTelemetry("custom_event", Map.of());

            verify(kafkaTemplate).send(eq("custom.report.requests"), eq("job-custom"), anyString());
            verify(kafkaTemplate).send(eq("custom.report.progress"), eq("job-custom"), anyString());
            verify(kafkaTemplate).send(eq("custom.telemetry"), eq("custom_event"), anyString());
        }

        @Test
        @DisplayName("Ignores blank topic names in setters")
        void ignoresBlankTopicNames() {
            service.setReportRequestsTopic("  ");
            service.setProgressTopic("");
            service.setTelemetryTopic(null);

            assertThat(service.getReportRequestsTopic()).isEqualTo(AstroKafkaService.DEFAULT_REPORT_REQUESTS_TOPIC);
            assertThat(service.getProgressTopic()).isEqualTo(AstroKafkaService.DEFAULT_PROGRESS_TOPIC);
            assertThat(service.getTelemetryTopic()).isEqualTo(AstroKafkaService.DEFAULT_TELEMETRY_TOPIC);
        }
    }

    @Nested
    @DisplayName("Connection Error Resilience")
    class ConnectionErrorResilienceTests {

        private AstroKafkaService service;

        @BeforeEach
        void init() {
            service = new AstroKafkaService(kafkaTemplate, objectMapper, true);
        }

        @Test
        @DisplayName("publishReportRequest catches KafkaException and does not throw")
        void publishReportRequestCatchesKafkaException() {
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new KafkaException("Kafka broker connection refused"));

            assertThatCode(() -> service.publishReportRequest("job-fail", createSampleRequest()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishProgress catches KafkaException and does not throw")
        void publishProgressCatchesKafkaException() {
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new KafkaException("Kafka broker timeout"));

            assertThatCode(() -> service.publishProgress("job-fail", 1, 10, "Title"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("publishTelemetry catches KafkaException and does not throw")
        void publishTelemetryCatchesKafkaException() {
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new KafkaException("Network unreachable"));

            assertThatCode(() -> service.publishTelemetry("event_fail", Map.of()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Asynchronous future exceptional completion logs and does not throw")
        void asyncFutureFailureHandledGracefully() {
            CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("Async broker disconnect"));

            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(failedFuture);

            assertThatCode(() -> {
                service.publishReportRequest("job-async", createSampleRequest());
                service.publishProgress("job-async", 0, 5, "Chap");
                service.publishTelemetry("event-async", Map.of());
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Successful asynchronous future completes cleanly")
        @SuppressWarnings("unchecked")
        void asyncFutureSuccessHandledGracefully() {
            CompletableFuture<SendResult<String, String>> successFuture =
                    CompletableFuture.completedFuture(mock(SendResult.class));

            when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(successFuture);

            assertThatCode(() -> {
                service.publishReportRequest("job-ok", createSampleRequest());
                service.publishProgress("job-ok", 0, 5, "Chap");
                service.publishTelemetry("event-ok", Map.of());
            }).doesNotThrowAnyException();
        }
    }
}
