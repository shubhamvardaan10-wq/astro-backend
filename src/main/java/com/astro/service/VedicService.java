package com.astro.service;

import com.astro.model.*;
import com.astro.util.AstroMath;
import com.astro.util.Ephemeris;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Vedic (Jyotisha) chart computation.
 *
 * Algorithms follow Brihat Parashara Hora Shastra (BPHS) and standard
 * computational references (Meeus, Swiss Ephemeris documentation).
 */
@Service
public class VedicService {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String[] RASHIS = {
        "Aries","Taurus","Gemini","Cancer","Leo","Virgo",
        "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"
    };

    private static final String[] NAKSHATRAS = {
        "Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
        "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni",
        "Uttara Phalguni","Hasta","Chitra","Swati","Vishakha","Anuradha",
        "Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha","Shravana",
        "Dhanishtha","Shatabhisha","Purva Bhadrapada","Uttara Bhadrapada","Revati"
    };

    // Dasha lords in Vimshottari order (repeats over 27 nakshatras in groups of 9)
    private static final String[] DASHA_LORDS  = {
        "Ketu","Venus","Sun","Moon","Mars","Rahu","Jupiter","Saturn","Mercury"
    };
    private static final int[]    DASHA_YEARS  = {7, 20, 6, 10, 7, 18, 16, 19, 17};
    private static final int      DASHA_TOTAL  = 120;   // years

    // Each nakshatra maps to a dasha lord: nakshatra_index % 9
    // Ashwini(0)→Ketu(0), Bharani(1)→Venus(1), Krittika(2)→Sun(2) ...
    // The pattern repeats every 9 nakshatras.

    // Planet dignities: [rashi_index(0-11)] → planet dignity map (partial)
    // Exaltation signs (0-based rashi): Sun=1(Aries? no, Aries=0,Taurus=1 → Sun exalted in Aries=0)
    // Sun exalted Aries(0), debilitated Libra(6)
    // Moon exalted Taurus(1), debilitated Scorpio(7)
    // Mars exalted Capricorn(9), debilitated Cancer(3)
    // Mercury exalted Virgo(5), debilitated Pisces(11)
    // Jupiter exalted Cancer(3), debilitated Capricorn(9)
    // Venus exalted Pisces(11), debilitated Virgo(5)
    // Saturn exalted Libra(6), debilitated Aries(0)
    // Rahu/Ketu — conventional: Rahu exalted Gemini(2)/Taurus(1), Ketu exalted Sagittarius(8)/Scorpio(7)

    private static final Map<String, int[]> EXALT_DEBIL = Map.of(
        "Sun",     new int[]{0,  6},
        "Moon",    new int[]{1,  7},
        "Mars",    new int[]{9,  3},
        "Mercury", new int[]{5, 11},
        "Jupiter", new int[]{3,  9},
        "Venus",   new int[]{11, 5},
        "Saturn",  new int[]{6,  0}
    );

    private static final Map<String, int[]> OWN_SIGNS = Map.of(
        "Sun",     new int[]{4},         // Leo
        "Moon",    new int[]{3},         // Cancer
        "Mars",    new int[]{0, 7},      // Aries, Scorpio
        "Mercury", new int[]{2, 5},      // Gemini, Virgo
        "Jupiter", new int[]{8, 11},     // Sagittarius, Pisces
        "Venus",   new int[]{1, 6},      // Taurus, Libra
        "Saturn",  new int[]{9, 10}     // Aquarius, Capricorn (Capricorn also considered)
    );

    private static final Map<String, int[][]> BAV_RULES = Map.of(
        "Sun", new int[][] {
            {1,2,4,7,8,9,10,11}, {3,6,10,11}, {1,2,4,7,8,9,10,11}, {3,5,6,9,10,11,12},
            {5,6,9,11}, {6,7,12}, {1,2,4,7,8,9,10,11}, {3,4,6,10,11,12}
        },
        "Moon", new int[][] {
            {3,6,7,8,10,11}, {1,3,6,7,10,11}, {2,3,5,6,9,10,11}, {1,3,4,5,7,8,10,11},
            {1,4,7,8,10,11,12}, {3,4,5,7,9,10,11}, {3,5,6,11}, {3,6,10,11}
        },
        "Mars", new int[][] {
            {3,5,6,10,11}, {3,6,11}, {1,2,4,7,8,10,11}, {3,5,6,11},
            {6,10,11,12}, {6,8,11,12}, {1,4,7,8,9,10,11}, {1,3,6,10,11}
        },
        "Mercury", new int[][] {
            {5,6,9,11,12}, {2,4,6,8,10,11}, {1,2,4,7,8,9,10,11}, {1,3,5,6,9,10,11,12},
            {6,8,11,12}, {1,2,3,4,5,8,9,11}, {1,2,4,7,8,9,10,11}, {1,2,4,6,8,10,11}
        },
        "Jupiter", new int[][] {
            {1,2,3,4,7,8,9,10,11}, {2,5,7,9,11}, {1,2,4,7,8,10,11}, {1,2,4,5,6,9,10,11},
            {1,2,3,4,7,8,10,11}, {2,5,6,9,10,11}, {3,5,6,12}, {1,2,4,5,6,7,9,10,11}
        },
        "Venus", new int[][] {
            {8,11,12}, {1,2,3,4,5,8,9,11,12}, {3,5,6,9,11,12}, {3,5,6,9,11},
            {5,8,9,10,11}, {1,2,3,4,5,8,9,10,11}, {3,4,5,8,9,10,11}, {1,2,3,4,5,8,9,11}
        },
        "Saturn", new int[][] {
            {1,2,4,7,8,10,11}, {3,6,11}, {3,5,6,10,11,12}, {6,8,9,10,11,12},
            {5,6,11,12}, {6,11,12}, {3,5,6,11}, {1,3,4,6,10,11}
        }
    );

    private final CityService cityService;

    public VedicService(CityService cityService) {
        this.cityService = cityService;
    }

    // ─── Main entry point ─────────────────────────────────────────────────────

    public VedicChartResponse compute(BirthRequest req) {
        // ── Parse input ───────────────────────────────────────────────────────
        CityInfo city = resolveCity(req.getCity());

        // Convert IST → UT (subtract 5h 30m = 5.5 hours)
        var utc = req.utcDateTime();
        double jd = AstroMath.julianDay(utc);

        double T        = AstroMath.julianCenturies(jd);
        double eps      = AstroMath.obliquity(T);
        double lst      = AstroMath.localSiderealTime(jd, city.getLongitude());
        double ramc     = lst;   // RAMC = LST in degrees
        double ayanamsa = AstroMath.lahiriAyanamsa(jd);

        double ascTrop  = AstroMath.ascendant(ramc, eps, city.getLatitude());
        double ascSid   = AstroMath.norm360(ascTrop - ayanamsa);

        // ── Planetary positions ───────────────────────────────────────────────
        Map<String, double[]> rawPlanets = Ephemeris.allPlanets(jd);  // name → [lon, retro]

        List<PlanetPosition> planets = new ArrayList<>();
        for (Map.Entry<String, double[]> e : rawPlanets.entrySet()) {
            double tropLon  = e.getValue()[0];
            boolean retro   = e.getValue()[1] == 1.0;
            double sidLon   = AstroMath.norm360(tropLon - ayanamsa);
            planets.add(buildPlanetPosition(e.getKey(), tropLon, sidLon, retro, ascSid));
        }

        // ── Lagna (Ascendant) ─────────────────────────────────────────────────
        int    lagnaRashi  = (int)(ascSid / 30.0);
        double lagnaInSign = ascSid % 30.0;
        String lagnaSign   = RASHIS[lagnaRashi];

        PlanetPosition moonPos = planets.stream()
            .filter(p -> "Moon".equals(p.getName())).findFirst().orElseThrow();

        int lagnaNakshatra = (int) (ascSid / (360.0 / 27));
        int lagnaPada = (int) ((ascSid % (360.0 / 27)) / (360.0 / 108)) + 1;
        VedicChartResponse.Lagna lagna = new VedicChartResponse.Lagna(
            lagnaSign, lagnaRashi + 1, lagnaInSign,
            NAKSHATRAS[lagnaNakshatra], lagnaPada,
            navamsaSign(ascSid)
        );

        // ── Whole-sign houses ─────────────────────────────────────────────────
        List<VedicChartResponse.House> houses = buildWholeSignHouses(lagnaRashi, planets);

        // ── Dasha ─────────────────────────────────────────────────────────────
        DashaInfo dasha = computeDasha(AstroMath.norm360(rawPlanets.get("Moon")[0] - ayanamsa), req.getDob());

        // ── Yogas ─────────────────────────────────────────────────────────────
        List<String> yogas = detectYogas(planets, lagnaRashi);

        // ── Aspects (Vedic: graha drishti) ────────────────────────────────────
        List<VedicChartResponse.Aspect> aspects = computeVedicAspects(planets);

        // ── Ashtakavarga summary ──────────────────────────────────────────────
        Map<String, Object> avSummary = ashtakavargaSummary(planets, lagnaRashi);

        // ── Build response ────────────────────────────────────────────────────
        String utcStr = utc.toInstant().toString();

        VedicChartResponse resp = new VedicChartResponse();
        resp.setInput(new VedicChartResponse.InputSummary(
            req.getDob(), req.getTime(), city.getName(),
            city.getLatitude(), city.getLongitude(),
            utcStr, jd,
            Math.round(ayanamsa * 1000) / 1000.0, "Lahiri (Chitrapaksha)"
        ));
        resp.setLagna(lagna);
        resp.setPlanets(planets);
        resp.setHouses(houses);
        resp.setDasha(dasha);
        resp.setYogas(yogas);
        resp.setAspects(aspects);
        resp.setAshtakavargaSummary(avSummary);
        return resp;
    }

    // ─── Planet position builder ──────────────────────────────────────────────

    private PlanetPosition buildPlanetPosition(String name, double tropLon, double sidLon,
                                               boolean retro, double ascSid) {
        PlanetPosition p = new PlanetPosition();
        p.setName(name);
        p.setTropicalLongitude(round2(tropLon));
        p.setSiderealLongitude(round2(sidLon));

        int rashiIdx = (int)(sidLon / 30.0);
        p.setRashi(rashiIdx + 1);
        p.setRashiName(RASHIS[rashiIdx]);
        p.setDegreeInSign(round2(sidLon % 30.0));

        int nakshatraIdx = (int)(sidLon / (360.0 / 27.0));
        nakshatraIdx = Math.min(nakshatraIdx, 26);
        p.setNakshatra(nakshatraIdx + 1);
        p.setNakshatraName(NAKSHATRAS[nakshatraIdx]);

        int dashaLordIdx = nakshatraIdx % 9;
        p.setNakshatraLord(DASHA_LORDS[dashaLordIdx]);

        double nakshatraSpan = 360.0 / 27.0;        // 13.333°
        double padaSpan      = nakshatraSpan / 4.0;  // 3.333°
        double posInNak      = sidLon % nakshatraSpan;
        p.setPada((int)(posInNak / padaSpan) + 1);

        // Whole-sign house (1-based, lagna rashi = house 1)
        int lagnaRashi = (int)(ascSid / 30.0);
        int house = ((rashiIdx - lagnaRashi + 12) % 12) + 1;
        p.setHouse(house);

        p.setRetrograde(retro);
        p.setDignity(dignity(name, rashiIdx));
        return p;
    }

    // ─── Whole-sign houses ────────────────────────────────────────────────────

    private List<VedicChartResponse.House> buildWholeSignHouses(int lagnaRashi,
                                                                  List<PlanetPosition> planets) {
        List<VedicChartResponse.House> houses = new ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            int signIdx = (lagnaRashi + h - 1) % 12;
            final int hFinal = h;
            List<String> planetsInHouse = planets.stream()
                .filter(p -> p.getHouse() == hFinal)
                .map(PlanetPosition::getName)
                .toList();
            houses.add(new VedicChartResponse.House(h, RASHIS[signIdx], signIdx + 1, planetsInHouse));
        }
        return houses;
    }

    // ─── Vimshottari Dasha ────────────────────────────────────────────────────

    private DashaInfo computeDasha(double moonSid, String dobStr) {
        double nakshatraSpan = 360.0 / 27.0;
        int nakshatraIdx = (int)(moonSid / nakshatraSpan);
        nakshatraIdx = Math.min(nakshatraIdx, 26);

        int    birthLordIdx  = nakshatraIdx % 9;
        double posInNak      = moonSid % nakshatraSpan;
        double fracElapsed   = posInNak / nakshatraSpan;
        double balanceYears  = (1.0 - fracElapsed) * DASHA_YEARS[birthLordIdx];

        LocalDate birthDate = LocalDate.parse(dobStr, DateTimeFormatter.ISO_LOCAL_DATE);

        // Build full 120-year sequence from birth
        List<DashaInfo.MahaPeriod> sequence = new ArrayList<>();
        double horizonDays = DASHA_TOTAL * 365.25;

        // First maha: partial (balance)
        double mahaStart = -(DASHA_YEARS[birthLordIdx] - balanceYears) * 365.25;
        double cursor = balanceYears * 365.25;
        DashaInfo.MahaPeriod firstMaha = buildMahaPeriod(birthLordIdx, birthDate, mahaStart, cursor, horizonDays);
        sequence.add(firstMaha);

        // Subsequent full mahas
        for (int k = 1; k <= 9 && cursor < horizonDays; k++) {
            int lordIdx = (birthLordIdx + k) % 9;
            double end = cursor + DASHA_YEARS[lordIdx] * 365.25;
            DashaInfo.MahaPeriod maha = buildMahaPeriod(lordIdx, birthDate, cursor, end, horizonDays);
            if (maha != null) sequence.add(maha);
            cursor = end;
        }

        // Current running dashas
        LocalDate today = LocalDate.now(java.time.ZoneOffset.ofHoursMinutes(5, 30));
        DashaInfo.MahaPeriod currentMaha = null;
        DashaInfo.AntarPeriod currentAntar = null;
        DashaInfo.Period currentPratyantar = null;

        outer:
        for (DashaInfo.MahaPeriod m : sequence) {
            if (!today.isBefore(LocalDate.parse(m.start())) &&
                today.isBefore(LocalDate.parse(m.end()))) {
                currentMaha = m;
                for (DashaInfo.AntarPeriod a : m.antardashas()) {
                    if (!today.isBefore(LocalDate.parse(a.start())) &&
                        today.isBefore(LocalDate.parse(a.end()))) {
                        currentAntar = a;
                        for (DashaInfo.Period pt : a.pratyantardashas()) {
                            if (!today.isBefore(LocalDate.parse(pt.start())) &&
                                today.isBefore(LocalDate.parse(pt.end()))) {
                                currentPratyantar = pt;
                                break;
                            }
                        }
                        break outer;
                    }
                }
                break;
            }
        }

        DashaInfo info = new DashaInfo();
        info.setBirthMahadasha(DASHA_LORDS[birthLordIdx]);
        DashaInfo.AntarPeriod birthAntar = firstMaha.antardashas().getFirst();
        info.setBirthAntardasha(birthAntar.lord());
        info.setBirthPratyantardasha(birthAntar.pratyantardashas().getFirst().lord());
        if (currentMaha != null) {
            info.setCurrentMahadasha(new DashaInfo.Period(
                currentMaha.lord(), currentMaha.start(), currentMaha.end(), currentMaha.durationYears()));
        }
        info.setCurrentAntardasha(currentAntar);
        info.setCurrentPratyantardasha(currentPratyantar);
        info.setDashaSequence(sequence);
        return info;
    }

    private DashaInfo.MahaPeriod buildMahaPeriod(int lordIdx, LocalDate birthDate,
                                                 double start, double end, double horizonDays) {
        DashaInfo.Period maha = dashaPeriod(lordIdx, birthDate, start, end, horizonDays);
        if (maha == null) return null;
        List<DashaInfo.AntarPeriod> antars = new ArrayList<>();
        double cursor = start;
        int weight = 0;
        for (int k = 0; k < 9; k++) {
            int antarLordIdx = (lordIdx + k) % 9;
            weight += DASHA_YEARS[antarLordIdx];
            double antarEnd = k == 8 ? end : start + (end - start) * weight / DASHA_TOTAL;
            DashaInfo.Period antar = dashaPeriod(antarLordIdx, birthDate, cursor, antarEnd, horizonDays);
            if (antar != null) {
                antars.add(new DashaInfo.AntarPeriod(antar.lord(), antar.start(), antar.end(),
                    antar.durationYears(), buildPratyantardashas(antarLordIdx, birthDate, cursor, antarEnd, horizonDays)));
            }
            cursor = antarEnd;
        }
        return new DashaInfo.MahaPeriod(maha.lord(), maha.start(), maha.end(), maha.durationYears(), antars);
    }

    private List<DashaInfo.Period> buildPratyantardashas(int antarLordIdx, LocalDate birthDate,
                                                       double start, double end, double horizonDays) {
        List<DashaInfo.Period> list = new ArrayList<>();
        double cursor = start;
        int weight = 0;
        for (int k = 0; k < 9; k++) {
            int lordIdx = (antarLordIdx + k) % 9;
            weight += DASHA_YEARS[lordIdx];
            double ptEnd = k == 8 ? end : start + (end - start) * weight / DASHA_TOTAL;
            DashaInfo.Period period = dashaPeriod(lordIdx, birthDate, cursor, ptEnd, horizonDays);
            if (period != null) list.add(period);
            cursor = ptEnd;
        }
        return list;
    }

    private DashaInfo.Period dashaPeriod(int lordIdx, LocalDate birthDate,
                                        double start, double end, double horizonDays) {
        double clippedStart = Math.max(0, start);
        double clippedEnd = Math.min(horizonDays, end);
        if (clippedEnd <= clippedStart) return null;
        LocalDate startDate = birthDate.plusDays((long) Math.ceil(clippedStart));
        LocalDate endDate = birthDate.plusDays((long) Math.ceil(clippedEnd));
        if (!endDate.isAfter(startDate)) return null;
        return new DashaInfo.Period(DASHA_LORDS[lordIdx], startDate.toString(), endDate.toString(),
            round2((clippedEnd - clippedStart) / 365.25));
    }

    // ─── Yoga detection (major, commonly checked) ─────────────────────────────

    private List<String> detectYogas(List<PlanetPosition> planets, int lagnaRashi) {
        List<String> yogas = new ArrayList<>();
        Map<String, PlanetPosition> pm = new HashMap<>();
        for (PlanetPosition p : planets) pm.put(p.getName(), p);

        PlanetPosition sun     = pm.get("Sun");
        PlanetPosition moon    = pm.get("Moon");
        PlanetPosition mars    = pm.get("Mars");
        PlanetPosition mercury = pm.get("Mercury");
        PlanetPosition jupiter = pm.get("Jupiter");
        PlanetPosition venus   = pm.get("Venus");
        PlanetPosition saturn  = pm.get("Saturn");

        // Gaja Kesari: Jupiter in kendra (1,4,7,10) from Moon
        if (jupiter != null && moon != null) {
            int diff = ((jupiter.getHouse() - moon.getRashi() + 12) % 12) + 1;
            // kendra from Moon means 1, 4, 7, 10th from Moon's rashi
            int rel = ((jupiter.getRashi() - moon.getRashi() + 12) % 12) + 1;
            if (rel == 1 || rel == 4 || rel == 7 || rel == 10) {
                yogas.add("Gaja Kesari Yoga (Jupiter in kendra from Moon)");
            }
        }

        // Panch Mahapurusha Yogas (planet in own/exaltation + kendra from Lagna)
        checkMahapurusha(mars,    "Ruchaka",  lagnaRashi, yogas);
        checkMahapurusha(mercury, "Bhadra",   lagnaRashi, yogas);
        checkMahapurusha(jupiter, "Hamsa",    lagnaRashi, yogas);
        checkMahapurusha(venus,   "Malavya",  lagnaRashi, yogas);
        checkMahapurusha(saturn,  "Sasa",     lagnaRashi, yogas);

        // Budhaditya Yoga: Sun + Mercury in same sign
        if (sun != null && mercury != null && sun.getRashi() == mercury.getRashi()) {
            yogas.add("Budhaditya Yoga (Sun + Mercury conjunct)");
        }

        // Chandra-Mangal Yoga: Moon + Mars conjunct
        if (moon != null && mars != null && moon.getRashi() == mars.getRashi()) {
            yogas.add("Chandra-Mangal Yoga (Moon + Mars conjunct)");
        }

        // Kemadruma Yoga: no planet in 2nd or 12th from Moon (malefic)
        if (moon != null) {
            int moonRashi = moon.getRashi();
            int sign2  = (moonRashi % 12) + 1;
            int sign12 = ((moonRashi - 2 + 12) % 12) + 1;
            boolean has2  = planets.stream().anyMatch(p -> !p.getName().equals("Moon")
                && !p.getName().equals("Rahu") && !p.getName().equals("Ketu")
                && p.getRashi() == sign2);
            boolean has12 = planets.stream().anyMatch(p -> !p.getName().equals("Moon")
                && !p.getName().equals("Rahu") && !p.getName().equals("Ketu")
                && p.getRashi() == sign12);
            if (!has2 && !has12) yogas.add("Kemadruma Yoga (Moon isolated)");
        }

        // Adhi Yoga: benefics (Mercury, Jupiter, Venus) in 6,7,8 from Moon
        if (moon != null) {
            int m6  = ((moon.getRashi() + 4) % 12) + 1;
            int m7  = ((moon.getRashi() + 5) % 12) + 1;
            int m8  = ((moon.getRashi() + 6) % 12) + 1;
            Set<Integer> adhi = new HashSet<>(Set.of(m6, m7, m8));
            boolean jIn = jupiter != null && adhi.contains(jupiter.getRashi());
            boolean vIn = venus   != null && adhi.contains(venus.getRashi());
            boolean mIn = mercury != null && adhi.contains(mercury.getRashi());
            if (jIn && vIn && mIn) yogas.add("Adhi Yoga (benefics in 6-7-8 from Moon)");
        }

        return yogas;
    }

    private void checkMahapurusha(PlanetPosition p, String name, int lagnaRashi,
                                   List<String> yogas) {
        if (p == null) return;
        String dignity = p.getDignity();
        if (!"Own".equals(dignity) && !"Exalted".equals(dignity)) return;
        int house = p.getHouse();
        if (house == 1 || house == 4 || house == 7 || house == 10) {
            yogas.add(name + " Yoga (" + p.getName() + " in kendra in " + dignity + " sign)");
        }
    }

    // ─── Vedic aspects (Graha drishti) ───────────────────────────────────────
    // All planets aspect the 7th from their position.
    // Mars also aspects 4th and 8th; Jupiter aspects 5th and 9th; Saturn aspects 3rd and 10th.

    private List<VedicChartResponse.Aspect> computeVedicAspects(List<PlanetPosition> planets) {
        List<VedicChartResponse.Aspect> aspects = new ArrayList<>();
        Map<String, PlanetPosition> pm = new HashMap<>();
        for (PlanetPosition p : planets) pm.put(p.getName(), p);

        for (PlanetPosition source : planets) {
            if ("Rahu".equals(source.getName()) || "Ketu".equals(source.getName())) continue;
            Set<Integer> aspectedHouses = new HashSet<>();
            aspectedHouses.add(((source.getHouse() - 1 + 6) % 12) + 1); // 7th aspect
            if ("Mars".equals(source.getName())) {
                aspectedHouses.add(((source.getHouse() - 1 + 3) % 12) + 1);  // 4th
                aspectedHouses.add(((source.getHouse() - 1 + 7) % 12) + 1);  // 8th
            }
            if ("Jupiter".equals(source.getName())) {
                aspectedHouses.add(((source.getHouse() - 1 + 4) % 12) + 1);  // 5th
                aspectedHouses.add(((source.getHouse() - 1 + 8) % 12) + 1);  // 9th
            }
            if ("Saturn".equals(source.getName())) {
                aspectedHouses.add(((source.getHouse() - 1 + 2) % 12) + 1);  // 3rd
                aspectedHouses.add(((source.getHouse() - 1 + 9) % 12) + 1);  // 10th
            }

            for (PlanetPosition target : planets) {
                if (target.getName().equals(source.getName())) continue;
                if (aspectedHouses.contains(target.getHouse())) {
                    double angle = AstroMath.norm360(
                        target.getSiderealLongitude() - source.getSiderealLongitude());
                    aspects.add(new VedicChartResponse.Aspect(
                        source.getName(), target.getName(),
                        round2(angle), "Graha Drishti"));
                }
            }
        }
        return aspects;
    }

    // ─── Ashtakavarga summary ─────────────────────────────────────────────────
    // Returns sign-wise bindu count for each planet (simplified: uses fixed BAV totals)

    private Map<String, Object> ashtakavargaSummary(List<PlanetPosition> planets, int lagnaRashi) {
        // Simplified: compute each planet's BAV score in the transiting sign
        // Full BAV requires checking 8 reference points per planet (complex table lookup).
        // Here we return the reference totals and transiting planet sign scores.
        Map<String, Object> summary = new LinkedHashMap<>();
        int[] bavTotals = {48, 49, 39, 54, 56, 52, 39};  // Sun,Moon,Mars,Mercury,Jupiter,Venus,Saturn
        String[] bavPlanets = {"Sun","Moon","Mars","Mercury","Jupiter","Venus","Saturn"};
        int[] referenceSigns = new int[8];
        for (int i = 0; i < bavPlanets.length; i++) {
            String name = bavPlanets[i];
            referenceSigns[i] = planets.stream().filter(p -> name.equals(p.getName()))
                .findFirst().orElseThrow().getRashi() - 1;
        }
        referenceSigns[7] = lagnaRashi;
        int[] sarv = new int[12];
        Map<String, int[]> bav = new LinkedHashMap<>();
        Map<String, Integer> bavMap = new LinkedHashMap<>();
        for (int i = 0; i < bavPlanets.length; i++) {
            int[] scores = new int[12];
            int[][] rules = BAV_RULES.get(bavPlanets[i]);
            for (int reference = 0; reference < referenceSigns.length; reference++) {
                for (int house : rules[reference]) {
                    scores[(referenceSigns[reference] + house - 1) % 12]++;
                }
            }
            int total = Arrays.stream(scores).sum();
            if (total != bavTotals[i]) throw new IllegalStateException("Invalid Ashtakavarga rule table");
            bav.put(bavPlanets[i], scores);
            bavMap.put(bavPlanets[i], total);
            for (int sign = 0; sign < 12; sign++) sarv[sign] += scores[sign];
        }
        summary.put("note", "Unreduced Parashari BAV and SAV from seven planets and Lagna; no Trikona or Ekadhipatya reductions.");
        summary.put("signs", List.of(RASHIS));
        summary.put("bhinnashtakavarga", bav);
        summary.put("sarvashtakavarga", sarv);
        summary.put("sarvashtakavargaTotal", Arrays.stream(sarv).sum());
        summary.put("bhinnashtakavargaTotals", bavMap);
        return summary;
    }

    // ─── Navamsa sign (D9) ────────────────────────────────────────────────────

    private String navamsaSign(double sidLon) {
        int rashi = (int)(sidLon / 30.0);
        int navamsaNum = (int)((sidLon % 30.0) / (30.0 / 9.0)); // 0-8
        // Starting sign for navamsa depends on rashi type
        int startSignIdx;
        if (rashi % 3 == 0) startSignIdx = rashi;          // movable: same sign
        else if (rashi % 3 == 1) startSignIdx = rashi + 8; // fixed: 9th sign
        else startSignIdx = rashi + 4;                       // dual: 5th sign
        return RASHIS[(startSignIdx + navamsaNum) % 12];
    }

    // ─── Dignity ──────────────────────────────────────────────────────────────

    private String dignity(String planet, int rashiIdx) {
        int[] ed = EXALT_DEBIL.get(planet);
        if (ed != null) {
            if (ed[0] == rashiIdx) return "Exalted";
            if (ed[1] == rashiIdx) return "Debilitated";
        }
        int[] own = OWN_SIGNS.get(planet);
        if (own != null) {
            for (int s : own) if (s == rashiIdx) return "Own";
        }
        return "Neutral";
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private CityInfo resolveCity(String name) {
        CityInfo c = cityService.findCity(name);
        if (c == null) throw new IllegalArgumentException(
            "City not found: '" + name + "'. Call GET /api/astro/cities for the list.");
        return c;
    }

    private static int[] parseDate(String dob) {
        String[] p = dob.split("-");
        return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])};
    }

    private static int[] parseTime(String t) {
        String[] p = t.split(":");
        return new int[]{
            Integer.parseInt(p[0]),
            Integer.parseInt(p[1]),
            p.length > 2 ? Integer.parseInt(p[2]) : 0
        };
    }

    /** Adjusts calendar date for UT overflow (e.g. negative UT hour crosses midnight). */
    private static int[] adjustDate(int year, int month, int day, double hour) {
        int[] result = {year, month, day};
        if (hour < 0) {
            result[2]--;
            if (result[2] == 0) {
                result[1]--;
                if (result[1] == 0) { result[1] = 12; result[0]--; }
                result[2] = daysInMonth(result[0], result[1]);
            }
        }
        return result;
    }

    private static int daysInMonth(int year, int month) {
        return switch (month) {
            case 4,6,9,11 -> 30;
            case 2 -> (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default -> 31;
        };
    }

    private static LocalDate addYears(LocalDate date, double years) {
        long totalDays = Math.round(years * 365.25);
        return date.plusDays(totalDays);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
