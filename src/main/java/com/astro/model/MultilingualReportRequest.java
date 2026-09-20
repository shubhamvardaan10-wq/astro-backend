package com.astro.model;

public class MultilingualReportRequest extends BirthRequest {
    private String targetLanguage = "hi";

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public void setTargetLanguage(String targetLanguage) {
        this.targetLanguage = targetLanguage;
    }
}
