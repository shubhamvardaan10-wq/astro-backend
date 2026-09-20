package com.astro.ai.telemetry;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class AiTelemetryService {

    public record TraceSpan(
        String traceId,
        String spanId,
        String parentSpanId,
        String operationName,
        long startTimeMs,
        long durationMs,
        String status, // "OK", "ERROR"
        Map<String, String> attributes
    ) {}

    private final Map<String, List<TraceSpan>> traces = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<Long> recentLatencies = new ConcurrentLinkedDeque<>();
    private static final int MAX_LATENCIES_SAMPLES = 500;
    private static final int MAX_TRACES_RETAINED = 200;

    public String startTrace() {
        return "tr-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void recordSpan(String traceId, String operationName, long durationMs, String status, Map<String, String> attributes) {
        String spanId = "sp-" + UUID.randomUUID().toString().substring(0, 8);
        TraceSpan span = new TraceSpan(traceId, spanId, null, operationName, System.currentTimeMillis() - durationMs, durationMs, status, attributes != null ? attributes : Map.of());

        traces.computeIfAbsent(traceId, k -> new ArrayList<>()).add(span);
        if (traces.size() > MAX_TRACES_RETAINED) {
            Iterator<String> it = traces.keySet().iterator();
            if (it.hasNext()) {
                traces.remove(it.next());
            }
        }

        recentLatencies.add(durationMs);
        while (recentLatencies.size() > MAX_LATENCIES_SAMPLES) {
            recentLatencies.poll();
        }
    }

    public List<TraceSpan> getTrace(String traceId) {
        return traces.getOrDefault(traceId, List.of());
    }

    public double getP95LatencyMs() {
        if (recentLatencies.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(recentLatencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    public double getP50LatencyMs() {
        if (recentLatencies.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(recentLatencies);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.50) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }
}
