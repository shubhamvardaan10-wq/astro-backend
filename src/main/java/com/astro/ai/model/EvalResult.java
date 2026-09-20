package com.astro.ai.model;

import java.util.List;

public class EvalResult {
    private String benchmarkId;
    private String query;
    private String generatedAnswer;
    private double contextRelevanceScore; // 0.0 to 1.0
    private double faithfulnessScore;     // 0.0 to 1.0
    private double answerRelevanceScore;   // 0.0 to 1.0
    private double compositeScore;        // weighted average
    private boolean passed;
    private List<String> feedbackNotes;
    private long evalLatencyMs;

    public EvalResult() {}

    public EvalResult(String benchmarkId, String query, String generatedAnswer, double contextRelevance, double faithfulness, double answerRelevance, List<String> notes) {
        this.benchmarkId = benchmarkId;
        this.query = query;
        this.generatedAnswer = generatedAnswer;
        this.contextRelevanceScore = contextRelevance;
        this.faithfulnessScore = faithfulness;
        this.answerRelevanceScore = answerRelevance;
        this.compositeScore = Math.round((contextRelevance * 0.3 + faithfulness * 0.4 + answerRelevance * 0.3) * 1000.0) / 1000.0;
        this.passed = this.compositeScore >= 0.75;
        this.feedbackNotes = notes;
    }

    public String getBenchmarkId() { return benchmarkId; }
    public void setBenchmarkId(String benchmarkId) { this.benchmarkId = benchmarkId; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getGeneratedAnswer() { return generatedAnswer; }
    public void setGeneratedAnswer(String generatedAnswer) { this.generatedAnswer = generatedAnswer; }

    public double getContextRelevanceScore() { return contextRelevanceScore; }
    public void setContextRelevanceScore(double contextRelevanceScore) { this.contextRelevanceScore = contextRelevanceScore; }

    public double getFaithfulnessScore() { return faithfulnessScore; }
    public void setFaithfulnessScore(double faithfulnessScore) { this.faithfulnessScore = faithfulnessScore; }

    public double getAnswerRelevanceScore() { return answerRelevanceScore; }
    public void setAnswerRelevanceScore(double answerRelevanceScore) { this.answerRelevanceScore = answerRelevanceScore; }

    public double getCompositeScore() { return compositeScore; }
    public void setCompositeScore(double compositeScore) { this.compositeScore = compositeScore; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }

    public List<String> getFeedbackNotes() { return feedbackNotes; }
    public void setFeedbackNotes(List<String> feedbackNotes) { this.feedbackNotes = feedbackNotes; }

    public long getEvalLatencyMs() { return evalLatencyMs; }
    public void setEvalLatencyMs(long evalLatencyMs) { this.evalLatencyMs = evalLatencyMs; }
}
