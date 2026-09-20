package com.astro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VedicMasteryFeaturesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testKpHoraryEndpoint() throws Exception {
        String req = "{\"horaryNumber\":108,\"question\":\"Will my enterprise secure high-tier strategic expansion?\",\"category\":\"CAREER_FINANCE\"}";
        mvc.perform(post("/api/astro/kp-horary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Krishnamurti Paddhati (KP System) & Prashna Horary Engine"))
            .andExpect(jsonPath("$.horarySeedNumber").value(108))
            .andExpect(jsonPath("$.ascendantCoordinates.subLord").isString())
            .andExpect(jsonPath("$.kpBinaryOutcome").isString())
            .andExpect(jsonPath("$.probabilityPercentage").isNumber());
    }

    @Test
    void testJaiminiKarakamshaEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/jaimini-karakamsha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Maharishi Jaimini Upadesha Sutras: Chara Karaka & Karakamsha Matrix"))
            .andExpect(jsonPath("$.sevenCharaKarakas").isMap())
            .andExpect(jsonPath("$.atmakarakaSoulPlanet").isString())
            .andExpect(jsonPath("$.karakamshaLagna").isString());
    }

    @Test
    void testMuhurtaFinderEndpoint() throws Exception {
        String req = "{\"eventType\":\"STARTUP_INCORPORATION\",\"startDate\":\"2026-09-22\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/muhurta-finder")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Electional Astrology (Shubh Muhurta & Panchanga Filter Engine)"))
            .andExpect(jsonPath("$.selectedEventType").value("STARTUP_INCORPORATION"))
            .andExpect(jsonPath("$.recommendedOptimalWindows").isArray())
            .andExpect(jsonPath("$.highestRankedMuhurta.windowName").isString());
    }

    @Test
    void testPanchPakshiEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/panch-pakshi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Tamil Siddhar Panch-Pakshi Shastra (Five-Bird Chronobiology Engine)"))
            .andExpect(jsonPath("$.nativeRulingBird").isString())
            .andExpect(jsonPath("$.dailyDiurnalYamaSchedule").isArray())
            .andExpect(jsonPath("$.goldenExecutionWindow").isString());
    }

    @Test
    void testKalasarpaOptimizerEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/kalasarpa-optimizer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Kala Sarpa & 12-Nodal Axis Neutralizer Engine"))
            .andExpect(jsonPath("$.detectedKalaSarpaType").isString())
            .andExpect(jsonPath("$.doshaStatus").isString())
            .andExpect(jsonPath("$.neutralizationProtocols").isArray());
    }
}
