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
class GodTierFeaturesTest {

    private static final String VALID_BIRTH = "{\"dob\":\"1989-10-30\",\"time\":\"10:10\",\"city\":\"Hajipur\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    void testNadiEndpoint() throws Exception {
        mvc.perform(post("/api/astro/nadi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.nadiSystem").isString())
            .andExpect(jsonPath("$.jivaKarakaSoul").isMap())
            .andExpect(jsonPath("$.karmaKarakaCareer").isMap())
            .andExpect(jsonPath("$.sacredPalmLeafTranscript").isString());
    }

    @Test
    void testLifeVerificationEndpoint() throws Exception {
        mvc.perform(post("/api/astro/life-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.verificationEngine").isString())
            .andExpect(jsonPath("$.confidenceAccuracyScore").isNumber())
            .andExpect(jsonPath("$.pastMilestonesScorecard").isArray())
            .andExpect(jsonPath("$.upcomingImminentGate").isMap());
    }

    @Test
    void testAncestralKarmaEndpoint() throws Exception {
        mvc.perform(post("/api/astro/ancestral-karma")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.lineageAssessment").isString())
            .andExpect(jsonPath("$.pitraDoshaStatus").isString())
            .andExpect(jsonPath("$.fiveAncestralDebts").isArray())
            .andExpect(jsonPath("$.sacredRemedialDirectives.sacredTirthas").isArray());
    }

    @Test
    void testTransitAlertsEndpoint() throws Exception {
        mvc.perform(post("/api/astro/transit-alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.asOfDate").isString())
            .andExpect(jsonPath("$.transitAlarms").isArray())
            .andExpect(jsonPath("$.pushWebhookPayload.event").value("ASTRO_TRANSIT_ALARM"));
    }

    @Test
    void testGemologyEndpoint() throws Exception {
        String req = "{\"dob\":\"1989-10-30\",\"time\":\"10:10\",\"city\":\"Hajipur\",\"bodyWeightKg\":65.0}";
        mvc.perform(post("/api/astro/gemology")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.precisionDosage.recommendedCaratWeight").value(7.0))
            .andExpect(jsonPath("$.sovereignGemstone.gemstoneName").isString())
            .andExpect(jsonPath("$.workstationCrystalYantraGrid.gridName").isString());
    }

    @Test
    void testVoiceAgentEndpoint() throws Exception {
        String req = "{\"dob\":\"1989-10-30\",\"time\":\"10:10\",\"city\":\"Hajipur\",\"userQuery\":\"Should I launch my tech venture?\"}";
        mvc.perform(post("/api/astro/voice-agent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.rishiVoice").isString())
            .andExpect(jsonPath("$.spokenVedicResponse").isString())
            .andExpect(jsonPath("$.synthesizedSpeechAudioUrl").isString());
    }
}
