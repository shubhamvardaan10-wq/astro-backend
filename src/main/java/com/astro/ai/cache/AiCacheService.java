package com.astro.ai.cache;

import com.astro.ai.model.LlmResponse;
import com.astro.ai.vector.EmbeddingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AiCacheService {

    private static final Logger log = LoggerFactory.getLogger(AiCacheService.class);
    private static final double SEMANTIC_SIMILARITY_THRESHOLD = 0.75;

    private final EmbeddingEngine embeddingEngine;

    // Tier 1: Exact Hash Cache (SHA-256 -> LlmResponse)
    private final Map<String, LlmResponse> exactCache = new ConcurrentHashMap<>();

    // Tier 2: Semantic Cache (CacheEntry with embedding vector -> LlmResponse)
    private final Map<String, SemanticCacheEntry> semanticCache = new ConcurrentHashMap<>();

    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);

    public record SemanticCacheEntry(String prompt, float[] embedding, LlmResponse response) {}

    public AiCacheService(EmbeddingEngine embeddingEngine) {
        this.embeddingEngine = embeddingEngine;
    }

    public LlmResponse get(String prompt, String model, double temperature) {
        // 1. Check Tier 1: Exact Cache
        String exactKey = computeHash(model + ":" + temperature + ":" + prompt);
        LlmResponse exactMatch = exactCache.get(exactKey);
        if (exactMatch != null) {
            hits.incrementAndGet();
            log.debug("Exact AI Cache Hit for hash: {}", exactKey);
            return cloneCached(exactMatch);
        }

        // 2. Check Tier 2: Semantic Cache
        float[] promptEmbedding = embeddingEngine.embed(prompt);
        for (SemanticCacheEntry entry : semanticCache.values()) {
            double sim = embeddingEngine.cosineSimilarity(promptEmbedding, entry.embedding());
            log.info("Semantic cache comparison sim: {} against cached prompt: {}", sim, entry.prompt());
            if (sim >= SEMANTIC_SIMILARITY_THRESHOLD) {
                hits.incrementAndGet();
                log.debug("Semantic AI Cache Hit (Similarity: {}) for prompt: {}", sim, prompt);
                return cloneCached(entry.response());
            }
        }

        misses.incrementAndGet();
        return null;
    }

    public void put(String prompt, String model, double temperature, LlmResponse response) {
        if (response == null || response.getContent() == null) return;

        // Save to Exact Cache
        String exactKey = computeHash(model + ":" + temperature + ":" + prompt);
        exactCache.put(exactKey, response);

        // Save to Semantic Cache
        float[] embedding = embeddingEngine.embed(prompt);
        semanticCache.put(exactKey, new SemanticCacheEntry(prompt, embedding, response));
    }

    public long getHits() { return hits.get(); }
    public long getMisses() { return misses.get(); }

    public double getHitRatio() {
        long h = hits.get();
        long total = h + misses.get();
        if (total == 0) return 0.0;
        return Math.round((double) h / total * 1000.0) / 1000.0;
    }

    public void clear() {
        exactCache.clear();
        semanticCache.clear();
        hits.set(0);
        misses.set(0);
    }

    private LlmResponse cloneCached(LlmResponse source) {
        LlmResponse copy = new LlmResponse();
        copy.setId("cached-" + UUID.randomUUID());
        copy.setContent(source.getContent());
        copy.setModel(source.getModel());
        copy.setProvider(source.getProvider());
        copy.setFinishReason(source.getFinishReason());
        copy.setToolCalls(source.getToolCalls());
        copy.setCached(true);
        copy.setLatencyMs(1);
        copy.setUsage(com.astro.ai.model.UsageStats.zero()); // 0 tokens consumed on cache hit!
        return copy;
    }

    private String computeHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 unavailable", e);
        }
    }
}
