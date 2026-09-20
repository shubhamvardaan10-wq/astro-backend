package com.astro.model;

import jakarta.validation.Valid;

public class SolarLunarReturnRequest {
    @Valid
    private BirthRequest natal;
    private Integer returnYear = 2026;
    private String returnType = "SOLAR";
    private String currentCity = "New Delhi";

    public SolarLunarReturnRequest() {}

    public SolarLunarReturnRequest(BirthRequest natal, Integer returnYear, String returnType, String currentCity) {
        this.natal = natal;
        this.returnYear = returnYear;
        this.returnType = returnType;
        this.currentCity = currentCity;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }

    public Integer getReturnYear() {
        return returnYear;
    }

    public void setReturnYear(Integer returnYear) {
        this.returnYear = returnYear;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getCurrentCity() {
        return currentCity;
    }

    public void setCurrentCity(String currentCity) {
        this.currentCity = currentCity;
    }
}
