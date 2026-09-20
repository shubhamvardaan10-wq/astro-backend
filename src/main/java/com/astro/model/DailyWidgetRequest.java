package com.astro.model;

public class DailyWidgetRequest {
    private Double latitude = 28.6139;
    private Double longitude = 77.2090;
    private String date;

    public DailyWidgetRequest() {}

    public DailyWidgetRequest(Double latitude, Double longitude, String date) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.date = date;
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
