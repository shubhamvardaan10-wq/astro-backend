package com.astro.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UltimateExpansionTest {

    @Autowired
    private MockMvc mvc;

    private static final String DEFAULT_BIRTH = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";

    @Test
    void testLalKitab() throws Exception {
        mvc.perform(post("/api/astro/lal-kitab")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Classical Lal Kitab Kundli, Karmic Debts & Upayas Engine"))
            .andExpect(jsonPath("$.fixedHouseChart").isMap())
            .andExpect(jsonPath("$.sleepingPlanets").isArray())
            .andExpect(jsonPath("$.detectedKarmicDebts").isArray())
            .andExpect(jsonPath("$.customizedPracticalUpayas").isArray());
    }

    @Test
    void testPanchangam() throws Exception {
        String req = "{\"latitude\":28.6139,\"longitude\":77.2090,\"date\":\"2026-10-20\"}";
        mvc.perform(post("/api/astro/panchangam")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Full-Fledged Panchangam & Hindu Festival Calendar Engine"))
            .andExpect(jsonPath("$.fiveLimbsOfPanchang.tithi.name").isString())
            .andExpect(jsonPath("$.fiveLimbsOfPanchang.vara.day").isString())
            .andExpect(jsonPath("$.fiveLimbsOfPanchang.nakshatra.name").isString())
            .andExpect(jsonPath("$.fiveLimbsOfPanchang.yoga.name").isString())
            .andExpect(jsonPath("$.fiveLimbsOfPanchang.karana.name").isString());
    }

    @Test
    void testDoshaCancellation() throws Exception {
        String req = "{"
            + "\"partner1\":{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"},"
            + "\"partner2\":{\"dob\":\"1992-08-22\",\"time\":\"09:15:00\",\"city\":\"Mumbai\"}"
            + "}";
        mvc.perform(post("/api/astro/dosha-cancellation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Deep Manglik & Nadi Dosha Cancellation Matrix"))
            .andExpect(jsonPath("$.initialManglikPlacement").isMap())
            .andExpect(jsonPath("$.finalManglikVerdict.hasEffectiveManglikDosha").isBoolean())
            .andExpect(jsonPath("$.nadiDoshaMitigations.nadiStatus").isString());
    }

    @Test
    void testMarketGann() throws Exception {
        String req = "{\"symbol\":\"BTC\",\"targetDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/market-gann")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Astro-Financial & W.D. Gann Square of 9 Timing Engine"))
            .andExpect(jsonPath("$.assetAnalyzed.symbol").value("BTC"))
            .andExpect(jsonPath("$.gannGeometricHarmonics").isArray())
            .andExpect(jsonPath("$.trendBias").isString());
    }

    @Test
    void testFamousHoroscopes() throws Exception {
        String req = "{\"query\":\"Albert Einstein\",\"category\":\"SCIENCE\"}";
        mvc.perform(post("/api/astro/famous-horoscopes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Famous & Historic Horoscopes Database & Search Engine"))
            .andExpect(jsonPath("$.matchesFound").isNumber())
            .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testReportCustomizer() throws Exception {
        String req = "{\"brandName\":\"AstroTech Enterprise\",\"theme\":\"ROYAL_GOLD\",\"astrologerTitle\":\"Chief Jyotish Guru\",\"watermark\":\"OFFICIAL\"}";
        mvc.perform(post("/api/astro/report-customizer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("White-Label Enterprise Report Designer & Customizer"))
            .andExpect(jsonPath("$.brandingProfile.organizationName").value("AstroTech Enterprise"))
            .andExpect(jsonPath("$.stylingPalette.primary").isString())
            .andExpect(jsonPath("$.stylingPalette.fontFamily").isString());
    }

    @Test
    void testKundliVaultCRUD() throws Exception {
        // 1. List existing
        mvc.perform(get("/api/astro/vault/list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());

        // 2. Save profile
        String saveReq = "{\"id\":\"test-user-1\",\"name\":\"John Doe\",\"category\":\"Client\",\"dob\":\"1995-05-10\",\"time\":\"12:00:00\",\"city\":\"Chennai\"}";
        mvc.perform(post("/api/astro/vault/save")
                .contentType(MediaType.APPLICATION_JSON)
                .content(saveReq))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("test-user-1"))
            .andExpect(jsonPath("$.name").value("John Doe"));

        // 3. Get profile
        mvc.perform(get("/api/astro/vault/test-user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("test-user-1"))
            .andExpect(jsonPath("$.city").value("Chennai"));

        // 4. Delete profile
        mvc.perform(delete("/api/astro/vault/test-user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deleted").value(true));

        // 5. Verify deleted profile returns 404
        mvc.perform(get("/api/astro/vault/test-user-1"))
            .andExpect(status().isNotFound());
    }
}
