package com.astro.model;

import jakarta.validation.Valid;

public class DraconicChartRequest {
    @Valid
    private BirthRequest natal;

    public DraconicChartRequest() {}

    public DraconicChartRequest(BirthRequest natal) {
        this.natal = natal;
    }

    public BirthRequest getNatal() {
        return natal;
    }

    public void setNatal(BirthRequest natal) {
        this.natal = natal;
    }
}
