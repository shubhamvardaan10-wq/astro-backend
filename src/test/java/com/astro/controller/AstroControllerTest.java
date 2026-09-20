package com.astro.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AstroControllerTest {

    private static final String VALID = "{\"dob\":\"1990-01-15\",\"time\":\"14:30\",\"city\":\"Mumbai\"}";

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"vedic-chart", "western-chart", "dasha", "nakshatra", "planets", "yogas", "prediction"})
    void validBirthDataWorksOnEveryEndpoint(String endpoint) throws Exception {
        mvc.perform(post("/api/astro/" + endpoint).contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @ParameterizedTest
    @ValueSource(strings = {"vedic-chart", "western-chart", "dasha", "nakshatra", "planets", "yogas", "prediction"})
    void rejectsInvalidBirthDataOnEveryEndpoint(String endpoint) throws Exception {
        for (String body : new String[]{
            "{}", "null", "{", "{\"dob\":null,\"time\":null,\"city\":null}",
            VALID.replace("1990-01-15", "1990-02-31"),
            VALID.replace("1990-01-15", "1900-02-29"),
            VALID.replace("1990-01-15", "1990-13-01"),
            VALID.replace("14:30", "99:99"),
            VALID.replace("14:30", "24:00"),
            VALID.replace("14:30", "12:60"),
            VALID.replace("14:30", "12:30:60"),
            VALID.replace("Mumbai", " "),
            VALID.replace("Mumbai", "NotACity")
        }) {
            mvc.perform(post("/api/astro/" + endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        }
    }

    @Test
    void acceptsLeapDaysSecondsAndCityAliases() throws Exception {
        mvc.perform(post("/api/astro/vedic-chart").contentType(MediaType.APPLICATION_JSON)
                .content("{\"dob\":\"2000-02-29\",\"time\":\"00:00:01\",\"city\":\" bengaluru \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.input.utcDateTime").value("2000-02-28T18:30:01Z"));
    }

    @Test
    void preservesHttpProtocolErrors() throws Exception {
        mvc.perform(get("/api/astro/vedic-chart")).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/astro/vedic-chart").contentType(MediaType.TEXT_PLAIN).content(VALID))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void doesNotExposeInternalExceptionDetails() {
        var response = new AstroController(null, null, null, null)
            .handleGeneric(new IllegalStateException("private internal detail"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Internal server error");
    }

    @Test
    void citiesAndPredictionHaveExpectedStructure() throws Exception {
        mvc.perform(get("/api/astro/cities"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(75));
        mvc.perform(post("/api/astro/prediction").contentType(MediaType.APPLICATION_JSON).content(VALID))
            .andExpect(status().isOk()).andExpect(jsonPath("$.sections").isMap())
            .andExpect(jsonPath("$.estimatedWordCount").isNumber());
    }
}
