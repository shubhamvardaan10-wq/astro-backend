package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class AstroKafkaService {

    private static final Logger log = LoggerFactory.getLogger(AstroKafkaService.class);

    public static final String DEFAULT_REPORT_REQUESTS_TOPIC = "astro.report.requests";
    public static final String DEFAULT_PROGRESS_TOPIC = "astro.report.progress";
    public static final String DEFAULT_TELEMETRY_TOPIC = "astro.telemetry";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    private String reportRequestsTopic = DEFAULT_REPORT_REQUESTS_TOPIC;
    private String progressTopic = DEFAULT_PROGRESS_TOPIC;
    private String telemetryTopic = DEFAULT_TELEMETRY_TOPIC;

    @Autowired
    public AstroKafkaService(
            @Autowired(required = false) @Nullable KafkaTemplate<String, String> kafkaTemplate,
            @Autowired(required = false) ObjectMapper objectMapper,
            @Value("${astro.kafka.enabled:false}") boolean enabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.enabled = enabled;
    }

    public AstroKafkaService(KafkaTemplate<String, String> kafkaTemplate, boolean enabled) {
        this(kafkaTemplate, new ObjectMapper(), enabled);
    }

    public AstroKafkaService() {
        this(null, new ObjectMapper(), false);
    }

    @Value("${astro.kafka.report-topic:${astro.kafka.topics.report-requests:" + DEFAULT_REPORT_REQUESTS_TOPIC + "}}")
    public void setReportRequestsTopic(String reportRequestsTopic) {
        if (reportRequestsTopic != null && !reportRequestsTopic.isBlank()) {
            this.reportRequestsTopic = reportRequestsTopic;
        }
    }

    @Value("${astro.kafka.progress-topic:${astro.kafka.topics.progress:" + DEFAULT_PROGRESS_TOPIC + "}}")
    public void setProgressTopic(String progressTopic) {
        if (progressTopic != null && !progressTopic.isBlank()) {
            this.progressTopic = progressTopic;
        }
    }

    @Value("${astro.kafka.telemetry-topic:${astro.kafka.topics.telemetry:" + DEFAULT_TELEMETRY_TOPIC + "}}")
    public void setTelemetryTopic(String telemetryTopic) {
        if (telemetryTopic != null && !telemetryTopic.isBlank()) {
            this.telemetryTopic = telemetryTopic;
        }
    }

    public String getReportRequestsTopic() {
        return reportRequestsTopic;
    }

    public String getProgressTopic() {
        return progressTopic;
    }

    public String getTelemetryTopic() {
        return telemetryTopic;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isAvailable() {
        return enabled && kafkaTemplate != null;
    }

    public KafkaTemplate<String, String> getKafkaTemplate() {
        return kafkaTemplate;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Publishes report generation request to Kafka topic.
     * Gracefully no-ops if Kafka is disabled or unavailable.
     */
    public void publishReportRequest(String jobId, AdvancedRequest request) {
        if (!isAvailable()) {
            log.debug("Kafka is disabled or KafkaTemplate is null; skipping publishReportRequest for jobId: {}", jobId);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jobId", jobId);
            payload.put("request", request);
            payload.put("timestamp", Instant.now().toString());

            String json = objectMapper.writeValueAsString(payload);
            send(reportRequestsTopic, jobId, json);
        } catch (Exception e) {
            log.warn("Failed to publish report request to Kafka for jobId {}: {}", jobId, e.getMessage());
        }
    }

    /**
     * Publishes chapter generation progress to Kafka topic.
     * Gracefully no-ops if Kafka is disabled or unavailable.
     */
    public void publishProgress(String jobId, int chapterIndex, int totalChapters, String chapterTitle) {
        if (!isAvailable()) {
            log.debug("Kafka is disabled or KafkaTemplate is null; skipping publishProgress for jobId: {}", jobId);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("jobId", jobId);
            payload.put("chapterIndex", chapterIndex);
            payload.put("totalChapters", totalChapters);
            payload.put("chapterTitle", chapterTitle);
            double progressPercent = totalChapters > 0
                    ? Math.min(100.0, Math.max(0.0, ((double) (chapterIndex + 1) / totalChapters) * 100.0))
                    : 0.0;
            payload.put("progressPercent", progressPercent);
            payload.put("timestamp", Instant.now().toString());

            String json = objectMapper.writeValueAsString(payload);
            send(progressTopic, jobId, json);
        } catch (Exception e) {
            log.warn("Failed to publish progress to Kafka for jobId {}: {}", jobId, e.getMessage());
        }
    }

    /**
     * Publishes system / operational telemetry to Kafka topic.
     * Gracefully no-ops if Kafka is disabled or unavailable.
     */
    public void publishTelemetry(String eventType, Map<String, Object> details) {
        if (!isAvailable()) {
            log.debug("Kafka is disabled or KafkaTemplate is null; skipping publishTelemetry for eventType: {}", eventType);
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventType", eventType);
            payload.put("details", details != null ? details : Collections.emptyMap());
            payload.put("timestamp", Instant.now().toString());

            String json = objectMapper.writeValueAsString(payload);
            send(telemetryTopic, eventType, json);
        } catch (Exception e) {
            log.warn("Failed to publish telemetry to Kafka for eventType {}: {}", eventType, e.getMessage());
        }
    }

    private void send(String topic, String key, String json) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, json);
            if (future != null) {
                future.whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Asynchronous Kafka send failed for topic [{}], key [{}]: {}", topic, key, ex.getMessage());
                    } else {
                        log.debug("Successfully published message to Kafka topic [{}] with key [{}]", topic, key);
                    }
                });
            }
        } catch (Exception e) {
            log.warn("Synchronous Kafka send failed for topic [{}], key [{}]: {}", topic, key, e.getMessage());
        }
    }
}
