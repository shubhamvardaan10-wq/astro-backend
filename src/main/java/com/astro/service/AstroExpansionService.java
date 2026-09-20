package com.astro.service;

import com.astro.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * AstroExpansionService
 *
 * Coordinates execution of the 5 killer features:
 *  1. Dynamic SVG/Vector Chart Visualizer (North Indian, South Indian, Western 360°)
 *  2. 36-Guna Kundli Milan Matchmaking & Synastry
 *  3. Real-Time Daily "Gochar" (Transits) & 4-Sector Energy Score
 *  4. Vedic Remedial Prescription Engine (Gemstones, Rudraksha, Mantras, Daan)
 *  5. Prashna Kundli (Horary Instant Query Astrology)
 */
@Service
public class AstroExpansionService {

    private static final Logger log = LoggerFactory.getLogger(AstroExpansionService.class);

    private final VedicService vedicService;
    private final WesternService westernService;
    private final CityService cityService;
    private final ObjectMapper objectMapper;
    private final String pythonExecutable;

    public AstroExpansionService(
            VedicService vedicService,
            @org.springframework.beans.factory.annotation.Autowired(required = false) WesternService westernService,
            CityService cityService,
            ObjectMapper objectMapper,
            @Value("${astro.python.executable:}") String customPython) {
        this.vedicService = vedicService;
        this.westernService = westernService != null ? westernService : new WesternService(cityService);
        this.cityService = cityService;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        Path venvPython = Paths.get("target/engine-venv/bin/python").toAbsolutePath();
        if (customPython != null && !customPython.isBlank() && Files.exists(Paths.get(customPython))) {
            this.pythonExecutable = customPython;
        } else if (Files.exists(venvPython)) {
            this.pythonExecutable = venvPython.toString();
        } else {
            this.pythonExecutable = "/usr/bin/python3";
        }
        log.info("Initialized AstroExpansionService with Python: {}", this.pythonExecutable);
    }

    // ── 1. Dynamic SVG Chart Visualizer ───────────────────────────────────────
    public Map<String, Object> generateChartsSvg(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "vedic", buildVedicPayload(chart)
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from chart_svg import generate_all_charts; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_all_charts(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("SVG chart generation error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate SVG charts: " + e.getMessage(), e);
        }
    }

    // ── 2. 36-Guna Kundli Milan Matchmaking ───────────────────────────────────
    public Map<String, Object> matchmaking(MatchmakingRequest req) {
        VedicChartResponse c1 = vedicService.compute(req.getPartner1());
        VedicChartResponse c2 = vedicService.compute(req.getPartner2());

        try {
            Map<String, Object> payload = Map.of(
                "chart1", Map.of("vedic", buildVedicPayload(c1)),
                "chart2", Map.of("vedic", buildVedicPayload(c2))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from matchmaking_engine import full_matchmaking; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(full_matchmaking(data['chart1'], data['chart2'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Matchmaking computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute matchmaking: " + e.getMessage(), e);
        }
    }

    // ── 3. Real-Time Daily "Gochar" & Energy Score ────────────────────────────
    public Map<String, Object> dailyHoroscope(DailyHoroscopeRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "targetDate", req.getTargetDate() != null ? req.getTargetDate() : ""
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; from datetime import datetime; sys.path.insert(0, 'worker'); " +
                    "from daily_transit_engine import get_daily_horoscope_report; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "t_date = datetime.strptime(data['targetDate'], '%Y-%m-%d') if data.get('targetDate') else None; " +
                    "print(json.dumps(get_daily_horoscope_report(data['natal'], t_date)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Daily horoscope computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute daily horoscope: " + e.getMessage(), e);
        }
    }

    // ── 4. Vedic Remedial Prescription Engine ─────────────────────────────────
    public Map<String, Object> remedies(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from remedies_engine import compute_remedies; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_remedies(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Remedial computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute remedies: " + e.getMessage(), e);
        }
    }

    // ── 5. Prashna Kundli (Horary) ────────────────────────────────────────────
    public Map<String, Object> prashna(PrashnaRequest req) {
        try {
            double lat = req.getLatitude() != null ? req.getLatitude() : 25.6858;
            double lon = req.getLongitude() != null ? req.getLongitude() : 85.2146;
            String question = req.getQuestion() != null ? req.getQuestion() : "General Question";

            Map<String, Object> payload = Map.of(
                "question", question,
                "latitude", lat,
                "longitude", lon
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from prashna_engine import compute_prashna; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_prashna(data['question'], data['latitude'], data['longitude'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Prashna computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute Prashna chart: " + e.getMessage(), e);
        }
    }

    // ── 6. Astro-Cartography & Relocation Engine ──────────────────────────────
    public Map<String, Object> relocation(BirthRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "dob", req.getDob(),
                "time", req.getTime(),
                "city", req.getCity()
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from relocation_engine import compute_relocation_report; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_relocation_report(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Relocation engine error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute relocation report: " + e.getMessage(), e);
        }
    }

    // ── 7. 30-Year Destiny & Wealth Trajectory ────────────────────────────────
    public Map<String, Object> destinyCurve(BirthRequest req) {
        try {
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from destiny_curve_engine import compute_destiny_curve; " +
                    "print(json.dumps(compute_destiny_curve(2026, 30)))";

            String resultJson = executePythonScript(script, "{}");
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Destiny curve error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute destiny curve: " + e.getMessage(), e);
        }
    }

    // ── 8. Karmic Debt & D60 Past-Life Decoder ────────────────────────────────
    public Map<String, Object> karmicDebts(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from karmic_debt_engine import evaluate_karmic_debts; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(evaluate_karmic_debts(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Karmic debt error: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate karmic debts: " + e.getMessage(), e);
        }
    }

    // ── 9. Vedic Baby Name & Nama-Karan Generator ─────────────────────────────
    public Map<String, Object> namakaran(BirthRequest req, String gender) {
        try {
            Map<String, Object> payload = Map.of(
                "dob", req.getDob(),
                "time", req.getTime(),
                "gender", gender != null ? gender : "unspecified"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from namakaran_engine import compute_namakaran; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_namakaran(data['dob'], data['time'], data['gender'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Namakaran error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Nama-Karan: " + e.getMessage(), e);
        }
    }

    // ── 10. "Cosmic Market Pulse" — Financial & Crypto Astro-Sentiment ────────
    public Map<String, Object> marketPulse(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from market_pulse_engine import compute_personal_market_pulse; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_personal_market_pulse(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Market pulse error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute market pulse: " + e.getMessage(), e);
        }
    }

    // ── 11. Conversational Astrologer Webhook Adapter ─────────────────────────
    public Map<String, Object> webhookChat(String message, String sender) {
        try {
            Map<String, Object> payload = Map.of(
                "message", message != null ? message : "Hello",
                "sender", sender != null ? sender : "user"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from conversational_bot import format_bot_response; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(format_bot_response(data['message'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Webhook chat error: {}", e.getMessage());
            throw new RuntimeException("Failed to process conversational query: " + e.getMessage(), e);
        }
    }

    // ── 12. Medical Astrology & Ayurvedic Dosha Diagnostics ──────────────────
    public Map<String, Object> medical(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from medical_astrology_engine import compute_medical_profile; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_medical_profile(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Medical astrology error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute medical astrology profile: " + e.getMessage(), e);
        }
    }

    // ── 13. Astrological Ikigai & Career Dharma Matrix ────────────────────────
    public Map<String, Object> careerIkigai(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from career_ikigai_engine import compute_career_ikigai; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_career_ikigai(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Career Ikigai error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute Career Ikigai profile: " + e.getMessage(), e);
        }
    }

    // ── 14. Auspicious Life Event Muhurta Scheduler ───────────────────────────
    public Map<String, Object> eventMuhurta(EventMuhurtaRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "eventType", req.getEventType() != null ? req.getEventType() : "marriage",
                "daysAhead", req.getDaysAhead() != null ? req.getDaysAhead() : 90,
                "maxResults", req.getMaxResults() != null ? req.getMaxResults() : 6
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from event_muhurta_engine import find_top_muhurtas; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(find_top_muhurtas(data['eventType'], data['daysAhead'], data['maxResults'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Event muhurta error: {}", e.getMessage());
            throw new RuntimeException("Failed to scan event muhurtas: " + e.getMessage(), e);
        }
    }

    // ── 15. Numerology & Cosmic Name-Tuning Engine ────────────────────────────
    public Map<String, Object> numerology(NumerologyRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "name", req.getFullName() != null ? req.getFullName() : "User",
                "dob", req.getDob() != null ? req.getDob() : "1990-01-01"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from numerology_engine import compute_numerology; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_numerology(data['name'], data['dob'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Numerology error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute numerology profile: " + e.getMessage(), e);
        }
    }

    // ── 16. Astro-Vastu Directional Energy Grid ───────────────────────────────
    public Map<String, Object> vastu(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from vastu_engine import compute_vastu_guidance; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_vastu_guidance(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Vastu engine error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute Vastu guidance: " + e.getMessage(), e);
        }
    }

    // ── 17. Automated Morning Astro-Briefing Generator ─────────────────────────
    public Map<String, Object> dailyDigest(DailyHoroscopeRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            if (req.getTargetDate() != null && !req.getTargetDate().isBlank()) {
                payload.put("targetDate", req.getTargetDate());
            } else {
                payload.put("targetDate", null);
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from daily_digest_engine import generate_daily_digest; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_daily_digest(data['natal'], data.get('targetDate'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Daily digest error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate daily digest: " + e.getMessage(), e);
        }
    }

    // ── 18. AI Palmistry & Hastarekha Shastra Future Predictor ───────────────
    public Map<String, Object> palmistryPredict(PalmistryRequest req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("handImageBase64", req.getHandImageBase64() != null ? req.getHandImageBase64() : "");
            payload.put("handType", req.getHandType() != null ? req.getHandType() : "right");
            payload.put("gender", req.getGender() != null ? req.getGender() : "male");
            payload.put("currentAge", req.getCurrentAge() != null ? req.getCurrentAge() : 30);
            if (req.getBirthDetails() != null) {
                payload.put("birthData", Map.of(
                    "dob", req.getBirthDetails().getDob() != null ? req.getBirthDetails().getDob() : "",
                    "time", req.getBirthDetails().getTime() != null ? req.getBirthDetails().getTime() : "",
                    "city", req.getBirthDetails().getCity() != null ? req.getBirthDetails().getCity() : ""
                ));
            } else {
                payload.put("birthData", null);
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from palmistry_vision_engine import analyze_palm_image; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(analyze_palm_image(data['handImageBase64'], data['handType'], data['gender'], data['currentAge'], data.get('birthData'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Palmistry prediction error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate palmistry future prediction: " + e.getMessage(), e);
        }
    }

    // ── 19. AI Face Reading & Vedic Physiognomy (Samudrika Mukh-Lakshana) ────
    public Map<String, Object> faceReading(FaceReadingRequest req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("faceImageBase64", req.getFaceImageBase64() != null ? req.getFaceImageBase64() : "");
            payload.put("gender", req.getGender() != null ? req.getGender() : "male");
            if (req.getBirthDetails() != null) {
                payload.put("birthData", Map.of(
                    "dob", req.getBirthDetails().getDob() != null ? req.getBirthDetails().getDob() : "",
                    "time", req.getBirthDetails().getTime() != null ? req.getBirthDetails().getTime() : "",
                    "city", req.getBirthDetails().getCity() != null ? req.getBirthDetails().getCity() : ""
                ));
            } else {
                payload.put("birthData", null);
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from face_reading_engine import analyze_face_mukh_lakshana; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(analyze_face_mukh_lakshana(data['faceImageBase64'], data['gender'], data.get('birthData'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Face reading error: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze face reading: " + e.getMessage(), e);
        }
    }

    // ── 20. Acoustic Voice & Planetary Aura Analyzer ─────────────────────────
    public Map<String, Object> voiceAura(VoiceAuraRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "audioBase64", req.getAudioBase64() != null ? req.getAudioBase64() : "",
                "speakerName", req.getSpeakerName() != null ? req.getSpeakerName() : "The Native",
                "gender", req.getGender() != null ? req.getGender() : "male"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from voice_aura_engine import analyze_vocal_acoustics; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(analyze_vocal_acoustics(data['audioBase64'], data['speakerName'], data['gender'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Voice aura error: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze voice aura: " + e.getMessage(), e);
        }
    }

    // ── 21. Astro-Swapna Shastra & Dream Decoding Engine ─────────────────────
    public Map<String, Object> dreamDecode(DreamDecodeRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "dreamText", req.getDreamText() != null ? req.getDreamText() : "Spiritual journey",
                "dreamDate", req.getDreamDate() != null ? req.getDreamDate() : "",
                "prahar", req.getPrahar() != null ? req.getPrahar() : "brahma_muhurta"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from dream_decode_engine import decode_dream_swapna_shastra; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(decode_dream_swapna_shastra(data['dreamText'], data.get('dreamDate'), data.get('prahar'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Dream decode error: {}", e.getMessage());
            throw new RuntimeException("Failed to decode dream: " + e.getMessage(), e);
        }
    }

    // ── 22. Planetary Binaural Frequency & Soundscape Generator ──────────────
    public Map<String, Object> soundTherapy(SoundTherapyRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "targetPlanet", req.getTargetPlanet() != null ? req.getTargetPlanet() : "Jupiter",
                "purpose", req.getPurpose() != null ? req.getPurpose() : "wealth_meditation",
                "durationSeconds", req.getDurationSeconds() != null ? req.getDurationSeconds() : 10
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from sound_therapy_engine import generate_planetary_soundscape; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_planetary_soundscape(data['targetPlanet'], data['purpose'], data['durationSeconds'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Sound therapy error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate planetary soundscape: " + e.getMessage(), e);
        }
    }

    // ── 23. Corporate Boardroom & Co-Founder Astro-Matrix ────────────────────
    public Map<String, Object> teamSynergy(TeamSynergyRequest req) {
        try {
            List<Map<String, String>> membersPayload = new ArrayList<>();
            if (req != null && req.getMembers() != null) {
                for (TeamSynergyRequest.TeamMember m : req.getMembers()) {
                    membersPayload.add(Map.of(
                        "name", m.getName() != null ? m.getName() : "Executive",
                        "role", m.getRole() != null ? m.getRole() : "Leader",
                        "dob", m.getDob() != null ? m.getDob() : "1990-01-01"
                    ));
                }
            }
            Map<String, Object> payload = Map.of("members", membersPayload);
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from team_synergy_engine import evaluate_team_synergy; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(evaluate_team_synergy(data['members'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Team synergy error: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate team synergy: " + e.getMessage(), e);
        }
    }

    // ── 24. Bio-Field Aura & 7-Chakra Energy Scanner ─────────────────────────
    public Map<String, Object> auraChakra(AuraChakraRequest req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("personImageBase64", req.getPersonImageBase64() != null ? req.getPersonImageBase64() : "");
            payload.put("subjectName", req.getSubjectName() != null ? req.getSubjectName() : "The Native");
            if (req.getBirthDetails() != null) {
                payload.put("birthData", Map.of(
                    "dob", req.getBirthDetails().getDob() != null ? req.getBirthDetails().getDob() : "",
                    "time", req.getBirthDetails().getTime() != null ? req.getBirthDetails().getTime() : "",
                    "city", req.getBirthDetails().getCity() != null ? req.getBirthDetails().getCity() : ""
                ));
            } else {
                payload.put("birthData", null);
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from aura_chakra_engine import scan_biofield_and_chakras; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(scan_biofield_and_chakras(data['personImageBase64'], data['subjectName'], data.get('birthData'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Aura chakra scan error: {}", e.getMessage());
            throw new RuntimeException("Failed to scan biofield and chakras: " + e.getMessage(), e);
        }
    }

    // ── 25. Bhrigu Nandi Nadi & Palm Leaf Destiny Decoder ────────────────────
    public Map<String, Object> nadi(BirthRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "dob", req.getDob() != null ? req.getDob() : "1989-10-30",
                "time", req.getTime() != null ? req.getTime() : "12:00"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from nadi_astrology_engine import compute_nadi_destiny; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_nadi_destiny(data['dob'], data['time'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Nadi prediction error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute Nadi destiny transcript: " + e.getMessage(), e);
        }
    }

    // ── 26. Vedic Time Machine & Past Life Verification Mode ──────────────────
    public Map<String, Object> lifeVerification(BirthRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "dob", req.getDob() != null ? req.getDob() : "1989-10-30",
                "time", req.getTime() != null ? req.getTime() : "12:00"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from life_verification_engine import compute_life_verification; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_life_verification(data['dob'], data['time'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Life verification error: {}", e.getMessage());
            throw new RuntimeException("Failed to verify life milestones: " + e.getMessage(), e);
        }
    }

    // ── 27. 7-Generation Ancestral Karma & Pitra Lineage Engine ──────────────
    public Map<String, Object> ancestralKarma(BirthRequest req) {
        try {
            Map<String, Object> payload = Map.of(
                "dob", req.getDob() != null ? req.getDob() : "1989-10-30",
                "time", req.getTime() != null ? req.getTime() : "12:00"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from ancestral_karma_engine import analyze_ancestral_karma; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(analyze_ancestral_karma(data['dob'], data['time'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Ancestral karma error: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze ancestral karma: " + e.getMessage(), e);
        }
    }

    // ── 28. Real-Time Transit Alarms & Proactive Webhooks ─────────────────────
    public Map<String, Object> transitAlerts(TransitAlertsRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            if (req.getTargetDate() != null && !req.getTargetDate().isBlank()) {
                payload.put("targetDate", req.getTargetDate());
            } else {
                payload.put("targetDate", null);
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from transit_alerts_engine import generate_transit_alerts; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_transit_alerts(data['natal'], data.get('targetDate'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Transit alerts error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate transit alerts: " + e.getMessage(), e);
        }
    }

    // ── 29. Precision Astro-Gemology & Crystal Yantra Grid ────────────────────
    public Map<String, Object> gemology(GemologyRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "bodyWeightKg", req.getBodyWeightKg() != null ? req.getBodyWeightKg() : 70.0
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from gemology_engine import compute_gemology_profile; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(compute_gemology_profile(data['natal'], data['bodyWeightKg'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Gemology calculation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute gemology profile: " + e.getMessage(), e);
        }
    }

    // ── 30. Real-Time Conversational Rishi Voice Agent ────────────────────────
    public Map<String, Object> voiceAgent(VoiceAgentRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "queryText", req.getUserQuery() != null ? req.getUserQuery() : "What does my future hold?",
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "voiceName", req.getVoiceName() != null ? req.getVoiceName() : "Acharya Antigravity"
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from voice_agent_engine import converse_with_rishi; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(converse_with_rishi(data['queryText'], data['natal'], data['voiceName'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Voice agent consultation error: {}", e.getMessage());
            throw new RuntimeException("Failed to conduct voice agent consultation: " + e.getMessage(), e);
        }
    }

    // ── 32. Astrocartography & Relocation Matrix ───────────────────────────────
    public Map<String, Object> astrocartography(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from astrocartography_engine import calculate_relocation_matrix; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_relocation_matrix(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Astrocartography computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate astrocartography relocation matrix: " + e.getMessage(), e);
        }
    }

    // ── 33. Financial Astrology & Algorithmic Market Timing ───────────────────
    public Map<String, Object> financialTiming(FinancialTimingRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            if (req.getTargetDate() != null) {
                payload.put("targetDate", req.getTargetDate());
            }
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from financial_timing_engine import calculate_financial_timing; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_financial_timing(data['natal'], data.get('targetDate'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Financial timing computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute financial market timing: " + e.getMessage(), e);
        }
    }

    // ── 34. Ayur-Jyotish: Medical Astrology & Tridosha Constitution ───────────
    public Map<String, Object> ayurJyotish(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from ayur_jyotish_engine import calculate_ayur_jyotish; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_ayur_jyotish(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Ayur-Jyotish computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to compute Ayur-Jyotish diagnostics: " + e.getMessage(), e);
        }
    }

    // ── 35. Sarvatobhadra Chakra & 28-Nakshatra Vedha Engine ───────────────────
    public Map<String, Object> sarvatobhadra(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from sarvatobhadra_engine import calculate_sarvatobhadra_vedha; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_sarvatobhadra_vedha(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Sarvatobhadra Vedha computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Sarvatobhadra Vedha rays: " + e.getMessage(), e);
        }
    }

    // ── 36. Karmic Soul-Tie Network & Multi-Person Graph ──────────────────────
    public Map<String, Object> soulGraph(SoulGraphRequest req) {
        try {
            String inputJson = objectMapper.writeValueAsString(req);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from soul_graph_engine import build_soul_graph; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(build_soul_graph(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Soul graph computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to build soul graph network: " + e.getMessage(), e);
        }
    }

    // ── 37. Krishnamurti Paddhati (KP System) & Prashna Horary Engine ─────────
    public Map<String, Object> kpHorary(KpHoraryRequest req) {
        try {
            String inputJson = objectMapper.writeValueAsString(req);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from kp_horary_engine import calculate_kp_horary; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_kp_horary(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("KP Horary computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate KP Prashna horary outcome: " + e.getMessage(), e);
        }
    }

    // ── 38. Maharishi Jaimini Chara Karakas & Karakamsha Matrix ───────────────
    public Map<String, Object> jaiminiKarakamsha(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from jaimini_karakamsha_engine import calculate_jaimini_karakamsha; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_jaimini_karakamsha(data['natal'])))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Jaimini Karakamsha computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Jaimini Karakamsha matrix: " + e.getMessage(), e);
        }
    }

    // ── 39. Vedic Electional Astrology: Shubh Muhurta Finder ──────────────────
    public Map<String, Object> muhurtaFinder(MuhurtaRequest req) {
        try {
            String inputJson = objectMapper.writeValueAsString(req);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from muhurta_finder_engine import calculate_shubh_muhurta; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_shubh_muhurta(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Shubh Muhurta finder error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Shubh Muhurta windows: " + e.getMessage(), e);
        }
    }

    // ── 40. Tamil Siddhar Panch-Pakshi Chronobiology Engine ───────────────────
    public Map<String, Object> panchPakshi(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from panch_pakshi_engine import calculate_panch_pakshi; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_panch_pakshi(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Panch-Pakshi computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Panch-Pakshi diurnal rhythms: " + e.getMessage(), e);
        }
    }

    // ── 41. Kala Sarpa & 12-Nodal Axis Dosha Neutralizer ──────────────────────
    public Map<String, Object> kalasarpaOptimizer(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from kalasarpa_optimizer_engine import calculate_kalasarpa_dosha; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_kalasarpa_dosha(data)))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Kala Sarpa optimizer error: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze Kala Sarpa dosha: " + e.getMessage(), e);
        }
    }

    // ── 42. Tajika Nilakanthi Varshaphala (Solar Return) Engine ──────────────
    public Map<String, Object> varshaphala(VarshaphalaRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            payload.put("targetYear", req.getTargetYear() != null ? req.getTargetYear() : 2026);
            payload.put("dob", req.getDob());
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from varshaphala_engine import calculate_varshaphala; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_varshaphala(data['natal'], data.get('targetYear'), data.get('dob'))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Varshaphala computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Varshaphala solar return: " + e.getMessage(), e);
        }
    }

    // ── 43. Western Synastry, Midpoint Composite & Davison Engine ─────────────
    public Map<String, Object> synastryComposite(SynastryCompositeRequest req) {
        VedicChartResponse c1 = vedicService.compute(req.getPartner1());
        VedicChartResponse c2 = vedicService.compute(req.getPartner2());

        try {
            Map<String, Object> payload = Map.of(
                "chart1", Map.of("vedic", buildVedicPayload(c1)),
                "chart2", Map.of("vedic", buildVedicPayload(c2)),
                "relationshipType", req.getRelationshipType() != null ? req.getRelationshipType() : "ROMANTIC",
                "orbTolerance", req.getOrbTolerance() != null ? req.getOrbTolerance() : 6.0
            );
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from synastry_composite_engine import calculate_synastry_and_composite; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_synastry_and_composite(data['chart1'], data['chart2'], data.get('relationshipType', 'ROMANTIC'), float(data.get('orbTolerance', 6.0)))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Synastry and Composite computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate synastry and composite chart: " + e.getMessage(), e);
        }
    }

    // ── 44. Vedic Birth Time Rectification (BTR) Assistant Engine ─────────────
    public Map<String, Object> birthTimeRectification(BirthTimeRectificationRequest req) {
        VedicChartResponse chart = vedicService.compute(req);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart), "time", req.getTime()));
            payload.put("uncertaintyMinutes", req.getUncertaintyMinutes() != null ? req.getUncertaintyMinutes() : 30);
            payload.put("stepMinutes", req.getStepMinutes() != null ? req.getStepMinutes() : 2);
            payload.put("gender", req.getGender() != null ? req.getGender() : "MALE");
            payload.put("lifeEvents", req.getLifeEvents());
            String inputJson = objectMapper.writeValueAsString(payload);

            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from btr_engine import rectify_birth_time; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(rectify_birth_time(data['natal'], int(data.get('uncertaintyMinutes', 30)), int(data.get('stepMinutes', 2)), data.get('gender', 'MALE'), data.get('lifeEvents', []))))";

            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Birth Time Rectification computation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate birth time rectification: " + e.getMessage(), e);
        }
    }

    // ── 45. Saturn Sade Sati, Dhaiya & Kantaka Shani Engine ────────────────────
    public Map<String, Object> sadeSatiTimeline(SadeSatiRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "targetYearsAhead", req.getTargetYearsAhead() != null ? req.getTargetYearsAhead() : 30
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from sade_sati_engine import calculate_sade_sati; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_sade_sati(data['natal'], int(data.get('targetYearsAhead', 30)))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Sade Sati timeline error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Sade Sati timeline: " + e.getMessage(), e);
        }
    }

    // ── 46. Personal Astrological Calendar (.ics / CalDAV Feed) ───────────────
    public Map<String, Object> calendarFeed(CalendarFeedRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "targetYear", req.getTargetYear() != null ? req.getTargetYear() : 2026
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from calendar_feed_engine import generate_ics_calendar; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_ics_calendar(data['natal'], int(data.get('targetYear', 2026)))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Calendar feed error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate astrological calendar feed: " + e.getMessage(), e);
        }
    }

    // ── 47. Real-Time Planetary Clock & Sky Live Stream ───────────────────────
    public Map<String, Object> planetaryClock(PlanetaryClockRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("latitude", req.getLatitude() != null ? req.getLatitude() : 28.6139);
            payload.put("longitude", req.getLongitude() != null ? req.getLongitude() : 77.2090);
            if (req.getTimestamp() != null) payload.put("timestamp", req.getTimestamp());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from planetary_clock_engine import calculate_planetary_clock; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_planetary_clock(float(data['latitude']), float(data['longitude']), data.get('timestamp'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Planetary clock error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate real-time planetary clock: " + e.getMessage(), e);
        }
    }

    // ── 48. Western Secondary Progressions & Solar Arc Directions ──────────────
    public Map<String, Object> progressionsDirections(ProgressionsDirectionsRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart), "dob", req.getDob()));
            if (req.getTargetDate() != null) payload.put("targetDate", req.getTargetDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from progressions_directions_engine import calculate_progressions_and_directions; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_progressions_and_directions(data['natal'], data.get('targetDate'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Progressions and directions error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate secondary progressions and solar arc: " + e.getMessage(), e);
        }
    }

    // ── 49. Ashtakavarga Transit Heatmap & Kaksha Precision Engine ─────────────
    public Map<String, Object> ashtakavargaKaksha(AshtakavargaKakshaRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            if (req.getTransitDate() != null) payload.put("transitDate", req.getTransitDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from ashtakavarga_kaksha_engine import calculate_kaksha_transits; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_kaksha_transits(data['natal'], data.get('transitDate'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Ashtakavarga kaksha error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Ashtakavarga kaksha transits: " + e.getMessage(), e);
        }
    }

    // ── 50. Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations ───────
    public Map<String, Object> bhriguNandiNadi(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from bhrigu_nandi_nadi_engine import calculate_bnn; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_bnn(data['natal'])))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Bhrigu Nandi Nadi error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Bhrigu Nandi Nadi alignments: " + e.getMessage(), e);
        }
    }

    // ── 51. Astrocartography GeoJSON Vector Line Generator ─────────────────────
    public Map<String, Object> astrocartographyGeoJson(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from astrocartography_geojson_engine import generate_astrocartography_geojson; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_astrocartography_geojson(data['natal'])))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Astrocartography GeoJSON error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate astrocartography GeoJSON: " + e.getMessage(), e);
        }
    }

    // ── 52. WhatsApp & Telegram Bot Messaging Gateway ──────────────────────────
    public Map<String, Object> messagingWebhook(MessagingWebhookRequest req) {
        try {
            BirthRequest bReq = new BirthRequest();
            bReq.setDob(req.getDob() != null ? req.getDob() : "1990-05-15");
            bReq.setTime(req.getTime() != null ? req.getTime() : "14:30:00");
            bReq.setCity(req.getCity() != null ? req.getCity() : "New Delhi");
            VedicChartResponse chart = vedicService.compute(bReq);

            String platform = req.getPlatform() != null ? req.getPlatform().toUpperCase() : "WHATSAPP";
            String sender = req.getSenderId() != null ? req.getSenderId() : "anon-user";
            String text = req.getMessageText() != null ? req.getMessageText() : "How is my day?";

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("gateway", "Omnichannel Astrological Messaging Webhook");
            resp.put("platform", platform);
            resp.put("senderId", sender);
            resp.put("incomingQuery", text);
            String lagnaSign = (chart.getLagna() != null ? chart.getLagna().sign() : "Sagittarius");
            String moonSign = "Leo";
            if (chart.getPlanets() != null) {
                for (PlanetPosition p : chart.getPlanets()) {
                    if ("Moon".equalsIgnoreCase(p.getName())) {
                        moonSign = p.getRashiName();
                        break;
                    }
                }
            }
            resp.put("botReply", "Namaste! Your current Lagna is " + lagnaSign
                + " and your Moon resides in " + moonSign
                + ". Cosmic indicators suggest high intellectual focus and commercial momentum.");
            resp.put("sessionStatus", "ACTIVE_SESSION");
            resp.put("suggestedQuickActions", List.of("Daily Horoscope", "Check Current Dasha", "Auspicious Muhurta Today"));
            return resp;
        } catch (Exception e) {
            log.error("Messaging webhook error: {}", e.getMessage());
            throw new RuntimeException("Failed to process messaging webhook: " + e.getMessage(), e);
        }
    }

    // ── 53. Sacred Vedic Baby Namakaran & Phonetic Tuning ─────────────────────
    public Map<String, Object> namakaranTuning(NamakaranTuningRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "gender", req.getGender() != null ? req.getGender() : "MALE",
                "preferredCategory", req.getPreferredCategory() != null ? req.getPreferredCategory() : "SANSKRIT"
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from namakaran_tuning_engine import calculate_namakaran_tuning; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_namakaran_tuning(data['natal'], data.get('gender', 'MALE'), data.get('preferredCategory', 'SANSKRIT'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Namakaran tuning error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Namakaran phonetic tuning: " + e.getMessage(), e);
        }
    }

    // ── 54. Native Multilingual Astrological Synthesis (i18n) ─────────────────
    public Map<String, Object> multilingualReport(MultilingualReportRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart)),
                "targetLanguage", req.getTargetLanguage() != null ? req.getTargetLanguage() : "hi"
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from multilingual_report_engine import generate_multilingual_report; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_multilingual_report(data['natal'], data.get('targetLanguage', 'hi'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Multilingual report error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate multilingual report: " + e.getMessage(), e);
        }
    }

    // ── 55. Classical Lal Kitab Kundli, Karmic Debts & Upayas ─────────────────
    public Map<String, Object> lalKitab(BirthRequest req) {
        VedicChartResponse chart = vedicService.compute(req);
        try {
            Map<String, Object> payload = Map.of(
                "natal", Map.of("vedic", buildVedicPayload(chart))
            );
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from lal_kitab_engine import calculate_lal_kitab; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_lal_kitab(data['natal'])))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Lal Kitab calculation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Lal Kitab Kundli: " + e.getMessage(), e);
        }
    }

    // ── 56. Full-Fledged Panchangam & Hindu Festival Engine ───────────────────
    public Map<String, Object> panchangam(PanchangamRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("latitude", req.getLatitude() != null ? req.getLatitude() : 28.6139);
            payload.put("longitude", req.getLongitude() != null ? req.getLongitude() : 77.2090);
            if (req.getDate() != null) payload.put("date", req.getDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from panchangam_engine import calculate_panchangam; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_panchangam(float(data['latitude']), float(data['longitude']), data.get('date'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Panchangam calculation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Panchangam: " + e.getMessage(), e);
        }
    }

    // ── 57. Deep Manglik & Nadi Dosha Cancellation Matrix ─────────────────────
    public Map<String, Object> doshaCancellation(DoshaCancellationRequest req) {
        VedicChartResponse c1 = vedicService.compute(req.getPartner1());
        VedicChartResponse c2 = req.getPartner2() != null ? vedicService.compute(req.getPartner2()) : null;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("partner1", Map.of("vedic", buildVedicPayload(c1)));
            if (c2 != null) {
                payload.put("partner2", Map.of("vedic", buildVedicPayload(c2)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from dosha_cancellation_engine import evaluate_dosha_cancellation; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(evaluate_dosha_cancellation(data['partner1'], data.get('partner2'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Dosha cancellation error: {}", e.getMessage());
            throw new RuntimeException("Failed to evaluate dosha cancellations: " + e.getMessage(), e);
        }
    }

    // ── 58. Astro-Financial & W.D. Gann Square of 9 Timing ────────────────────
    public Map<String, Object> marketGann(MarketGannRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("symbol", req.getSymbol() != null ? req.getSymbol() : "BTC");
            if (req.getTargetDate() != null) payload.put("targetDate", req.getTargetDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from market_gann_engine import calculate_market_gann; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_market_gann(data.get('symbol', 'BTC'), data.get('targetDate'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Market Gann calculation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Market Gann timing: " + e.getMessage(), e);
        }
    }

    // ── 59. Famous & Historic Horoscopes Search ───────────────────────────────
    public Map<String, Object> famousCharts(FamousChartsRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("query", req.getQuery() != null ? req.getQuery() : "");
            payload.put("category", req.getCategory() != null ? req.getCategory() : "ALL");
            payload.put("yoga", req.getYoga() != null ? req.getYoga() : "");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from famous_charts_engine import search_famous_charts; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(search_famous_charts(data.get('query',''), data.get('category','ALL'), data.get('yoga',''))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Famous charts search error: {}", e.getMessage());
            throw new RuntimeException("Failed to search famous charts: " + e.getMessage(), e);
        }
    }

    // ── 60. White-Label Enterprise Report Designer & Customizer ───────────────
    public Map<String, Object> reportCustomizer(ReportCustomizerRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("brandName", req.getBrandName() != null ? req.getBrandName() : "Jyotish Shastra Institute");
            payload.put("theme", req.getTheme() != null ? req.getTheme() : "ROYAL_GOLD");
            payload.put("astrologerTitle", req.getAstrologerTitle() != null ? req.getAstrologerTitle() : "Acharya Shastry");
            payload.put("watermark", req.getWatermark() != null ? req.getWatermark() : "CONFIDENTIAL");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from report_customizer_engine import customize_report; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(customize_report(data.get('brandName'), data.get('theme'), data.get('astrologerTitle'), data.get('watermark'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Report customizer error: {}", e.getMessage());
            throw new RuntimeException("Failed to customize report branding: " + e.getMessage(), e);
        }
    }

    // ── 61. Classical Vedic & KP Horary Astrological Engine ───────────────────
    public Map<String, Object> prashnaHorary(PrashnaHoraryRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("questionType", req.getQuestionType() != null ? req.getQuestionType() : "CAREER");
            if (req.getKpSeed() != null) payload.put("kpSeed", req.getKpSeed());
            if (req.getQueryDatetime() != null) payload.put("queryDatetime", req.getQueryDatetime());
            payload.put("queryText", req.getQueryText() != null ? req.getQueryText() : "Will my venture succeed?");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from prashna_horary_engine import calculate_prashna; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_prashna(data.get('questionType','CAREER'), data.get('kpSeed'), data.get('queryDatetime'), data.get('queryText'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Prashna horary error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Prashna horary: " + e.getMessage(), e);
        }
    }

    // ── 62. Sarvatobhadra Chakra (SBC) & 4-Directional Vedha Engine ───────────
    public Map<String, Object> sarvatobhadraChakra(SarvatobhadraRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                VedicChartResponse chart = vedicService.compute(req.getNatal());
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            if (req.getTransitDate() != null) payload.put("transitDate", req.getTransitDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from sarvatobhadra_engine import calculate_sarvatobhadra; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_sarvatobhadra(data.get('natal'), data.get('transitDate'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Sarvatobhadra error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Sarvatobhadra Chakra: " + e.getMessage(), e);
        }
    }

    // ── 63. Medical Astrology & Ayur-Jyotish Tridosha Engine ──────────────────
    public Map<String, Object> medicalAyurveda(MedicalAyurvedaRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                VedicChartResponse chart = vedicService.compute(req.getNatal());
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from medical_ayurveda_engine import analyze_medical_astrology; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(analyze_medical_astrology(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Medical Ayurveda error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Medical Ayurveda profile: " + e.getMessage(), e);
        }
    }

    // ── 64. Navatara Chakra & Daily Tara Bala / Chandra Bala Engine ───────────
    public Map<String, Object> taraBalaCalendar(TaraBalaRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                VedicChartResponse chart = vedicService.compute(req.getNatal());
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            payload.put("targetMonth", req.getTargetMonth() != null ? req.getTargetMonth() : "2026-10");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from tara_bala_engine import calculate_tara_bala; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_tara_bala(data.get('natal'), data.get('targetMonth','2026-10'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Tara Bala error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Tara Bala calendar: " + e.getMessage(), e);
        }
    }

    // ── 65. Classical Vedic Muhurta & Electional Timing Assistant ─────────────
    public Map<String, Object> electionalMuhurta(MuhurtaRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("activityType", req.getActivityType() != null ? req.getActivityType() : "BUSINESS");
            if (req.getStartDate() != null) payload.put("startDate", req.getStartDate());
            payload.put("durationDays", req.getDurationDays() != null ? req.getDurationDays() : 7);
            payload.put("latitude", req.getLatitude() != null ? req.getLatitude() : 28.6139);
            payload.put("longitude", req.getLongitude() != null ? req.getLongitude() : 77.2090);
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from muhurta_engine import find_muhurta; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(find_muhurta(data.get('activityType','BUSINESS'), data.get('startDate'), int(data.get('durationDays',7)), float(data.get('latitude',28.6139)), float(data.get('longitude',77.2090)))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Electional Muhurta error: {}", e.getMessage());
            throw new RuntimeException("Failed to find auspicious Muhurta windows: " + e.getMessage(), e);
        }
    }

    // ── 66. Western Solar & Lunar Return Precision Engine ─────────────────────
    public Map<String, Object> solarLunarReturn(SolarLunarReturnRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                WesternChartResponse west = westernService.compute(req.getNatal());
                Map<String, Object> westMap = new LinkedHashMap<>();
                if (west.getAscendant() != null) {
                    westMap.put("ascendant", Map.of("longitude", west.getAscendant().degree()));
                }
                Map<String, Object> pMap = new LinkedHashMap<>();
                if (west.getPlanets() != null) {
                    for (PlanetPosition p : west.getPlanets()) {
                        pMap.put(p.getName(), Map.of("longitude", p.getTropicalLongitude()));
                    }
                }
                westMap.put("planets", pMap);
                payload.put("natal", Map.of("western", westMap));
            }
            payload.put("returnYear", req.getReturnYear() != null ? req.getReturnYear() : 2026);
            payload.put("returnType", req.getReturnType() != null ? req.getReturnType() : "SOLAR");
            payload.put("currentCity", req.getCurrentCity() != null ? req.getCurrentCity() : "New Delhi");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from solar_lunar_return_engine import calculate_solar_lunar_return; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_solar_lunar_return(data.get('natal'), int(data.get('returnYear', 2026)), data.get('returnType','SOLAR'), data.get('currentCity','New Delhi'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Solar/Lunar return error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Solar/Lunar return: " + e.getMessage(), e);
        }
    }

    // ── 67. Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine ──────────
    public Map<String, Object> gemstoneRudraksha(GemstoneRudrakshaRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                VedicChartResponse chart = vedicService.compute(req.getNatal());
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from gemstone_rudraksha_engine import recommend_gemstones_and_rudraksha; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(recommend_gemstones_and_rudraksha(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Gemstone/Rudraksha error: {}", e.getMessage());
            throw new RuntimeException("Failed to recommend gemstones and rudraksha: " + e.getMessage(), e);
        }
    }

    // ── 68. Western Draconic Chart (Soul Purpose & Higher Self) Engine ────────
    public Map<String, Object> draconicChart(DraconicChartRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                WesternChartResponse west = westernService.compute(req.getNatal());
                Map<String, Object> westMap = new LinkedHashMap<>();
                if (west.getAscendant() != null) {
                    westMap.put("ascendant", Map.of("longitude", west.getAscendant().degree()));
                }
                Map<String, Object> pMap = new LinkedHashMap<>();
                if (west.getPlanets() != null) {
                    for (PlanetPosition p : west.getPlanets()) {
                        pMap.put(p.getName(), Map.of("longitude", p.getTropicalLongitude()));
                    }
                }
                westMap.put("planets", pMap);
                payload.put("natal", Map.of("western", westMap));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from draconic_chart_engine import calculate_draconic_chart; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_draconic_chart(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Draconic chart error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Draconic chart: " + e.getMessage(), e);
        }
    }

    // ── 69. Classical Vedic Kota Chakra (Fortress Chart) Engine ───────────────
    public Map<String, Object> kotaChakra(KotaChakraRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req.getNatal() != null) {
                VedicChartResponse chart = vedicService.compute(req.getNatal());
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            if (req.getTransitDate() != null) payload.put("transitDate", req.getTransitDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from kota_chakra_engine import calculate_kota_chakra; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_kota_chakra(data.get('natal'), data.get('transitDate'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Kota Chakra error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Kota Chakra: " + e.getMessage(), e);
        }
    }

    // ── 70. Classical Astro-Numerology & Lo Shu Magic Square Engine ───────────
    public Map<String, Object> loShuGrid(LoShuRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dob", req.getDob() != null ? req.getDob() : "1990-12-15");
            payload.put("gender", req.getGender() != null ? req.getGender() : "MALE");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from lo_shu_engine import calculate_lo_shu; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_lo_shu(data.get('dob','1990-12-15'), data.get('gender','MALE'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Lo Shu calculation error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Lo Shu grid: " + e.getMessage(), e);
        }
    }

    // ── 71. Unified Daily Cosmic Dashboard Widget Engine ──────────────────────
    public Map<String, Object> dailyWidget(DailyWidgetRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("latitude", req.getLatitude() != null ? req.getLatitude() : 28.6139);
            payload.put("longitude", req.getLongitude() != null ? req.getLongitude() : 77.2090);
            if (req.getDate() != null) payload.put("date", req.getDate());
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from daily_widget_engine import calculate_daily_widget; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_daily_widget(float(data['latitude']), float(data['longitude']), data.get('date'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Daily widget error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate daily widget summary: " + e.getMessage(), e);
        }
    }

    // ── 72. Jaimini Chara Dasha & Karakamsha Soul Blueprint Engine ────────────
    public Map<String, Object> jaiminiCharaDasha(BirthRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req != null) {
                VedicChartResponse chart = vedicService.compute(req);
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from jaimini_engine import calculate_jaimini_details; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_jaimini_details(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Jaimini Chara Dasha error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Jaimini Chara Dasha: " + e.getMessage(), e);
        }
    }

    // ── 73. KP Sub-Lord 4-Step Theory & Cuspal Significator Engine ───────────
    public Map<String, Object> kpSignificators(BirthRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req != null) {
                VedicChartResponse chart = vedicService.compute(req);
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from kp_engine import calculate_kp_significators; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_kp_significators(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("KP Significators error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate KP Significators: " + e.getMessage(), e);
        }
    }

    // ── 74. South Indian Dasa Koota (10-Porutham) Matching Engine ─────────────
    public Map<String, Object> dasaKoota(DasaKootaRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            Map<String, Object> p1 = new LinkedHashMap<>();
            Map<String, Object> p2 = new LinkedHashMap<>();

            if (req.getPartner1() != null) {
                VedicChartResponse c1 = vedicService.compute(req.getPartner1());
                PlanetPosition moon1 = c1.getPlanets().stream().filter(p -> "Moon".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
                if (moon1 != null) {
                    p1.put("nakshatra", moon1.getNakshatra());
                    p1.put("sign", moon1.getRashiName());
                }
            } else {
                p1.put("nakshatra", req.getGroomNakshatra() != null ? req.getGroomNakshatra() : "Rohini");
                p1.put("sign", req.getGroomSign() != null ? req.getGroomSign() : "Taurus");
            }

            if (req.getPartner2() != null) {
                VedicChartResponse c2 = vedicService.compute(req.getPartner2());
                PlanetPosition moon2 = c2.getPlanets().stream().filter(p -> "Moon".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
                if (moon2 != null) {
                    p2.put("nakshatra", moon2.getNakshatra());
                    p2.put("sign", moon2.getRashiName());
                }
            } else {
                p2.put("nakshatra", req.getBrideNakshatra() != null ? req.getBrideNakshatra() : "Anuradha");
                p2.put("sign", req.getBrideSign() != null ? req.getBrideSign() : "Scorpio");
            }

            payload.put("partner1", p1);
            payload.put("partner2", p2);
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from dasa_koota_engine import calculate_dasa_koota; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_dasa_koota(data.get('partner1'), data.get('partner2'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Dasa Koota error: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate Dasa Koota matching: " + e.getMessage(), e);
        }
    }

    // ── 75. Sacred Yantra Sacred Geometry & Planetary Mantra Frequency Engine ─
    public Map<String, Object> yantraMantra(YantraMantraRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("planet", req.getPlanet() != null ? req.getPlanet() : "SURYA");
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from yantra_mantra_engine import generate_yantra_mantra; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(generate_yantra_mantra(data.get('planet','SURYA'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Yantra Mantra error: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Yantra and Mantra: " + e.getMessage(), e);
        }
    }

    // ── 76. Astro-Genealogy & Pitru Dosha Karmic Lineage Analyzer ─────────────
    public Map<String, Object> ancestralLineage(BirthRequest req) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            if (req != null) {
                VedicChartResponse chart = vedicService.compute(req);
                payload.put("natal", Map.of("vedic", buildVedicPayload(chart)));
            }
            String inputJson = objectMapper.writeValueAsString(payload);
            String script = "import sys, json; sys.path.insert(0, 'worker'); " +
                    "from ancestral_lineage_engine import calculate_ancestral_lineage; " +
                    "data = json.loads(sys.stdin.read()); " +
                    "print(json.dumps(calculate_ancestral_lineage(data.get('natal'))))";
            String resultJson = executePythonScript(script, inputJson);
            return objectMapper.readValue(resultJson, Map.class);
        } catch (Exception e) {
            log.error("Ancestral Lineage error: {}", e.getMessage());
            throw new RuntimeException("Failed to analyze ancestral lineage and Pitru Dosha: " + e.getMessage(), e);
        }
    }

    // ── Helper: Execute Python script via pipe ────────────────────────────────
    private String executePythonScript(String pythonCode, String inputJson) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(pythonExecutable, "-c", pythonCode);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        if (inputJson != null) {
            process.getOutputStream().write(inputJson.getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().flush();
            process.getOutputStream().close();
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        boolean completed = process.waitFor(30, TimeUnit.SECONDS);

        if (!completed || process.exitValue() != 0) {
            throw new RuntimeException("Engine script failed (exit " + process.exitValue() + "): " + output);
        }
        return output.trim();
    }

    // ── Helper: Format VedicChartResponse into engine-compatible payload ─────
    private Map<String, Object> buildVedicPayload(VedicChartResponse chart) {
        Map<String, Object> ascMap = Map.of(
            "sign", chart.getLagna() != null ? chart.getLagna().sign() : "Sagittarius",
            "signIndex", chart.getLagna() != null ? chart.getLagna().rashi() : 8,
            "degree", chart.getLagna() != null ? chart.getLagna().degree() : 0.0,
            "longitude", chart.getLagna() != null ? (chart.getLagna().rashi() * 30.0 + chart.getLagna().degree()) : 247.8
        );

        Map<String, Object> planetsMap = new LinkedHashMap<>();
        if (chart.getPlanets() != null) {
            for (PlanetPosition p : chart.getPlanets()) {
                planetsMap.put(p.getName(), Map.of(
                    "sign", p.getRashiName(),
                    "signIndex", p.getRashi(),
                    "house", p.getHouse(),
                    "degreeInSign", p.getDegreeInSign(),
                    "longitude", p.getSiderealLongitude(),
                    "retrograde", p.isRetrograde()
                ));
            }
        }

        return Map.of(
            "ascendant", ascMap,
            "planets", planetsMap
        );
    }
}
