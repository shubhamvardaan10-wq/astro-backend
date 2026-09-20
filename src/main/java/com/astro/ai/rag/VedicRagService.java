package com.astro.ai.rag;

import com.astro.ai.model.RagResult;
import com.astro.ai.model.VectorDocument;
import com.astro.ai.vector.EmbeddingEngine;
import com.astro.ai.vector.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class VedicRagService {

    private final VectorStore vectorStore;
    private final EmbeddingEngine embeddingEngine;

    public VedicRagService(VectorStore vectorStore, EmbeddingEngine embeddingEngine) {
        this.vectorStore = vectorStore;
        this.embeddingEngine = embeddingEngine;
    }

    public RagResult search(String query, String tradition, String category, int topK) {
        long start = System.currentTimeMillis();
        String expandedQuery = expandQuery(query);

        // 1. Vector Dense Retrieval
        float[] queryEmbedding = embeddingEngine.embed(expandedQuery);
        List<VectorDocument> vectorMatches = vectorStore.similaritySearch(queryEmbedding, topK * 2, tradition, category);

        // 2. Keyword / Lexical Scoring (BM25 approximation)
        List<VectorDocument> keywordMatches = keywordSearch(expandedQuery, topK * 2, tradition, category);

        // 3. Reciprocal Rank Fusion (RRF) Reranking
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, VectorDocument> docLookup = new HashMap<>();

        for (int rank = 0; rank < vectorMatches.size(); rank++) {
            VectorDocument doc = vectorMatches.get(rank);
            docLookup.put(doc.getId(), doc);
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (60.0 + rank + 1)));
        }

        for (int rank = 0; rank < keywordMatches.size(); rank++) {
            VectorDocument doc = keywordMatches.get(rank);
            docLookup.put(doc.getId(), doc);
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (60.0 + rank + 1)));
        }

        // Sort by RRF score descending
        List<VectorDocument> fusedDocs = rrfScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topK > 0 ? topK : 3)
            .map(e -> {
                VectorDocument doc = docLookup.get(e.getKey());
                VectorDocument scored = new VectorDocument(doc.getId(), doc.getTitle(), doc.getContent(), doc.getTradition(), doc.getCategory(), doc.getSourceCitation());
                scored.setScore(Math.round(e.getValue() * 10000.0) / 10000.0);
                scored.setMetadata(doc.getMetadata());
                return scored;
            })
            .collect(Collectors.toList());

        // 4. Generate Fused Grounded Context & Citations
        StringBuilder contextBuilder = new StringBuilder();
        List<String> citations = new ArrayList<>();

        for (VectorDocument doc : fusedDocs) {
            contextBuilder.append("### Classical Reference: ").append(doc.getTitle()).append("\n")
                .append("**Source Citation**: ").append(doc.getSourceCitation()).append("\n")
                .append("**Tradition**: ").append(doc.getTradition().toUpperCase(Locale.ROOT)).append(" | **Domain**: ").append(doc.getCategory()).append("\n")
                .append("**Verses & Dictum**: ").append(doc.getContent()).append("\n\n");

            citations.add(doc.getSourceCitation());
        }

        long latency = System.currentTimeMillis() - start;
        RagResult result = new RagResult(query, fusedDocs, contextBuilder.toString().trim(), citations, latency);
        result.setTradition(tradition != null ? tradition : "all");
        return result;
    }

    private List<VectorDocument> keywordSearch(String query, int topK, String traditionFilter, String categoryFilter) {
        String[] queryTokens = query.toLowerCase(Locale.ROOT).split("\\s+");
        // We reuse vectorStore similarity search as candidate pool, then re-rank lexically
        List<VectorDocument> candidates = vectorStore.similaritySearch(new float[EmbeddingEngine.DIMENSIONS], 50, traditionFilter, categoryFilter);

        List<Map.Entry<VectorDocument, Integer>> scored = new ArrayList<>();
        for (VectorDocument doc : candidates) {
            String text = (doc.getTitle() + " " + doc.getContent() + " " + doc.getSourceCitation()).toLowerCase(Locale.ROOT);
            int matchCount = 0;
            for (String token : queryTokens) {
                if (token.length() > 2 && text.contains(token)) {
                    matchCount++;
                }
            }
            if (matchCount > 0) {
                scored.add(Map.entry(doc, matchCount));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return scored.stream().limit(topK).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private String expandQuery(String originalQuery) {
        if (originalQuery == null) return "";
        String lower = originalQuery.toLowerCase(Locale.ROOT);
        StringBuilder expanded = new StringBuilder(originalQuery);

        if (lower.contains("career") || lower.contains("job") || lower.contains("profession") || lower.contains("business")) {
            expanded.append(" 10th house karma saturn amatyakaraka vocation leadership");
        }
        if (lower.contains("wealth") || lower.contains("money") || lower.contains("finance") || lower.contains("rich")) {
            expanded.append(" 2nd house 11th house dhana yoga jupiter lakshmi capital cashflow");
        }
        if (lower.contains("marriage") || lower.contains("partner") || lower.contains("spouse") || lower.contains("love")) {
            expanded.append(" 7th house venus kuta guna compatibility navamsa");
        }
        if (lower.contains("remedy") || lower.contains("remedies") || lower.contains("gemstone") || lower.contains("curse")) {
            expanded.append(" lal kitab pitra rin mantra gemstone yantra dana");
        }

        return expanded.toString();
    }
}
