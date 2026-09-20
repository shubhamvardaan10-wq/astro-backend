package com.astro.model;

public class TransitAlertsRequest extends BirthRequest {

    private String targetDate;

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }
}
