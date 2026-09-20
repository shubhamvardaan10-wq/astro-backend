package com.astro.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record AdvancedRequest(
    Map<String, Object> birth,
    @NotBlank String asOf,
    @Size(min = 1, max = 64) List<@NotBlank String> methods,
    Map<String, Object> options,
    Map<String, Object> partner,
    @Size(max = 2000) String question,
    @Size(max = 40) String topic,
    @JsonAlias("fullName") @Size(max = 200) String name,
    @Size(max = 20) String gender,
    @Size(max = 128) String seed,
    Map<String, Object> location,
    @Size(max = 40) String spread,
    @Size(max = 40) String purpose,
    @Size(max = 20) String lot,
    Integer horizonDays,
    Integer count,
    Integer yearsAhead,
    Integer returnYear,
    Integer dashaLevels,
    Integer divisionalChartFactor,
    Double durationHours
) {}
