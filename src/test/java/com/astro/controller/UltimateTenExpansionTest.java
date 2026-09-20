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
class UltimateTenExpansionTest {

    @Autowired
    private MockMvc mvc;

    private static final String DEFAULT_NATAL = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";

    @Test
    void testPrashnaHorary() throws Exception {
        String req = "{\"questionType\":\"CAREER\",\"kpSeed\":108,\"queryText\":\"Will my venture succeed?\"}";
        mvc.perform(post("/api/astro/prashna-horary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Classical Vedic & KP Horary Astrological Engine"))
            .andExpect(jsonPath("$.horaryChart.lagnesha").isString())
            .andExpect(jsonPath("$.karyaSiddhiAssessment.probabilityPercentage").isNumber());
    }

    @Test
    void testSarvatobhadraChakra() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + ",\"transitDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/sarvatobhadra-chakra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Sarvatobhadra Chakra (SBC) & 4-Directional Vedha Engine"))
            .andExpect(jsonPath("$.sbcFortitudeRating").isString())
            .andExpect(jsonPath("$.detectedVedhas").isArray());
    }

    @Test
    void testMedicalAyurveda() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + "}";
        mvc.perform(post("/api/astro/medical-ayurveda")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Medical Astrology & Ayur-Jyotish Tridosha Engine"))
            .andExpect(jsonPath("$.tridoshaProfile.prakritiConstitution").isString())
            .andExpect(jsonPath("$.ayurvedicPrescriptions.recommendedHerbs").isArray());
    }

    @Test
    void testTaraBalaCalendar() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + ",\"targetMonth\":\"2026-10\"}";
        mvc.perform(post("/api/astro/tara-bala-calendar")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Navatara Chakra & Daily Tara Bala / Chandra Bala Engine"))
            .andExpect(jsonPath("$.totalDaysEvaluated").value(14))
            .andExpect(jsonPath("$.dailyForecastTimeline").isArray());
    }

    @Test
    void testMuhurtaFinder() throws Exception {
        String req = "{\"activityType\":\"BUSINESS\",\"startDate\":\"2026-10-01\",\"durationDays\":7,\"latitude\":28.6139,\"longitude\":77.2090}";
        mvc.perform(post("/api/astro/electional-muhurta")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Classical Vedic Muhurta & Electional Timing Assistant"))
            .andExpect(jsonPath("$.totalAuspiciousWindowsFound").isNumber())
            .andExpect(jsonPath("$.topRankedWindows").isArray());
    }

    @Test
    void testSolarLunarReturn() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + ",\"returnYear\":2026,\"returnType\":\"SOLAR\",\"currentCity\":\"New Delhi\"}";
        mvc.perform(post("/api/astro/solar-lunar-return")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Western Solar & Lunar Return Precision Engine"))
            .andExpect(jsonPath("$.returnType").value("SOLAR"))
            .andExpect(jsonPath("$.returnAngles.solarAscendant.sign").isString());
    }

    @Test
    void testGemstoneRudraksha() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + "}";
        mvc.perform(post("/api/astro/gemstone-rudraksha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine"))
            .andExpect(jsonPath("$.prescribedGemstones.jeevaRatnaLifeStone.rulingPlanet").isString())
            .andExpect(jsonPath("$.prescribedSacredRudraksha.primaryAuspiciousBead").isString());
    }

    @Test
    void testDraconicChart() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + "}";
        mvc.perform(post("/api/astro/draconic-chart")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Western Draconic Chart (Soul Purpose & Higher Self) Engine"))
            .andExpect(jsonPath("$.draconicPlanetaryPositions.Sun.draconicSign").isString())
            .andExpect(jsonPath("$.karmicConjunctionsToNatal").isArray());
    }

    @Test
    void testKotaChakra() throws Exception {
        String req = "{\"natal\":" + DEFAULT_NATAL + ",\"transitDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/kota-chakra")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Classical Vedic Kota Chakra (Fortress Chart) Engine"))
            .andExpect(jsonPath("$.fortressPillars.kotaSwami").isString())
            .andExpect(jsonPath("$.concentricFortZones").isArray())
            .andExpect(jsonPath("$.fortressDefenseRating").isString());
    }

    @Test
    void testLoShuGrid() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"gender\":\"MALE\"}";
        mvc.perform(post("/api/astro/lo-shu-grid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Classical Astro-Numerology & Lo Shu Magic Square Engine"))
            .andExpect(jsonPath("$.coreVedicNumerology.mulankDriverNumber").value(6))
            .andExpect(jsonPath("$.loShuGridFrequencies.topRow").isMap())
            .andExpect(jsonPath("$.remedialCures").isArray());
    }

    @Test
    void testDailyWidget() throws Exception {
        String req = "{\"latitude\":28.6139,\"longitude\":77.2090,\"date\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/widget/daily-summary")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Unified Daily Cosmic Dashboard Widget Engine"))
            .andExpect(jsonPath("$.panchangCore.tithi").isString())
            .andExpect(jsonPath("$.realtimeSky.currentHoraLord").isString())
            .andExpect(jsonPath("$.dailyAtmosphereSentiment.rating").isString());
    }
}
