package com.astro.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GeocodingRequest(
    @NotBlank @Size(max = 160) String city,
    @Size(max = 160) String state,
    @NotBlank @Size(max = 160) String country,
    @Pattern(regexp = "[A-Za-z]{2}") String countryCode
) {}
