package com.astro.ai.model;

import java.util.List;
import java.util.Map;

public record PromptTemplate(
    String id,
    String version,
    String systemPrompt,
    String userTemplate,
    List<String> inputVariables,
    String description
) {
    public String render(Map<String, Object> variables) {
        String rendered = userTemplate;
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String val = entry.getValue() != null ? entry.getValue().toString() : "";
                rendered = rendered.replace("{{" + entry.getKey() + "}}", val);
            }
        }
        return rendered;
    }
}
