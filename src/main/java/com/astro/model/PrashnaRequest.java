package com.astro.model;

import jakarta.validation.constraints.NotBlank;

public class PrashnaRequest {

    @NotBlank(message = "question cannot be blank")
    private String question;

    private String city = "Hajipur";
    private Double latitude = 25.6858;
    private Double longitude = 85.2146;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
