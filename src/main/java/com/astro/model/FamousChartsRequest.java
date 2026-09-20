package com.astro.model;

public class FamousChartsRequest {
    private String query = "";
    private String category = "ALL";
    private String yoga = "";

    public FamousChartsRequest() {}

    public FamousChartsRequest(String query, String category, String yoga) {
        this.query = query;
        this.category = category;
        this.yoga = yoga;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getYoga() {
        return yoga;
    }

    public void setYoga(String yoga) {
        this.yoga = yoga;
    }
}
