package com.astro.service;

import com.astro.model.AdvancedRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest
@EnabledIfSystemProperty(named = "astro.ai.live", matches = "true")
class AiReportLiveTest {
    @Autowired private AiReportService service;
    @Autowired private ObjectMapper mapper;
    @SpyBean private LocalOllamaClient client;

    @Test
    void localModelProducesValidatedReportForSyntheticBirth() throws Exception {
        AtomicReference<JsonNode> generated = new AtomicReference<>();
        doAnswer(invocation -> {
            JsonNode response = (JsonNode) invocation.callRealMethod();
            generated.set(response);
            JsonNode context = mapper.readTree((String) invocation.getArgument(1));
            int words = 0;
            for (JsonNode paragraph : response.path("paragraphs")) words += paragraph.asText().trim().split("\\s+").length;
            System.out.println("Synthetic live chapter: " + context.path("section").path("key").asText() + ", words=" + words
                + ", previous validation=" + context.path("revisionReason").asText("none"));
            return response;
        }).when(client).generate(anyString(), anyString(), any(), anyInt(), any(java.time.Duration.class));
        AdvancedRequest request = mapper.readValue("""
            {"birth":{"dob":"1990-01-15","time":"14:30","city":"Mumbai"},
             "asOf":"2026-09-18T00:00:00Z","topic":"general"}
            """, AdvancedRequest.class);
        try {
            String chapterKey = System.getProperty("astro.ai.live.chapter");
            if (chapterKey != null) {
                client.verifyLocalModel();
                java.util.List<?> plan = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "chapterPlan", "general");
                Object chapter = plan.stream().filter(c -> chapterKey.equals(org.springframework.test.util.ReflectionTestUtils.getField(c, "key"))).findFirst().orElseThrow();
                String topic = (String) org.springframework.test.util.ReflectionTestUtils.getField(chapter, "topic");
                String selection = (String) org.springframework.test.util.ReflectionTestUtils.getField(chapter, "selection");
                AdvancedRequest normalized = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "calculationRequest", request);
                var source = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "topicEvidence", normalized, topic);
                var evidence = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "selectEvidence", source, selection);
                JsonNode result = org.springframework.test.util.ReflectionTestUtils.invokeMethod(service, "generateChapter", chapter, plan,
                    evidence, new java.util.HashSet<String>(), System.nanoTime() + java.time.Duration.ofMinutes(5).toNanos());
                assertThat(result.path("wordCount").asInt()).isBetween(250, 400);
                System.out.println("Verified isolated chapter words: " + result.path("wordCount").asInt());
                return;
            }
            JsonNode report = service.report(request);
            assertThat(report.path("status").asText()).isEqualTo("complete");
            assertThat(report.path("metadata").path("provider").asText()).isEqualTo("local-ollama");
            assertThat(report.path("reportLength").asText()).isEqualTo("detailed");
            assertThat(report.at("/report/sections").size()).isEqualTo(12);
            assertThat(report.at("/report/wordCount").asInt()).isBetween(3000, 4800);
            assertThat(report.at("/metadata/analyzedTopics").size()).isGreaterThanOrEqualTo(8);
            System.out.println("Verified detailed AI narrative words: " + report.at("/report/wordCount").asInt());
        } catch (RuntimeException error) {
            throw new AssertionError("Live model output for synthetic fixture failed validation: " + generated.get(), error);
        }
    }
}
