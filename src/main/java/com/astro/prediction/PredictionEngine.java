package com.astro.prediction;

import com.astro.model.*;
import com.astro.service.VedicService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Assembles a ~4000-word personalised Vedic astrological prediction report
 * from the computed VedicChartResponse using classical interpretive texts.
 */
@Service
public class PredictionEngine {

    private static final String[] ORDINALS = {
        "", "First", "Second", "Third", "Fourth", "Fifth", "Sixth",
        "Seventh", "Eighth", "Ninth", "Tenth", "Eleventh", "Twelfth"
    };

    private final VedicService vedicService;
    private final Map<String, PredictionResponse> reportCache = Collections.synchronizedMap(
        new LinkedHashMap<>(64, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, PredictionResponse> eldest) {
                return size() > 1000;
            }
        }
    );

    public PredictionEngine(VedicService vedicService) {
        this.vedicService = vedicService;
    }

    public PredictionResponse generate(BirthRequest req) {
        String cacheKey = (req != null)
            ? (req.getDob() + "|" + req.getTime() + "|" + (req.getCity() != null ? req.getCity().trim().toLowerCase() : ""))
            : null;
        if (cacheKey != null) {
            PredictionResponse cached = reportCache.get(cacheKey);
            if (cached != null) return cached;
        }

        VedicChartResponse chart = vedicService.compute(req);
        PredictionResponse report = new PredictionResponse();

        VedicChartResponse.Lagna lagna = chart.getLagna();
        List<PlanetPosition>        planets = chart.getPlanets();
        List<VedicChartResponse.House> houses = chart.getHouses();
        DashaInfo dasha = chart.getDasha();

        PlanetPosition moonPos = planet(planets, "Moon");
        PlanetPosition sunPos  = planet(planets, "Sun");
        PlanetPosition ascPos  = null; // lagna is not a PlanetPosition but we have lagna object

        // ── Birth summary ─────────────────────────────────────────────────────
        report.setBirthSummary(String.format(
            "Vedic birth chart for %s, %s (IST), %s. " +
            "Lagna: %s (%s nakshatra, Pada %d). " +
            "Birth Mahadasha: %s. Ayanamsa: %.3f° Lahiri.",
            req.getDob(), req.getTime(), req.getCity(),
            lagna.sign(), lagna.nakshatra(), lagna.pada(),
            dasha.getBirthMahadasha(), chart.getInput().ayanamsa()
        ));

        // ── Section 1: Rising Sign & Overall Chart Overview ───────────────────
        report.addSection("Rising Sign & Chart Overview",
            "═══════════════════════════════════════════════════════════\n" +
            "RISING SIGN (LAGNA): " + lagna.sign().toUpperCase() + " — " + lagna.degree() + "°\n" +
            "═══════════════════════════════════════════════════════════\n\n" +
            LagnaTexts.getLagna(lagna.sign()) + "\n\n" +
            "Your Ascendant occupies " + lagna.sign() + " at " + round2(lagna.degree()) + "° within the zodiac, " +
            "placing it in the nakshatra " + lagna.nakshatra() + ", Pada " + lagna.pada() + ". " +
            "The Navamsa (D9) Lagna sign is " + lagna.navamsaSign() + ", indicating that at the soul level " +
            "your inner nature carries the qualities of " + lagna.navamsaSign() + " as a refining influence " +
            "beneath the surface expression of " + lagna.sign() + ". " +
            "The integration of these two signatures — the outer Lagna and the inner Navamsa Lagna — " +
            "is one of the most personally meaningful axes of your entire chart."
        );

        // ── Section 2: Personality, Mind & Soul ──────────────────────────────
        StringBuilder personalitySection = new StringBuilder();
        personalitySection.append("═══════════════════════════════════════════════════════════\n");
        personalitySection.append("PERSONALITY, MIND & SOUL — SUN, MOON & MERCURY\n");
        personalitySection.append("═══════════════════════════════════════════════════════════\n\n");

        if (sunPos != null) {
            personalitySection.append("SUN in ").append(sunPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[sunPos.getHouse()]).append(" House):\n");
            personalitySection.append(PlanetSignTexts.get("Sun", sunPos.getRashiName())).append(" ");
            personalitySection.append(PlanetHouseTexts.get("Sun", sunPos.getHouse())).append(" ");
            personalitySection.append(dignityNote(sunPos)).append("\n\n");
        }
        if (moonPos != null) {
            personalitySection.append("MOON in ").append(moonPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[moonPos.getHouse()]).append(" House):\n");
            personalitySection.append(PlanetSignTexts.get("Moon", moonPos.getRashiName())).append(" ");
            personalitySection.append(PlanetHouseTexts.get("Moon", moonPos.getHouse())).append(" ");
            personalitySection.append(dignityNote(moonPos)).append("\n\n");
        }
        PlanetPosition mercuryPos = planet(planets, "Mercury");
        if (mercuryPos != null) {
            personalitySection.append("MERCURY in ").append(mercuryPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[mercuryPos.getHouse()]).append(" House):\n");
            personalitySection.append(PlanetSignTexts.get("Mercury", mercuryPos.getRashiName())).append(" ");
            personalitySection.append(PlanetHouseTexts.get("Mercury", mercuryPos.getHouse())).append(" ");
            personalitySection.append(dignityNote(mercuryPos));
        }
        report.addSection("Personality, Mind & Soul", personalitySection.toString());

        // ── Section 3: Birth Nakshatra Deep Dive ─────────────────────────────
        if (moonPos != null) {
            report.addSection("Birth Nakshatra — " + moonPos.getNakshatraName() + " (Pada " + moonPos.getPada() + ")",
                "═══════════════════════════════════════════════════════════\n" +
                "JANMA NAKSHATRA: " + moonPos.getNakshatraName().toUpperCase() +
                " | Ruler: " + moonPos.getNakshatraLord() + " | Pada: " + moonPos.getPada() + "\n" +
                "Moon at " + moonPos.getSiderealLongitude() + "° sidereal\n" +
                "═══════════════════════════════════════════════════════════\n\n" +
                "Your birth nakshatra is the most fundamental layer of your Vedic astrological identity — " +
                "more primary than your Sun sign or even your rising sign in many classical interpretive traditions. " +
                "It is the lunar mansion occupied by the Moon at the precise moment of your birth, " +
                "describing your instinctive emotional nature, your deepest psychological patterns, " +
                "your relationship to time and to the rhythms of your life, and the soul quality you carry " +
                "as your most authentic inner signature.\n\n" +
                NakshatraTexts.get(moonPos.getNakshatraName()) + "\n\n" +
                "Pada " + moonPos.getPada() + " of " + moonPos.getNakshatraName() + " corresponds to the " + padaSign(moonPos) + " navamsa, " +
                "adding a layer of " + padaSign(moonPos) + " quality to the expression of this nakshatra's core energy in your life. " +
                "The nakshatra lord " + moonPos.getNakshatraLord() + " also governs your Vimshottari birth Mahadasha, " +
                "creating an important thematic link between your most instinctive emotional nature " +
                "and the planetary period that initiated your life's unfolding trajectory."
            );
        }

        // ── Section 4: Venus, Mars — Love, Desire & Vitality ─────────────────
        StringBuilder venusMarsSec = new StringBuilder();
        venusMarsSec.append("═══════════════════════════════════════════════════════════\n");
        venusMarsSec.append("VENUS & MARS — LOVE, DESIRE & VITAL ENERGY\n");
        venusMarsSec.append("═══════════════════════════════════════════════════════════\n\n");

        PlanetPosition venusPos = planet(planets, "Venus");
        PlanetPosition marsPos  = planet(planets, "Mars");

        if (venusPos != null) {
            venusMarsSec.append("VENUS in ").append(venusPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[venusPos.getHouse()]).append(" House)");
            if (venusPos.isRetrograde()) venusMarsSec.append(" [RETROGRADE]");
            venusMarsSec.append(":\n");
            venusMarsSec.append(PlanetSignTexts.get("Venus", venusPos.getRashiName())).append(" ");
            venusMarsSec.append(PlanetHouseTexts.get("Venus", venusPos.getHouse())).append(" ");
            venusMarsSec.append(dignityNote(venusPos)).append("\n\n");
            venusMarsSec.append("In matters of love and relationship, Venus' placement in the ")
                .append(ORDINALS[venusPos.getHouse()]).append(" house means that your romantic and aesthetic life ")
                .append(houseTheme(venusPos.getHouse())).append("\n\n");
        }
        if (marsPos != null) {
            venusMarsSec.append("MARS in ").append(marsPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[marsPos.getHouse()]).append(" House)");
            if (marsPos.isRetrograde()) venusMarsSec.append(" [RETROGRADE]");
            venusMarsSec.append(":\n");
            venusMarsSec.append(PlanetSignTexts.get("Mars", marsPos.getRashiName())).append(" ");
            venusMarsSec.append(PlanetHouseTexts.get("Mars", marsPos.getHouse())).append(" ");
            venusMarsSec.append(dignityNote(marsPos));
        }
        report.addSection("Venus & Mars — Love, Desire & Vital Energy", venusMarsSec.toString());

        // ── Section 5: Jupiter & Saturn — Dharma, Karma & Expansion ─────────
        StringBuilder jupSatSec = new StringBuilder();
        jupSatSec.append("═══════════════════════════════════════════════════════════\n");
        jupSatSec.append("JUPITER & SATURN — DHARMA, KARMA & EXPANSION\n");
        jupSatSec.append("═══════════════════════════════════════════════════════════\n\n");

        PlanetPosition jupiterPos = planet(planets, "Jupiter");
        PlanetPosition saturnPos  = planet(planets, "Saturn");

        if (jupiterPos != null) {
            jupSatSec.append("JUPITER in ").append(jupiterPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[jupiterPos.getHouse()]).append(" House)");
            if (jupiterPos.isRetrograde()) jupSatSec.append(" [RETROGRADE]");
            jupSatSec.append(":\n");
            jupSatSec.append(PlanetSignTexts.get("Jupiter", jupiterPos.getRashiName())).append(" ");
            jupSatSec.append(PlanetHouseTexts.get("Jupiter", jupiterPos.getHouse())).append(" ");
            jupSatSec.append(dignityNote(jupiterPos)).append("\n\n");
        }
        if (saturnPos != null) {
            jupSatSec.append("SATURN in ").append(saturnPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[saturnPos.getHouse()]).append(" House)");
            if (saturnPos.isRetrograde()) jupSatSec.append(" [RETROGRADE]");
            jupSatSec.append(":\n");
            jupSatSec.append(PlanetSignTexts.get("Saturn", saturnPos.getRashiName())).append(" ");
            jupSatSec.append(PlanetHouseTexts.get("Saturn", saturnPos.getHouse())).append(" ");
            jupSatSec.append(dignityNote(saturnPos));
        }
        report.addSection("Jupiter & Saturn — Dharma, Karma & Expansion", jupSatSec.toString());

        // ── Section 6: Rahu & Ketu — Karmic Axis ─────────────────────────────
        PlanetPosition rahuPos = planet(planets, "Rahu");
        PlanetPosition ketuPos = planet(planets, "Ketu");
        StringBuilder rahuKetuSec = new StringBuilder();
        rahuKetuSec.append("═══════════════════════════════════════════════════════════\n");
        rahuKetuSec.append("RAHU & KETU — THE KARMIC AXIS OF DESTINY\n");
        rahuKetuSec.append("═══════════════════════════════════════════════════════════\n\n");
        rahuKetuSec.append("The Rahu-Ketu axis is the most karmically significant axis in your chart. " +
            "Rahu, the North Node, represents the soul's evolutional edge in this incarnation — " +
            "the direction of growth, desire, and necessary new experience. " +
            "Ketu, the South Node, represents accumulated past-life mastery — " +
            "the gifts you carry forward and the patterns the soul is ready to transcend.\n\n");
        if (rahuPos != null) {
            rahuKetuSec.append("RAHU in ").append(rahuPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[rahuPos.getHouse()]).append(" House):\n");
            rahuKetuSec.append(PlanetSignTexts.get("Rahu", rahuPos.getRashiName())).append(" ");
            rahuKetuSec.append(PlanetHouseTexts.get("Rahu", rahuPos.getHouse())).append("\n\n");
        }
        if (ketuPos != null) {
            rahuKetuSec.append("KETU in ").append(ketuPos.getRashiName().toUpperCase())
                .append(" (").append(ORDINALS[ketuPos.getHouse()]).append(" House):\n");
            rahuKetuSec.append(PlanetSignTexts.get("Ketu", ketuPos.getRashiName())).append(" ");
            rahuKetuSec.append(PlanetHouseTexts.get("Ketu", ketuPos.getHouse()));
        }
        report.addSection("Rahu & Ketu — Karmic Axis", rahuKetuSec.toString());

        // ── Section 7: Life Areas — House-by-House Analysis ───────────────────
        StringBuilder houseSec = new StringBuilder();
        houseSec.append("═══════════════════════════════════════════════════════════\n");
        houseSec.append("LIFE AREAS — HOUSE-BY-HOUSE ANALYSIS\n");
        houseSec.append("═══════════════════════════════════════════════════════════\n\n");
        for (VedicChartResponse.House h : houses) {
            houseSec.append("HOUSE ").append(h.house()).append(" — ").append(h.sign().toUpperCase()).append(":\n");
            houseSec.append(LagnaTexts.getHouseMeaning(h.house())).append(" ");
            if (!h.planets().isEmpty()) {
                houseSec.append("Planets occupying this house: ").append(String.join(", ", h.planets())).append(". ");
                houseSec.append("Their presence here significantly intensifies and personalizes these themes, " +
                    "making this one of the more active and personally significant domains of your life experience. ");
            } else {
                houseSec.append("No planets currently occupy this house; it is governed purely by its lord. " +
                    "The lord's placement elsewhere in the chart determines how actively and productively these themes manifest. ");
            }
            houseSec.append("The sign of ").append(h.sign())
                .append(" here adds its distinctive qualities to the governance of this domain.\n\n");
        }
        report.addSection("Life Areas — House-by-House Analysis", houseSec.toString());

        // ── Section 8: Career, Finance & Public Life ──────────────────────────
        String careerSection = buildCareerSection(planets, houses, lagna.sign());
        report.addSection("Career, Finance & Public Life", careerSection);

        // ── Section 9: Health & Vitality ──────────────────────────────────────
        String healthSection = buildHealthSection(planets, lagna.sign(), moonPos);
        report.addSection("Health & Vitality", healthSection);

        // ── Section 10: Relationships & Marriage ──────────────────────────────
        String relationshipSection = buildRelationshipSection(planets, lagna.sign(), venusPos, marsPos);
        report.addSection("Relationships & Marriage", relationshipSection);

        // ── Section 11: Yogas & Special Combinations ──────────────────────────
        List<String> yogas = chart.getYogas();
        StringBuilder yogaSec = new StringBuilder();
        yogaSec.append("═══════════════════════════════════════════════════════════\n");
        yogaSec.append("YOGAS & SPECIAL PLANETARY COMBINATIONS\n");
        yogaSec.append("═══════════════════════════════════════════════════════════\n\n");
        if (yogas.isEmpty()) {
            yogaSec.append("No major classical yogas are detected in this configuration. " +
                "This does not diminish the chart's power — many of the most effective and accomplished " +
                "lives are built through the disciplined development of individual planetary strengths " +
                "rather than the dramatic fireworks of rare yoga combinations. " +
                "The chart's fundamental strength lies in the quality and placement of its individual planets " +
                "and the authentic application of their gifts.");
        } else {
            yogaSec.append("The following classical yogas are present in your birth chart, each representing " +
                "a specific configuration of planetary energy that the tradition recognizes as producing " +
                "distinctive life outcomes:\n\n");
            for (String yoga : yogas) {
                yogaSec.append("• ").append(yoga).append("\n  ");
                yogaSec.append(yogaInterpretation(yoga)).append("\n\n");
            }
        }
        report.addSection("Yogas & Special Planetary Combinations", yogaSec.toString());

        // ── Section 12: Vimshottari Dasha — Timing of Life ───────────────────
        StringBuilder dashaSec = new StringBuilder();
        dashaSec.append("═══════════════════════════════════════════════════════════\n");
        dashaSec.append("VIMSHOTTARI DASHA — TIMING OF LIFE\n");
        dashaSec.append("═══════════════════════════════════════════════════════════\n\n");

        dashaSec.append("The Vimshottari Dasha system is Vedic astrology's primary predictive timing mechanism, " +
            "assigning planetary rulership over sequential life periods totaling 120 years across a complete cycle. " +
            "Each Mahadasha activates the themes of its ruling planet as the dominant experiential current of that period; " +
            "within each Mahadasha, Antardashas (sub-periods) provide finer temporal resolution.\n\n");

        dashaSec.append("BIRTH MAHADASHA: ").append(dasha.getBirthMahadasha()).append("\n");
        dashaSec.append("At the moment of your birth, you were born into the ")
            .append(dasha.getBirthMahadasha()).append(" Mahadasha — a significant signature " +
            "that colors the themes of your earliest life experience and reveals much about the karmic " +
            "momentum you carried into this incarnation.\n\n");

        if (dasha.getCurrentMahadasha() != null) {
            DashaInfo.Period cm = dasha.getCurrentMahadasha();
            dashaSec.append("CURRENT MAHADASHA: ").append(cm.lord().toUpperCase()).append("\n");
            dashaSec.append("Period: ").append(cm.start()).append(" → ").append(cm.end())
                .append(" (").append(round2(cm.durationYears())).append(" years)\n\n");
            dashaSec.append(DashaTexts.getMaha(cm.lord())).append("\n\n");
        }
        if (dasha.getCurrentAntardasha() != null) {
            DashaInfo.AntarPeriod ca = dasha.getCurrentAntardasha();
            dashaSec.append("CURRENT ANTARDASHA: ").append(ca.lord().toUpperCase()).append("\n");
            dashaSec.append("Period: ").append(ca.start()).append(" → ").append(ca.end())
                .append(" (").append(round2(ca.durationYears())).append(" years)\n\n");
            if (dasha.getCurrentMahadasha() != null) {
                dashaSec.append(DashaTexts.getAntar(dasha.getCurrentMahadasha().lord(), ca.lord())).append("\n\n");
            }
        }
        if (dasha.getCurrentPratyantardasha() != null) {
            DashaInfo.Period cp = dasha.getCurrentPratyantardasha();
            dashaSec.append("CURRENT PRATYANTARDASHA: ").append(cp.lord().toUpperCase()).append("\n");
            dashaSec.append("Period: ").append(cp.start()).append(" → ").append(cp.end()).append("\n");
            dashaSec.append("At the finest resolution, the ").append(cp.lord())
                .append(" Pratyantardasha adds its specific planetary signature as an additional sub-current ")
                .append("operating within the broader Antardasha context, providing nuance and specificity ")
                .append("to the timing of events during this narrow but significant window.\n\n");
        }

        // Upcoming dashas
        List<DashaInfo.MahaPeriod> seq = dasha.getDashaSequence();
        if (seq != null && seq.size() > 1) {
            dashaSec.append("UPCOMING MAHADASHAS:\n");
            int shown = 0;
            for (DashaInfo.MahaPeriod mp : seq) {
                if (!LocalDate.parse(mp.start()).isAfter(LocalDate.now(ZoneOffset.ofHoursMinutes(5, 30)))) continue;
                dashaSec.append("• ").append(mp.lord()).append(" Mahadasha: ")
                    .append(mp.start()).append(" → ").append(mp.end())
                    .append(" (").append(round2(mp.durationYears())).append(" yrs): ")
                    .append(upcomingDashaNote(mp.lord())).append("\n");
                if (++shown >= 3) break;
            }
        }
        report.addSection("Vimshottari Dasha — Timing of Life", dashaSec.toString());

        // ── Section 13: Spiritual Path, Remedies & Practices ──────────────────
        report.addSection("Spiritual Path, Remedies & Practices",
            buildSpiritualSection(planets, lagna.sign(), moonPos, dasha));

        report.recalcWordCount();
        if (cacheKey != null) {
            reportCache.put(cacheKey, report);
        }
        return report;
    }

    // ─── Section builders ─────────────────────────────────────────────────────

    private String buildCareerSection(List<PlanetPosition> planets, List<VedicChartResponse.House> houses, String lagnaSign) {
        PlanetPosition h10Planet = planets.stream().filter(p -> p.getHouse() == 10).findFirst().orElse(null);
        PlanetPosition h2Planet  = planets.stream().filter(p -> p.getHouse() == 2).findFirst().orElse(null);
        PlanetPosition saturnPos = planet(planets, "Saturn");
        PlanetPosition jupiterPos = planet(planets, "Jupiter");
        PlanetPosition sunPos    = planet(planets, "Sun");

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("CAREER, FINANCE & PUBLIC LIFE\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");

        String h10Sign = houses.stream().filter(h -> h.house() == 10).findFirst().map(VedicChartResponse.House::sign).orElse("unknown");
        sb.append("The Tenth House in your chart falls in ").append(h10Sign)
            .append(", establishing the foundational sign-quality of your professional life and public reputation. ")
            .append("The sign of ").append(h10Sign)
            .append(" imparts its essential qualities to all career matters: how you approach professional authority, ")
            .append("what environments allow your best professional expression to emerge, ")
            .append("and what qualities the world most readily recognizes and rewards in your work.\n\n");

        if (h10Planet != null) {
            sb.append("The planet ").append(h10Planet.getName()).append(" occupies your Tenth House directly, ")
                .append("making it one of the most personally significant career indicators in your chart. ")
                .append(PlanetHouseTexts.get(h10Planet.getName(), 10)).append("\n\n");
        }

        sb.append("KEY CAREER INDICATORS:\n");
        if (sunPos != null) {
            sb.append("• Sun in the ").append(ORDINALS[sunPos.getHouse()]).append(" House (").append(sunPos.getRashiName())
                .append("): Your solar identity and professional ambition express through ")
                .append(houseTheme(sunPos.getHouse())).append("\n");
        }
        if (saturnPos != null) {
            sb.append("• Saturn in the ").append(ORDINALS[saturnPos.getHouse()]).append(" House (").append(saturnPos.getRashiName())
                .append("): ").append(saturnCareerNote(saturnPos)).append("\n");
        }
        if (jupiterPos != null) {
            sb.append("• Jupiter in the ").append(ORDINALS[jupiterPos.getHouse()]).append(" House (").append(jupiterPos.getRashiName())
                .append("): Jupiter's position provides expansion and grace in the domains of ")
                .append(houseTheme(jupiterPos.getHouse())).append("\n");
        }

        sb.append("\nFINANCE & WEALTH:\n");
        String h2Sign = houses.stream().filter(h -> h.house() == 2).findFirst().map(VedicChartResponse.House::sign).orElse("unknown");
        String h11Sign = houses.stream().filter(h -> h.house() == 11).findFirst().map(VedicChartResponse.House::sign).orElse("unknown");
        sb.append("The Second House of accumulated wealth falls in ").append(h2Sign)
            .append(", and the Eleventh House of gains and income falls in ").append(h11Sign).append(". ")
            .append("These two houses, along with their lords, define the primary channels through which ")
            .append("financial resources flow into your life. ")
            .append("The ").append(h2Sign).append(" quality of your 2nd House suggests a ")
            .append(wealthStyle(h2Sign)).append(" approach to financial security and material accumulation. ")
            .append("The ").append(h11Sign).append(" quality of the 11th House describes ")
            .append(gainsStyle(h11Sign)).append(" as the primary channel through which gains and income flow.");

        return sb.toString();
    }

    private String buildHealthSection(List<PlanetPosition> planets, String lagnaSign, PlanetPosition moonPos) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("HEALTH & VITALITY\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");

        sb.append("In Vedic astrology, health is assessed primarily through the Ascendant, its lord, ")
            .append("the Sun, the Moon, and the Sixth and Eighth houses, with special attention to any ")
            .append("afflictions or debilitations affecting these sensitive points.\n\n");

        sb.append("CONSTITUTION: Your ").append(lagnaSign).append(" Ascendant indicates a ").append(lagnaHealthNote(lagnaSign))
            .append(" body type with specific constitutional strengths and vulnerabilities that benefit from ")
            .append("lifestyle choices aligned with your particular elemental and planetary makeup.\n\n");

        if (moonPos != null) {
            sb.append("EMOTIONAL HEALTH: The Moon in ").append(moonPos.getRashiName()).append(" in the ")
                .append(ORDINALS[moonPos.getHouse()]).append(" House indicates that ")
                .append(moonHealthNote(moonPos.getRashiName())).append(" ")
                .append("Emotional balance — achieved through the specific practices suited to your Moon placement — ")
                .append("directly supports physical health in ways that conventional medical frameworks sometimes underestimate.\n\n");
        }

        planets.stream()
            .filter(p -> List.of("Saturn", "Mars", "Rahu", "Ketu").contains(p.getName()) &&
                         List.of(1, 6, 8).contains(p.getHouse()))
            .forEach(p -> sb.append("ATTENTION: ").append(p.getName()).append(" in the ")
                .append(ORDINALS[p.getHouse()]).append(" House warrants mindful attention to ")
                .append(planetHealthWarning(p.getName())).append("\n\n"));

        sb.append("RECOMMENDED PRACTICES: Regular, appropriate physical exercise aligned with your Mars placement, ")
            .append("dietary choices that honor your Ascendant's elemental nature, ")
            .append("and consistent sleep rhythms that respect the Moon's cyclical influence are the three most ")
            .append("fundamental preventive health practices for your specific chart configuration. ")
            .append("Annual preventive health checks focused on the body systems associated with your Ascendant sign ")
            .append("provide early awareness of any developing vulnerabilities.");

        return sb.toString();
    }

    private String buildRelationshipSection(List<PlanetPosition> planets, String lagnaSign,
                                             PlanetPosition venusPos, PlanetPosition marsPos) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("RELATIONSHIPS & MARRIAGE\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");

        PlanetPosition h7Planet = planets.stream().filter(p -> p.getHouse() == 7).findFirst().orElse(null);

        sb.append("SEVENTH HOUSE — THE HOUSE OF PARTNERSHIP:\n");
        sb.append("The Seventh House governs all significant one-on-one partnerships, most importantly marriage and life partnership. ")
            .append("The qualities of this house and its planetary occupants describe both what you seek ")
            .append("in a partner and what the partnership experience itself characteristically brings.\n\n");

        if (h7Planet != null) {
            sb.append(h7Planet.getName()).append(" in the Seventh House creates a ").append(h7Planet.getRashiName())
                .append("-flavored partnership dynamic. ")
                .append(PlanetHouseTexts.get(h7Planet.getName(), 7)).append("\n\n");
        }

        if (venusPos != null) {
            sb.append("VENUS — YOUR LOVE NATURE:\n")
                .append("Venus in ").append(venusPos.getRashiName()).append(" reveals your love language, ")
                .append("your aesthetic preferences in relationships, and what your heart most deeply seeks in ")
                .append("intimate connection. ")
                .append(PlanetSignTexts.get("Venus", venusPos.getRashiName())).append("\n\n");
        }

        if (marsPos != null) {
            sb.append("MARS — PASSION & COMPATIBILITY:\n")
                .append("Mars in ").append(marsPos.getRashiName()).append(" describes the quality of your ")
                .append("sexual and assertive energy within intimate relationships — how you pursue, ")
                .append("how you engage in conflict, and how physical vitality expresses within partnership. ")
                .append(PlanetSignTexts.get("Mars", marsPos.getRashiName())).append("\n\n");
        }

        sb.append("COMPATIBILITY PRINCIPLES: From a Vedic perspective, lasting partnership compatibility ")
            .append("is assessed through Ashtakuta matching (36-point system), Nadi dosha analysis, ")
            .append("Mangal dosha examination, and the synastry of both partners' complete charts. ")
            .append("The most important single factor is the compatibility of Moon signs and nakshatras — ")
            .append("emotional resonance and instinctive attunement being the deepest foundation ")
            .append("of sustainable intimate connection beyond initial attraction.");

        return sb.toString();
    }

    private String buildSpiritualSection(List<PlanetPosition> planets, String lagnaSign,
                                          PlanetPosition moonPos, DashaInfo dasha) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("SPIRITUAL PATH, REMEDIES & PRACTICES\n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");

        PlanetPosition ketuPos    = planet(planets, "Ketu");
        PlanetPosition jupiterPos = planet(planets, "Jupiter");

        sb.append("SPIRITUAL ORIENTATION:\n")
            .append("Your ").append(lagnaSign).append(" Ascendant and ")
            .append(moonPos != null ? moonPos.getNakshatraName() : "birth nakshatra")
            .append(" together describe your most natural approach to spiritual practice and the divine. ")
            .append(lagnaSpiritual(lagnaSign)).append("\n\n");

        if (ketuPos != null) {
            sb.append("KETU — SPIRITUAL INHERITANCE:\n")
                .append("Ketu in the ").append(ORDINALS[ketuPos.getHouse()]).append(" House in ").append(ketuPos.getRashiName())
                .append(" reveals the domain of past-life spiritual accomplishment that you carry as an inner resource. ")
                .append(PlanetHouseTexts.get("Ketu", ketuPos.getHouse())).append("\n\n");
        }

        if (jupiterPos != null) {
            sb.append("JUPITER — GURU & DHARMA:\n")
                .append("Jupiter in the ").append(ORDINALS[jupiterPos.getHouse()]).append(" House describes ")
                .append("the form your dharma most naturally takes and the kind of teacher or guide ")
                .append("whose wisdom is most genuinely nourishing for your specific spiritual development. ")
                .append(PlanetHouseTexts.get("Jupiter", jupiterPos.getHouse())).append("\n\n");
        }

        sb.append("CLASSICAL REMEDIES (Upayas):\n")
            .append("Classical Vedic remedies work by consciously engaging the planetary energies that need ")
            .append("strengthening or pacifying in your specific chart configuration:\n\n")
            .append("• MANTRA PRACTICE: Chanting the Beej (seed) mantras of your chart's key planets ")
            .append("activates their positive dimensions and reduces the influence of afflictions. ")
            .append("A consistent daily practice of even 108 repetitions produces measurable shifts over time.\n\n")
            .append("• GEMSTONE THERAPY: Wearing the gemstone of your Lagna lord (").append(lagnaLord(lagnaSign))
            .append(") on the appropriate finger during the appropriate planetary hour strengthens ")
            .append("the foundational signature of your chart. Consult a qualified Jyotishi before committing to gemstone therapy.\n\n")
            .append("• CHARITABLE SERVICE: Service to those who embody the qualities of challenging planets in your chart ")
            .append("— service to the elderly and marginalized for Saturn afflictions, to spiritual seekers for Ketu afflictions, ")
            .append("to women and children for Moon afflictions — creates karmic merit that softens difficult period influences.\n\n")
            .append("• LIFESTYLE ALIGNMENT: The most powerful remedy of all is aligning your daily lifestyle ")
            .append("with the qualities of your chart's most beneficial planets — their timing, their diet, ")
            .append("their colors, and their activities woven consciously into your ordinary days.\n\n")
            .append("• PILGRIMAGE & SACRED SPACE: Visiting temples, sacred sites, or natural environments ")
            .append("associated with your chart's significant planets provides genuine energetic replenishment ")
            .append("that supports both the inner and outer dimensions of your life journey.");

        return sb.toString();
    }

    // ─── Utility methods ──────────────────────────────────────────────────────

    private PlanetPosition planet(List<PlanetPosition> planets, String name) {
        return planets.stream().filter(p -> name.equals(p.getName())).findFirst().orElse(null);
    }

    private String dignityNote(PlanetPosition p) {
        return switch (p.getDignity()) {
            case "Exalted" -> p.getName() + " is EXALTED in " + p.getRashiName() +
                " — operating at peak strength and conferring exceptional results in its domains.";
            case "Debilitated" -> p.getName() + " is DEBILITATED in " + p.getRashiName() +
                " — its expression is challenged and requires conscious attention and supportive practices " +
                "to reach its full positive potential. Debilitation cancellation (Neecha Bhanga) may apply.";
            case "Own" -> p.getName() + " is in its OWN SIGN (" + p.getRashiName() +
                ") — settled, self-reliant, and operating with natural ease and self-sufficiency.";
            default -> "";
        };
    }

    private String houseTheme(int house) {
        return switch (house) {
            case 1  -> "the self, personality, and overall vitality.";
            case 2  -> "wealth, family, speech, and accumulated resources.";
            case 3  -> "courage, communication, and immediate environment.";
            case 4  -> "home, mother, emotional foundations, and property.";
            case 5  -> "intelligence, creativity, children, and romance.";
            case 6  -> "service, competition, health management, and overcoming obstacles.";
            case 7  -> "partnership, marriage, and significant one-on-one relationships.";
            case 8  -> "transformation, occult knowledge, and hidden resources.";
            case 9  -> "dharma, higher wisdom, fortune, and spiritual aspiration.";
            case 10 -> "career, public reputation, and professional achievement.";
            case 11 -> "gains, income, friendship networks, and the fulfillment of desires.";
            case 12 -> "spiritual liberation, foreign connection, and the transcendence of ordinary identity.";
            default -> "its associated life domains.";
        };
    }

    private String padaSign(PlanetPosition moonPos) {
        String[] signs = {"Aries","Taurus","Gemini","Cancer","Leo","Virgo","Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"};
        int nak = moonPos.getNakshatra() - 1;
        int startSign = (nak * 4) / 9;
        return signs[((startSign + moonPos.getPada() - 1)) % 12];
    }

    private String lagnaHealthNote(String sign) {
        return switch (sign) {
            case "Aries"       -> "a strong, energetic, hot-temperament (pitta-dominant)";
            case "Taurus"      -> "a sturdy, endurance-oriented, earth-element";
            case "Gemini"      -> "a slender, quick, vata-dominant";
            case "Cancer"      -> "a soft-featured, emotionally sensitive, kapha-moon";
            case "Leo"         -> "a robust, strong-constitution, solar pitta";
            case "Virgo"       -> "a sensitive digestion, analytically-inclined, mixed vata-pitta";
            case "Libra"       -> "a balanced constitution, kidney-sensitive, vata-airy";
            case "Scorpio"     -> "an intense, regenerative, immune-sensitive";
            case "Sagittarius" -> "an expansive, athletic, liver-sensitive";
            case "Capricorn"   -> "a bone-structured, joint-sensitive, vata-kapha";
            case "Aquarius"    -> "a nervous-system-sensitive, circulatory-aware, air-dominant";
            case "Pisces"      -> "a lymphatic-sensitive, immune-delicate, kapha-water";
            default            -> "a distinctive constitution";
        };
    }

    private String moonHealthNote(String sign) {
        return switch (sign) {
            case "Aries"       -> "emotional health is tied to physical activity — anger and stress need active physical discharge.";
            case "Taurus"      -> "emotional stability is strongly connected to physical comfort and sensory well-being.";
            case "Gemini"      -> "nervous system health is closely linked to mental stimulation and variety.";
            case "Cancer"      -> "the digestive system mirrors emotional states most directly — gut health requires emotional equilibrium.";
            case "Leo"         -> "the heart and circulatory system reflect emotional states; creative expression is medicinal.";
            case "Virgo"       -> "anxiety and digestive disturbance are the most common emotional-physical interfaces.";
            case "Libra"       -> "hormonal balance and kidney health reflect relational harmony or discord.";
            case "Scorpio"     -> "immune function and reproductive health are the primary emotional-physical interface.";
            case "Sagittarius" -> "liver health and hip/thigh wellbeing reflect philosophical alignment and freedom.";
            case "Capricorn"   -> "joint, bone, and skin health reflect the accumulated weight of responsibility.";
            case "Aquarius"    -> "nervous system and circulatory health reflect the quality of intellectual and social engagement.";
            case "Pisces"      -> "immune and lymphatic health reflect the degree of emotional boundary maintenance.";
            default            -> "emotional states directly influence physical health through the Moon's domain.";
        };
    }

    private String planetHealthWarning(String planet) {
        return switch (planet) {
            case "Saturn" -> "chronic conditions, joint health, and the cumulative effects of sustained stress.";
            case "Mars"   -> "inflammatory conditions, accidents, fever, and the physical consequences of overexertion.";
            case "Rahu"   -> "unusual or difficult-to-diagnose conditions, toxicity, and nervous system irregularities.";
            case "Ketu"   -> "immune function, mysterious or recurrent conditions, and the health dimensions of spiritual practice.";
            default       -> "its associated body systems and health themes.";
        };
    }

    private String saturnCareerNote(PlanetPosition saturnPos) {
        return "Saturn's disciplined energy in the " + ORDINALS[saturnPos.getHouse()] +
            " House shapes career development through patient, methodical effort — success arrives " +
            "later than peers but proves far more durable once established.";
    }

    private String wealthStyle(String sign) {
        return switch (sign) {
            case "Taurus","Capricorn"  -> "steady, patient, long-term accumulation";
            case "Aries","Leo"         -> "bold, entrepreneurial, initiative-driven";
            case "Gemini","Aquarius"   -> "intellectual, communicative, diversified";
            case "Cancer","Pisces"     -> "intuitive, emotionally attuned, service-connected";
            case "Virgo","Scorpio"     -> "analytical, investigative, precision-oriented";
            case "Libra","Sagittarius" -> "partnership-oriented, philosophical, ethics-guided";
            default -> "distinctive and personally characteristic";
        };
    }

    private String gainsStyle(String sign) {
        return switch (sign) {
            case "Aries","Leo","Sagittarius" -> "bold enterprise, competitive achievement, and inspired initiative";
            case "Taurus","Virgo","Capricorn" -> "disciplined effort, practical service, and methodical investment";
            case "Gemini","Libra","Aquarius"  -> "intellectual work, social networking, and communicative enterprise";
            default -> "emotionally attuned service, intuitive investment, and relational trust-building";
        };
    }

    private String lagnaSpiritual(String sign) {
        return switch (sign) {
            case "Aries"       -> "Your spiritual path has the quality of a warrior's quest — direct, courageous, and experiential rather than doctrinal. Physical practices like yoga and martial arts often serve as genuine spiritual vehicles.";
            case "Taurus"      -> "Your spiritual path moves through beauty, the body, and the senses toward the sacred — finding the divine in the natural world, in music, in the quality of present physical experience.";
            case "Gemini"      -> "Your spiritual path moves through the mind — through study, dialogue, mantra, and the investigation of sacred texts — toward the recognition of the witnessing consciousness beneath all thought.";
            case "Cancer"      -> "Your spiritual path moves through the heart, devotion, and the cultivation of deep inner peace — bhakti yoga and practices of emotional surrender to the divine are your most natural vehicles.";
            case "Leo"         -> "Your spiritual path involves the creative self-expression of the divine nature — art, devotional performance, and the experience of the divine as radiant conscious presence at the core of your own being.";
            case "Virgo"       -> "Your spiritual path moves through service, through the perfection of craft, and through the daily disciplines that purify the body-mind instrument — karma yoga and jnana yoga are your natural approaches.";
            case "Libra"       -> "Your spiritual path moves through relationship — through the experience of the divine in the face of the beloved, through the cultivation of justice and beauty as sacred expressions of cosmic order.";
            case "Scorpio"     -> "Your spiritual path moves through depth, transformation, and the courageous descent into what is most hidden and most difficult — tantra, shadow work, and genuine psychological transformation are your natural vehicles.";
            case "Sagittarius" -> "Your spiritual path is philosophical and expansive — the seeking of higher truth through study, pilgrimage, and the direct experience of wisdom traditions from multiple cultures and perspectives.";
            case "Capricorn"   -> "Your spiritual path moves through disciplined practice, karmic accountability, and the patient dismantling of ego-identification through sustained, sincere service and the acceptance of necessary difficulty.";
            case "Aquarius"    -> "Your spiritual path moves through the collective — through service to humanity, through the direct experience of universal consciousness beyond individual identity, and through visionary practices that expand beyond personal concerns.";
            case "Pisces"      -> "Your spiritual path is devotional, surrendered, and naturally mystical — the direct dissolution of individual consciousness into universal awareness is both your natural inclination and your most authentic spiritual destination.";
            default -> "Your spiritual path reflects the unique combination of planetary influences in your specific chart, revealing a distinctive approach to the sacred that is most authentic when it honors your particular planetary configuration.";
        };
    }

    private String lagnaLord(String sign) {
        return switch (sign) {
            case "Aries","Scorpio"     -> "Mars";
            case "Taurus","Libra"      -> "Venus";
            case "Gemini","Virgo"      -> "Mercury";
            case "Cancer"              -> "Moon";
            case "Leo"                 -> "Sun";
            case "Sagittarius","Pisces"-> "Jupiter";
            case "Capricorn","Aquarius"-> "Saturn";
            default -> "the Lagna lord";
        };
    }

    private String yogaInterpretation(String yoga) {
        if (yoga.contains("Gaja Kesari"))  return "One of the most celebrated classical yogas. Jupiter and Moon in mutual kendra positions create an elephant-and-lion energy — vast wisdom paired with commanding emotional presence. Traditionally associated with fame, generosity, and long-lasting positive impact on one's community.";
        if (yoga.contains("Ruchaka"))      return "Mars in kendra in own or exalted sign — the Panch Mahapurusha yoga of martial excellence. Ruchaka natives display outstanding physical courage, competitive excellence, and executive leadership that earns genuine authority through proven capability.";
        if (yoga.contains("Bhadra"))       return "Mercury in kendra in own or exalted sign — the Panch Mahapurusha yoga of intellectual excellence. Bhadra natives possess extraordinary communicative intelligence, commercial aptitude, and the capacity for technical mastery that defines their professional contributions.";
        if (yoga.contains("Hamsa"))        return "Jupiter in kendra in own or exalted sign — the Panch Mahapurusha yoga of wisdom and grace. Hamsa natives radiate natural nobility, philosophical generosity, and a quality of inherent good fortune that serves both themselves and all those within their sphere of influence.";
        if (yoga.contains("Malavya"))      return "Venus in kendra in own or exalted sign — the Panch Mahapurusha yoga of beauty and refinement. Malavya natives possess exceptional aesthetic gifts, social grace, and a natural magnetism that attracts abundance, beauty, and significant romantic or creative fulfillment.";
        if (yoga.contains("Sasa"))         return "Saturn in kendra in own or exalted sign — the Panch Mahapurusha yoga of disciplined mastery. Sasa natives develop formidable professional authority through patient sustained effort, eventually achieving positions of significant institutional power and lasting social contribution.";
        if (yoga.contains("Budhaditya"))   return "Sun and Mercury conjunct — the yoga of solar intelligence. Natives typically display exceptional analytical ability, communicative confidence, and a capacity for leadership through intellectual authority. Writing, teaching, and advisory roles often feature prominently.";
        if (yoga.contains("Chandra-Mangal")) return "Moon and Mars conjunct — a yoga associated with commercial intelligence, emotional courage, and the capacity to turn emotional drive into material prosperity. Real estate, entrepreneurship, and emotionally engaged competitive fields often prosper.";
        if (yoga.contains("Kemadruma"))    return "Moon isolated without flanking planets — a yoga associated with some emotional vulnerability and the need to develop inner resourcefulness independent of consistent outer support. The productive response is cultivating inner self-sufficiency and genuine spiritual independence as primary life tools.";
        if (yoga.contains("Adhi"))         return "All three benefics (Mercury, Jupiter, Venus) in the 6th, 7th, and 8th from the Moon — a powerful yoga associated with exceptional intelligence, strategic positioning, and eventually high social status through the combined benefic force of all three natural benefic planets.";
        return "This yoga activates specific planetary combinations that the classical tradition recognizes as producing distinctive and significant life outcomes in the areas governed by the planets and houses involved.";
    }

    private String upcomingDashaNote(String lord) {
        return switch (lord) {
            case "Sun"     -> "period of career visibility, authority, and solar self-expression.";
            case "Moon"    -> "period of emotional depth, home, mother, and intuitive awakening.";
            case "Mars"    -> "period of competitive action, courage, property, and physical vitality.";
            case "Rahu"    -> "period of worldly ambition, transformation, and karmic acceleration.";
            case "Jupiter" -> "period of expansion, wisdom, grace, children, and philosophical growth.";
            case "Saturn"  -> "period of discipline, karmic accountability, and sustained achievement.";
            case "Mercury" -> "period of intellectual enterprise, communication, and commercial activity.";
            case "Ketu"    -> "period of spiritual deepening, inner turning, and liberation from limiting patterns.";
            case "Venus"   -> "period of love, beauty, artistic abundance, and material prosperity.";
            default -> "period activating that planet's key themes and chart domains.";
        };
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
