package com.astro.ai.model;

import java.util.ArrayList;
import java.util.List;

public class AgentExecutionPlan {
    private String sessionId;
    private String userQuery;
    private List<AgentStep> steps = new ArrayList<>();
    private String finalAnswer;
    private int totalTokens;
    private double totalCostUsd;
    private long executionDurationMs;
    private boolean guardrailPassed;
    private String routingModel;

    public AgentExecutionPlan() {}

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public List<AgentStep> getSteps() { return steps; }
    public void setSteps(List<AgentStep> steps) { this.steps = steps; }

    public String getFinalAnswer() { return finalAnswer; }
    public void setFinalAnswer(String finalAnswer) { this.finalAnswer = finalAnswer; }

    public int getTotalTokens() { return totalTokens; }
    public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

    public double getTotalCostUsd() { return totalCostUsd; }
    public void setTotalCostUsd(double totalCostUsd) { this.totalCostUsd = totalCostUsd; }

    public long getExecutionDurationMs() { return executionDurationMs; }
    public void setExecutionDurationMs(long executionDurationMs) { this.executionDurationMs = executionDurationMs; }

    public boolean isGuardrailPassed() { return guardrailPassed; }
    public void setGuardrailPassed(boolean guardrailPassed) { this.guardrailPassed = guardrailPassed; }

    public String getRoutingModel() { return routingModel; }
    public void setRoutingModel(String routingModel) { this.routingModel = routingModel; }
}
