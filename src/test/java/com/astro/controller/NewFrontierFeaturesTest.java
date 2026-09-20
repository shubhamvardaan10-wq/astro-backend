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
class NewFrontierFeaturesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testFaceReadingEndpoint() throws Exception {
        mvc.perform(post("/api/astro/face-reading")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.facialArchetype").isString())
            .andExpect(jsonPath("$.compositePhysiognomyScore").isNumber())
            .andExpect(jsonPath("$.facialZoneAnalysis.forehead").isMap())
            .andExpect(jsonPath("$.facialZoneAnalysis.noseAndBridge").isMap())
            .andExpect(jsonPath("$.annotatedFaceImageUrl").isString());
    }

    @Test
    void testVoiceAuraEndpoint() throws Exception {
        mvc.perform(post("/api/astro/voice-aura")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"speakerName\":\"Shubham Vardaan\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.vocalArchetype").isString())
            .andExpect(jsonPath("$.acousticMetrics.fundamentalPitchF0Hz").isNumber())
            .andExpect(jsonPath("$.acousticMetrics.persuasionIndex").isNumber())
            .andExpect(jsonPath("$.chakraAcousticResonance.vishuddha_throat").isMap());
    }

    @Test
    void testDreamDecodeEndpoint() throws Exception {
        String req = "{\"dreamText\":\"I saw a golden serpent swimming in a holy river\",\"dreamDate\":\"2026-09-19\",\"prahar\":\"brahma_muhurta\"}";
        mvc.perform(post("/api/astro/dream-decode")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.auspiciousnessIndex").isNumber())
            .andExpect(jsonPath("$.astrologicalTiming.moonNakshatra").isString())
            .andExpect(jsonPath("$.detectedArchetypes").isArray());
    }

    @Test
    void testSoundTherapyEndpoint() throws Exception {
        String req = "{\"targetPlanet\":\"Jupiter\",\"purpose\":\"wealth_meditation\",\"durationSeconds\":10}";
        mvc.perform(post("/api/astro/sound-therapy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.targetPlanet").value("Jupiter"))
            .andExpect(jsonPath("$.carrierFrequencyHz").value(183.58))
            .andExpect(jsonPath("$.audioDataUrl").isString());
    }

    @Test
    void testTeamSynergyEndpoint() throws Exception {
        String req = "{\"members\":["
                + "{\"name\":\"Shubham\",\"role\":\"CEO\",\"dob\":\"1989-10-30\"},"
                + "{\"name\":\"Alex\",\"role\":\"COO\",\"dob\":\"1992-04-12\"}"
                + "]}";
        mvc.perform(post("/api/astro/team-synergy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.boardroomSynergyScore").isNumber())
            .andExpect(jsonPath("$.teamArchetype").isString())
            .andExpect(jsonPath("$.optimalRoleAllocation").isArray());
    }

    @Test
    void testAuraChakraEndpoint() throws Exception {
        mvc.perform(post("/api/astro/aura-chakra")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectName\":\"Shubham Vardaan\"}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.overallBiofieldVitality").isNumber())
            .andExpect(jsonPath("$.primaryAuraColor").isString())
            .andExpect(jsonPath("$.chakraDiagnostics.sahasrara_crown").isMap())
            .andExpect(jsonPath("$.annotatedAuraImageUrl").isString());
    }
}
