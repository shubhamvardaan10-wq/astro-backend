package com.astro.service;

import com.astro.model.*;
import com.astro.util.AstroMath;
import com.astro.util.Ephemeris;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Western (tropical) chart computation.
 * House system: Placidus (approximated via equal-house for simplicity at
 * this layer; full iterative Placidus is added in a future phase).
 * Aspects: Ptolemaic major aspects with standard orbs.
 */
@Service
public class WesternService {

    private static final String[] SIGNS = {
        "Aries","Taurus","Gemini","Cancer","Leo","Virgo",
        "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces"
    };

    // Major Ptolemaic aspects: angle → name
    private record AspectDef(double angle, String name, double orb) {}
    private static final List<AspectDef> ASPECTS = List.of(
        new AspectDef(  0, "Conjunction", 8.0),
        new AspectDef( 60, "Sextile",     6.0),
        new AspectDef( 90, "Square",      8.0),
        new AspectDef(120, "Trine",       8.0),
        new AspectDef(150, "Quincunx",    3.0),
        new AspectDef(180, "Opposition",  8.0)
    );

    private final CityService cityService;

    public WesternService(CityService cityService) {
        this.cityService = cityService;
    }

    public WesternChartResponse compute(BirthRequest req) {
        CityInfo city = cityService.findCity(req.getCity());
        if (city == null) throw new IllegalArgumentException("City not found: " + req.getCity());

        var utc = req.utcDateTime();
        // Fine-tune with fractional minutes
        double jd = AstroMath.julianDay(utc);

        double T    = AstroMath.julianCenturies(jd);
        double eps  = AstroMath.obliquity(T);
        double lst  = AstroMath.localSiderealTime(jd, city.getLongitude());
        double asc  = AstroMath.ascendant(lst, eps, city.getLatitude());
        double mc   = AstroMath.midheaven(lst, eps);
        double[] cusps = AstroMath.placidusCusps(lst, eps, city.getLatitude());

        // Tropical planets (no ayanamsa)
        Map<String, double[]> raw = Ephemeris.allPlanets(jd);
        List<PlanetPosition> planets = new ArrayList<>();
        for (Map.Entry<String, double[]> e : raw.entrySet()) {
            double lon  = e.getValue()[0];
            boolean ret = e.getValue()[1] == 1.0;
            planets.add(buildTropicalPlanet(e.getKey(), lon, ret, cusps));
        }

        // Equal house cusps from Ascendant
        List<WesternChartResponse.HouseCusp> houses = new ArrayList<>();
        for (int h = 1; h <= 12; h++) {
            double cusp = cusps[h - 1];
            int signIdx = (int)(cusp / 30.0);
            houses.add(new WesternChartResponse.HouseCusp(h, SIGNS[signIdx], cusp));
        }

        List<WesternChartResponse.Aspect> aspects = computeAspects(planets, Ephemeris.allPlanets(jd + 1.0 / 1440));

        int ascSign = (int)(asc / 30.0);
        int mcSign  = (int)(mc  / 30.0);

        WesternChartResponse resp = new WesternChartResponse();
        resp.setInput(new VedicChartResponse.InputSummary(
            req.getDob(), req.getTime(), city.getName(),
            city.getLatitude(), city.getLongitude(),
            utc.toInstant().toString(), jd, 0, "none (tropical)"));
        resp.setAscendant(new WesternChartResponse.Angle("Ascendant", SIGNS[ascSign], round2(asc % 30)));
        resp.setMidheaven(new WesternChartResponse.Angle("MC", SIGNS[mcSign], round2(mc % 30)));
        resp.setPlanets(planets);
        resp.setHouses(houses);
        resp.setAspects(aspects);
        return resp;
    }

    private PlanetPosition buildTropicalPlanet(String name, double lon, boolean retro, double[] cusps) {
        PlanetPosition p = new PlanetPosition();
        p.setName(name);
        p.setTropicalLongitude(lon);
        p.setSiderealLongitude(lon); // tropical — no conversion
        int signIdx = (int)(lon / 30.0);
        p.setRashi(signIdx + 1);
        p.setRashiName(SIGNS[signIdx]);
        p.setDegreeInSign(round2(lon % 30.0));
        p.setHouse(houseOf(lon, cusps));
        p.setRetrograde(retro);
        return p;
    }

    private List<WesternChartResponse.Aspect> computeAspects(List<PlanetPosition> planets, Map<String, double[]> nextPositions) {
        List<WesternChartResponse.Aspect> results = new ArrayList<>();
        List<PlanetPosition> list = planets.stream()
            .filter(p -> !p.getName().equals("Rahu") && !p.getName().equals("Ketu"))
            .toList();

        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                PlanetPosition a = list.get(i), b = list.get(j);
                double diff = Math.abs(AstroMath.norm180(
                    b.getTropicalLongitude() - a.getTropicalLongitude()));
                for (AspectDef def : ASPECTS) {
                    double orb = Math.abs(diff - def.angle());
                    if (orb <= def.orb()) {
                        // Applying: faster planet moving toward exact aspect
                        double nextAngle = Math.abs(AstroMath.norm180(
                            nextPositions.get(b.getName())[0] - nextPositions.get(a.getName())[0]));
                        boolean applying = Math.abs(nextAngle - def.angle()) < orb;
                        results.add(new WesternChartResponse.Aspect(
                            a.getName(), b.getName(), round2(diff), def.name(), round2(orb), applying));
                    }
                }
            }
        }
        return results;
    }

    static int houseOf(double longitude, double[] cusps) {
        for (int i = 0; i < cusps.length; i++) {
            double span = AstroMath.norm360(cusps[(i + 1) % cusps.length] - cusps[i]);
            if (AstroMath.norm360(longitude - cusps[i]) < span) return i + 1;
        }
        throw new IllegalArgumentException("Longitude is not within the house cusps");
    }
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
