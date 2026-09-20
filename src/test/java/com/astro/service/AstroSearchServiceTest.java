package com.astro.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AstroSearchServiceTest {

    private AstroSearchService disabledService;
    private ElasticsearchClient mockClient;
    private AstroSearchService enabledService;

    @BeforeEach
    void setup() {
        disabledService = new AstroSearchService(false);
        mockClient = mock(ElasticsearchClient.class);
        enabledService = new AstroSearchService(true, mockClient);
    }

    // ── 1. Configuration & Flag Tests ──────────────────────────────────────────

    @Test
    void testDefaultConstructorDisabled() {
        AstroSearchService defaultService = new AstroSearchService();
        assertThat(defaultService.isEnabled()).isFalse();
        assertThat(defaultService.getCatalogSize()).isGreaterThanOrEqualTo(286);
    }

    @Test
    void testEnabledFlagReflectsConfiguration() {
        assertThat(disabledService.isEnabled()).isFalse();
        assertThat(enabledService.isEnabled()).isTrue();
    }

    // ── 2. Built-in In-Memory Catalog Prediction Text Tests ─────────────────────

    @Test
    void testCatalogContainsAllClassicalPredictionSources() {
        // 12 Lagna + 12 House + 27 Nakshatra + 108 PlanetSign + 108 PlanetHouse + 9 Maha + 10 Antar = 286 rules
        assertThat(disabledService.getCatalogSize()).isEqualTo(286);

        // Verify LagnaText rule
        Map<String, Object> ariesLagna = disabledService.getRuleById("lagna_aries");
        assertThat(ariesLagna).isNotNull();
        assertThat(ariesLagna.get("title")).isEqualTo("Aries Lagna (Ascendant)");
        assertThat(ariesLagna.get("tradition")).isEqualTo("Vedic");
        assertThat(ariesLagna.get("category")).isEqualTo("Lagna");
        assertThat((String) ariesLagna.get("content")).contains("Mars governs your entire chart");

        // Verify House meaning rule
        Map<String, Object> h1House = disabledService.getRuleById("house_1");
        assertThat(h1House).isNotNull();
        assertThat(h1House.get("title")).isEqualTo("First House Meaning");
        assertThat((String) h1House.get("content")).contains("physical self");

        // Verify NakshatraText rule
        Map<String, Object> ashwini = disabledService.getRuleById("nakshatra_ashwini");
        assertThat(ashwini).isNotNull();
        assertThat(ashwini.get("title")).isEqualTo("Ashwini Nakshatra");
        assertThat((String) ashwini.get("content")).contains("Ketu");

        // Verify PlanetSignText rule
        Map<String, Object> sunAries = disabledService.getRuleById("planetsign_sun_aries");
        assertThat(sunAries).isNotNull();
        assertThat(sunAries.get("title")).isEqualTo("Sun in Aries");
        assertThat((String) sunAries.get("content")).contains("Exalted");

        // Verify PlanetHouseText rule
        Map<String, Object> marsH10 = disabledService.getRuleById("planethouse_mars_h10");
        assertThat(marsH10).isNotNull();
        assertThat(marsH10.get("title")).isEqualTo("Mars in Tenth House (H10)");
        assertThat((String) marsH10.get("content")).contains("career");

        // Verify DashaText rule (Maha)
        Map<String, Object> jupiterMaha = disabledService.getRuleById("dasha_maha_jupiter");
        assertThat(jupiterMaha).isNotNull();
        assertThat(jupiterMaha.get("title")).isEqualTo("Jupiter Mahadasha");
        assertThat((String) jupiterMaha.get("content")).contains("Jupiter Mahadasha (16 years)");

        // Verify DashaText rule (Antar blend)
        Map<String, Object> jupSat = disabledService.getRuleById("dasha_antar_jupiter_saturn");
        assertThat(jupSat).isNotNull();
        assertThat(jupSat.get("title")).isEqualTo("Jupiter Mahadasha - Saturn Antardasha");
        assertThat((String) jupSat.get("content")).contains("Saturn's discipline");
    }

    // ── 3. Rule Search (In-Memory Fallback) ─────────────────────────────────────

    @Test
    void testSearchRulesLagnaMatch() {
        List<Map<String, Object>> results = disabledService.searchRules("Scorpio Lagna", 5);
        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("lagna_scorpio");
        assertThat(top.get("title")).isEqualTo("Scorpio Lagna (Ascendant)");
        assertThat((String) top.get("content")).contains("Mars governs your chart");
        assertThat((Double) top.get("score")).isGreaterThan(0.0);
    }

    @Test
    void testSearchRulesNakshatraMatch() {
        List<Map<String, Object>> results = disabledService.searchRules("Rohini Nakshatra", 3);
        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("nakshatra_rohini");
        assertThat(top.get("title")).isEqualTo("Rohini Nakshatra");
        assertThat((String) top.get("content")).contains("Brahma");
    }

    @Test
    void testSearchRulesPlanetInSignMatch() {
        List<Map<String, Object>> results = disabledService.searchRules("Mars Capricorn", 5);
        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("planetsign_mars_capricorn");
        assertThat(top.get("title")).isEqualTo("Mars in Capricorn");
        assertThat((String) top.get("content")).contains("Exalted");
    }

    @Test
    void testSearchRulesPlanetInHouseMatch() {
        List<Map<String, Object>> results = disabledService.searchRules("Jupiter in Ninth House", 5);
        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("planethouse_jupiter_h9");
        assertThat((String) top.get("content")).contains("9th");
    }

    @Test
    void testSearchRulesDashaMatch() {
        List<Map<String, Object>> results = disabledService.searchRules("Rahu Mahadasha", 5);
        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("dasha_maha_rahu");
        assertThat((String) top.get("content")).contains("18 years");
    }

    @Test
    void testSearchRulesDeepContentKeywords() {
        // "kundalini" appears in Ashlesha Nakshatra
        List<Map<String, Object>> results = disabledService.searchRules("kundalini", 5);
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(r -> "nakshatra_ashlesha".equals(r.get("id")));

        // "exalted" appears across multiple astrological rules
        List<Map<String, Object>> exaltedResults = disabledService.searchRules("exalted", 10);
        assertThat(exaltedResults).hasSizeGreaterThanOrEqualTo(5);
    }

    // ── 4. Edge Cases in Search ────────────────────────────────────────────────

    @Test
    void testSearchRulesEdgeCases() {
        assertThat(disabledService.searchRules(null, 10)).isEmpty();
        assertThat(disabledService.searchRules("", 10)).isEmpty();
        assertThat(disabledService.searchRules("    ", 10)).isEmpty();
        assertThat(disabledService.searchRules("Aries", 0)).isEmpty();
        assertThat(disabledService.searchRules("Aries", -5)).isEmpty();
        assertThat(disabledService.searchRules("xyznonexistentunrealtoken12345", 10)).isEmpty();
    }

    @Test
    void testSearchRulesLimitCap() {
        List<Map<String, Object>> results = disabledService.searchRules("Sun", 3);
        assertThat(results).hasSize(3);
    }

    // ── 5. Index Operation Tests ───────────────────────────────────────────────

    @Test
    void testIndexRuleInMemory() {
        String customId = "rule_kp_cusp_1";
        String customTitle = "KP 1st Cusp Sublord Rule";
        String customTradition = "KP Krishnamurti";
        String customContent = "The sublord of the 1st cusp determines physical health and appearance.";

        disabledService.indexRule(customId, customTitle, customTradition, customContent);

        // Verify retrieval by ID
        Map<String, Object> rule = disabledService.getRuleById(customId);
        assertThat(rule).isNotNull();
        assertThat(rule.get("id")).isEqualTo(customId);
        assertThat(rule.get("ruleId")).isEqualTo(customId);
        assertThat(rule.get("title")).isEqualTo(customTitle);
        assertThat(rule.get("tradition")).isEqualTo(customTradition);
        assertThat(rule.get("content")).isEqualTo(customContent);

        // Verify retrieval via search
        List<Map<String, Object>> searchHits = disabledService.searchRules("Krishnamurti Cusp Sublord", 5);
        assertThat(searchHits).isNotEmpty();
        assertThat(searchHits.get(0).get("id")).isEqualTo(customId);

        // Verify catalog size increased
        assertThat(disabledService.getCatalogSize()).isEqualTo(287);
    }

    @Test
    void testIndexRuleUpdateExisting() {
        disabledService.indexRule("rule_update_test", "Initial Title", "Vedic", "Initial text");
        assertThat(disabledService.getRuleById("rule_update_test").get("title")).isEqualTo("Initial Title");

        disabledService.indexRule("rule_update_test", "Updated Title", "Vedic", "Updated text");
        assertThat(disabledService.getRuleById("rule_update_test").get("title")).isEqualTo("Updated Title");
        assertThat(disabledService.getRuleById("rule_update_test").get("content")).isEqualTo("Updated text");
    }

    @Test
    void testIndexRuleValidation() {
        assertThatThrownBy(() -> disabledService.indexRule(null, "Title", "Tradition", "Content"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> disabledService.indexRule("   ", "Title", "Tradition", "Content"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── 6. Elasticsearch Online Tests ──────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void testElasticsearchOnlineSearchHit() throws Exception {
        SearchResponse<AstroSearchService.AstroRule> mockResponse = mock(SearchResponse.class);
        HitsMetadata<AstroSearchService.AstroRule> mockHitsMetadata = mock(HitsMetadata.class);
        Hit<AstroSearchService.AstroRule> mockHit = mock(Hit.class);

        AstroSearchService.AstroRule esDoc = new AstroSearchService.AstroRule(
                "es_rule_1",
                "Elasticsearch Mars Rule",
                "Vedic",
                "Mars in 1st house gives athletic prowess from cluster.",
                "PlanetHouse"
        );

        when(mockHit.source()).thenReturn(esDoc);
        when(mockHit.score()).thenReturn(4.25);
        when(mockHitsMetadata.hits()).thenReturn(List.of(mockHit));
        when(mockResponse.hits()).thenReturn(mockHitsMetadata);
        when(mockClient.search(any(SearchRequest.class), eq(AstroSearchService.AstroRule.class))).thenReturn(mockResponse);

        List<Map<String, Object>> results = enabledService.searchRules("Mars athletic", 5);

        verify(mockClient, times(1)).search(any(SearchRequest.class), eq(AstroSearchService.AstroRule.class));
        assertThat(results).hasSize(1);
        Map<String, Object> hit = results.get(0);
        assertThat(hit.get("id")).isEqualTo("es_rule_1");
        assertThat(hit.get("title")).isEqualTo("Elasticsearch Mars Rule");
        assertThat(hit.get("content")).isEqualTo("Mars in 1st house gives athletic prowess from cluster.");
        assertThat(hit.get("score")).isEqualTo(4.25);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testElasticsearchOnlineIndexOperation() throws Exception {
        IndexResponse mockIndexResponse = mock(IndexResponse.class);
        when(mockClient.index(any(IndexRequest.class))).thenReturn(mockIndexResponse);

        enabledService.indexRule("es_rule_new", "Online Indexed Title", "Jaimini", "Chara Karaka rules");

        verify(mockClient, times(1)).index(any(IndexRequest.class));

        // Also verify rule was added to in-memory catalog
        Map<String, Object> cached = enabledService.getRuleById("es_rule_new");
        assertThat(cached).isNotNull();
        assertThat(cached.get("tradition")).isEqualTo("Jaimini");
    }

    // ── 7. Elasticsearch Offline Graceful Fallback Tests ───────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void testElasticsearchOfflineFallsBackGracefullyOnSearch() throws Exception {
        // Simulate cluster offline / network timeout / connection refused
        when(mockClient.search(any(SearchRequest.class), eq(AstroSearchService.AstroRule.class)))
                .thenThrow(new RuntimeException("Elasticsearch cluster unavailable: Connection refused to http://127.0.0.1:9200"));

        // Should NOT throw exception; should fall back to built-in in-memory catalog
        List<Map<String, Object>> results = enabledService.searchRules("Aries Lagna", 5);

        assertThat(results).isNotEmpty();
        Map<String, Object> top = results.get(0);
        assertThat(top.get("id")).isEqualTo("lagna_aries");
        assertThat(top.get("title")).isEqualTo("Aries Lagna (Ascendant)");
        assertThat((String) top.get("content")).contains("Mars governs your entire chart");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testElasticsearchIOExceptionFallsBackGracefullyOnSearch() throws Exception {
        when(mockClient.search(any(SearchRequest.class), eq(AstroSearchService.AstroRule.class)))
                .thenThrow(new IOException("Remote host terminated handshake"));

        List<Map<String, Object>> results = enabledService.searchRules("Ashwini", 3);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).get("id")).isEqualTo("nakshatra_ashwini");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testElasticsearchOfflineFallsBackGracefullyOnIndex() throws Exception {
        when(mockClient.index(any(IndexRequest.class)))
                .thenThrow(new RuntimeException("Cluster down: write operation failed"));

        // Should NOT throw exception
        enabledService.indexRule("offline_rule_1", "Offline Title", "Western", "Saved during cluster outage");

        // Verify stored in in-memory catalog
        Map<String, Object> rule = enabledService.getRuleById("offline_rule_1");
        assertThat(rule).isNotNull();
        assertThat(rule.get("title")).isEqualTo("Offline Title");
        assertThat(rule.get("content")).isEqualTo("Saved during cluster outage");

        // Verify search finds it via fallback
        when(mockClient.search(any(SearchRequest.class), eq(AstroSearchService.AstroRule.class)))
                .thenThrow(new RuntimeException("Search failed too"));

        List<Map<String, Object>> hits = enabledService.searchRules("Saved during cluster outage", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).get("id")).isEqualTo("offline_rule_1");
    }

    @Test
    void testElasticsearchEnabledWithNullClientFallsBack() {
        AstroSearchService serviceNullClient = new AstroSearchService(true, null);
        assertThat(serviceNullClient.isEnabled()).isTrue();

        // Search falls back cleanly
        List<Map<String, Object>> results = serviceNullClient.searchRules("Jupiter Mahadasha", 5);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).get("id")).isEqualTo("dasha_maha_jupiter");

        // Index falls back cleanly
        serviceNullClient.indexRule("null_client_rule", "Null Client Title", "Vedic", "Content");
        assertThat(serviceNullClient.getRuleById("null_client_rule")).isNotNull();
    }
}
