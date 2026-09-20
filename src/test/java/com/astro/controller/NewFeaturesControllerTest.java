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
class NewFeaturesControllerTest {

    private static final String VALID_BIRTH = "{\"dob\":\"1989-10-30\",\"time\":\"10:10\",\"city\":\"Hajipur\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    void testMedicalAstrologyEndpoint() throws Exception {
        mvc.perform(post("/api/astro/medical")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.ayurvedicConstitution").isMap())
            .andExpect(jsonPath("$.ayurvedicConstitution.prakriti").isString())
            .andExpect(jsonPath("$.organVulnerabilities").isArray())
            .andExpect(jsonPath("$.ayurvedicHerbalPrescriptions").isArray())
            .andExpect(jsonPath("$.dietaryRegimen").isMap());
    }

    @Test
    void testCareerIkigaiEndpoint() throws Exception {
        mvc.perform(post("/api/astro/career-ikigai")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.ikigaiCore").isString())
            .andExpect(jsonPath("$.fourPillars").isMap())
            .andExpect(jsonPath("$.primaryArchetypes").isArray())
            .andExpect(jsonPath("$.executiveStrategy").isMap());
    }

    @Test
    void testEventMuhurtaEndpoint() throws Exception {
        String req = "{\"eventType\":\"business\",\"daysAhead\":60,\"maxResults\":5}";
        mvc.perform(post("/api/astro/event-muhurta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.eventType").value("business"))
            .andExpect(jsonPath("$.topRecommendedMuhurtas").isArray());
    }

    @Test
    void testNumerologyTuningEndpoint() throws Exception {
        String req = "{\"fullName\":\"Shubham Vardaan\",\"dob\":\"1989-10-30\"}";
        mvc.perform(post("/api/astro/numerology-tuning")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.coreNumbers.lifePathNumber").isNumber())
            .andExpect(jsonPath("$.coreNumbers.chaldeanDestiny").isNumber())
            .andExpect(jsonPath("$.lifePathProfile.governingPlanet").isString())
            .andExpect(jsonPath("$.nameVibrationTuning.isHarmonious").isBoolean());
    }

    @Test
    void testVastuEndpoint() throws Exception {
        mvc.perform(post("/api/astro/vastu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.ascendant").isString())
            .andExpect(jsonPath("$.vastuGridOverview").isMap())
            .andExpect(jsonPath("$.personalSpaceOptimization.workstationOrientation").isMap())
            .andExpect(jsonPath("$.personalSpaceOptimization.sleepAndRestAlignment").isMap())
            .andExpect(jsonPath("$.personalSpaceOptimization.wealthCornerActivation").isMap());
    }

    @Test
    void testDailyDigestEndpoint() throws Exception {
        mvc.perform(post("/api/astro/daily-digest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.cosmicWeatherScore").isNumber())
            .andExpect(jsonPath("$.dayTheme").isString())
            .andExpect(jsonPath("$.timingWindows.goldenHour").isString())
            .andExpect(jsonPath("$.timingWindows.cautionHourRahuKaal").isString())
            .andExpect(jsonPath("$.dailyEnergyAlignments.powerColor").isString())
            .andExpect(jsonPath("$.briefingCardText").isString())
            .andExpect(jsonPath("$.htmlSummaryCard").isString());
    }
}
