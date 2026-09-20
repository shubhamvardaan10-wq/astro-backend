package com.astro.model;

public class PalmistryRequest {

    private String handImageBase64;
    private String handType = "right"; // right, left
    private String gender = "male";    // male, female, other
    private Integer currentAge = 30;
    private BirthRequest birthDetails;

    public String getHandImageBase64() {
        return handImageBase64;
    }

    public void setHandImageBase64(String handImageBase64) {
        this.handImageBase64 = handImageBase64;
    }

    public String getHandType() {
        return handType;
    }

    public void setHandType(String handType) {
        if (handType != null && !handType.isBlank()) {
            this.handType = handType;
        }
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        if (gender != null && !gender.isBlank()) {
            this.gender = gender;
        }
    }

    public Integer getCurrentAge() {
        return currentAge;
    }

    public void setCurrentAge(Integer currentAge) {
        if (currentAge != null && currentAge > 0) {
            this.currentAge = currentAge;
        }
    }

    public BirthRequest getBirthDetails() {
        return birthDetails;
    }

    public void setBirthDetails(BirthRequest birthDetails) {
        this.birthDetails = birthDetails;
    }
}
