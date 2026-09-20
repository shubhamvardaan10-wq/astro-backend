package com.astro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PalmistryControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testPalmistryPredictionWithDefaultHand() throws Exception {
        mvc.perform(post("/api/astro/palmistry/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.reportMetrics.totalWordCount", greaterThanOrEqualTo(5000)))
            .andExpect(jsonPath("$.reportMetrics.isExceeding5000Words").value(true))
            .andExpect(jsonPath("$.dossierChapters").isMap())
            .andExpect(jsonPath("$.dossierChapters.chirognomyMorphology").isString())
            .andExpect(jsonPath("$.dossierChapters.fiveMajorLines").isString())
            .andExpect(jsonPath("$.dossierChapters.planetaryMounts").isString())
            .andExpect(jsonPath("$.dossierChapters.sacredMarks").isString())
            .andExpect(jsonPath("$.dossierChapters.chronologicalTimeline").isString())
            .andExpect(jsonPath("$.dossierChapters.multiDomainPredictions").isString())
            .andExpect(jsonPath("$.dossierChapters.kundliFusion").isString())
            .andExpect(jsonPath("$.dossierChapters.remedialDirectives").isString())
            .andExpect(jsonPath("$.dossierChapters.executiveSummary").isString())
            .andExpect(jsonPath("$.majorLines.lifeLine").isMap())
            .andExpect(jsonPath("$.majorLines.headLine").isMap())
            .andExpect(jsonPath("$.majorLines.heartLine").isMap())
            .andExpect(jsonPath("$.majorLines.fateLine").isMap())
            .andExpect(jsonPath("$.majorLines.sunLine").isMap())
            .andExpect(jsonPath("$.planetaryMounts.jupiter").isMap())
            .andExpect(jsonPath("$.planetaryMounts.mercury").isMap())
            .andExpect(jsonPath("$.annotatedHandImageUrl").isString());
    }

    @Test
    void testPalmistryPredictionWithBirthDetailsFusion() throws Exception {
        String req = "{"
                + "\"handType\":\"right\","
                + "\"gender\":\"male\","
                + "\"currentAge\":34,"
                + "\"birthDetails\":{\"dob\":\"1989-10-30\",\"time\":\"10:10\",\"city\":\"Hajipur\"}"
                + "}";

        mvc.perform(post("/api/astro/palmistry/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.reportMetrics.totalWordCount", greaterThanOrEqualTo(5000)))
            .andExpect(jsonPath("$.handMetadata.analyzedAge").value(34))
            .andExpect(jsonPath("$.reportMetrics.isExceeding5000Words").value(true));
    }
}
