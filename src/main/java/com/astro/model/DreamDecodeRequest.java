package com.astro.model;

import jakarta.validation.constraints.NotBlank;

public class DreamDecodeRequest {

    @NotBlank(message = "dreamText is required")
    private String dreamText;
    private String dreamDate;
    private String prahar = "brahma_muhurta"; // brahma_muhurta, midnight, early_night

    public String getDreamText() {
        return dreamText;
    }

    public void setDreamText(String dreamText) {
        this.dreamText = dreamText;
    }

    public String getDreamDate() {
        return dreamDate;
    }

    public void setDreamDate(String dreamDate) {
        this.dreamDate = dreamDate;
    }

    public String getPrahar() {
        return prahar;
    }

    public void setPrahar(String prahar) {
        if (prahar != null && !prahar.isBlank()) {
            this.prahar = prahar;
        }
    }
}
