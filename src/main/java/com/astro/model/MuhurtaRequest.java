package com.astro.model;

public class MuhurtaRequest {
    private String eventType = "STARTUP_INCORPORATION";
    private String activityType = "BUSINESS";
    private String city = "New Delhi";
    private String startDate;
    private Integer durationDays = 7;
    private Double latitude = 28.6139;
    private Double longitude = 77.2090;

    public MuhurtaRequest() {}

    public MuhurtaRequest(String activityType, String startDate, Integer durationDays, Double latitude, Double longitude) {
        this.activityType = activityType;
        this.eventType = activityType;
        this.startDate = startDate;
        this.durationDays = durationDays;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
        this.activityType = eventType;
    }

    public String getActivityType() {
        return activityType != null ? activityType : eventType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
        this.eventType = activityType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
