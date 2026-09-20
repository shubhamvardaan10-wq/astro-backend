package com.astro.model;

public class CalendarFeedRequest extends BirthRequest {
    private Integer targetYear = 2026;

    public Integer getTargetYear() {
        return targetYear;
    }

    public void setTargetYear(Integer targetYear) {
        this.targetYear = targetYear;
    }
}
