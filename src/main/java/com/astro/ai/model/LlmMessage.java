package com.astro.ai.model;

import java.util.List;

public class LlmMessage {
    private LlmRole role;
    private String content;
    private String name;
    private List<ToolCall> toolCalls;

    public LlmMessage() {}

    public LlmMessage(LlmRole role, String content) {
        this.role = role;
        this.content = content;
    }

    public LlmMessage(LlmRole role, String content, String name, List<ToolCall> toolCalls) {
        this.role = role;
        this.content = content;
        this.name = name;
        this.toolCalls = toolCalls;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(LlmRole.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(LlmRole.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(LlmRole.ASSISTANT, content);
    }

    public static LlmMessage assistantWithTools(String content, List<ToolCall> toolCalls) {
        return new LlmMessage(LlmRole.ASSISTANT, content, null, toolCalls);
    }

    public static LlmMessage tool(String toolName, String content) {
        return new LlmMessage(LlmRole.TOOL, content, toolName, null);
    }

    public LlmRole getRole() { return role; }
    public void setRole(LlmRole role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<ToolCall> getToolCalls() { return toolCalls; }
    public void setToolCalls(List<ToolCall> toolCalls) { this.toolCalls = toolCalls; }
}
