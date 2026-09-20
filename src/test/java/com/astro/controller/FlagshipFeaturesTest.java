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
class FlagshipFeaturesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testVarshaphalaEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetYear\":2026}";
        mvc.perform(post("/api/astro/varshaphala")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Tajika Nilakanthi Varshaphala (Solar Return) Engine"))
            .andExpect(jsonPath("$.targetYear").value(2026))
            .andExpect(jsonPath("$.ageInYear").isNumber())
            .andExpect(jsonPath("$.varshaLagna.sign").isString())
            .andExpect(jsonPath("$.muntha.sign").isString())
            .andExpect(jsonPath("$.muntha.annualHouse").isNumber())
            .andExpect(jsonPath("$.panchaadhikaris.varsheshwaraLordOfTheYear").isString())
            .andExpect(jsonPath("$.muddaDashaSchedule").isArray())
            .andExpect(jsonPath("$.annualSynthesis.annualVitalityIndex").isString());
    }

    @Test
    void testSynastryCompositeEndpoint() throws Exception {
        String req = "{"
            + "\"partner1\":{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"},"
            + "\"partner2\":{\"dob\":\"1992-08-22\",\"time\":\"09:15:00\",\"city\":\"Mumbai\"},"
            + "\"relationshipType\":\"ROMANTIC\","
            + "\"orbTolerance\":6.0"
            + "}";

        mvc.perform(post("/api/astro/synastry-composite")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Western Synastry & Midpoint Composite Relationship Engine"))
            .andExpect(jsonPath("$.overallSynergyScore").isString())
            .andExpect(jsonPath("$.synergyStatus").isString())
            .andExpect(jsonPath("$.multiDimensionalScores.emotionalResonance").isString())
            .andExpect(jsonPath("$.multiDimensionalScores.romanticAndPhysicalMagnetism").isString())
            .andExpect(jsonPath("$.prominentSynastryAspects").isArray())
            .andExpect(jsonPath("$.midpointCompositeChart.compositeAscendant.sign").isString())
            .andExpect(jsonPath("$.davisonTimeSpaceChart.engine").isString());
    }

    @Test
    void testBirthTimeRectificationEndpoint() throws Exception {
        String req = "{"
            + "\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\","
            + "\"uncertaintyMinutes\":15,\"stepMinutes\":2,\"gender\":\"MALE\","
            + "\"lifeEvents\":["
            + "  {\"eventType\":\"MARRIAGE\",\"eventDate\":\"2019-11-20\",\"description\":\"Wedding Day\"},"
            + "  {\"eventType\":\"CAREER_BREAKTHROUGH\",\"eventDate\":\"2022-04-10\",\"description\":\"Promoted to Principal Engineer\"}"
            + "]"
            + "}";

        mvc.perform(post("/api/astro/birth-time-rectification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Birth Time Rectification (BTR) Assistant Engine"))
            .andExpect(jsonPath("$.originalTime").value("14:30:00"))
            .andExpect(jsonPath("$.optimalRectifiedTime").isString())
            .andExpect(jsonPath("$.confidenceScore").isString())
            .andExpect(jsonPath("$.rectifiedVargaLagnas.d1RashiLagna.sign").isString())
            .andExpect(jsonPath("$.rectifiedVargaLagnas.d9NavamshaLagna.sign").isString())
            .andExpect(jsonPath("$.tattvaShodhanaVerification.activeBreathTattva").isString())
            .andExpect(jsonPath("$.topCandidateRankings").isArray());
    }
}
