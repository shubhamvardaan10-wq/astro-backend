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
class AllTenNewFeaturesTest {

    @Autowired
    private MockMvc mvc;

    private static final String DEFAULT_BIRTH = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\"}";

    @Test
    void testSadeSatiTimeline() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetYearsAhead\":30}";
        mvc.perform(post("/api/astro/sade-sati-timeline")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Vedic Saturn Sade Sati, Dhaiya & Kantaka Shani Engine"))
            .andExpect(jsonPath("$.activeSadeSatiStatus").isMap())
            .andExpect(jsonPath("$.sadeSatiLifetimeCycles").isArray())
            .andExpect(jsonPath("$.classicalRemedies").isArray());
    }

    @Test
    void testCalendarFeed() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetYear\":2026}";
        mvc.perform(post("/api/astro/calendar-feed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Personal Astrological Calendar (.ics / CalDAV Feed) Generator"))
            .andExpect(jsonPath("$.icsCalendarFeed").isString())
            .andExpect(jsonPath("$.totalEventsGenerated").isNumber());
    }

    @Test
    void testPlanetaryClock() throws Exception {
        String req = "{\"latitude\":28.6139,\"longitude\":77.2090}";
        mvc.perform(post("/api/astro/planetary-clock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Real-Time Planetary Clock & Sky Live Stream Engine"))
            .andExpect(jsonPath("$.liveAscendant.sign").isString())
            .andExpect(jsonPath("$.currentHora.rulingPlanet").isString())
            .andExpect(jsonPath("$.activeChoghadiya.name").isString());
    }

    @Test
    void testProgressionsDirections() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/progressions-directions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Western Secondary Progressions & Solar Arc Directions Engine"))
            .andExpect(jsonPath("$.progressedMoonPhase.phase").isString())
            .andExpect(jsonPath("$.secondaryProgressedPlanets.Sun").isMap())
            .andExpect(jsonPath("$.solarArcDirectedPoints.directedAscendant").isMap());
    }

    @Test
    void testAshtakavargaKaksha() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"transitDate\":\"2026-09-20\"}";
        mvc.perform(post("/api/astro/ashtakavarga-kaksha")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Ashtakavarga Transit Heatmap & Kaksha Precision Engine"))
            .andExpect(jsonPath("$.overallDailyProductivityIndex").isString())
            .andExpect(jsonPath("$.kakshaTransitDetails").isArray());
    }

    @Test
    void testBhriguNandiNadi() throws Exception {
        mvc.perform(post("/api/astro/bhrigu-nandi-nadi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations Engine"))
            .andExpect(jsonPath("$.directionalZodiacGrouping").isMap())
            .andExpect(jsonPath("$.prominentNadiYogas").isArray());
    }

    @Test
    void testAstrocartographyGeoJson() throws Exception {
        mvc.perform(post("/api/astro/astrocartography/geojson")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DEFAULT_BIRTH))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Astrocartography GeoJSON Vector Line Generator"))
            .andExpect(jsonPath("$.geoJson.type").value("FeatureCollection"))
            .andExpect(jsonPath("$.geoJson.features").isArray());
    }

    @Test
    void testMessagingWebhook() throws Exception {
        String req = "{\"platform\":\"WHATSAPP\",\"senderId\":\"user-777\",\"messageText\":\"What is my current Dasha?\"}";
        mvc.perform(post("/api/astro/webhook/messaging")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.gateway").value("Omnichannel Astrological Messaging Webhook"))
            .andExpect(jsonPath("$.platform").value("WHATSAPP"))
            .andExpect(jsonPath("$.botReply").isString());
    }

    @Test
    void testNamakaranTuning() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"gender\":\"MALE\",\"preferredCategory\":\"SANSKRIT\"}";
        mvc.perform(post("/api/astro/namakaran-tuning")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Sacred Vedic Baby Namakaran & Phonetic Tuning Engine"))
            .andExpect(jsonPath("$.moonNakshatra").isString())
            .andExpect(jsonPath("$.sacredStartingSyllables.primaryNamaAkshara").isString())
            .andExpect(jsonPath("$.curatedCosmicNames").isArray());
    }

    @Test
    void testMultilingualReport() throws Exception {
        String req = "{\"dob\":\"1990-12-15\",\"time\":\"14:30:00\",\"city\":\"New Delhi\",\"targetLanguage\":\"hi\"}";
        mvc.perform(post("/api/astro/multilingual-report")
                .contentType(MediaType.APPLICATION_JSON)
                .content(req))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.engine").value("Native Multilingual Astrological Synthesis (i18n)"))
            .andExpect(jsonPath("$.targetLanguage").value("hi"))
            .andExpect(jsonPath("$.reportHeader").isString());
    }
}
