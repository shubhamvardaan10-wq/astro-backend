package com.astro.prediction;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Full ~4000-word astrological prediction report.
 * Sections are ordered and named for structured reading.
 */
public class PredictionResponse {

    private String birthSummary;
    private Map<String, String> sections = new LinkedHashMap<>();
    private int estimatedWordCount;
    private String disclaimer =
        "This report is generated using classical Vedic astrological algorithms " +
        "(Meeus ephemeris, Brihat Parashara Hora Shastra, Vimshottari Dasha). " +
        "Astrological prediction is a traditional interpretive framework. " +
        "Predictive validity has not been established by controlled scientific study " +
        "(Carlson, Nature 1985). Use as one of many inputs for self-reflection.";

    public String getBirthSummary()                    { return birthSummary; }
    public void   setBirthSummary(String v)            { this.birthSummary = v; }
    public Map<String, String> getSections()           { return sections; }
    public void   setSections(Map<String, String> v)   { this.sections = v; }
    public int    getEstimatedWordCount()               { return estimatedWordCount; }
    public void   setEstimatedWordCount(int v)         { this.estimatedWordCount = v; }
    public String getDisclaimer()                      { return disclaimer; }
    public void   setDisclaimer(String v)              { this.disclaimer = v; }

    public void addSection(String title, String content) {
        sections.put(title, content);
    }

    public void recalcWordCount() {
        int count = sections.values().stream()
            .mapToInt(s -> s.split("\\s+").length).sum();
        count += birthSummary != null ? birthSummary.split("\\s+").length : 0;
        this.estimatedWordCount = count;
    }
}
