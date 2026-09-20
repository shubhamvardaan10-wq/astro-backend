package com.astro.ai.model;

import java.util.ArrayList;
import java.util.List;

public class LlmRequest {
    private String model;
    private List<LlmMessage> messages = new ArrayList<>();
    private List<ToolDefinition> tools = new ArrayList<>();
    private double temperature = 0.7;
    private int maxTokens = 2048;
    private boolean stream = false;
    private String taskType = "general"; // "fast", "reasoning", "local", "vedic_synthesis"

    public LlmRequest() {}

    public LlmRequest(String model, List<LlmMessage> messages) {
        this.model = model;
        this.messages = messages;
    }

    public static LlmRequest of(String model, LlmMessage... messages) {
        LlmRequest req = new LlmRequest();
        req.setModel(model);
        req.setMessages(List.of(messages));
        return req;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<LlmMessage> getMessages() { return messages; }
    public void setMessages(List<LlmMessage> messages) { this.messages = messages; }

    public List<ToolDefinition> getTools() { return tools; }
    public void setTools(List<ToolDefinition> tools) { this.tools = tools; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
}
