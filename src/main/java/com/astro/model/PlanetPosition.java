package com.astro.model;

public class PlanetPosition {
    private String name;
    private double tropicalLongitude;     // raw ecliptic longitude (tropical)
    private double siderealLongitude;     // after ayanamsa subtraction
    private int    rashi;                 // 1-12 (Aries=1)
    private String rashiName;
    private double degreeInSign;          // 0-30
    private int    nakshatra;             // 1-27
    private String nakshatraName;
    private String nakshatraLord;
    private int    pada;                  // 1-4
    private int    house;                 // 1-12 (whole-sign)
    private boolean retrograde;
    private String dignity;               // Own/Exalted/Debilitated/Neutral

    public PlanetPosition() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String  getName()               { return name; }
    public void    setName(String n)       { this.name = n; }
    public double  getTropicalLongitude()  { return tropicalLongitude; }
    public void    setTropicalLongitude(double v) { this.tropicalLongitude = v; }
    public double  getSiderealLongitude()  { return siderealLongitude; }
    public void    setSiderealLongitude(double v) { this.siderealLongitude = v; }
    public int     getRashi()              { return rashi; }
    public void    setRashi(int v)         { this.rashi = v; }
    public String  getRashiName()          { return rashiName; }
    public void    setRashiName(String v)  { this.rashiName = v; }
    public double  getDegreeInSign()       { return degreeInSign; }
    public void    setDegreeInSign(double v) { this.degreeInSign = v; }
    public int     getNakshatra()          { return nakshatra; }
    public void    setNakshatra(int v)     { this.nakshatra = v; }
    public String  getNakshatraName()      { return nakshatraName; }
    public void    setNakshatraName(String v) { this.nakshatraName = v; }
    public String  getNakshatraLord()      { return nakshatraLord; }
    public void    setNakshatraLord(String v) { this.nakshatraLord = v; }
    public int     getPada()               { return pada; }
    public void    setPada(int v)          { this.pada = v; }
    public int     getHouse()              { return house; }
    public void    setHouse(int v)         { this.house = v; }
    public boolean isRetrograde()          { return retrograde; }
    public void    setRetrograde(boolean v){ this.retrograde = v; }
    public String  getDignity()            { return dignity; }
    public void    setDignity(String v)    { this.dignity = v; }
}
