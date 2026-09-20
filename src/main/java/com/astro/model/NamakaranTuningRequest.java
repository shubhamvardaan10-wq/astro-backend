package com.astro.model;

public class NamakaranTuningRequest extends BirthRequest {
    private String gender = "MALE";
    private String preferredCategory = "SANSKRIT";

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPreferredCategory() {
        return preferredCategory;
    }

    public void setPreferredCategory(String preferredCategory) {
        this.preferredCategory = preferredCategory;
    }
}
