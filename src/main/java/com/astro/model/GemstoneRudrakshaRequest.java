package com.astro.model;

import jakarta.validation.Valid;

public class GemstoneRudrakshaRequest {
    @Valid
    private BirthRequest natal;

    public GemstoneRudrakshaRequest() {}

    public GemstoneRudrakshaRequest(BirthRequest natal) {
        this.natal = natal;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }
}
