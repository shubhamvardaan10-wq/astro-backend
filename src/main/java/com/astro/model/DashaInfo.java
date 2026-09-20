package com.astro.model;

import java.util.List;

public class DashaInfo {

    public record Period(String lord, String start, String end, double durationYears) {}

    public record AntarPeriod(String lord, String start, String end,
                               double durationYears, List<Period> pratyantardashas) {}

    public record MahaPeriod(String lord, String start, String end,
                              double durationYears, List<AntarPeriod> antardashas) {}

    private String          birthMahadasha;
    private String          birthAntardasha;
    private String          birthPratyantardasha;
    private Period          currentMahadasha;
    private AntarPeriod     currentAntardasha;
    private Period          currentPratyantardasha;
    private List<MahaPeriod> dashaSequence;   // next 120 years from birth

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String       getBirthMahadasha()              { return birthMahadasha; }
    public void         setBirthMahadasha(String v)      { this.birthMahadasha = v; }
    public String       getBirthAntardasha()             { return birthAntardasha; }
    public void         setBirthAntardasha(String v)     { this.birthAntardasha = v; }
    public String       getBirthPratyantardasha()        { return birthPratyantardasha; }
    public void         setBirthPratyantardasha(String v){ this.birthPratyantardasha = v; }
    public Period       getCurrentMahadasha()            { return currentMahadasha; }
    public void         setCurrentMahadasha(Period v)    { this.currentMahadasha = v; }
    public AntarPeriod  getCurrentAntardasha()           { return currentAntardasha; }
    public void         setCurrentAntardasha(AntarPeriod v) { this.currentAntardasha = v; }
    public Period       getCurrentPratyantardasha()      { return currentPratyantardasha; }
    public void         setCurrentPratyantardasha(Period v) { this.currentPratyantardasha = v; }
    public List<MahaPeriod> getDashaSequence()           { return dashaSequence; }
    public void         setDashaSequence(List<MahaPeriod> v) { this.dashaSequence = v; }
}
