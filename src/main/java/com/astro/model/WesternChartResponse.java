package com.astro.model;

import java.util.List;

public class WesternChartResponse {

    public record Angle(String name, String sign, double degree) {}
    public record Aspect(String planet1, String planet2, double angle,
                          String type, double orb, boolean applying) {}
    public record HouseCusp(int house, String sign, double degree) {}

    private VedicChartResponse.InputSummary input;
    private String                 houseSystem = "Placidus";
    private Angle                  ascendant;
    private Angle                  midheaven;
    private List<PlanetPosition>   planets;   // tropical sign
    private List<HouseCusp>        houses;    // Placidus approximated
    private List<Aspect>           aspects;

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public VedicChartResponse.InputSummary getInput()          { return input; }
    public void setInput(VedicChartResponse.InputSummary v)    { this.input = v; }
    public String             getHouseSystem()               { return houseSystem; }
    public Angle              getAscendant()                    { return ascendant; }
    public void               setAscendant(Angle v)            { this.ascendant = v; }
    public Angle              getMidheaven()                    { return midheaven; }
    public void               setMidheaven(Angle v)            { this.midheaven = v; }
    public List<PlanetPosition> getPlanets()                   { return planets; }
    public void               setPlanets(List<PlanetPosition> v){ this.planets = v; }
    public List<HouseCusp>    getHouses()                      { return houses; }
    public void               setHouses(List<HouseCusp> v)     { this.houses = v; }
    public List<Aspect>       getAspects()                     { return aspects; }
    public void               setAspects(List<Aspect> v)       { this.aspects = v; }
}
