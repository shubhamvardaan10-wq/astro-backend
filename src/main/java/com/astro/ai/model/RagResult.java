package com.astro.ai.model;

import java.util.List;

public class RagResult {
    private String query;
    private List<VectorDocument> documents;
    private String fusedContext;
    private List<String> citations;
    private long searchLatencyMs;
    private String tradition;

    public RagResult() {}

    public RagResult(String query, List<VectorDocument> documents, String fusedContext, List<String> citations, long latencyMs) {
        this.query = query;
        this.documents = documents;
        this.fusedContext = fusedContext;
        this.citations = citations;
        this.searchLatencyMs = latencyMs;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<VectorDocument> getDocuments() { return documents; }
    public void setDocuments(List<VectorDocument> documents) { this.documents = documents; }

    public String getFusedContext() { return fusedContext; }
    public void setFusedContext(String fusedContext) { this.fusedContext = fusedContext; }

    public List<String> getCitations() { return citations; }
    public void setCitations(List<String> citations) { this.citations = citations; }

    public long getSearchLatencyMs() { return searchLatencyMs; }
    public void setSearchLatencyMs(long searchLatencyMs) { this.searchLatencyMs = searchLatencyMs; }

    public String getTradition() { return tradition; }
    public void setTradition(String tradition) { this.tradition = tradition; }
}
