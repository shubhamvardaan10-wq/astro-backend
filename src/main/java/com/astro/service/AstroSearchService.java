package com.astro.service;

import com.astro.prediction.DashaTexts;
import com.astro.prediction.LagnaTexts;
import com.astro.prediction.NakshatraTexts;
import com.astro.prediction.PlanetHouseTexts;
import com.astro.prediction.PlanetSignTexts;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.astro.util.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Astrological rule search service supporting both Elasticsearch cluster queries
 * and an offline/disabled in-memory searchable catalog based on classical prediction texts
 * (LagnaTexts, NakshatraTexts, PlanetSignTexts, PlanetHouseTexts, DashaTexts).
 */
@Service
public class AstroSearchService {

    private static final Logger log = LoggerFactory.getLogger(AstroSearchService.class);
    public static final String DEFAULT_INDEX_NAME = "astro_rules";

    @Value("${astro.elasticsearch.enabled:false}")
    private boolean enabled;

    private final String indexName;
    private final ElasticsearchClient esClient;
    private final ElasticsearchOperations elasticsearchOperations;
    private final Map<String, AstroRule> inMemoryCatalog = new ConcurrentHashMap<>();
    private final CircuitBreaker circuitBreaker = new CircuitBreaker("elasticsearch", 3, Duration.ofSeconds(30), 2);

    private static final String[] ZODIAC_SIGNS = {
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    };

    private static final String[] NAKSHATRAS = {
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    };

    private static final String[] PLANETS = {
        "Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn", "Rahu", "Ketu"
    };

    private static final String[] HOUSE_ORDINALS = {
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth",
        "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth"
    };

    private static final String[][] ANTAR_PAIRS = {
        {"Jupiter", "Saturn"}, {"Saturn", "Jupiter"},
        {"Sun", "Moon"}, {"Moon", "Sun"},
        {"Venus", "Jupiter"}, {"Jupiter", "Venus"},
        {"Mars", "Saturn"}, {"Saturn", "Mars"},
        {"Rahu", "Jupiter"}, {"Ketu", "Venus"}
    };

    public AstroSearchService() {
        this(false, null, null, DEFAULT_INDEX_NAME);
    }

    public AstroSearchService(boolean enabled) {
        this(enabled, null, null, DEFAULT_INDEX_NAME);
    }

    public AstroSearchService(boolean enabled, ElasticsearchClient esClient) {
        this(enabled, esClient, null, DEFAULT_INDEX_NAME);
    }

    public AstroSearchService(boolean enabled, ElasticsearchClient esClient, String indexName) {
        this(enabled, esClient, null, indexName);
    }

    @Autowired
    public AstroSearchService(
            @Value("${astro.elasticsearch.enabled:false}") boolean enabled,
            @Autowired(required = false) ElasticsearchClient esClient,
            @Autowired(required = false) ElasticsearchOperations elasticsearchOperations) {
        this(enabled, esClient, elasticsearchOperations, DEFAULT_INDEX_NAME);
    }

    public AstroSearchService(
            boolean enabled,
            ElasticsearchClient esClient,
            ElasticsearchOperations elasticsearchOperations,
            String indexName) {
        this.enabled = enabled;
        this.esClient = esClient;
        this.elasticsearchOperations = elasticsearchOperations;
        this.indexName = (indexName != null && !indexName.isBlank()) ? indexName : DEFAULT_INDEX_NAME;
        initCatalog();
    }

    /**
     * Pre-seeds the in-memory catalog from classical prediction texts.
     */
    private void initCatalog() {
        // 1. LagnaTexts: Ascendants & Houses
        for (String sign : ZODIAC_SIGNS) {
            String text = LagnaTexts.getLagna(sign);
            String id = "lagna_" + sign.toLowerCase();
            inMemoryCatalog.put(id, new AstroRule(
                id,
                sign + " Lagna (Ascendant)",
                "Vedic",
                text,
                "Lagna"
            ));
        }

        for (int h = 1; h <= 12; h++) {
            String text = LagnaTexts.getHouseMeaning(h);
            String id = "house_" + h;
            inMemoryCatalog.put(id, new AstroRule(
                id,
                HOUSE_ORDINALS[h] + " House Meaning",
                "Vedic",
                text,
                "House"
            ));
        }

        // 2. NakshatraTexts: 27 Lunar mansions
        for (String nakshatra : NAKSHATRAS) {
            String text = NakshatraTexts.get(nakshatra);
            String id = "nakshatra_" + nakshatra.toLowerCase().replace(" ", "_");
            inMemoryCatalog.put(id, new AstroRule(
                id,
                nakshatra + " Nakshatra",
                "Vedic",
                text,
                "Nakshatra"
            ));
        }

        // 3. PlanetSignTexts: 9 Planets x 12 Signs
        for (String planet : PLANETS) {
            for (String sign : ZODIAC_SIGNS) {
                String text = PlanetSignTexts.get(planet, sign);
                String id = "planetsign_" + planet.toLowerCase() + "_" + sign.toLowerCase();
                inMemoryCatalog.put(id, new AstroRule(
                    id,
                    planet + " in " + sign,
                    "Vedic",
                    text,
                    "PlanetSign"
                ));
            }
        }

        // 4. PlanetHouseTexts: 9 Planets x 12 Houses
        for (String planet : PLANETS) {
            for (int h = 1; h <= 12; h++) {
                String text = PlanetHouseTexts.get(planet, h);
                String id = "planethouse_" + planet.toLowerCase() + "_h" + h;
                inMemoryCatalog.put(id, new AstroRule(
                    id,
                    planet + " in " + HOUSE_ORDINALS[h] + " House (H" + h + ")",
                    "Vedic",
                    text,
                    "PlanetHouse"
                ));
            }
        }

        // 5. DashaTexts: Mahadashas & Antardashas
        for (String lord : PLANETS) {
            String text = DashaTexts.getMaha(lord);
            String id = "dasha_maha_" + lord.toLowerCase();
            inMemoryCatalog.put(id, new AstroRule(
                id,
                lord + " Mahadasha",
                "Vedic",
                text,
                "Dasha"
            ));
        }

        for (String[] pair : ANTAR_PAIRS) {
            String maha = pair[0];
            String antar = pair[1];
            String text = DashaTexts.getAntar(maha, antar);
            String id = "dasha_antar_" + maha.toLowerCase() + "_" + antar.toLowerCase();
            inMemoryCatalog.put(id, new AstroRule(
                id,
                maha + " Mahadasha - " + antar + " Antardasha",
                "Vedic",
                text,
                "Dasha"
            ));
        }

        log.info("Initialized in-memory astrological rules catalog with {} entries", inMemoryCatalog.size());
    }

    /**
     * Checks if Elasticsearch integration is enabled via configuration flag.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Indexes a classical astrological rule.
     * When Elasticsearch is enabled and reachable, indexes into the cluster.
     * Always indexes into the in-memory catalog to ensure seamless fallback availability.
     */
    public void indexRule(String ruleId, String title, String tradition, String content) {
        indexRule(ruleId, title, tradition, content, "Custom");
    }

    public void indexRule(String ruleId, String title, String tradition, String content, String category) {
        if (ruleId == null || ruleId.trim().isEmpty()) {
            throw new IllegalArgumentException("ruleId must not be null or blank");
        }

        AstroRule rule = new AstroRule(
            ruleId.trim(),
            title != null ? title : "",
            tradition != null ? tradition : "Vedic",
            content != null ? content : "",
            category != null ? category : "Custom"
        );

        if (enabled && esClient != null) {
            circuitBreaker.executeVoid(() -> {
                IndexRequest<AstroRule> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(ruleId.trim())
                    .document(rule)
                );
                try {
                    esClient.index(request);
                    log.debug("Successfully indexed rule {} in Elasticsearch index {}", ruleId, indexName);
                } catch (Exception e) {
                    throw new RuntimeException("Elasticsearch indexing failed: " + e.getMessage(), e);
                }
            }, () -> {
                log.debug("Elasticsearch circuit breaker active; cached rule {} in in-memory catalog only", ruleId);
            });
        }

        inMemoryCatalog.put(ruleId.trim(), rule);
    }

    /**
     * Searches classical astrological rules matching the given query string.
     * Protected by CircuitBreaker: when Elasticsearch is online and healthy, queries cluster.
     * When Elasticsearch trips, is offline, or encounters errors, immediately falls back to in-memory catalog with 0ms delay.
     */
    public List<Map<String, Object>> searchRules(String query, int limit) {
        if (query == null || query.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        int effectiveLimit = Math.min(limit, 100);

        if (enabled && esClient != null) {
            return circuitBreaker.execute(() -> {
                try {
                    List<Map<String, Object>> esResults = searchElasticsearch(query.trim(), effectiveLimit);
                    if (!esResults.isEmpty()) {
                        return esResults;
                    }
                    // If ES returned 0 hits, check in-memory fallback
                    List<Map<String, Object>> fallbackResults = searchInMemory(query.trim(), effectiveLimit);
                    return !fallbackResults.isEmpty() ? fallbackResults : esResults;
                } catch (Exception e) {
                    throw new RuntimeException("Elasticsearch query error: " + e.getMessage(), e);
                }
            }, () -> {
                log.debug("Elasticsearch circuit breaker OPEN or tripped; serving search from in-memory rules catalog");
                return searchInMemory(query.trim(), effectiveLimit);
            });
        }

        return searchInMemory(query.trim(), effectiveLimit);
    }

    /**
     * Returns the circuit breaker managing Elasticsearch connectivity.
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    /**
     * Telemetry status combining in-memory catalog size and circuit breaker state.
     */
    public Map<String, Object> getStatus() {
        return Map.of(
            "elasticsearchEnabled", enabled,
            "indexName", indexName,
            "inMemoryCatalogSize", inMemoryCatalog.size(),
            "circuitBreaker", circuitBreaker.getMetrics()
        );
    }

    private List<Map<String, Object>> searchElasticsearch(String query, int limit) throws Exception {
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(indexName)
            .size(limit)
            .query(q -> q
                .multiMatch(m -> m
                    .query(query)
                    .fields("title^3", "content", "tradition", "category", "id")
                )
            )
        );

        SearchResponse<AstroRule> response = esClient.search(searchRequest, AstroRule.class);
        if (response == null || response.hits() == null || response.hits().hits() == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<AstroRule> hit : response.hits().hits()) {
            AstroRule doc = hit.source();
            double score = hit.score() != null ? hit.score() : 1.0;
            if (doc != null) {
                results.add(doc.toMap(score));
            } else if (hit.id() != null) {
                AstroRule cached = inMemoryCatalog.get(hit.id());
                if (cached != null) {
                    results.add(cached.toMap(score));
                }
            }
        }
        return results;
    }

    private List<Map<String, Object>> searchInMemory(String query, int limit) {
        String fullQueryLower = query.toLowerCase().trim();
        String[] rawTokens = fullQueryLower.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String t : rawTokens) {
            String clean = t.replaceAll("[^a-zA-Z0-9_-]", "");
            if (!clean.isEmpty()) {
                tokens.add(clean);
            }
        }

        List<ScoredRule> matches = new ArrayList<>();

        for (AstroRule rule : inMemoryCatalog.values()) {
            double score = computeRelevance(rule, fullQueryLower, tokens);
            if (score > 0.0) {
                matches.add(new ScoredRule(rule, score));
            }
        }

        matches.sort((a, b) -> {
            int cmp = Double.compare(b.score, a.score);
            if (cmp != 0) return cmp;
            return a.rule.getTitle().compareToIgnoreCase(b.rule.getTitle());
        });

        return matches.stream()
            .limit(limit)
            .map(sr -> sr.rule.toMap(sr.score))
            .collect(Collectors.toList());
    }

    private double computeRelevance(AstroRule rule, String fullQuery, List<String> tokens) {
        String id = rule.getId().toLowerCase();
        String title = rule.getTitle().toLowerCase();
        String content = rule.getContent() != null ? rule.getContent().toLowerCase() : "";
        String tradition = rule.getTradition() != null ? rule.getTradition().toLowerCase() : "";
        String category = rule.getCategory() != null ? rule.getCategory().toLowerCase() : "";

        double score = 0.0;

        // 1. Exact or partial full-query matches
        if (id.equals(fullQuery)) {
            score += 150.0;
        } else if (id.contains(fullQuery)) {
            score += 50.0;
        }

        if (title.equals(fullQuery)) {
            score += 120.0;
        } else if (title.contains(fullQuery)) {
            score += 60.0;
        }

        if (!content.isEmpty() && content.contains(fullQuery)) {
            score += 25.0;
        }

        // 2. Token-level matches
        int matchedTokens = 0;
        for (String token : tokens) {
            boolean tokenMatched = false;

            if (id.contains(token)) {
                score += 15.0;
                tokenMatched = true;
            }
            if (title.contains(token)) {
                score += 20.0;
                tokenMatched = true;
            }
            if (category.contains(token) || tradition.contains(token)) {
                score += 10.0;
                tokenMatched = true;
            }
            if (!content.isEmpty() && content.contains(token)) {
                score += 5.0;
                int tf = countOccurrences(content, token);
                score += Math.min(tf, 5) * 1.0;
                tokenMatched = true;
            }

            if (tokenMatched) {
                matchedTokens++;
            }
        }

        // 3. Multi-token coverage bonus
        if (tokens.size() > 1) {
            if (matchedTokens == tokens.size()) {
                score += 40.0;
            } else if (matchedTokens == 0) {
                return 0.0;
            }
        }

        return score;
    }

    private static int countOccurrences(String haystack, String needle) {
        if (needle.isEmpty() || haystack.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    public int getCatalogSize() {
        return inMemoryCatalog.size();
    }

    public Map<String, Object> getRuleById(String ruleId) {
        if (ruleId == null) return null;
        AstroRule rule = inMemoryCatalog.get(ruleId.trim());
        return rule != null ? rule.toMap(1.0) : null;
    }

    private static class ScoredRule {
        final AstroRule rule;
        final double score;

        ScoredRule(AstroRule rule, double score) {
            this.rule = rule;
            this.score = score;
        }
    }

    /**
     * Document representation of an astrological rule.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Document(indexName = "astro_rules")
    public static class AstroRule implements Serializable {
        private static final long serialVersionUID = 1L;

        @Id
        @Field(type = FieldType.Keyword)
        private String id;

        @Field(type = FieldType.Text)
        private String title;

        @Field(type = FieldType.Keyword)
        private String tradition;

        @Field(type = FieldType.Text)
        private String content;

        @Field(type = FieldType.Keyword)
        private String category;

        public AstroRule() {}

        @JsonCreator
        public AstroRule(
                @JsonProperty("id") String id,
                @JsonProperty("title") String title,
                @JsonProperty("tradition") String tradition,
                @JsonProperty("content") String content,
                @JsonProperty("category") String category) {
            this.id = id;
            this.title = title;
            this.tradition = tradition;
            this.content = content;
            this.category = category;
        }

        public AstroRule(String id, String title, String tradition, String content) {
            this(id, title, tradition, content, "Custom");
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTradition() { return tradition; }
        public void setTradition(String tradition) { this.tradition = tradition; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Map<String, Object> toMap(double score) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", id);
            map.put("ruleId", id);
            map.put("title", title);
            map.put("tradition", tradition);
            map.put("content", content);
            if (category != null && !category.isBlank()) {
                map.put("category", category);
            }
            map.put("score", score);
            return map;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AstroRule astroRule = (AstroRule) o;
            return Objects.equals(id, astroRule.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "AstroRule{" +
                    "id='" + id + '\'' +
                    ", title='" + title + '\'' +
                    ", tradition='" + tradition + '\'' +
                    ", category='" + category + '\'' +
                    '}';
        }
    }
}
