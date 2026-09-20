package com.astro.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class KpHoraryRequest {

    @Min(1)
    @Max(249)
    private int horaryNumber = 108;

    private String question = "Will my enterprise secure high-tier strategic expansion?";
    private String category = "CAREER_FINANCE";

    public KpHoraryRequest() {}

    public KpHoraryRequest(int horaryNumber, String question, String category) {
        this.horaryNumber = horaryNumber;
        this.question = question;
        this.category = category;
    }

    public int getHoraryNumber() {
        return horaryNumber;
    }

    public void setHoraryNumber(int horaryNumber) {
        this.horaryNumber = horaryNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
