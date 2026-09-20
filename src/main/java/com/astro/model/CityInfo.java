package com.astro.model;

public class CityInfo {
    private final String name;
    private final double latitude;
    private final double longitude;
    private final String timezone;       // always "Asia/Kolkata" for India
    private final double utcOffsetHours; // +5.5

    public CityInfo(String name, double latitude, double longitude) {
        this.name           = name;
        this.latitude       = latitude;
        this.longitude      = longitude;
        this.timezone       = "Asia/Kolkata";
        this.utcOffsetHours = 5.5;
    }

    public String getName()            { return name; }
    public double getLatitude()        { return latitude; }
    public double getLongitude()       { return longitude; }
    public String getTimezone()        { return timezone; }
    public double getUtcOffsetHours()  { return utcOffsetHours; }
}
