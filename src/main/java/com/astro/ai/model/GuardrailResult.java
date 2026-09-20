package com.astro.ai.model;

import java.util.List;

public record GuardrailResult(
    boolean allowed,
    List<String> violations,
    String sanitizedText,
    String flagReason,
    boolean ethicalFramingApplied
) {
    public static GuardrailResult pass(String cleanText) {
        return new GuardrailResult(true, List.of(), cleanText, null, false);
    }

    public static GuardrailResult block(String reason, List<String> violations) {
        return new GuardrailResult(false, violations, null, reason, false);
    }

    public static GuardrailResult framed(String framedText, String reason, List<String> notices) {
        return new GuardrailResult(true, notices, framedText, reason, true);
    }
}
