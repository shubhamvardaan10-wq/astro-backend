package com.astro.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class BirthRequest {

    @NotBlank(message = "dob is required (YYYY-MM-DD)")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "dob must be YYYY-MM-DD")
    private String dob;

    @NotBlank(message = "time is required (HH:mm or HH:mm:ss)")
    @Pattern(regexp = "\\d{2}:\\d{2}(:\\d{2})?", message = "time must be HH:mm or HH:mm:ss")
    private String time;

    @NotBlank(message = "city is required")
    private String city;

    public String getDob()             { return dob; }
    public void   setDob(String dob)   { this.dob = dob; }
    public String getTime()            { return time; }
    public void   setTime(String time) { this.time = time; }
    public String getCity()            { return city; }
    public void   setCity(String city) { this.city = city; }

    public OffsetDateTime utcDateTime() {
        if (dob == null || !dob.matches("\\d{4}-\\d{2}-\\d{2}") ||
            time == null || !time.matches("\\d{2}:\\d{2}(:\\d{2})?")) {
            throw new IllegalArgumentException("Provide dob as YYYY-MM-DD and time as HH:mm or HH:mm:ss");
        }
        try {
            return LocalDate.parse(dob).atTime(LocalTime.parse(time))
                .atOffset(ZoneOffset.ofHoursMinutes(5, 30)).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("dob must be a valid calendar date and time must be between 00:00:00 and 23:59:59");
        }
    }
}
