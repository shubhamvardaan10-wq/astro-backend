package com.astro.model;

import jakarta.validation.Valid;

public class SarvatobhadraRequest {
    @Valid
    private BirthRequest natal;
    private String transitDate;

    public SarvatobhadraRequest() {}

    public SarvatobhadraRequest(BirthRequest natal, String transitDate) {
        this.natal = natal;
        this.transitDate = transitDate;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }

    public String getTransitDate() {
        return transitDate;
    }

    public void setTransitDate(String transitDate) {
        this.transitDate = transitDate;
    }
}
