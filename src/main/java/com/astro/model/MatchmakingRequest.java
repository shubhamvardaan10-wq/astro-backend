package com.astro.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class MatchmakingRequest {

    @NotNull(message = "partner1 details are required")
    @Valid
    private BirthRequest partner1;

    @NotNull(message = "partner2 details are required")
    @Valid
    private BirthRequest partner2;

    public BirthRequest getPartner1() { return partner1; }
    public void setPartner1(BirthRequest partner1) { this.partner1 = partner1; }

    public BirthRequest getPartner2() { return partner2; }
    public void setPartner2(BirthRequest partner2) { this.partner2 = partner2; }
}
