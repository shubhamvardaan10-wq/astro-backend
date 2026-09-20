package com.astro.model;

import jakarta.validation.Valid;

public class TaraBalaRequest {
    @Valid
    private BirthRequest natal;
    private String targetMonth = "2026-10";

    public TaraBalaRequest() {}

    public TaraBalaRequest(BirthRequest natal, String targetMonth) {
        this.natal = natal;
        this.targetMonth = targetMonth;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }

    public String getTargetMonth() {
        return targetMonth;
    }

    public void setTargetMonth(String targetMonth) {
        this.targetMonth = targetMonth;
    }
}
