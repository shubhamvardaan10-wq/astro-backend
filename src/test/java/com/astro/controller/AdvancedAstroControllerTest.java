package com.astro.controller;

import com.astro.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdvancedAstroControllerTest {

    private AdvancedEngineService engine;
    private AiReportService reports;
    private GeocodingService geocoding;
    private AstroCacheService cacheService;
    private AstroKafkaService kafkaService;
    private AstroSearchService searchService;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        engine = mock(AdvancedEngineService.class);
        reports = mock(AiReportService.class);
        geocoding = mock(GeocodingService.class);
        cacheService = mock(AstroCacheService.class);
        kafkaService = mock(AstroKafkaService.class);
        searchService = mock(AstroSearchService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AdvancedAstroController(
                engine, reports, geocoding, cacheService, kafkaService, searchService)).build();
    }

    @Test
    void listsOnlyImplementedMethods() throws Exception {
        when(engine.capabilities()).thenReturn(new ObjectMapper().readTree("{\"methods\":{\"natal\":\"Swiss Ephemeris natal chart\"}}"));
        mvc.perform(get("/api/astro/v2/methods")).andExpect(status().isOk())
            .andExpect(jsonPath("$.methods.natal").isString());
    }

    @Test
    void validatesBeforeLaunchingAWorker() throws Exception {
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        verifyNoInteractions(engine);
    }

    @Test
    void forwardsMethodParametersAndNameAlias() throws Exception {
        when(cacheService.computeCalculationKey(any())).thenReturn("cache-key-1");
        when(cacheService.getCalculation("cache-key-1")).thenReturn(Optional.empty());
        when(engine.analyze(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\"}"));
        String body = """
            {"birth":{},"asOf":"2026-09-18T00:00:00Z","methods":["scenarios","numerology"],
             "fullName":"Ada Lovelace","horizonDays":90,"spread":"timing","count":2,
             "location":{"city":"Mumbai"},"options":{"directionKey":"ptolemy"}}
            """;
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk());
        verify(engine).analyze(argThat(r -> "Ada Lovelace".equals(r.name()) && r.horizonDays() == 90
            && "timing".equals(r.spread()) && r.count() == 2 && "Mumbai".equals(r.location().get("city"))
            && "ptolemy".equals(r.options().get("directionKey"))));
        verify(cacheService).putCalculation(eq("cache-key-1"), any(), any(Duration.class));
    }

    @Test
    void analyzeReturnsCachedResultWhenPresent() throws Exception {
        when(cacheService.computeCalculationKey(any())).thenReturn("hit-key");
        when(cacheService.getCalculation("hit-key")).thenReturn(Optional.of(new ObjectMapper().readTree("{\"status\":\"complete\",\"cached\":true}")));

        String body = "{\"asOf\":\"2026-09-18T00:00:00Z\",\"methods\":[\"tarot\"],\"spread\":\"timing\"}";
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cached").value(true));

        verifyNoInteractions(engine);
    }

    @Test
    void allowsNonBirthMethodsWithoutBirth() throws Exception {
        when(cacheService.computeCalculationKey(any())).thenReturn("key-nonbirth");
        when(cacheService.getCalculation("key-nonbirth")).thenReturn(Optional.empty());
        when(engine.analyze(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\"}"));
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON)
            .content("{\"asOf\":\"2026-09-18T00:00:00Z\",\"methods\":[\"tarot\"],\"spread\":\"timing\"}"))
            .andExpect(status().isOk());
        verify(engine).analyze(argThat(r -> r.birth() == null && "timing".equals(r.spread())));
    }

    @Test
    void aiEndpointValidatesAndPreservesServiceStatuses() throws Exception {
        mvc.perform(post("/api/astro/v2/ai/report").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(reports);
        when(reports.report(any())).thenReturn(new ObjectMapper().readTree("{\"aiGenerated\":true}"));
        String body = "{\"birth\":{},\"asOf\":\"2026-09-18T00:00:00Z\",\"topic\":\"career\"}";
        mvc.perform(post("/api/astro/v2/ai/report").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.aiGenerated").value(true));
        for (HttpStatus status : new HttpStatus[]{HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT, HttpStatus.BAD_GATEWAY}) {
            doThrow(new ResponseStatusException(status, "Local AI error")).when(reports).report(any());
            mvc.perform(post("/api/astro/v2/ai/report").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(status.value())).andExpect(jsonPath("$.error").value("Local AI error"));
        }
        verifyNoInteractions(engine);
    }

    @Test
    void aiReportAsyncQueuesTaskAndReturns202() throws Exception {
        String body = "{\"birth\":{},\"asOf\":\"2026-09-18T00:00:00Z\",\"topic\":\"career\"}";
        mvc.perform(post("/api/astro/v2/ai/report/async").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.status").value("QUEUED"))
            .andExpect(jsonPath("$.jobId").isString())
            .andExpect(jsonPath("$.message").isString());

        verify(kafkaService).publishReportRequest(anyString(), any());
        verify(kafkaService).publishTelemetry(eq("report_requested_async"), any());
    }

    @Test
    void structuredLocationSearchValidatesAndReturnsCandidates() throws Exception {
        for (String body : new String[]{"{}", "{\"city\":\"Munger\"}",
                "{\"city\":\"Munger\",\"country\":\"India\",\"countryCode\":\"IND\"}"}) {
            mvc.perform(post("/api/astro/v2/locations/search").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(geocoding);
        when(geocoding.search(any())).thenReturn(new ObjectMapper().readTree("{\"candidates\":[],\"selectionRequired\":false}"));
        mvc.perform(post("/api/astro/v2/locations/search").contentType(MediaType.APPLICATION_JSON)
            .content("{\"city\":\"Munger\",\"state\":\"Bihar\",\"country\":\"India\",\"countryCode\":\"IN\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.candidates").isArray());
        verify(geocoding).search(argThat(q -> "Munger".equals(q.city()) && "Bihar".equals(q.state()) && "India".equals(q.country())));
        verifyNoInteractions(engine, reports);
    }

    @Test
    void locationSearchPreservesUnavailableStatus() throws Exception {
        when(geocoding.search(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure self-hosted geocoding"));
        mvc.perform(post("/api/astro/v2/locations/search").contentType(MediaType.APPLICATION_JSON)
            .content("{\"city\":\"Singapore\",\"country\":\"Singapore\"}"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("Configure self-hosted geocoding"));
    }

    @Test
    void preservesUnavailableAndInvalidCalculationStatuses() throws Exception {
        String body = "{\"birth\":{},\"asOf\":\"2026-09-18T00:00:00Z\"}";
        when(cacheService.computeCalculationKey(any())).thenReturn("key-err");
        when(cacheService.getCalculation("key-err")).thenReturn(Optional.empty());
        when(engine.analyze(any())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Worker is not installed"));
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").value("Worker is not installed"));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ambiguous birth time")).when(engine).analyze(any());
        mvc.perform(post("/api/astro/v2/analyze").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Ambiguous birth time"));
    }

    @Test
    void aiChatEndpointInvokesServiceAndSavesSession() throws Exception {
        when(reports.chat(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\",\"answer\":\"Grounded answer.\"}"));
        String body = """
            {"birth":{},"asOf":"2026-09-18T00:00:00Z","question":"How is career?",
             "options":{"sessionId":"sess-123"}}
            """;
        mvc.perform(post("/api/astro/v2/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Grounded answer."));
        verify(reports).chat(argThat(r -> "How is career?".equals(r.question())));
        verify(cacheService).saveChatMessage("sess-123", "user", "How is career?");
        verify(cacheService).saveChatMessage("sess-123", "assistant", "Grounded answer.");
    }

    @Test
    void chatHistoryEndpointReturnsMessages() throws Exception {
        when(cacheService.getChatHistory("sess-123")).thenReturn(List.of(
            Map.of("role", "user", "content", "Hello"),
            Map.of("role", "assistant", "content", "Greetings")
        ));
        mvc.perform(get("/api/astro/v2/ai/chat/history/sess-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("sess-123"))
            .andExpect(jsonPath("$.count").value(2))
            .andExpect(jsonPath("$.history[0].role").value("user"));
    }

    @Test
    void aiCompatibilityEndpointInvokesService() throws Exception {
        when(reports.compatibility(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\",\"topic\":\"compatibility\"}"));
        String body = "{\"birth\":{},\"partner\":{},\"asOf\":\"2026-09-18T00:00:00Z\"}";
        mvc.perform(post("/api/astro/v2/ai/compatibility").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.topic").value("compatibility"));
        verify(reports).compatibility(any());
    }

    @Test
    void aiCrossTraditionEndpointInvokesService() throws Exception {
        when(reports.crossTradition(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\",\"topic\":\"cross-tradition\"}"));
        String body = "{\"birth\":{},\"asOf\":\"2026-09-18T00:00:00Z\"}";
        mvc.perform(post("/api/astro/v2/ai/cross-tradition").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.topic").value("cross-tradition"));
        verify(reports).crossTradition(any());
    }

    @Test
    void aiMuhurtaEndpointInvokesService() throws Exception {
        when(reports.muhurta(any())).thenReturn(new ObjectMapper().readTree("{\"status\":\"complete\",\"topic\":\"timing\"}"));
        String body = "{\"location\":{\"city\":\"Mumbai\"},\"asOf\":\"2026-09-18T00:00:00Z\"}";
        mvc.perform(post("/api/astro/v2/ai/muhurta").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.topic").value("timing"));
        verify(reports).muhurta(any());
    }

    @Test
    void searchRulesEndpointReturnsResults() throws Exception {
        when(searchService.isEnabled()).thenReturn(true);
        when(searchService.searchRules("aries", 5)).thenReturn(List.of(
            Map.of("id", "lagna_aries", "title", "Aries Lagna", "tradition", "Vedic", "content", "Mars rules Aries...")
        ));

        mvc.perform(get("/api/astro/v2/search/rules").param("q", "aries").param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.query").value("aries"))
            .andExpect(jsonPath("$.count").value(1))
            .andExpect(jsonPath("$.elasticsearchEnabled").value(true))
            .andExpect(jsonPath("$.results[0].title").value("Aries Lagna"));
    }

    @Test
    void indexRuleEndpointStoresRule() throws Exception {
        String body = """
            {"id":"custom_1","title":"Yoga Rule","tradition":"Vedic","content":"Auspicious placement","category":"Yoga"}
            """;
        mvc.perform(post("/api/astro/v2/search/rules").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("indexed"))
            .andExpect(jsonPath("$.id").value("custom_1"));

        verify(searchService).indexRule("custom_1", "Yoga Rule", "Vedic", "Auspicious placement", "Yoga");
    }

    @Test
    void indexRuleValidatesMissingFields() throws Exception {
        mvc.perform(post("/api/astro/v2/search/rules").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").isString());
        verifyNoInteractions(searchService);
    }
}
