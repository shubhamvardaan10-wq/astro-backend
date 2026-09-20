package com.astro.model;

public class LoShuRequest {
    private String dob = "1990-12-15";
    private String gender = "MALE";

    public LoShuRequest() {}

    public LoShuRequest(String dob, String gender) {
        this.dob = dob;
        this.gender = gender;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
