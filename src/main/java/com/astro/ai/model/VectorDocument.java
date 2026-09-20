package com.astro.ai.model;

import java.util.Map;

public class VectorDocument {
    private String id;
    private String title;
    private String content;
    private String tradition; // e.g. "parashari", "jaimini", "nadi", "lal_kitab"
    private String category;  // e.g. "dashas", "yogas", "remedies", "transits"
    private String sourceCitation; // e.g. "Brihat Parashara Hora Shastra Ch. 12 Sloka 4"
    private float[] embedding;
    private Map<String, Object> metadata;
    private double score; // similarity or RRF score

    public VectorDocument() {}

    public VectorDocument(String id, String title, String content, String tradition, String category, String sourceCitation) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tradition = tradition;
        this.category = category;
        this.sourceCitation = sourceCitation;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTradition() { return tradition; }
    public void setTradition(String tradition) { this.tradition = tradition; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSourceCitation() { return sourceCitation; }
    public void setSourceCitation(String sourceCitation) { this.sourceCitation = sourceCitation; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) { this.embedding = embedding; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
