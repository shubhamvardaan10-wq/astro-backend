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
class FinalFiveExpansionTest {

    @Autowired
    private MockMvc mvc;

    private static final String DEFAULT_NATAL = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";

    @Test
    void testJaiminiCharaDasha() throws Exception {
        mvc.perform(post("/api/astro/jaimini-chara-dasha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_NATAL))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.atmakaraka.planet").isString())
            .andExpect(jsonPath("$.sevenKarakas").isMap())
            .andExpect(jsonPath("$.karakamsha.karakamshaSign").isString())
            .andExpect(jsonPath("$.ishtaDevata.recommendedDeity").isString())
            .andExpect(jsonPath("$.arudhaPadas").isMap())
            .andExpect(jsonPath("$.charaDashaTimeline").isArray());
    }

    @Test
    void testKpSignificators() throws Exception {
        mvc.perform(post("/api/astro/kp-significators")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_NATAL))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.totalKpSubDivisions").value(249))
            .andExpect(jsonPath("$.planetKpTable").isMap())
            .andExpect(jsonPath("$.cuspalSubLords").isMap())
            .andExpect(jsonPath("$.fourStepTheory.marriageSeventhCsl").isMap())
            .andExpect(jsonPath("$.fourStepTheory.careerTenthCsl").isMap())
            .andExpect(jsonPath("$.rulingPlanets").isMap());
    }

    @Test
    void testDasaKootaMatching() throws Exception {
        String req = "{\"partner1\":" + DEFAULT_NATAL + ",\"partner2\":" + DEFAULT_NATAL + "}";
        mvc.perform(post("/api/astro/dasa-koota-matching")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.poruthamScore").isString())
            .andExpect(jsonPath("$.verdict").isString())
            .andExpect(jsonPath("$.poruthams").isArray())
            .andExpect(jsonPath("$.ashtaKootaScoreEstimate").isString());
    }

    @Test
    void testYantraMantra() throws Exception {
        String req = "{\"planet\":\"SHANI\"}";
        mvc.perform(post("/api/astro/remedies-yantra-mantra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.sacredMagicSum").value(33))
            .andExpect(jsonPath("$.mathematicallyVerified").value(true))
            .andExpect(jsonPath("$.beejaMantra").isString())
            .andExpect(jsonPath("$.yantraSvg").isString());
    }

    @Test
    void testAncestralLineage() throws Exception {
        mvc.perform(post("/api/astro/ancestral-lineage")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_NATAL))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.severityScore").isString())
            .andExpect(jsonPath("$.severityTier").isString())
            .andExpect(jsonPath("$.detectedAncestralDoshas").isArray())
            .andExpect(jsonPath("$.ancestralKarmicDebts").isArray())
            .andExpect(jsonPath("$.remedialProtocol").isArray());
    }
}
