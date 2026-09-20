package com.astro.ai.controller;

import com.astro.ai.agent.AiAgentService;
import com.astro.ai.cache.AiCacheService;
import com.astro.ai.cost.AiCostTracker;
import com.astro.ai.eval.AiEvaluationService;
import com.astro.ai.guardrail.AiGuardrailService;
import com.astro.ai.model.*;
import com.astro.ai.prompt.PromptManager;
import com.astro.ai.provider.ModelFallbackChain;
import com.astro.ai.provider.ModelRouter;
import com.astro.ai.rag.VedicRagService;
import com.astro.ai.telemetry.AiTelemetryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiAgentService agentService;
    private final VedicRagService ragService;
    private final ModelFallbackChain modelFallbackChain;
    private final ModelRouter modelRouter;
    private final PromptManager promptManager;
    private final AiCostTracker costTracker;
    private final AiCacheService cacheService;
    private final AiTelemetryService telemetryService;
    private final AiGuardrailService guardrailService;
    private final AiEvaluationService evaluationService;

    public AiController(AiAgentService agentService,
                        VedicRagService ragService,
                        ModelFallbackChain modelFallbackChain,
                        ModelRouter modelRouter,
                        PromptManager promptManager,
                        AiCostTracker costTracker,
                        AiCacheService cacheService,
                        AiTelemetryService telemetryService,
                        AiGuardrailService guardrailService,
                        AiEvaluationService evaluationService) {
        this.agentService = agentService;
        this.ragService = ragService;
        this.modelFallbackChain = modelFallbackChain;
        this.modelRouter = modelRouter;
        this.promptManager = promptManager;
        this.costTracker = costTracker;
        this.cacheService = cacheService;
        this.telemetryService = telemetryService;
        this.guardrailService = guardrailService;
        this.evaluationService = evaluationService;
    }

    // ─── 1. Autonomous ReAct AI Agent Consultation ─────────────────────────────
    @PostMapping("/agent/chat")
    public ResponseEntity<AgentExecutionPlan> agentChat(@RequestBody Map<String, Object> req) {
        String sessionId = (String) req.getOrDefault("sessionId", "sess-default");
        String userQuery = (String) req.getOrDefault("userQuery", "What is my astrological career potential and what gemstone should I wear?");
        String model = (String) req.getOrDefault("model", "gpt-4o-mini");

        AgentExecutionPlan plan = agentService.runAgent(sessionId, userQuery, model);
        return ResponseEntity.ok(plan);
    }

    // ─── 2. Hybrid RAG Classical Search (BM25 + Vector + RRF) ──────────────────
    @PostMapping("/rag/search")
    public ResponseEntity<RagResult> ragSearch(@RequestBody Map<String, Object> req) {
        String query = (String) req.getOrDefault("query", "Dharma Karmadhipati Raja Yoga");
        String tradition = (String) req.getOrDefault("tradition", "all");
        String category = (String) req.getOrDefault("category", "all");
        Object topKObj = req.get("topK");
        int topK = (topKObj instanceof Number n) ? n.intValue() : 3;

        RagResult result = ragService.search(query, tradition, category, topK);
        return ResponseEntity.ok(result);
    }

    // ─── 3. Direct Model-Routed LLM Completion with Fallback & Cache ───────────
    @PostMapping("/llm/complete")
    public ResponseEntity<LlmResponse> complete(@RequestBody LlmRequest request) {
        long start = System.currentTimeMillis();
        String traceId = telemetryService.startTrace();

        // 1. Guardrail validation
        String prompt = request.getMessages() != null && !request.getMessages().isEmpty()
            ? request.getMessages().get(request.getMessages().size() - 1).getContent() : "";
        GuardrailResult guard = guardrailService.validateInput(prompt);
        if (!guard.allowed()) {
            LlmResponse blocked = LlmResponse.text("Security Guardrail Blocked: " + guard.flagReason(), request.getModel(), "guardrail");
            return ResponseEntity.badRequest().body(blocked);
        }

        // 2. Model Routing
        String activeModel = modelRouter.routeModel(request);
        request.setModel(activeModel);

        // 3. Cache check
        LlmResponse cached = cacheService.get(guard.sanitizedText(), activeModel, request.getTemperature());
        if (cached != null) {
            telemetryService.recordSpan(traceId, "ai.cache.hit", System.currentTimeMillis() - start, "OK", Map.of("model", activeModel));
            return ResponseEntity.ok(cached);
        }

        // 4. Fallback execution
        LlmResponse response = modelFallbackChain.executeWithFallback(request);
        response.setContent(guardrailService.sanitizeOutput(response.getContent()));

        // 5. Cost and Telemetry Recording
        int promptToks = costTracker.estimateTokens(prompt);
        int compToks = costTracker.estimateTokens(response.getContent());
        UsageStats stats = costTracker.record(activeModel, promptToks, compToks);
        response.setUsage(stats);
        response.setLatencyMs(System.currentTimeMillis() - start);

        // 6. Cache store
        cacheService.put(guard.sanitizedText(), activeModel, request.getTemperature(), response);

        telemetryService.recordSpan(traceId, "ai.llm.complete", response.getLatencyMs(), "OK", Map.of("model", activeModel));
        return ResponseEntity.ok(response);
    }

    // ─── 4. Prompt Registry & Versioning ───────────────────────────────────────
    @GetMapping("/prompts")
    public ResponseEntity<List<PromptTemplate>> listPrompts() {
        return ResponseEntity.ok(promptManager.listAll());
    }

    @GetMapping("/prompts/{id}")
    public ResponseEntity<PromptTemplate> getPrompt(@PathVariable String id,
                                                    @RequestParam(defaultValue = "latest") String version) {
        PromptTemplate prompt = promptManager.get(id, version);
        if (prompt == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(prompt);
    }

    // ─── 5. AI Cost & Telemetry Dashboard ──────────────────────────────────────
    @GetMapping("/telemetry/stats")
    public ResponseEntity<AiCostTelemetry> getTelemetry() {
        AiCostTelemetry telemetry = costTracker.getTelemetry(
            cacheService.getHits(),
            cacheService.getMisses(),
            cacheService.getHitRatio(),
            telemetryService.getP95LatencyMs()
        );
        return ResponseEntity.ok(telemetry);
    }

    @GetMapping("/telemetry/trace/{traceId}")
    public ResponseEntity<List<AiTelemetryService.TraceSpan>> getTrace(@PathVariable String traceId) {
        return ResponseEntity.ok(telemetryService.getTrace(traceId));
    }

    // ─── 6. Evaluation Framework (RAG Triad & Benchmarks) ──────────────────────
    @PostMapping("/eval/evaluate")
    public ResponseEntity<EvalResult> evaluate(@RequestBody Map<String, String> req) {
        String query = req.getOrDefault("query", "");
        String context = req.getOrDefault("context", "");
        String answer = req.getOrDefault("answer", "");
        String benchmarkId = req.getOrDefault("benchmarkId", "eval-" + UUID.randomUUID().toString().substring(0, 6));

        EvalResult result = evaluationService.evaluate(benchmarkId, query, context, answer);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/eval/benchmark")
    public ResponseEntity<Map<String, Object>> runBenchmarkSuite() {
        List<EvalResult> results = evaluationService.runStandardBenchmarkSuite();
        long passedCount = results.stream().filter(EvalResult::isPassed).count();
        double avgScore = results.stream().mapToDouble(EvalResult::getCompositeScore).average().orElse(0.0);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("suiteName", "Classical Vedic RAG Triad Benchmark");
        summary.put("totalBenchmarks", results.size());
        summary.put("passedCount", passedCount);
        summary.put("benchmarkPassRate", Math.round((double) passedCount / results.size() * 100.0) + "%");
        summary.put("averageCompositeScore", Math.round(avgScore * 1000.0) / 1000.0);
        summary.put("benchmarkDetails", results);

        return ResponseEntity.ok(summary);
    }
}
