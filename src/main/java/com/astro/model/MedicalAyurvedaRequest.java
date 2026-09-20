package com.astro.model;

import jakarta.validation.Valid;

public class MedicalAyurvedaRequest {
    @Valid
    private BirthRequest natal;

    public MedicalAyurvedaRequest() {}

    public MedicalAyurvedaRequest(BirthRequest natal) {
        this.natal = natal;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }
}
