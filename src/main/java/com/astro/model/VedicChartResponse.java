package com.astro.model;

import java.util.List;
import java.util.Map;

public class VedicChartResponse {

    public record InputSummary(String dob, String time, String city,
                                double latitude, double longitude,
                                String utcDateTime, double julianDay,
                                double ayanamsa, String ayanamsaName) {}

    public record Lagna(String sign, int rashi, double degree,
                         String nakshatra, int pada, String navamsaSign) {}

    public record House(int house, String sign, int rashi,
                         List<String> planets) {}

    public record Aspect(String planet1, String planet2,
                          double angle, String type) {}

    private InputSummary          input;
    private Lagna                 lagna;
    private List<PlanetPosition>  planets;
    private List<House>           houses;
    private DashaInfo             dasha;
    private Map<String, Object>   ashtakavargaSummary;
    private List<String>          yogas;
    private List<Aspect>          aspects;

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public InputSummary         getInput()               { return input; }
    public void                 setInput(InputSummary v) { this.input = v; }
    public Lagna                getLagna()               { return lagna; }
    public void                 setLagna(Lagna v)        { this.lagna = v; }
    public List<PlanetPosition> getPlanets()             { return planets; }
    public void                 setPlanets(List<PlanetPosition> v) { this.planets = v; }
    public List<House>          getHouses()              { return houses; }
    public void                 setHouses(List<House> v) { this.houses = v; }
    public DashaInfo            getDasha()               { return dasha; }
    public void                 setDasha(DashaInfo v)    { this.dasha = v; }
    public Map<String, Object>  getAshtakavargaSummary() { return ashtakavargaSummary; }
    public void                 setAshtakavargaSummary(Map<String, Object> v) { this.ashtakavargaSummary = v; }
    public List<String>         getYogas()               { return yogas; }
    public void                 setYogas(List<String> v) { this.yogas = v; }
    public List<Aspect>         getAspects()             { return aspects; }
    public void                 setAspects(List<Aspect> v) { this.aspects = v; }
}
