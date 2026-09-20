package com.astro.model;

public class SadeSatiRequest extends BirthRequest {
    private Integer targetYearsAhead = 30;

    public Integer getTargetYearsAhead() {
        return targetYearsAhead;
    }

    public void setTargetYearsAhead(Integer targetYearsAhead) {
        this.targetYearsAhead = targetYearsAhead;
    }
}
