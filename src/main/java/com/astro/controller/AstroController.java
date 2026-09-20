package com.astro.controller;

import com.astro.model.*;
import com.astro.prediction.PredictionEngine;
import com.astro.prediction.PredictionResponse;
import com.astro.service.AstroExpansionService;
import com.astro.service.AstroSearchService;
import com.astro.service.CityService;
import com.astro.service.PdfExportService;
import com.astro.service.VedicService;
import com.astro.service.WesternService;
import com.astro.spouse.SpouseProfileResponse;
import com.astro.spouse.SpouseProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * REST API for astrological chart computation.
 *
 * All endpoints accept JSON body with: dob (YYYY-MM-DD), time (HH:mm or HH:mm:ss), city.
 * Local time is assumed to be IST (UTC+5:30) — the only Indian timezone.
 */
@RestController
@RequestMapping("/api/astro")
@CrossOrigin(origins = "*")
public class AstroController {

    private final VedicService          vedicService;
    private final WesternService        westernService;
    private final CityService           cityService;
    private final PredictionEngine      predictionEngine;
    private final SpouseProfileService  spouseProfileService;
    private final PdfExportService      pdfExportService;
    private final AstroSearchService    astroSearchService;
    private final AstroExpansionService astroExpansionService;
    private final com.astro.service.KundliVaultService kundliVaultService;
    private final com.astro.service.AstroChatService astroChatService;

    @Autowired
    public AstroController(VedicService vedicService, WesternService westernService,
                            CityService cityService, PredictionEngine predictionEngine,
                            SpouseProfileService spouseProfileService,
                            PdfExportService pdfExportService,
                            AstroSearchService astroSearchService,
                            AstroExpansionService astroExpansionService,
                            @Autowired(required = false) com.astro.service.KundliVaultService kundliVaultService,
                            @Autowired(required = false) com.astro.service.AstroChatService astroChatService) {
        this.vedicService           = vedicService;
        this.westernService         = westernService;
        this.cityService            = cityService;
        this.predictionEngine       = predictionEngine;
        this.spouseProfileService   = spouseProfileService;
        this.pdfExportService       = pdfExportService;
        this.astroSearchService     = astroSearchService;
        this.astroExpansionService  = astroExpansionService;
        this.kundliVaultService     = kundliVaultService != null ? kundliVaultService : new com.astro.service.KundliVaultService();
        this.astroChatService       = astroChatService != null ? astroChatService : new com.astro.service.AstroChatService(astroExpansionService, vedicService, null);
    }

    public AstroController(VedicService vedicService, WesternService westernService,
                            CityService cityService, PredictionEngine predictionEngine) {
        this(vedicService, westernService, cityService, predictionEngine, null, null, null, null, null, null);
    }

    // ─── Vedic chart ──────────────────────────────────────────────────────────
    /**
     * POST /api/astro/vedic-chart
     * Returns full Vedic (Jyotisha) birth chart: lagna, planets (sidereal),
     * whole-sign houses, Vimshottari Dasha (3 levels), yogas, Vedic aspects,
     * Ashtakavarga summary.
     *
     * Example body:
     * { "dob": "1990-01-15", "time": "14:30", "city": "Mumbai" }
     */
    @PostMapping("/vedic-chart")
    public ResponseEntity<VedicChartResponse> vedicChart(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(vedicService.compute(req));
    }

    // ─── Western chart ────────────────────────────────────────────────────────
    /**
     * POST /api/astro/western-chart
     * Returns Western tropical chart: ascendant, MC, planets (tropical),
     * equal-house cusps, Ptolemaic aspects.
     */
    @PostMapping("/western-chart")
    public ResponseEntity<WesternChartResponse> westernChart(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(westernService.compute(req));
    }

    // ─── Dasha only ───────────────────────────────────────────────────────────
    /**
     * POST /api/astro/dasha
     * Returns only the Vimshottari Dasha timeline (lighter response).
     */
    @PostMapping("/dasha")
    public ResponseEntity<DashaInfo> dasha(@Valid @RequestBody BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        return ResponseEntity.ok(chart.getDasha());
    }

    // ─── Nakshatra only ───────────────────────────────────────────────────────
    /**
     * POST /api/astro/nakshatra
     * Returns Moon nakshatra, pada, dasha lord and balance for the birth data.
     */
    @PostMapping("/nakshatra")
    public ResponseEntity<Map<String, Object>> nakshatra(@Valid @RequestBody BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        var moon = chart.getPlanets().stream()
            .filter(p -> "Moon".equals(p.getName()))
            .findFirst().orElseThrow();
        return ResponseEntity.ok(Map.of(
            "nakshatra",      moon.getNakshatraName(),
            "nakshatraIndex", moon.getNakshatra(),
            "pada",           moon.getPada(),
            "lord",           moon.getNakshatraLord(),
            "moonSiderealLon", moon.getSiderealLongitude(),
            "moonRashi",      moon.getRashiName(),
            "birthMahadasha", chart.getDasha().getBirthMahadasha()
        ));
    }

    // ─── Planetary positions only ─────────────────────────────────────────────
    /**
     * POST /api/astro/planets
     * Returns sidereal planet positions only (lighter response).
     */
    @PostMapping("/planets")
    public ResponseEntity<List<PlanetPosition>> planets(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(vedicService.compute(req).getPlanets());
    }

    // ─── Yogas only ───────────────────────────────────────────────────────────
    /**
     * POST /api/astro/yogas
     * Returns detected Vedic yogas.
     */
    @PostMapping("/yogas")
    public ResponseEntity<Map<String, Object>> yogas(@Valid @RequestBody BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        return ResponseEntity.ok(Map.of(
            "yogas", chart.getYogas(),
            "count", chart.getYogas().size()
        ));
    }

    // ─── Full ~4000-word prediction report ─────────────────────────────────
    /**
     * POST /api/astro/prediction
     * Returns a fully personalised ~4000-word Vedic prediction report covering:
     * rising sign, personality, birth nakshatra, every planet (sign + house + dignity),
     * house-by-house life analysis, career, finance, health, relationships,
     * Vimshottari Dasha timing (3 levels), yogas, and spiritual remedies.
     *
     * Example body: { "dob": "1990-01-15", "time": "14:30", "city": "Mumbai" }
     */
    @PostMapping("/prediction")
    public ResponseEntity<PredictionResponse> prediction(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(predictionEngine.generate(req));
    }

    // ─── Spouse Profile ───────────────────────────────────────────────────────
    /**
     * POST /api/astro/spouse-profile
     *
     * Returns a detailed astrological spouse profile including:
     *  - Physical appearance (skin tone, face shape, eyes, hair, lips, nose)
     *  - Body figure (height estimate, weight estimate, build, posture, gait)
     *  - Probable location & region of origin
     *  - Probable occupation & career fields
     *  - Personality traits & compatibility notes
     *  - Ready-to-use AI image prompts for ChatGPT, DALL-E 3, and MidJourney
     *
     * Example body: { "dob": "1989-10-30", "time": "10:10", "city": "Hajipur" }
     */
    @PostMapping("/spouse-profile")
    public ResponseEntity<SpouseProfileResponse> spouseProfile(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(spouseProfileService.generate(req));
    }

    // ─── City list ────────────────────────────────────────────────────────────
    /**
     * GET /api/astro/cities
     * Returns the list of supported Indian cities.
     */
    @GetMapping("/cities")
    public ResponseEntity<Map<String, Object>> cities() {
        List<String> names = cityService.listCities();
        return ResponseEntity.ok(Map.of(
            "cities", names,
            "count",  names.size(),
            "timezone", "Asia/Kolkata (IST = UTC+5:30)"
        ));
    }

    // ─── Automated PDF Export Service ─────────────────────────────────────────
    /**
     * POST /api/astro/export-pdf
     *
     * Converts a Markdown report into a publication-quality PDF on Desktop or specified path.
     * Supports:
     *   1. filePath: export existing .md file to .pdf
     *   2. scanDesktop: true -> automatically scan Desktop and batch-export all reports to PDF
     *   3. markdownContent + fileName: compile raw markdown text directly into a Desktop PDF
     */
    @PostMapping("/export-pdf")
    public ResponseEntity<Map<String, Object>> exportPdf(@RequestBody(required = false) PdfExportRequest req) {
        try {
            if (req != null && req.isScanDesktop()) {
                List<String> generated = pdfExportService.scanAndExportDesktopReports();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "action", "scanAndExportDesktop",
                    "generatedFiles", generated,
                    "count", generated.size(),
                    "message", "Desktop reports successfully exported to PDF"
                ));
            } else if (req != null && req.getFilePath() != null && !req.getFilePath().isBlank()) {
                String targetPdf = pdfExportService.exportFile(req.getFilePath(), null);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "action", "exportFile",
                    "inputFile", req.getFilePath(),
                    "generatedPdf", targetPdf,
                    "message", "Report exported to PDF successfully"
                ));
            } else if (req != null && req.getMarkdownContent() != null && !req.getMarkdownContent().isBlank()) {
                String fileName = (req.getFileName() != null && !req.getFileName().isBlank())
                        ? req.getFileName() : "astrology-report-" + System.currentTimeMillis();
                String targetPdf = pdfExportService.exportMarkdownContentToDesktop(req.getMarkdownContent(), fileName);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "action", "exportContent",
                    "generatedPdf", targetPdf,
                    "message", "Markdown content compiled and saved to Desktop as PDF"
                ));
            } else {
                // Default action: scan and export desktop reports
                List<String> generated = pdfExportService.scanAndExportDesktopReports();
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "action", "scanAndExportDesktop",
                    "generatedFiles", generated,
                    "count", generated.size(),
                    "message", "Desktop reports successfully scanned and exported to PDF"
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "PDF export error: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/astro/export-pdf/desktop
     *
     * Convenience endpoint: automatically scans Desktop for all astrological reports
     * and converts them to publication-grade PDFs.
     */
    @GetMapping("/export-pdf/desktop")
    public ResponseEntity<Map<String, Object>> exportDesktopPdfs() {
        try {
            List<String> generated = pdfExportService.scanAndExportDesktopReports();
            return ResponseEntity.ok(Map.of(
                "success", true,
                "generatedFiles", generated,
                "count", generated.size(),
                "message", "All Desktop astrological reports successfully converted to PDF"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Desktop PDF export failed: " + e.getMessage()
            ));
        }
    }

    // ─── Circuit Breakers & Resiliency Telemetry ──────────────────────────────
    /**
     * GET /api/astro/circuit-breakers
     *
     * Returns real-time health, circuit breaker states, and fallback metrics for
     * optional services (Elasticsearch, Redis, in-memory catalogs).
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, Object>> circuitBreakers() {
        Map<String, Object> esStatus = astroSearchService.getStatus();
        return ResponseEntity.ok(Map.of(
            "timestamp", System.currentTimeMillis(),
            "status", "HEALTHY",
            "elasticsearch", esStatus,
            "fallbackAvailable", true,
            "inMemoryCatalogReady", true
        ));
    }

    // ─── 1. Dynamic SVG/Vector Chart Visualizer ───────────────────────────────
    /**
     * POST /api/astro/chart-svg
     * Returns crisp, responsive SVG markup for:
     *  - North Indian Diamond Chart (traditional Hindi/Bihari style)
     *  - South Indian Square Box Chart (traditional South Indian style)
     *  - Western 360° Circular Wheel with zodiac glyphs and aspect chords
     */
    @PostMapping("/chart-svg")
    public ResponseEntity<Map<String, Object>> chartSvg(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.generateChartsSvg(req));
    }

    /**
     * GET /api/astro/chart-svg/view
     * Returns an interactive HTML browser view displaying all 3 charts side-by-side.
     */
    @GetMapping(value = "/chart-svg/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> chartSvgView(
            @RequestParam(defaultValue = "1989-10-30") String dob,
            @RequestParam(defaultValue = "10:10") String time,
            @RequestParam(defaultValue = "Hajipur") String city) {

        BirthRequest req = new BirthRequest();
        req.setDob(dob);
        req.setTime(time);
        req.setCity(city);

        Map<String, Object> svgs = astroExpansionService.generateChartsSvg(req);
        String north = (String) svgs.getOrDefault("northIndianSvg", "");
        String south = (String) svgs.getOrDefault("southIndianSvg", "");
        String west  = (String) svgs.getOrDefault("westernWheelSvg", "");

        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>Astrological Chart Visualizer — Astro Backend</title>
              <style>
                body { background: #060f1c; color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 24px; }
                h1 { text-align: center; color: #d4af37; margin-bottom: 6px; }
                p.sub { text-align: center; color: #94a3b8; font-size: 14px; margin-bottom: 28px; }
                .grid { display: flex; flex-wrap: wrap; gap: 24px; justify-content: center; max-width: 1400px; margin: 0 auto; }
                .card { background: #0f233a; border: 1px solid #1e3e62; border-radius: 12px; padding: 16px; box-shadow: 0 8px 24px rgba(0,0,0,0.5); width: 440px; }
                .card h2 { color: #d4af37; font-size: 16px; margin-top: 0; margin-bottom: 12px; border-bottom: 1px solid #1e3e62; padding-bottom: 6px; }
                .chart-container { width: 100%; height: 440px; }
              </style>
            </head>
            <body>
              <h1>Celestial Kundli & Wheel Visualizer</h1>
              <p class="sub">{{CITY}} • {{DOB}} • {{TIME}}</p>
              <div class="grid">
                <div class="card">
                  <h2>North Indian Diamond Chart</h2>
                  <div class="chart-container">{{NORTH}}</div>
                </div>
                <div class="card">
                  <h2>South Indian Box Chart</h2>
                  <div class="chart-container">{{SOUTH}}</div>
                </div>
                <div class="card">
                  <h2>Western 360° Circular Wheel</h2>
                  <div class="chart-container">{{WEST}}</div>
                </div>
              </div>
            </body>
            </html>
            """
            .replace("{{CITY}}", city)
            .replace("{{DOB}}", dob)
            .replace("{{TIME}}", time)
            .replace("{{NORTH}}", north)
            .replace("{{SOUTH}}", south)
            .replace("{{WEST}}", west);

        return ResponseEntity.ok(html);
    }

    // ─── 2. 36-Guna Kundli Milan Matchmaking ──────────────────────────────────
    /**
     * POST /api/astro/matchmaking
     * Computes classical Ashta-Kuta 36-Guna compatibility, Manglik Dosha evaluation
     * with cancellation rules, and multi-tradition compatibility score.
     */
    @PostMapping("/matchmaking")
    public ResponseEntity<Map<String, Object>> matchmaking(@Valid @RequestBody MatchmakingRequest req) {
        return ResponseEntity.ok(astroExpansionService.matchmaking(req));
    }

    // ─── 3. Real-Time Daily "Gochar" & Energy Score ────────────────────────────
    /**
     * POST /api/astro/daily-horoscope
     * Computes real-time planetary transits relative to natal chart, 4-sector energy scores
     * (Career, Wealth, Health, Romance: 0-100), Sade Sati status, and daily Muhurta windows.
     */
    @PostMapping("/daily-horoscope")
    public ResponseEntity<Map<String, Object>> dailyHoroscope(@Valid @RequestBody DailyHoroscopeRequest req) {
        return ResponseEntity.ok(astroExpansionService.dailyHoroscope(req));
    }

    // ─── 4. Vedic Remedial Prescription Engine ─────────────────────────────────
    /**
     * POST /api/astro/remedies
     * Returns mathematically derived Functional Benefics vs. Malefics for the Lagna,
     * gemstone prescriptions (Life, Fortune, Career stones with metal/finger/day),
     * Rudraksha recommendations, Vedic Beej Mantras, and Daan (charity) guidelines.
     */
    @PostMapping("/remedies")
    public ResponseEntity<Map<String, Object>> remedies(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.remedies(req));
    }

    // ─── 5. Prashna Kundli (Horary / Instant Query) ───────────────────────────
    /**
     * POST /api/astro/prashna
     * Casts an astronomical chart for the exact current moment and geographical coordinates.
     * Evaluates question domain, Lagnesh & Karyesh aspects, and outputs a deterministic
     * horary verdict (Yes / No / Delayed) with probability percentage and estimated timeline.
     */
    @PostMapping("/prashna")
    public ResponseEntity<Map<String, Object>> prashna(@Valid @RequestBody PrashnaRequest req) {
        return ResponseEntity.ok(astroExpansionService.prashna(req));
    }

    // ─── 6. Server-Sent Events (SSE) Streaming AI Report ───────────────────────
    /**
     * GET /api/astro/stream-report
     * Streams comprehensive astrological prediction sections token-by-token or section-by-section
     * in real-time using Server-Sent Events (SSE: text/event-stream).
     */
    @GetMapping(value = "/stream-report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReport(
            @RequestParam(defaultValue = "1989-10-30") String dob,
            @RequestParam(defaultValue = "10:10") String time,
            @RequestParam(defaultValue = "Hajipur") String city,
            @RequestParam(defaultValue = "Shubham Vardaan") String name) {

        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        Thread.ofVirtual().start(() -> {
            try {
                BirthRequest req = new BirthRequest();
                req.setDob(dob);
                req.setTime(time);
                req.setCity(city);

                emitter.send(SseEmitter.event().name("init").data(Map.of("message", "Initiating celestial calculations for " + name, "status", "CALCULATING")));

                PredictionResponse pred = predictionEngine.generate(req);

                // Stream general overview
                emitter.send(SseEmitter.event().name("overview").data(Map.of(
                    "estimatedWordCount", pred.getEstimatedWordCount(),
                    "birthSummary", pred.getBirthSummary() != null ? pred.getBirthSummary() : ""
                )));

                // Stream each prediction section sequentially
                if (pred.getSections() != null) {
                    for (Map.Entry<String, String> entry : pred.getSections().entrySet()) {
                        emitter.send(SseEmitter.event().name("section").data(Map.of(
                            "section", entry.getKey(),
                            "content", entry.getValue()
                        )));
                        Thread.sleep(80); // Smooth typewriter cadence
                    }
                }

                emitter.send(SseEmitter.event().name("complete").data(Map.of("status", "DONE", "message", "Master astrological report streamed successfully")));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // ─── 7. Astro-Cartography & Relocation Engine ─────────────────────────────
    /**
     * POST /api/astro/relocation
     * Calculates relocated Ascendant, MC, and planetary house positions across 20+ global cities.
     * Identifies top worldwide hubs for Wealth, Career Fame, Romance, and Healing.
     */
    @PostMapping("/relocation")
    public ResponseEntity<Map<String, Object>> relocation(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.relocation(req));
    }

    // ─── 8. 30-Year Destiny & Wealth Trajectory ───────────────────────────────
    /**
     * POST /api/astro/destiny-curve
     * Generates continuous year-by-year time-series indices (0-100) for Fortune, Wealth,
     * Career, and Health from 2026 to 2056 with milestone event flags.
     */
    @PostMapping("/destiny-curve")
    public ResponseEntity<Map<String, Object>> destinyCurve(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.destinyCurve(req));
    }

    // ─── 9. Karmic Debt & D60 Past-Life Decoder ───────────────────────────────
    /**
     * POST /api/astro/karmic-debts
     * Analyzes Jaimini Atmakaraka, Karakamsha, and classical Vedic Rinas (Pitra, Matru,
     * Guru, Sarpa Dosha) to reveal past-life karma and moral dissolution duties.
     */
    @PostMapping("/karmic-debts")
    public ResponseEntity<Map<String, Object>> karmicDebts(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.karmicDebts(req));
    }

    // ─── 10. Vedic Baby Name & Nama-Karan Generator ───────────────────────────
    /**
     * POST /api/astro/baby-namakaran
     * Computes Moon Nakshatra & Pada to determine the exact 4 auspicious starting phonetics
     * (Aksharas) and returns a catalog of matching modern Sanskrit names with meanings.
     */
    @PostMapping("/baby-namakaran")
    public ResponseEntity<Map<String, Object>> babyNamakaran(
            @Valid @RequestBody BirthRequest req,
            @RequestParam(required = false, defaultValue = "unspecified") String gender) {
        return ResponseEntity.ok(astroExpansionService.namakaran(req, gender));
    }

    // ─── 11. "Cosmic Market Pulse" — Financial Astro-Sentiment ────────────────
    /**
     * POST /api/astro/market-pulse
     * Macro-planetary financial sentiment index, intraday tech/crypto volatility score,
     * bullion trends, and personalized trading favorability based on natal houses.
     */
    @PostMapping("/market-pulse")
    public ResponseEntity<Map<String, Object>> marketPulse(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.marketPulse(req));
    }

    // ─── 12. Conversational Astrologer Webhook Adapter ────────────────────────
    /**
     * POST /api/astro/webhook/chat
     * Natural language conversational endpoint for WhatsApp, Telegram, or Webchat bots.
     * Automatically extracts intent, computes astrological answers, and formats responses.
     */
    @PostMapping("/webhook/chat")
    public ResponseEntity<Map<String, Object>> webhookChat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "Hello");
        String sender = body.getOrDefault("sender", "user");
        return ResponseEntity.ok(astroExpansionService.webhookChat(message, sender));
    }

    // ─── 13. Medical Astrology & Ayurvedic Dosha Diagnostics ──────────────────
    /**
     * POST /api/astro/medical
     * Tridosha balance (Vata, Pitta, Kapha), organ vulnerabilities, and Ayurvedic rasayanas.
     */
    @PostMapping("/medical")
    public ResponseEntity<Map<String, Object>> medical(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.medical(req));
    }

    // ─── 14. Astrological Ikigai & Career Dharma Matrix ────────────────────────
    /**
     * POST /api/astro/career-ikigai
     * Synthesizes 1st, 2nd, 6th, 9th, 10th, 11th houses into 4 Ikigai pillars & founder archetypes.
     */
    @PostMapping("/career-ikigai")
    public ResponseEntity<Map<String, Object>> careerIkigai(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.careerIkigai(req));
    }

    // ─── 15. Auspicious Life Event Muhurta Scheduler ───────────────────────────
    /**
     * POST /api/astro/event-muhurta
     * Scans upcoming 1-12 months for Marriage, Property, Business, Vehicle, or Medical muhurtas.
     */
    @PostMapping("/event-muhurta")
    public ResponseEntity<Map<String, Object>> eventMuhurta(@RequestBody(required = false) EventMuhurtaRequest req) {
        if (req == null) {
            req = new EventMuhurtaRequest();
        }
        return ResponseEntity.ok(astroExpansionService.eventMuhurta(req));
    }

    // ─── 16. Numerology & Cosmic Name-Tuning Engine ────────────────────────────
    /**
     * POST /api/astro/numerology-tuning
     * Computes Life Path, Pythagorean & Chaldean Destiny, Soul Urge, and name harmonic tuning.
     */
    @PostMapping("/numerology-tuning")
    public ResponseEntity<Map<String, Object>> numerologyTuning(@RequestBody NumerologyRequest req) {
        return ResponseEntity.ok(astroExpansionService.numerology(req));
    }

    // ─── 17. Astro-Vastu Directional Energy Grid ───────────────────────────────
    /**
     * POST /api/astro/vastu
     * Generates personalized 8-directional Vastu grid, workstation alignment, and wealth sector activation.
     */
    @PostMapping("/vastu")
    public ResponseEntity<Map<String, Object>> vastu(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.vastu(req));
    }

    // ─── 18. Automated Morning Astro-Briefing Generator ─────────────────────────
    /**
     * POST /api/astro/daily-digest
     * Personalized executive 60-second morning briefing with Golden Hour, Rahu Kaal, push text & HTML card.
     */
    @PostMapping("/daily-digest")
    public ResponseEntity<Map<String, Object>> dailyDigest(@Valid @RequestBody DailyHoroscopeRequest req) {
        return ResponseEntity.ok(astroExpansionService.dailyDigest(req));
    }

    // ─── 19. AI Palmistry & Hastarekha Shastra Future Predictor ───────────────
    /**
     * POST /api/astro/palmistry/predict
     *
     * Processes hand photo, extracts 5 major crease channels, 7 planetary mounts,
     * sacred markings (Mystic Cross, Money Triangle, Fish Sign), and generates a
     * comprehensive 5,000+ word multi-chapter future timeline dossier + annotated image.
     */
    @PostMapping("/palmistry/predict")
    public ResponseEntity<Map<String, Object>> palmistryPredict(@RequestBody(required = false) PalmistryRequest req) {
        if (req == null) {
            req = new PalmistryRequest();
        }
        return ResponseEntity.ok(astroExpansionService.palmistryPredict(req));
    }

    // ─── 20. AI Face Reading & Vedic Physiognomy ──────────────────────────────
    @PostMapping("/face-reading")
    public ResponseEntity<Map<String, Object>> faceReading(@RequestBody(required = false) FaceReadingRequest req) {
        if (req == null) {
            req = new FaceReadingRequest();
        }
        return ResponseEntity.ok(astroExpansionService.faceReading(req));
    }

    // ─── 21. Acoustic Voice & Planetary Aura Analyzer ─────────────────────────
    @PostMapping("/voice-aura")
    public ResponseEntity<Map<String, Object>> voiceAura(@RequestBody(required = false) VoiceAuraRequest req) {
        if (req == null) {
            req = new VoiceAuraRequest();
        }
        return ResponseEntity.ok(astroExpansionService.voiceAura(req));
    }

    // ─── 22. Astro-Swapna Shastra & Dream Decoding Engine ─────────────────────
    @PostMapping("/dream-decode")
    public ResponseEntity<Map<String, Object>> dreamDecode(@Valid @RequestBody DreamDecodeRequest req) {
        return ResponseEntity.ok(astroExpansionService.dreamDecode(req));
    }

    // ─── 23. Planetary Binaural Frequency & Soundscape Generator ──────────────
    @PostMapping("/sound-therapy")
    public ResponseEntity<Map<String, Object>> soundTherapy(@RequestBody(required = false) SoundTherapyRequest req) {
        if (req == null) {
            req = new SoundTherapyRequest();
        }
        return ResponseEntity.ok(astroExpansionService.soundTherapy(req));
    }

    // ─── 24. Corporate Boardroom & Co-Founder Astro-Matrix ────────────────────
    @PostMapping("/team-synergy")
    public ResponseEntity<Map<String, Object>> teamSynergy(@RequestBody(required = false) TeamSynergyRequest req) {
        if (req == null) {
            req = new TeamSynergyRequest();
        }
        return ResponseEntity.ok(astroExpansionService.teamSynergy(req));
    }

    // ─── 25. Bio-Field Aura & 7-Chakra Energy Scanner ─────────────────────────
    @PostMapping("/aura-chakra")
    public ResponseEntity<Map<String, Object>> auraChakra(@RequestBody(required = false) AuraChakraRequest req) {
        if (req == null) {
            req = new AuraChakraRequest();
        }
        return ResponseEntity.ok(astroExpansionService.auraChakra(req));
    }

    // ─── 26. Bhrigu Nandi Nadi & Palm Leaf Destiny Decoder ────────────────────
    @PostMapping("/nadi")
    public ResponseEntity<Map<String, Object>> nadi(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.nadi(req));
    }

    // ─── 27. Vedic Time Machine & Past Life Verification Mode ──────────────────
    @PostMapping("/life-verification")
    public ResponseEntity<Map<String, Object>> lifeVerification(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.lifeVerification(req));
    }

    // ─── 28. 7-Generation Ancestral Karma & Pitra Lineage Engine ──────────────
    @PostMapping("/ancestral-karma")
    public ResponseEntity<Map<String, Object>> ancestralKarma(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.ancestralKarma(req));
    }

    // ─── 29. Real-Time Transit Alarms & Proactive Webhooks ─────────────────────
    @PostMapping("/transit-alerts")
    public ResponseEntity<Map<String, Object>> transitAlerts(@Valid @RequestBody TransitAlertsRequest req) {
        return ResponseEntity.ok(astroExpansionService.transitAlerts(req));
    }

    // ─── 30. Precision Astro-Gemology & Crystal Yantra Grid ────────────────────
    @PostMapping("/gemology")
    public ResponseEntity<Map<String, Object>> gemology(@Valid @RequestBody GemologyRequest req) {
        return ResponseEntity.ok(astroExpansionService.gemology(req));
    }

    // ─── 31. Real-Time Conversational Rishi Voice Agent ────────────────────────
    @PostMapping("/voice-agent")
    public ResponseEntity<Map<String, Object>> voiceAgent(@Valid @RequestBody VoiceAgentRequest req) {
        return ResponseEntity.ok(astroExpansionService.voiceAgent(req));
    }

    // ─── 32. Astrocartography & Relocation Matrix ───────────────────────────────
    @PostMapping("/astrocartography")
    public ResponseEntity<Map<String, Object>> astrocartography(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.astrocartography(req));
    }

    // ─── 33. Financial Astrology & Algorithmic Market Timing ───────────────────
    @PostMapping("/financial-timing")
    public ResponseEntity<Map<String, Object>> financialTiming(@Valid @RequestBody FinancialTimingRequest req) {
        return ResponseEntity.ok(astroExpansionService.financialTiming(req));
    }

    // ─── 34. Ayur-Jyotish: Medical Astrology & Dosha Constitution ───────────────
    @PostMapping("/ayur-jyotish")
    public ResponseEntity<Map<String, Object>> ayurJyotish(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.ayurJyotish(req));
    }

    // ─── 35. Sarvatobhadra Chakra & 28-Nakshatra Vedha Engine ───────────────────
    @PostMapping("/sarvatobhadra")
    public ResponseEntity<Map<String, Object>> sarvatobhadra(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.sarvatobhadra(req));
    }

    // ─── 36. Karmic Soul-Tie Network & Multi-Person Graph ──────────────────────
    @PostMapping("/soul-graph")
    public ResponseEntity<Map<String, Object>> soulGraph(@Valid @RequestBody SoulGraphRequest req) {
        return ResponseEntity.ok(astroExpansionService.soulGraph(req));
    }

    // ─── 37. Krishnamurti Paddhati (KP System) & Prashna Horary Engine ─────────
    @PostMapping("/kp-horary")
    public ResponseEntity<Map<String, Object>> kpHorary(@Valid @RequestBody KpHoraryRequest req) {
        return ResponseEntity.ok(astroExpansionService.kpHorary(req));
    }

    // ─── 38. Maharishi Jaimini Chara Karakas & Karakamsha Matrix ───────────────
    @PostMapping("/jaimini-karakamsha")
    public ResponseEntity<Map<String, Object>> jaiminiKarakamsha(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.jaiminiKarakamsha(req));
    }

    // ─── 39. Vedic Electional Astrology: Shubh Muhurta Finder ──────────────────
    @PostMapping("/muhurta-finder")
    public ResponseEntity<Map<String, Object>> muhurtaFinder(@RequestBody MuhurtaRequest req) {
        return ResponseEntity.ok(astroExpansionService.muhurtaFinder(req));
    }

    // ─── 40. Tamil Siddhar Panch-Pakshi Chronobiology Engine ───────────────────
    @PostMapping("/panch-pakshi")
    public ResponseEntity<Map<String, Object>> panchPakshi(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.panchPakshi(req));
    }

    // ─── 41. Kala Sarpa & 12-Nodal Axis Dosha Neutralizer ──────────────────────
    @PostMapping("/kalasarpa-optimizer")
    public ResponseEntity<Map<String, Object>> kalasarpaOptimizer(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.kalasarpaOptimizer(req));
    }

    // ─── 42. Tajika Nilakanthi Varshaphala (Solar Return) Engine ──────────────
    @PostMapping("/varshaphala")
    public ResponseEntity<Map<String, Object>> varshaphala(@Valid @RequestBody VarshaphalaRequest req) {
        return ResponseEntity.ok(astroExpansionService.varshaphala(req));
    }

    // ─── 43. Western Synastry, Midpoint Composite & Davison Engine ─────────────
    @PostMapping("/synastry-composite")
    public ResponseEntity<Map<String, Object>> synastryComposite(@Valid @RequestBody SynastryCompositeRequest req) {
        return ResponseEntity.ok(astroExpansionService.synastryComposite(req));
    }

    // ─── 44. Vedic Birth Time Rectification (BTR) Assistant Engine ─────────────
    @PostMapping("/birth-time-rectification")
    public ResponseEntity<Map<String, Object>> birthTimeRectification(@Valid @RequestBody BirthTimeRectificationRequest req) {
        return ResponseEntity.ok(astroExpansionService.birthTimeRectification(req));
    }

    // ─── 45. Saturn Sade Sati, Dhaiya & Kantaka Shani Engine ────────────────────
    @PostMapping("/sade-sati-timeline")
    public ResponseEntity<Map<String, Object>> sadeSatiTimeline(@Valid @RequestBody SadeSatiRequest req) {
        return ResponseEntity.ok(astroExpansionService.sadeSatiTimeline(req));
    }

    // ─── 46. Personal Astrological Calendar (.ics / CalDAV Feed) ───────────────
    @PostMapping("/calendar-feed")
    public ResponseEntity<Map<String, Object>> calendarFeed(@Valid @RequestBody CalendarFeedRequest req) {
        return ResponseEntity.ok(astroExpansionService.calendarFeed(req));
    }

    // ─── 47. Real-Time Planetary Clock & Sky Live Stream ───────────────────────
    @PostMapping("/planetary-clock")
    public ResponseEntity<Map<String, Object>> planetaryClock(@RequestBody PlanetaryClockRequest req) {
        return ResponseEntity.ok(astroExpansionService.planetaryClock(req));
    }

    // ─── 48. Western Secondary Progressions & Solar Arc Directions ──────────────
    @PostMapping("/progressions-directions")
    public ResponseEntity<Map<String, Object>> progressionsDirections(@Valid @RequestBody ProgressionsDirectionsRequest req) {
        return ResponseEntity.ok(astroExpansionService.progressionsDirections(req));
    }

    // ─── 49. Ashtakavarga Transit Heatmap & Kaksha Precision Engine ─────────────
    @PostMapping("/ashtakavarga-kaksha")
    public ResponseEntity<Map<String, Object>> ashtakavargaKaksha(@Valid @RequestBody AshtakavargaKakshaRequest req) {
        return ResponseEntity.ok(astroExpansionService.ashtakavargaKaksha(req));
    }

    // ─── 50. Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations ───────
    @PostMapping("/bhrigu-nandi-nadi")
    public ResponseEntity<Map<String, Object>> bhriguNandiNadi(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.bhriguNandiNadi(req));
    }

    // ─── 51. Astrocartography GeoJSON Vector Line Generator ─────────────────────
    @PostMapping("/astrocartography/geojson")
    public ResponseEntity<Map<String, Object>> astrocartographyGeoJson(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.astrocartographyGeoJson(req));
    }

    // ─── 52. WhatsApp & Telegram Bot Messaging Gateway ──────────────────────────
    @PostMapping("/webhook/messaging")
    public ResponseEntity<Map<String, Object>> messagingWebhook(@RequestBody MessagingWebhookRequest req) {
        return ResponseEntity.ok(astroExpansionService.messagingWebhook(req));
    }

    // ─── 53. Sacred Vedic Baby Namakaran & Phonetic Tuning ─────────────────────
    @PostMapping("/namakaran-tuning")
    public ResponseEntity<Map<String, Object>> namakaranTuning(@Valid @RequestBody NamakaranTuningRequest req) {
        return ResponseEntity.ok(astroExpansionService.namakaranTuning(req));
    }

    // ─── 54. Native Multilingual Astrological Synthesis (i18n) ─────────────────
    @PostMapping("/multilingual-report")
    public ResponseEntity<Map<String, Object>> multilingualReport(@Valid @RequestBody MultilingualReportRequest req) {
        return ResponseEntity.ok(astroExpansionService.multilingualReport(req));
    }

    // ─── 55. Lal Kitab Kundli & Classical Remedial Upayas ──────────────────────
    @PostMapping("/lal-kitab")
    public ResponseEntity<Map<String, Object>> lalKitab(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.lalKitab(req));
    }

    // ─── 56. Full-Fledged Panchangam & Hindu Festival Engine ───────────────────
    @PostMapping("/panchangam")
    public ResponseEntity<Map<String, Object>> panchangam(@Valid @RequestBody PanchangamRequest req) {
        return ResponseEntity.ok(astroExpansionService.panchangam(req));
    }

    // ─── 57. Deep Manglik & Nadi Dosha Cancellation Matrix ─────────────────────
    @PostMapping("/dosha-cancellation")
    public ResponseEntity<Map<String, Object>> doshaCancellation(@Valid @RequestBody DoshaCancellationRequest req) {
        return ResponseEntity.ok(astroExpansionService.doshaCancellation(req));
    }

    // ─── 58. Astro-Financial & W.D. Gann Square of 9 Timing ────────────────────
    @PostMapping("/market-gann")
    public ResponseEntity<Map<String, Object>> marketGann(@Valid @RequestBody MarketGannRequest req) {
        return ResponseEntity.ok(astroExpansionService.marketGann(req));
    }

    // ─── 59. Famous & Historic Horoscopes Search ───────────────────────────────
    @PostMapping("/famous-horoscopes")
    public ResponseEntity<Map<String, Object>> famousHoroscopes(@Valid @RequestBody FamousChartsRequest req) {
        return ResponseEntity.ok(astroExpansionService.famousCharts(req));
    }

    // ─── 60. White-Label Enterprise Report Designer & Customizer ───────────────
    @PostMapping("/report-customizer")
    public ResponseEntity<Map<String, Object>> reportCustomizer(@Valid @RequestBody ReportCustomizerRequest req) {
        return ResponseEntity.ok(astroExpansionService.reportCustomizer(req));
    }

    // ─── 61. Kundli Profile Vault: Save Chart ──────────────────────────────────
    @PostMapping("/vault/save")
    public ResponseEntity<com.astro.service.KundliVaultService.VaultItem> saveVaultChart(@RequestBody Map<String, String> req) {
        String id = req.get("id");
        String name = req.get("name");
        String category = req.get("category");
        String dob = req.get("dob");
        String time = req.get("time");
        String city = req.get("city");
        return ResponseEntity.ok(kundliVaultService.saveChart(id, name, category, dob, time, city));
    }

    // ─── 62. Kundli Profile Vault: List All Charts ─────────────────────────────
    @GetMapping("/vault/list")
    public ResponseEntity<List<com.astro.service.KundliVaultService.VaultItem>> listVaultCharts() {
        return ResponseEntity.ok(kundliVaultService.listCharts());
    }

    // ─── 63. Kundli Profile Vault: Get Chart by ID ─────────────────────────────
    @GetMapping("/vault/{id}")
    public ResponseEntity<com.astro.service.KundliVaultService.VaultItem> getVaultChart(@PathVariable String id) {
        return kundliVaultService.getChart(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── 64. Kundli Profile Vault: Delete Chart by ID ──────────────────────────
    @DeleteMapping("/vault/{id}")
    public ResponseEntity<Map<String, Object>> deleteVaultChart(@PathVariable String id) {
        boolean deleted = kundliVaultService.deleteChart(id);
        return ResponseEntity.ok(Map.of("id", id, "deleted", deleted));
    }

    // ─── 65. Classical Vedic & KP Horary Astrological Engine ───────────────────
    @PostMapping("/prashna-horary")
    public ResponseEntity<Map<String, Object>> prashnaHorary(@Valid @RequestBody PrashnaHoraryRequest req) {
        return ResponseEntity.ok(astroExpansionService.prashnaHorary(req));
    }

    // ─── 66. Sarvatobhadra Chakra (SBC) & 4-Directional Vedha Engine ───────────
    @PostMapping("/sarvatobhadra-chakra")
    public ResponseEntity<Map<String, Object>> sarvatobhadraChakra(@Valid @RequestBody SarvatobhadraRequest req) {
        return ResponseEntity.ok(astroExpansionService.sarvatobhadraChakra(req));
    }

    // ─── 67. Medical Astrology & Ayur-Jyotish Tridosha Engine ──────────────────
    @PostMapping("/medical-ayurveda")
    public ResponseEntity<Map<String, Object>> medicalAyurveda(@Valid @RequestBody MedicalAyurvedaRequest req) {
        return ResponseEntity.ok(astroExpansionService.medicalAyurveda(req));
    }

    // ─── 68. Navatara Chakra & Daily Tara Bala / Chandra Bala Engine ───────────
    @PostMapping("/tara-bala-calendar")
    public ResponseEntity<Map<String, Object>> taraBalaCalendar(@Valid @RequestBody TaraBalaRequest req) {
        return ResponseEntity.ok(astroExpansionService.taraBalaCalendar(req));
    }

    // ─── 69. Classical Vedic Muhurta & Electional Timing Assistant ─────────────
    @PostMapping("/electional-muhurta")
    public ResponseEntity<Map<String, Object>> electionalMuhurta(@Valid @RequestBody MuhurtaRequest req) {
        return ResponseEntity.ok(astroExpansionService.electionalMuhurta(req));
    }

    // ─── 70. Western Solar & Lunar Return Precision Engine ─────────────────────
    @PostMapping("/solar-lunar-return")
    public ResponseEntity<Map<String, Object>> solarLunarReturn(@Valid @RequestBody SolarLunarReturnRequest req) {
        return ResponseEntity.ok(astroExpansionService.solarLunarReturn(req));
    }

    // ─── 71. Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine ──────────
    @PostMapping("/gemstone-rudraksha")
    public ResponseEntity<Map<String, Object>> gemstoneRudraksha(@Valid @RequestBody GemstoneRudrakshaRequest req) {
        return ResponseEntity.ok(astroExpansionService.gemstoneRudraksha(req));
    }

    // ─── 72. Western Draconic Chart (Soul Purpose & Higher Self) Engine ────────
    @PostMapping("/draconic-chart")
    public ResponseEntity<Map<String, Object>> draconicChart(@Valid @RequestBody DraconicChartRequest req) {
        return ResponseEntity.ok(astroExpansionService.draconicChart(req));
    }

    // ─── 73. Classical Vedic Kota Chakra (Fortress Chart) Engine ───────────────
    @PostMapping("/kota-chakra")
    public ResponseEntity<Map<String, Object>> kotaChakra(@Valid @RequestBody KotaChakraRequest req) {
        return ResponseEntity.ok(astroExpansionService.kotaChakra(req));
    }

    // ─── 74. Classical Astro-Numerology & Lo Shu Magic Square Engine ───────────
    @PostMapping("/lo-shu-grid")
    public ResponseEntity<Map<String, Object>> loShuGrid(@Valid @RequestBody LoShuRequest req) {
        return ResponseEntity.ok(astroExpansionService.loShuGrid(req));
    }

    // ─── 75. Unified Daily Cosmic Dashboard Widget Engine ──────────────────────
    @PostMapping("/widget/daily-summary")
    public ResponseEntity<Map<String, Object>> dailyWidget(@Valid @RequestBody DailyWidgetRequest req) {
        return ResponseEntity.ok(astroExpansionService.dailyWidget(req));
    }

    // ─── 72. Jaimini Chara Dasha & Karakamsha Soul Blueprint Engine ────────────
    @PostMapping("/jaimini-chara-dasha")
    public ResponseEntity<Map<String, Object>> jaiminiCharaDasha(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.jaiminiCharaDasha(req));
    }

    // ─── 73. KP Sub-Lord 4-Step Theory & Cuspal Significator Engine ───────────
    @PostMapping("/kp-significators")
    public ResponseEntity<Map<String, Object>> kpSignificators(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.kpSignificators(req));
    }

    // ─── 74. South Indian Dasa Koota (10-Porutham) Matching Engine ─────────────
    @PostMapping("/dasa-koota-matching")
    public ResponseEntity<Map<String, Object>> dasaKootaMatching(@Valid @RequestBody DasaKootaRequest req) {
        return ResponseEntity.ok(astroExpansionService.dasaKoota(req));
    }

    // ─── 75. Sacred Yantra Sacred Geometry & Planetary Mantra Frequency Engine ─
    @PostMapping("/remedies-yantra-mantra")
    public ResponseEntity<Map<String, Object>> yantraMantra(@RequestBody YantraMantraRequest req) {
        return ResponseEntity.ok(astroExpansionService.yantraMantra(req));
    }

    // ─── 76. Astro-Genealogy & Pitru Dosha Karmic Lineage Analyzer ─────────────
    @PostMapping("/ancestral-lineage")
    public ResponseEntity<Map<String, Object>> ancestralLineage(@Valid @RequestBody BirthRequest req) {
        return ResponseEntity.ok(astroExpansionService.ancestralLineage(req));
    }

    // ─── 77. Stateful Conversational Astrologer Chat with Session Memory ───────
    @PostMapping("/chat")
    public ResponseEntity<AstroChatResponse> chat(@Valid @RequestBody AstroChatRequest req) {
        return ResponseEntity.ok(astroChatService.chat(req));
    }

    @GetMapping("/chat/history/{sessionId}")
    public ResponseEntity<Map<String, Object>> chatHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "history", astroChatService.getHistory(sessionId)
        ));
    }

    @DeleteMapping("/chat/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> clearChatSession(@PathVariable String sessionId) {
        boolean cleared = astroChatService.clearSession(sessionId);
        return ResponseEntity.ok(Map.of(
            "sessionId", sessionId,
            "cleared", cleared
        ));
    }

    // ─── 78. High-Granularity Monthly Life Event Timing & Scoring Engine ───────
    @PostMapping("/timeline-forecast")
    public ResponseEntity<Map<String, Object>> timelineForecast(@Valid @RequestBody TimelineForecastRequest req) {
        return ResponseEntity.ok(astroExpansionService.timelineForecast(req));
    }

    // ─── Error handling ───────────────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Map<String, String>> handleInvalidRequest(Exception e) {
        String msg = "Invalid request: provide dob as YYYY-MM-DD, time as HH:mm or HH:mm:ss, and a supported city";
        if (e instanceof MethodArgumentNotValidException manve && manve.getBindingResult().getFieldError() != null) {
            msg = manve.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception e) {
        if (e instanceof ErrorResponse error && error.getStatusCode().is4xxClientError()) {
            return ResponseEntity.status(error.getStatusCode()).headers(error.getHeaders())
                .body(Map.of("error", HttpStatus.valueOf(error.getStatusCode().value()).getReasonPhrase()));
        }
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
