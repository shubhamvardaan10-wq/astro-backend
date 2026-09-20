package com.astro.model;

public class MarketGannRequest {
    private String symbol = "BTC";
    private String targetDate;

    public MarketGannRequest() {}

    public MarketGannRequest(String symbol, String targetDate) {
        this.symbol = symbol;
        this.targetDate = targetDate;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }
}
