package com.astro.model;

import jakarta.validation.Valid;

public class DasaKootaRequest {

    @Valid
    private BirthRequest partner1;

    @Valid
    private BirthRequest partner2;

    private String groomNakshatra = "Rohini";
    private String groomSign = "Taurus";
    private String brideNakshatra = "Anuradha";
    private String brideSign = "Scorpio";

    public DasaKootaRequest() {}

    public DasaKootaRequest(BirthRequest partner1, BirthRequest partner2) {
        this.partner1 = partner1;
        this.partner2 = partner2;
    }

    public BirthRequest getPartner1() { return partner1; }
    public void setPartner1(BirthRequest partner1) { this.partner1 = partner1; }

    public BirthRequest getPartner2() { return partner2; }
    public void setPartner2(BirthRequest partner2) { this.partner2 = partner2; }

    public String getGroomNakshatra() { return groomNakshatra; }
    public void setGroomNakshatra(String groomNakshatra) { this.groomNakshatra = groomNakshatra; }

    public String getGroomSign() { return groomSign; }
    public void setGroomSign(String groomSign) { this.groomSign = groomSign; }

    public String getBrideNakshatra() { return brideNakshatra; }
    public void setBrideNakshatra(String brideNakshatra) { this.brideNakshatra = brideNakshatra; }

    public String getBrideSign() { return brideSign; }
    public void setBrideSign(String brideSign) { this.brideSign = brideSign; }
}
