package com.astro.ai.model;

import java.util.List;

public class LlmResponse {
    private String id;
    private String model;
    private String provider;
    private String content;
    private List<ToolCall> toolCalls;
    private String finishReason; // "stop", "tool_calls", "length"
    private UsageStats usage;
    private boolean cached = false;
    private long latencyMs;

    public LlmResponse() {}

    public LlmResponse(String content) {
        this.content = content;
        this.finishReason = "stop";
        this.usage = UsageStats.zero();
    }

    public static LlmResponse text(String content, String model, String provider) {
        LlmResponse res = new LlmResponse();
        res.setContent(content);
        res.setModel(model);
        res.setProvider(provider);
        res.setFinishReason("stop");
        res.setUsage(UsageStats.of(30, 60, 0.0001));
        return res;
    }

    public static LlmResponse withTools(List<ToolCall> toolCalls, String model, String provider) {
        LlmResponse res = new LlmResponse();
        res.setToolCalls(toolCalls);
        res.setModel(model);
        res.setProvider(provider);
        res.setFinishReason("tool_calls");
        res.setUsage(UsageStats.of(40, 20, 0.00008));
        return res;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }

    public String getFinishReason() { return finishReason; }
    public void setFinishReason(String finishReason) { this.finishReason = finishReason; }

    public UsageStats getUsage() { return usage; }
    public void setUsage(UsageStats usage) { this.usage = usage; }

    public boolean isCached() { return cached; }
    public void setCached(boolean cached) { this.cached = cached; }

    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
}
