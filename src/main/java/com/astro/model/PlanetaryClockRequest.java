package com.astro.model;

public class PlanetaryClockRequest {
    private Double latitude = 28.6139;
    private Double longitude = 77.2090;
    private String timestamp;

    public PlanetaryClockRequest() {}

    public PlanetaryClockRequest(Double latitude, Double longitude, String timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
