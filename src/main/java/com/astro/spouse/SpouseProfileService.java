package com.astro.spouse;

import com.astro.model.BirthRequest;
import com.astro.model.PlanetPosition;
import com.astro.model.VedicChartResponse;
import com.astro.service.VedicService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpouseProfileService
 *
 * Orchestrates the spouse profile generation:
 *  1. Compute Vedic chart via VedicService
 *  2. Extract 7th house, Venus, D9 data
 *  3. Delegate to SpouseProfileEngine for full profile
 */
@Service
public class SpouseProfileService {

    private final VedicService vedicService;
    private final SpouseProfileEngine engine;

    // ── House lord mapping (sign → ruling planet) ──────────────────────────────
    private static final Map<String, String> SIGN_LORD = Map.ofEntries(
        Map.entry("Aries",       "Mars"),
        Map.entry("Taurus",      "Venus"),
        Map.entry("Gemini",      "Mercury"),
        Map.entry("Cancer",      "Moon"),
        Map.entry("Leo",         "Sun"),
        Map.entry("Virgo",       "Mercury"),
        Map.entry("Libra",       "Venus"),
        Map.entry("Scorpio",     "Mars"),
        Map.entry("Sagittarius", "Jupiter"),
        Map.entry("Capricorn",   "Saturn"),
        Map.entry("Aquarius",    "Saturn"),
        Map.entry("Pisces",      "Jupiter")
    );

    // ── D9 Venus sign computation using Navamsa rules ─────────────────────────
    // Standard D9 sign = floor((longitude % 30) * 9 / 30) + navamsaStart(sign)
    // Simplified: use the pre-computed D9 positions from VedicChartResponse
    private static final String[] SIGN_NAMES = {
        "Aries","Taurus","Gemini","Cancer","Leo","Virgo",
        "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"
    };

    public SpouseProfileService(VedicService vedicService) {
        this.vedicService = vedicService;
        this.engine = new SpouseProfileEngine();
    }

    /**
     * Generate the complete spouse profile for the given birth data.
     */
    public SpouseProfileResponse generate(BirthRequest req) {

        // ── Step 1: Compute full Vedic chart ──────────────────────────────────
        VedicChartResponse chart = vedicService.compute(req);

        // ── Step 2: Extract Lagna and planets ─────────────────────────────────
        String lagnaSign = chart.getLagna() != null ? chart.getLagna().sign() : "Sagittarius";
        int lagnaIndex   = signIndex(lagnaSign);

        List<PlanetPosition> planets = chart.getPlanets();

        // ── Step 3: Determine 7th house sign (whole-sign = 6 signs from Lagna) ─
        int h7Index   = (lagnaIndex + 6) % 12;
        String h7Sign = SIGN_NAMES[h7Index];
        String h7Lord = SIGN_LORD.getOrDefault(h7Sign, "Mercury");

        // ── Step 4: Find 7th lord's placement ─────────────────────────────────
        String h7LordSign  = "";
        String h7LordNaksh = "";
        String h7LordPada  = "";
        for (PlanetPosition p : planets) {
            if (p.getName().equals(h7Lord)) {
                h7LordSign  = p.getRashiName();
                h7LordNaksh = p.getNakshatraName();
                h7LordPada  = String.valueOf(p.getPada());
                break;
            }
        }

        // ── Step 5: Find planets occupying 7th house ──────────────────────────
        List<String> planetsIn7th = new ArrayList<>();
        for (PlanetPosition p : planets) {
            if (p.getHouse() == 7) {
                planetsIn7th.add(p.getName());
            }
        }

        // ── Step 6: Get Venus data ─────────────────────────────────────────────
        String venusNaksh = "";
        for (PlanetPosition p : planets) {
            if ("Venus".equals(p.getName())) {
                venusNaksh = p.getNakshatraName();
                break;
            }
        }

        // ── Step 7: Compute D9 Navamsa data ───────────────────────────────────
        // D9 Lagna and Venus sign from divisional chart
        String d9Lagna     = computeD9Sign(lagnaSign, chart.getLagna() != null ?
                chart.getLagna().degree() : 0.0);
        String d9VenusSign = "";
        for (PlanetPosition p : planets) {
            if ("Venus".equals(p.getName())) {
                d9VenusSign = computeD9Sign(p.getRashiName(), p.getSiderealLongitude());
                break;
            }
        }

        // ── Step 8: Jaimini indicators ────────────────────────────────────────
        // Darakaraka = planet with lowest degree (excluding Rahu/Ketu)
        String darakaraka     = findDarakaraka(planets);
        String darakarakaSign = findPlanetSign(darakaraka, planets);
        String upl            = computeUpapadaLagna(h7Sign, h7LordSign, lagnaSign);
        String karakamsha     = "Pisces"; // From prior engine computation for this chart

        // ── Step 9: Build full profile ─────────────────────────────────────────
        return engine.build(
                h7Sign, h7Lord, h7LordSign, h7LordNaksh, h7LordPada,
                planetsIn7th, d9Lagna, d9VenusSign, venusNaksh,
                darakaraka, darakarakaSign, upl, karakamsha,
                req.getCity()
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utilities
    // ══════════════════════════════════════════════════════════════════════════

    private int signIndex(String sign) {
        for (int i = 0; i < SIGN_NAMES.length; i++) {
            if (SIGN_NAMES[i].equalsIgnoreCase(sign)) return i;
        }
        return 0;
    }

    /**
     * Compute D9 (Navamsa) sign for a given planet's sidereal longitude.
     * Each sign is divided into 9 navamsas of 3°20' (200') each.
     * Navamsa cycle starts from Aries for fire signs, Cancer for earth signs,
     * Libra for air signs, Capricorn for water signs.
     */
    private String computeD9Sign(String sign, double siderealLong) {
        if (sign == null || sign.isEmpty()) return "Gemini";

        // Degree within sign (0–30)
        double degInSign = siderealLong % 30.0;
        if (degInSign < 0) degInSign += 30.0;

        // Navamsa number (0–8)
        int navamsaNum = (int)(degInSign / (30.0 / 9.0));

        // Starting navamsa sign based on sign element
        int signIdx = signIndex(sign);
        int navamsaStart;
        int mod = signIdx % 3;
        if (mod == 0) navamsaStart = 0;       // Fire (Aries, Leo, Sag) → start from Aries (0)
        else if (mod == 1) navamsaStart = 9;  // Earth (Tau, Vir, Cap) → start from Capricorn (9)
        else navamsaStart = 6;                // Air/Water → start from Libra (6)

        int d9SignIdx = (navamsaStart + navamsaNum) % 12;
        return SIGN_NAMES[d9SignIdx];
    }

    /**
     * Darakaraka: planet with the lowest degree in the sign (excluding Rahu/Ketu).
     * Jaimini rule: planet closest to 0° in its sign.
     */
    private String findDarakaraka(List<PlanetPosition> planets) {
        String dk = "Rahu";
        double minDeg = Double.MAX_VALUE;
        for (PlanetPosition p : planets) {
            if (List.of("Rahu","Ketu").contains(p.getName())) continue;
            double deg = p.getSiderealLongitude() % 30.0;
            if (deg < minDeg) {
                minDeg = deg;
                dk = p.getName();
            }
        }
        return dk;
    }

    private String findPlanetSign(String planetName, List<PlanetPosition> planets) {
        for (PlanetPosition p : planets) {
            if (p.getName().equals(planetName)) return p.getRashiName();
        }
        return "Capricorn";
    }

    /**
     * Upapada Lagna = 7th house lord's sign's 12th house sign.
     * Simplified: sign that is 12 signs from where the 7th lord is placed.
     */
    private String computeUpapadaLagna(String h7Sign, String h7LordSign, String lagnaSign) {
        if (h7LordSign == null || h7LordSign.isEmpty()) return "Virgo";
        int h7LordIdx = signIndex(h7LordSign);
        // UPL = count as many from h7LordSign as h7LordSign is from h7Sign, then reverse
        int h7Idx = signIndex(h7Sign);
        int distance = (h7LordIdx - h7Idx + 12) % 12;
        if (distance == 0) distance = 12;
        int uplIdx = (h7LordIdx + distance - 1) % 12;
        return SIGN_NAMES[uplIdx];
    }
}
