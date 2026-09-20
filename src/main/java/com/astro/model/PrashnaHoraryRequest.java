package com.astro.model;

public class PrashnaHoraryRequest {
    private String questionType = "CAREER";
    private Integer kpSeed;
    private String queryDatetime;
    private String queryText = "Will my venture succeed?";

    public PrashnaHoraryRequest() {}

    public PrashnaHoraryRequest(String questionType, Integer kpSeed, String queryDatetime, String queryText) {
        this.questionType = questionType;
        this.kpSeed = kpSeed;
        this.queryDatetime = queryDatetime;
        this.queryText = queryText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getKpSeed() {
        return kpSeed;
    }

    public void setKpSeed(Integer kpSeed) {
        this.kpSeed = kpSeed;
    }

    public String getQueryDatetime() {
        return queryDatetime;
    }

    public void setQueryDatetime(String queryDatetime) {
        this.queryDatetime = queryDatetime;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }
}
