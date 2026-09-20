package com.astro.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class DailyHoroscopeRequest extends BirthRequest {

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "targetDate must be YYYY-MM-DD")
    private String targetDate;

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
}
