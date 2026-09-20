package com.astro.model;

public class YantraMantraRequest {

    private String planet = "SURYA";

    public YantraMantraRequest() {}

    public YantraMantraRequest(String planet) {
        this.planet = planet;
    }

    public String getPlanet() {
        return planet;
    }

    public void setPlanet(String planet) {
        this.planet = planet;
    }
}
