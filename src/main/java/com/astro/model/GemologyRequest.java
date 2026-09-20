package com.astro.model;

public class GemologyRequest extends BirthRequest {

    private Double bodyWeightKg = 70.0;

    public Double getBodyWeightKg() {
        return bodyWeightKg;
    }

    public void setBodyWeightKg(Double bodyWeightKg) {
        if (bodyWeightKg != null && bodyWeightKg > 0) {
            this.bodyWeightKg = bodyWeightKg;
        }
    }
}
