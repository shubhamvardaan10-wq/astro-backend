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
class NextLevelFeaturesTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void testAstrocartographyEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/astrocartography")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Astrocartography & Geodetic Angular Matrix"))
            .andExpect(jsonPath("$.topPowerCities").isArray())
            .andExpect(jsonPath("$.planetaryLinesSummary").isArray())
            .andExpect(jsonPath("$.strategicDirective").isString());
    }

    @Test
    void testFinancialTimingEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/financial-timing")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Financial Astrology & Macro Cycle Timing"))
            .andExpect(jsonPath("$.assetClassRegimes").isArray())
            .andExpect(jsonPath("$.upcomingThirtyDayWindows").isArray())
            .andExpect(jsonPath("$.vedicWealthMantra").isString())
            .andExpect(jsonPath("$.executiveDirective").isString());
    }

    @Test
    void testAyurJyotishEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/ayur-jyotish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Ayur-Jyotish (Classical Medical Astrology & Charaka Samhita)"))
            .andExpect(jsonPath("$.constitutionalPrakriti").isString())
            .andExpect(jsonPath("$.doshaDistribution.vataPercentage").isNumber())
            .andExpect(jsonPath("$.biologicalSystemsVulnerabilityHeatmap").isArray())
            .andExpect(jsonPath("$.circadianDinacharyaSchedule").isMap())
            .andExpect(jsonPath("$.rasayanaAdaptogenPrescriptions").isArray());
    }

    @Test
    void testSarvatobhadraEndpoint() throws Exception {
        String req = "{\"dob\":\"1990-05-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/sarvatobhadra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Sarvatobhadra Chakra 28-Star Omniscient Vedha Engine"))
            .andExpect(jsonPath("$.gridDimensions").value("9x9 Classical Sarvatobhadra Yantra Matrix"))
            .andExpect(jsonPath("$.sixKeyDestinyNakshatras").isMap())
            .andExpect(jsonPath("$.totalActiveVedhas").isNumber())
            .andExpect(jsonPath("$.vedhaDetections").isArray());
    }

    @Test
    void testSoulGraphEndpoint() throws Exception {
        String req = """
            {
              "profiles": [
                {
                  "name": "Arjun",
                  "role": "Self",
                  "dob": "1990-05-15",
                  "time": "14:30:00",
                  "city": "New Delhi"
                },
                {
                  "name": "Priya",
                  "role": "Spouse",
                  "dob": "1992-08-20",
                  "time": "09:15:00",
                  "city": "Mumbai"
                }
              ]
            }
            """;
        mvc.perform(post("/api/astro/soul-graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Astro-Genealogy & Reincarnation Soul-Tie Network"))
            .andExpect(jsonPath("$.totalEntitiesAnalyzed").value(2))
            .andExpect(jsonPath("$.collectiveHarmonyIndex").isNumber())
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.links").isArray())
            .andExpect(jsonPath("$.collectiveKarmicMission").isString());
    }
}
