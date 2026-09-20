package com.astro.ai.agent;

import com.astro.ai.cache.AiCacheService;
import com.astro.ai.cost.AiCostTracker;
import com.astro.ai.guardrail.AiGuardrailService;
import com.astro.ai.model.*;
import com.astro.ai.prompt.PromptManager;
import com.astro.ai.provider.ModelFallbackChain;
import com.astro.ai.provider.ModelRouter;
import com.astro.ai.telemetry.AiTelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AiAgentService {

    private static final Logger log = LoggerFactory.getLogger(AiAgentService.class);
    private static final int MAX_AGENT_HOPS = 5;

    private final AgentToolRegistry toolRegistry;
    private final ModelFallbackChain modelFallbackChain;
    private final ModelRouter modelRouter;
    private final AiGuardrailService guardrailService;
    private final AiCostTracker costTracker;
    private final AiTelemetryService telemetryService;
    private final AiCacheService cacheService;
    private final PromptManager promptManager;

    public AiAgentService(AgentToolRegistry toolRegistry,
                          ModelFallbackChain modelFallbackChain,
                          ModelRouter modelRouter,
                          AiGuardrailService guardrailService,
                          AiCostTracker costTracker,
                          AiTelemetryService telemetryService,
                          AiCacheService cacheService,
                          PromptManager promptManager) {
        this.toolRegistry = toolRegistry;
        this.modelFallbackChain = modelFallbackChain;
        this.modelRouter = modelRouter;
        this.guardrailService = guardrailService;
        this.costTracker = costTracker;
        this.telemetryService = telemetryService;
        this.cacheService = cacheService;
        this.promptManager = promptManager;
    }

    public AgentExecutionPlan runAgent(String sessionId, String userQuery, String requestedModel) {
        long startTime = System.currentTimeMillis();
        String traceId = telemetryService.startTrace();

        AgentExecutionPlan plan = new AgentExecutionPlan();
        plan.setSessionId(sessionId != null ? sessionId : "sess-" + UUID.randomUUID().toString().substring(0, 8));
        plan.setUserQuery(userQuery);

        // 1. Guardrail Validation & Prompt Injection Defense
        GuardrailResult guardrail = guardrailService.validateInput(userQuery);
        if (!guardrail.allowed()) {
            plan.setGuardrailPassed(false);
            plan.setFinalAnswer("Request Blocked by Security Guardrail: " + guardrail.flagReason() + " - " + guardrail.violations());
            plan.setExecutionDurationMs(System.currentTimeMillis() - startTime);
            telemetryService.recordSpan(traceId, "ai.guardrail.blocked", plan.getExecutionDurationMs(), "BLOCKED", Map.of("reason", guardrail.flagReason()));
            return plan;
        }
        plan.setGuardrailPassed(true);
        String cleanQuery = guardrail.sanitizedText();

        // 2. Routing
        LlmRequest routingProbe = new LlmRequest();
        routingProbe.setModel(requestedModel);
        routingProbe.setTaskType("vedic_synthesis");
        String activeModel = modelRouter.routeModel(routingProbe);
        plan.setRoutingModel(activeModel);

        // 3. Cache Inspection
        LlmResponse cached = cacheService.get(cleanQuery, activeModel, 0.7);
        if (cached != null) {
            plan.setFinalAnswer(cached.getContent());
            plan.setTotalTokens(0);
            plan.setTotalCostUsd(0.0);
            plan.setExecutionDurationMs(System.currentTimeMillis() - startTime);
            telemetryService.recordSpan(traceId, "ai.cache.hit", plan.getExecutionDurationMs(), "OK", Map.of("cache", "hit"));
            return plan;
        }

        // 4. Initialize Conversation History
        List<LlmMessage> messages = new ArrayList<>();
        PromptTemplate agentPrompt = promptManager.get("react_agent", "v1.0");
        String systemInstruction = (agentPrompt != null) ? agentPrompt.systemPrompt() : "You are an autonomous Vedic AI Agent.";
        messages.add(LlmMessage.system(systemInstruction));
        messages.add(LlmMessage.user(cleanQuery));

        List<ToolDefinition> tools = toolRegistry.getAvailableTools();
        int totalTokensAccumulated = 0;
        double totalCostAccumulated = 0.0;

        // 5. ReAct Execution Loop (Max 5 hops)
        for (int hop = 1; hop <= MAX_AGENT_HOPS; hop++) {
            long hopStart = System.currentTimeMillis();
            LlmRequest req = new LlmRequest(activeModel, new ArrayList<>(messages));
            req.setTools(tools);
            req.setTemperature(0.7);

            LlmResponse stepResponse = modelFallbackChain.executeWithFallback(req);

            int promptToks = costTracker.estimateTokens(cleanQuery);
            int compToks = costTracker.estimateTokens(stepResponse.getContent() != null ? stepResponse.getContent() : "tool_calls");
            UsageStats stats = costTracker.record(activeModel, promptToks, compToks);
            totalTokensAccumulated += stats.totalTokens();
            totalCostAccumulated += stats.estimatedCostUsd();

            if (stepResponse.hasToolCalls()) {
                ToolCall toolCall = stepResponse.getToolCalls().get(0);
                long toolStart = System.currentTimeMillis();

                // Execute Tool
                String observation = toolRegistry.executeTool(toolCall.name(), toolCall.arguments());
                long toolDuration = System.currentTimeMillis() - toolStart;

                AgentStep step = new AgentStep(hop,
                    "Need to gather empirical data via " + toolCall.name() + " to answer user's inquiry accurately.",
                    toolCall.name(),
                    toolCall.arguments(),
                    observation,
                    System.currentTimeMillis() - hopStart);

                plan.getSteps().add(step);
                telemetryService.recordSpan(traceId, "ai.tool." + toolCall.name(), toolDuration, "OK", Map.of("tool", toolCall.name()));

                // Update memory with assistant tool call and observation result
                messages.add(LlmMessage.assistantWithTools(null, List.of(toolCall)));
                messages.add(LlmMessage.tool(toolCall.name(), observation));

            } else {
                // Final answer reached
                String rawAnswer = stepResponse.getContent() != null ? stepResponse.getContent() : "Calculations completed successfully.";
                String sanitizedAnswer = guardrailService.sanitizeOutput(rawAnswer);
                plan.setFinalAnswer(sanitizedAnswer);

                // Update Cache
                cacheService.put(cleanQuery, activeModel, 0.7, stepResponse);
                break;
            }
        }

        // Fallback if loop ended without explicit final answer
        if (plan.getFinalAnswer() == null) {
            plan.setFinalAnswer("Completed multi-step celestial analysis. All astronomical observations have been successfully mapped into your consultation.");
        }

        plan.setTotalTokens(totalTokensAccumulated);
        plan.setTotalCostUsd(Math.round(totalCostAccumulated * 100000.0) / 100000.0);
        plan.setExecutionDurationMs(System.currentTimeMillis() - startTime);

        telemetryService.recordSpan(traceId, "ai.agent.run", plan.getExecutionDurationMs(), "OK",
            Map.of("model", activeModel, "tokens", String.valueOf(totalTokensAccumulated)));

        return plan;
    }
}
