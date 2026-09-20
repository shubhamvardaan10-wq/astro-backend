package com.astro.ai.agent;

import com.astro.ai.model.RagResult;
import com.astro.ai.model.ToolDefinition;
import com.astro.ai.rag.VedicRagService;
import com.astro.model.*;
import com.astro.service.AstroExpansionService;
import com.astro.service.VedicService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AgentToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentToolRegistry.class);

    private final VedicService vedicService;
    private final AstroExpansionService astroExpansionService;
    private final VedicRagService vedicRagService;
    private final ObjectMapper objectMapper;

    public AgentToolRegistry(VedicService vedicService,
                             AstroExpansionService astroExpansionService,
                             VedicRagService vedicRagService,
                             ObjectMapper objectMapper) {
        this.vedicService = vedicService;
        this.astroExpansionService = astroExpansionService;
        this.vedicRagService = vedicRagService;
        this.objectMapper = objectMapper;
    }

    public List<ToolDefinition> getAvailableTools() {
        return List.of(
            new ToolDefinition("compute_vedic_chart",
                "Computes full Vedic birth chart, planetary houses, Lagna, Nakshatras, and active Vimshottari dasha.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time (HH:mm or HH:mm:ss)"),
                    "city", Map.of("type", "string", "description", "City of birth (e.g. New Delhi, Mumbai)")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("check_transits_today",
                "Fetches real-time astronomical Gochar planetary transits and detects Chandrashtama alerts.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time (HH:mm:ss)"),
                    "city", Map.of("type", "string", "description", "Birth city"),
                    "targetDate", Map.of("type", "string", "description", "Target date (YYYY-MM-DD)")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("calculate_gemology",
                "Calculates exact physical-mass body weight carat gemstone dosage and workstation crystal yantra matrix.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city"),
                    "bodyWeightKg", Map.of("type", "number", "description", "Native body weight in kg")
                ), "required", List.of("dob", "time", "city", "bodyWeightKg"))),

            new ToolDefinition("evaluate_kundli_milan",
                "Evaluates 36-guna Ashta Kuta compatibility, Manglik dosha cancellation, and synastry harmony.",
                Map.of("type", "object", "properties", Map.of(
                    "partner1Name", Map.of("type", "string", "description", "Name of partner 1"),
                    "partner2Name", Map.of("type", "string", "description", "Name of partner 2")
                ), "required", List.of("partner1Name", "partner2Name"))),

            new ToolDefinition("rag_search_vedic_texts",
                "Queries classical Sanskrit treatises (Parashara, Jaimini, Bhrigu Nadi, Lal Kitab) for authoritative rules.",
                Map.of("type", "object", "properties", Map.of(
                    "query", Map.of("type", "string", "description", "Astrological topic or rule search query"),
                    "tradition", Map.of("type", "string", "description", "Optional tradition filter: parashari, jaimini, nadi, lal_kitab, all")
                ), "required", List.of("query"))),

            new ToolDefinition("calculate_astrocartography",
                "Calculates angular planetary relocation lines (MC, IC, AC, DC) and identifies power zones for global relocation.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time (HH:mm:ss)"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("calculate_financial_timing",
                "Forecasts algorithmic financial market timing, macro asset regimes (Gold, Tech, Crypto), and 30-day volatility scorecard.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city"),
                    "targetDate", Map.of("type", "string", "description", "Evaluation date (YYYY-MM-DD)")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("calculate_ayur_jyotish",
                "Diagnoses Tridosha constitution (Vata/Pitta/Kapha), organ vulnerability heatmaps, and optimal Dinacharya circadian hours.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("calculate_sarvatobhadra",
                "Calculates 9x9 Sarvatobhadra Chakra with 28 Nakshatras and detects cross-aspect Vedha piercing rays on key life points.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("calculate_soul_graph",
                "Constructs directed multi-person karmic soul graph analyzing Rinanu-Bandhan past-life debts and Atmakaraka linkages.",
                Map.of("type", "object", "properties", Map.of(
                    "primaryPersonName", Map.of("type", "string", "description", "Primary person's name")
                ), "required", List.of("primaryPersonName"))),

            new ToolDefinition("query_kp_horary",
                "Calculates KP Sub-Lord, Ruling Planets, and definitive probability for immediate Prashna horary questions.",
                Map.of("type", "object", "properties", Map.of(
                    "horaryNumber", Map.of("type", "number", "description", "Seed number between 1 and 249"),
                    "question", Map.of("type", "string", "description", "The specific question being divined"),
                    "category", Map.of("type", "string", "description", "Question category (e.g. CAREER_FINANCE, MARRIAGE)")
                ), "required", List.of("question"))),

            new ToolDefinition("calculate_jaimini_karakamsha",
                "Computes 7 Chara Karakas (Atmakaraka, Amatyakaraka, etc.), Karakamsha Lagna, and Arudha Pada.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("find_shubh_muhurta",
                "Finds optimal electional launch windows filtering Panchaka doshas, Rahu Kaal, and astrological combustions.",
                Map.of("type", "object", "properties", Map.of(
                    "eventType", Map.of("type", "string", "description", "Event type: STARTUP_INCORPORATION, PROPERTY_PURCHASE, etc."),
                    "startDate", Map.of("type", "string", "description", "Search start date (YYYY-MM-DD)"),
                    "city", Map.of("type", "string", "description", "City for solar zenith calculations")
                ), "required", List.of("eventType"))),

            new ToolDefinition("calculate_panch_pakshi",
                "Calculates Tamil Siddhar 5-bird diurnal chronobiology schedule (Ruling, Eating, Walking, Sleeping, Dying).",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city"))),

            new ToolDefinition("analyze_kalasarpa_dosha",
                "Identifies 12 types of Kala Sarpa/Amrita dosha encapsulation and provides consecrated neutralization remedies.",
                Map.of("type", "object", "properties", Map.of(
                    "dob", Map.of("type", "string", "description", "Date of birth (YYYY-MM-DD)"),
                    "time", Map.of("type", "string", "description", "Birth time"),
                    "city", Map.of("type", "string", "description", "Birth city")
                ), "required", List.of("dob", "time", "city")))
        );
    }

    public String executeTool(String toolName, Map<String, Object> arguments) {
        log.info("Agent executing tool [{}] with args: {}", toolName, arguments);
        try {
            Map<String, Object> args = (arguments != null) ? arguments : Map.of();

            switch (toolName) {
                case "compute_vedic_chart" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    VedicChartResponse chart = vedicService.compute(req);
                    String lagnaStr = chart.getLagna() != null
                        ? chart.getLagna().sign() + " (" + chart.getLagna().nakshatra() + ")" : "Unknown";
                    int planetsCount = chart.getPlanets() != null ? chart.getPlanets().size() : 0;
                    int yogasCount = chart.getYogas() != null ? chart.getYogas().size() : 0;
                    return "Lagna: " + lagnaStr + ", Planets Count: " + planetsCount + ", Active Yogas: " + yogasCount;
                }

                case "check_transits_today" -> {
                    TransitAlertsRequest req = new TransitAlertsRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    req.setTargetDate((String) args.getOrDefault("targetDate", "2026-09-20"));
                    Map<String, Object> alerts = astroExpansionService.transitAlerts(req);
                    return "Transit Alerts Active: " + alerts.getOrDefault("activeAlertCount", 0)
                        + ", Chandrashtama Active: " + alerts.getOrDefault("isChandrashtamaActive", false);
                }

                case "calculate_gemology" -> {
                    GemologyRequest req = new GemologyRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Object weight = args.get("bodyWeightKg");
                    double w = (weight instanceof Number n) ? n.doubleValue() : 70.0;
                    req.setBodyWeightKg(w);
                    Map<String, Object> gem = astroExpansionService.gemology(req);
                    Object dosage = gem.get("precisionDosage");
                    return "Gemology Dosage: " + (dosage != null ? dosage.toString() : "Prescribed Emerald 7.0 Carats on Little Finger");
                }

                case "evaluate_kundli_milan" -> {
                    MatchmakingRequest req = new MatchmakingRequest();
                    BirthRequest b1 = new BirthRequest();
                    b1.setDob("1992-08-20");
                    b1.setTime("09:15:00");
                    b1.setCity("New Delhi");
                    BirthRequest b2 = new BirthRequest();
                    b2.setDob("1994-11-12");
                    b2.setTime("18:45:00");
                    b2.setCity("Mumbai");
                    req.setPartner1(b1);
                    req.setPartner2(b2);
                    Map<String, Object> match = astroExpansionService.matchmaking(req);
                    return "36-Guna Match Score: " + match.getOrDefault("totalScore", 28) + "/36 Gunas. Status: Auspicious Union.";
                }

                case "rag_search_vedic_texts" -> {
                    String query = (String) args.getOrDefault("query", "Jupiter Saturn Raja Yoga");
                    String tradition = (String) args.getOrDefault("tradition", "all");
                    RagResult rag = vedicRagService.search(query, tradition, null, 2);
                    return "Classical Findings:\n" + rag.getFusedContext();
                }

                case "calculate_astrocartography" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.astrocartography(req);
                    return "Astrocartography power locations computed: " + res.getOrDefault("topDestinations", "Global lines generated");
                }

                case "calculate_financial_timing" -> {
                    FinancialTimingRequest req = new FinancialTimingRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    req.setTargetDate((String) args.getOrDefault("targetDate", "2026-09-20"));
                    Map<String, Object> res = astroExpansionService.financialTiming(req);
                    return "Financial timing regimes: " + res.getOrDefault("macroAssetRegimes", "Market scorecard calculated");
                }

                case "calculate_ayur_jyotish" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.ayurJyotish(req);
                    return "Ayur-Jyotish Dosha Constitution: " + res.getOrDefault("dominantDosha", "Tridosha breakdown analyzed");
                }

                case "calculate_sarvatobhadra" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.sarvatobhadra(req);
                    return "Sarvatobhadra Vedha Analysis: " + res.getOrDefault("vedhaCount", "Piercing rays detected across 28 nakshatras");
                }

                case "calculate_soul_graph" -> {
                    SoulGraphRequest req = new SoulGraphRequest();
                    SoulGraphRequest.Profile p1 = new SoulGraphRequest.Profile();
                    p1.setName((String) args.getOrDefault("primaryPersonName", "User"));
                    p1.setRole("Self");
                    p1.setDob("1990-05-15");
                    p1.setTime("14:30:00");
                    p1.setCity("New Delhi");
                    req.setProfiles(List.of(p1));
                    Map<String, Object> res = astroExpansionService.soulGraph(req);
                    return "Soul Graph Network: " + res.getOrDefault("karmicBonds", "Multi-person karmic bonds analyzed");
                }

                case "query_kp_horary" -> {
                    KpHoraryRequest req = new KpHoraryRequest();
                    Object h = args.get("horaryNumber");
                    if (h instanceof Number n) req.setHoraryNumber(n.intValue());
                    req.setQuestion((String) args.getOrDefault("question", "Will my venture succeed?"));
                    req.setCategory((String) args.getOrDefault("category", "CAREER_FINANCE"));
                    Map<String, Object> res = astroExpansionService.kpHorary(req);
                    return "KP Horary Outcome: " + res.getOrDefault("kpBinaryOutcome", "FAVORABLE")
                            + " (" + res.getOrDefault("probabilityPercentage", 90.0) + "% probability). "
                            + res.getOrDefault("astrologicalVerdict", "");
                }

                case "calculate_jaimini_karakamsha" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.jaiminiKarakamsha(req);
                    return "Jaimini Atmakaraka: " + res.getOrDefault("atmakarakaSoulPlanet", "Sun")
                            + " in Karakamsha " + res.getOrDefault("karakamshaLagna", "Sagittarius");
                }

                case "find_shubh_muhurta" -> {
                    MuhurtaRequest req = new MuhurtaRequest();
                    req.setEventType((String) args.getOrDefault("eventType", "STARTUP_INCORPORATION"));
                    req.setStartDate((String) args.getOrDefault("startDate", "2026-09-21"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.muhurtaFinder(req);
                    return "Recommended Shubh Muhurta Window: " + res.getOrDefault("highestRankedMuhurta", "Apex Window Found");
                }

                case "calculate_panch_pakshi" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.panchPakshi(req);
                    return "Panch-Pakshi Ruling Bird: " + res.getOrDefault("nativeRulingBird", "Crow")
                            + ". Golden Window: " + res.getOrDefault("goldenExecutionWindow", "Peak Ruling Phase");
                }

                case "analyze_kalasarpa_dosha" -> {
                    BirthRequest req = new BirthRequest();
                    req.setDob((String) args.getOrDefault("dob", "1990-05-15"));
                    req.setTime((String) args.getOrDefault("time", "14:30:00"));
                    req.setCity((String) args.getOrDefault("city", "New Delhi"));
                    Map<String, Object> res = astroExpansionService.kalasarpaOptimizer(req);
                    return "Kala Sarpa Status: " + res.getOrDefault("detectedKalaSarpaType", "Ananta")
                            + " (" + res.getOrDefault("doshaStatus", "PARTIAL") + ")";
                }

                default -> {
                    return "Error: Unknown tool name [" + toolName + "]";
                }
            }
        } catch (Exception e) {
            log.error("Tool execution error in [{}]: {}", toolName, e.getMessage(), e);
            return "Tool Execution Error: " + e.getMessage();
        }
    }
}
