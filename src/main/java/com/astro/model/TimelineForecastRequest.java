package com.astro.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Request payload for month-by-month life event timing and domain scoring.
 */
public class TimelineForecastRequest extends BirthRequest {

    @Min(value = 1, message = "horizonMonths must be at least 1")
    @Max(value = 36, message = "horizonMonths cannot exceed 36")
    private Integer horizonMonths = 12;

    public TimelineForecastRequest() {
        super();
    }

    public Integer getHorizonMonths() {
        return horizonMonths;
    }

    public void setHorizonMonths(Integer horizonMonths) {
        this.horizonMonths = horizonMonths;
    }
}
