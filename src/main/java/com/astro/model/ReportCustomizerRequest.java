package com.astro.model;

public class ReportCustomizerRequest {
    private String brandName = "Jyotish Shastra Institute";
    private String theme = "ROYAL_GOLD";
    private String astrologerTitle = "Acharya Shastry";
    private String watermark = "CONFIDENTIAL";

    public ReportCustomizerRequest() {}

    public ReportCustomizerRequest(String brandName, String theme, String astrologerTitle, String watermark) {
        this.brandName = brandName;
        this.theme = theme;
        this.astrologerTitle = astrologerTitle;
        this.watermark = watermark;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getAstrologerTitle() {
        return astrologerTitle;
    }

    public void setAstrologerTitle(String astrologerTitle) {
        this.astrologerTitle = astrologerTitle;
    }

    public String getWatermark() {
        return watermark;
    }

    public void setWatermark(String watermark) {
        this.watermark = watermark;
    }
}
