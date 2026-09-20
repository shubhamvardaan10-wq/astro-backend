package com.astro.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class VarshaphalaRequest extends BirthRequest {

    @Min(value = 1900, message = "targetYear must be at least 1900")
    @Max(value = 2100, message = "targetYear must be at most 2100")
    private Integer targetYear = 2026;

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }
}
