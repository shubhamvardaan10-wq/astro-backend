package com.astro.model;

public class FaceReadingRequest {

    private String faceImageBase64;
    private String gender = "male";
    private BirthRequest birthDetails;

    public String getFaceImageBase64() {
        return faceImageBase64;
    }

    public void setFaceImageBase64(String faceImageBase64) {
        this.faceImageBase64 = faceImageBase64;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        if (gender != null && !gender.isBlank()) {
            this.gender = gender;
        }
    }

    public BirthRequest getBirthDetails() {
        return birthDetails;
    }

    public void setBirthDetails(BirthRequest birthDetails) {
        this.birthDetails = birthDetails;
    }
}
