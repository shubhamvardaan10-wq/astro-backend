package com.astro.ai.model;

import java.util.Map;

public class AgentStep {
    private int stepNumber;
    private String thought;
    private String action;
    private Map<String, Object> actionArguments;
    private String observation;
    private long stepDurationMs;

    public AgentStep() {}

    public AgentStep(int stepNumber, String thought, String action, Map<String, Object> actionArguments, String observation, long durationMs) {
        this.stepNumber = stepNumber;
        this.thought = thought;
        this.action = action;
        this.actionArguments = actionArguments;
        this.observation = observation;
        this.stepDurationMs = durationMs;
    }

    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

    public String getThought() { return thought; }
    public void setThought(String thought) { this.thought = thought; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Map<String, Object> getActionArguments() { return actionArguments; }
    public void setActionArguments(Map<String, Object> actionArguments) { this.actionArguments = actionArguments; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }

    public long getStepDurationMs() { return stepDurationMs; }
    public void setStepDurationMs(long stepDurationMs) { this.stepDurationMs = stepDurationMs; }
}
