package com.astro.ai.provider;

import com.astro.ai.model.LlmRequest;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ModelRouter {

    private static final Map<String, String> TASK_TO_MODEL = Map.of(
        "fast", "gemini-2-0-flash",
        "classification", "gemini-2-0-flash",
        "chat_simple", "gpt-4o-mini",
        "reasoning", "claude-3-5-sonnet",
        "vedic_synthesis", "claude-3-5-sonnet",
        "local", "ollama-local",
        "sensitive", "ollama-local"
    );

    public String routeModel(LlmRequest request) {
        if (request.getModel() != null && !request.getModel().isBlank() && !request.getModel().equals("default")) {
            return request.getModel();
        }
        String taskType = request.getTaskType() != null ? request.getTaskType().toLowerCase() : "fast";
        return TASK_TO_MODEL.getOrDefault(taskType, "gpt-4o-mini");
    }
}
