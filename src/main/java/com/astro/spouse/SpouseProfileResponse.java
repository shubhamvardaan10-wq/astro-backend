package com.astro.spouse;

import java.util.List;
import java.util.Map;

/**
 * Full spouse profile response.
 * Contains physical appearance, body figure, probable location,
 * probable occupation, personality, and a ready-to-use ChatGPT/DALL-E image prompt.
 */
public class SpouseProfileResponse {

    // ── Astrological basis ────────────────────────────────────────────────────
    private String seventhHouseSign;
    private String seventhHouseLord;
    private String seventhLordSign;
    private String seventhLordNakshatra;
    private String seventhLordPada;
    private String planetsIn7th;
    private String d9Lagna;
    private String d9VenusSign;
    private String d9VenusDignity;
    private String venusNakshatra;
    private String darakaraka;
    private String upapadaLagna;
    private String karakamsha;

    // ── Physical Appearance ───────────────────────────────────────────────────
    private Appearance appearance;

    // ── Body Figure ───────────────────────────────────────────────────────────
    private Figure figure;

    // ── Probable Location / Region ────────────────────────────────────────────
    private Location probableLocation;

    // ── Probable Occupation ───────────────────────────────────────────────────
    private Occupation probableOccupation;

    // ── Personality ───────────────────────────────────────────────────────────
    private Personality personality;

    // ── AI Image Prompt ───────────────────────────────────────────────────────
    private String chatGptImagePrompt;
    private String dallePrompt;
    private String midJourneyPrompt;

    // ── Confidence & Disclaimer ───────────────────────────────────────────────
    private String confidence;
    private String disclaimer;

    // ═══════════════════════════════════════════════════════════════════════════
    // Inner classes
    // ═══════════════════════════════════════════════════════════════════════════

    public static class Appearance {
        private String overallBeauty;        // e.g. "Exceptionally attractive"
        private String beautyType;           // e.g. "Intense, magnetic, Scorpionic depth"
        private String skinTone;             // e.g. "Wheatish-fair, luminous"
        private String faceShape;            // e.g. "Oval, symmetrical"
        private String eyes;                 // e.g. "Large, deep brown/black, magnetic"
        private String eyebrows;             // e.g. "Well-arched, defined"
        private String nose;                 // e.g. "Straight, proportionate"
        private String lips;                 // e.g. "Full, well-defined"
        private String hair;                 // e.g. "Dark, thick, wavy/straight"
        private String hairLength;           // e.g. "Medium to long"
        private String complexion;           // e.g. "Clear, radiant"
        private String distinctiveFeatures;  // e.g. "Piercing eyes, graceful neck"
        private String ageAppearance;        // e.g. "Appears younger than actual age"
        private String overallRating;        // e.g. "8.5/10 by classical Jyotish standards"

        public String getOverallBeauty()       { return overallBeauty; }
        public void setOverallBeauty(String v) { overallBeauty = v; }
        public String getBeautyType()          { return beautyType; }
        public void setBeautyType(String v)    { beautyType = v; }
        public String getSkinTone()            { return skinTone; }
        public void setSkinTone(String v)      { skinTone = v; }
        public String getFaceShape()           { return faceShape; }
        public void setFaceShape(String v)     { faceShape = v; }
        public String getEyes()                { return eyes; }
        public void setEyes(String v)          { eyes = v; }
        public String getEyebrows()            { return eyebrows; }
        public void setEyebrows(String v)      { eyebrows = v; }
        public String getNose()                { return nose; }
        public void setNose(String v)          { nose = v; }
        public String getLips()                { return lips; }
        public void setLips(String v)          { lips = v; }
        public String getHair()                { return hair; }
        public void setHair(String v)          { hair = v; }
        public String getHairLength()          { return hairLength; }
        public void setHairLength(String v)    { hairLength = v; }
        public String getComplexion()          { return complexion; }
        public void setComplexion(String v)    { complexion = v; }
        public String getDistinctiveFeatures() { return distinctiveFeatures; }
        public void setDistinctiveFeatures(String v) { distinctiveFeatures = v; }
        public String getAgeAppearance()       { return ageAppearance; }
        public void setAgeAppearance(String v) { ageAppearance = v; }
        public String getOverallRating()       { return overallRating; }
        public void setOverallRating(String v) { overallRating = v; }
    }

    public static class Figure {
        private String bodyType;         // e.g. "Slim, well-proportioned"
        private String heightEstimate;   // e.g. "157–165 cm (5'2\"–5'5\")"
        private String weightEstimate;   // e.g. "48–58 kg"
        private String build;            // e.g. "Slender with feminine curves"
        private String posture;          // e.g. "Upright, graceful, elegant"
        private String gait;             // e.g. "Light, quick, confident"
        private String hands;            // e.g. "Delicate, well-shaped"
        private String overallFigure;    // e.g. "Petite-to-medium, feminine, graceful"

        public String getBodyType()          { return bodyType; }
        public void setBodyType(String v)    { bodyType = v; }
        public String getHeightEstimate()    { return heightEstimate; }
        public void setHeightEstimate(String v) { heightEstimate = v; }
        public String getWeightEstimate()    { return weightEstimate; }
        public void setWeightEstimate(String v) { weightEstimate = v; }
        public String getBuild()             { return build; }
        public void setBuild(String v)       { build = v; }
        public String getPosture()           { return posture; }
        public void setPosture(String v)     { posture = v; }
        public String getGait()              { return gait; }
        public void setGait(String v)        { gait = v; }
        public String getHands()             { return hands; }
        public void setHands(String v)       { hands = v; }
        public String getOverallFigure()     { return overallFigure; }
        public void setOverallFigure(String v) { overallFigure = v; }
    }

    public static class Location {
        private String probableRegion;       // e.g. "North India or North-East India"
        private String probableState;        // e.g. "Bihar, UP, Delhi, Jharkhand"
        private String direction;            // e.g. "North or North-East of birth place"
        private String settingType;          // e.g. "Urban / semi-urban educated family"
        private String familyBackground;     // e.g. "Middle-class professional family"
        private String howMet;               // e.g. "Through social/professional network, mutual friends"
        private String meetingCircumstance;  // e.g. "Work environment or family introduction"
        private String locationBasis;        // astrological basis

        public String getProbableRegion()        { return probableRegion; }
        public void setProbableRegion(String v)  { probableRegion = v; }
        public String getProbableState()         { return probableState; }
        public void setProbableState(String v)   { probableState = v; }
        public String getDirection()             { return direction; }
        public void setDirection(String v)       { direction = v; }
        public String getSettingType()           { return settingType; }
        public void setSettingType(String v)     { settingType = v; }
        public String getFamilyBackground()      { return familyBackground; }
        public void setFamilyBackground(String v){ familyBackground = v; }
        public String getHowMet()                { return howMet; }
        public void setHowMet(String v)          { howMet = v; }
        public String getMeetingCircumstance()   { return meetingCircumstance; }
        public void setMeetingCircumstance(String v) { meetingCircumstance = v; }
        public String getLocationBasis()         { return locationBasis; }
        public void setLocationBasis(String v)   { locationBasis = v; }
    }

    public static class Occupation {
        private List<String> probableFields;     // Top 3–5 career fields
        private String primaryField;             // Most probable single field
        private String workStyle;                // e.g. "Detail-oriented, service-driven"
        private String educationLevel;           // e.g. "Graduate or post-graduate"
        private String professionalTraits;       // e.g. "Analytical, health-conscious, organized"
        private String incomeLevel;              // e.g. "Self-sufficient, moderate to good income"
        private String occupationBasis;          // astrological basis

        public List<String> getProbableFields()       { return probableFields; }
        public void setProbableFields(List<String> v) { probableFields = v; }
        public String getPrimaryField()               { return primaryField; }
        public void setPrimaryField(String v)         { primaryField = v; }
        public String getWorkStyle()                  { return workStyle; }
        public void setWorkStyle(String v)            { workStyle = v; }
        public String getEducationLevel()             { return educationLevel; }
        public void setEducationLevel(String v)       { educationLevel = v; }
        public String getProfessionalTraits()         { return professionalTraits; }
        public void setProfessionalTraits(String v)   { professionalTraits = v; }
        public String getIncomeLevel()                { return incomeLevel; }
        public void setIncomeLevel(String v)          { incomeLevel = v; }
        public String getOccupationBasis()            { return occupationBasis; }
        public void setOccupationBasis(String v)      { occupationBasis = v; }
    }

    public static class Personality {
        private String coreNature;           // e.g. "Intense, loyal, ambitious"
        private String communicationStyle;   // e.g. "Articulate, sharp, witty"
        private String emotionalNature;      // e.g. "Deep, private, emotionally strong"
        private String strengthTraits;       // e.g. "Disciplined, health-aware, independent"
        private String challengeTraits;      // e.g. "Can be secretive, high standards"
        private String compatibilityNote;    // e.g. "Well-suited to Sagittarius Lagna natives"
        private String spiritualTendency;    // e.g. "Philosophical, interested in deeper truths"

        public String getCoreNature()           { return coreNature; }
        public void setCoreNature(String v)     { coreNature = v; }
        public String getCommunicationStyle()   { return communicationStyle; }
        public void setCommunicationStyle(String v) { communicationStyle = v; }
        public String getEmotionalNature()      { return emotionalNature; }
        public void setEmotionalNature(String v){ emotionalNature = v; }
        public String getStrengthTraits()       { return strengthTraits; }
        public void setStrengthTraits(String v) { strengthTraits = v; }
        public String getChallengeTraits()      { return challengeTraits; }
        public void setChallengeTraits(String v){ challengeTraits = v; }
        public String getCompatibilityNote()    { return compatibilityNote; }
        public void setCompatibilityNote(String v) { compatibilityNote = v; }
        public String getSpiritualTendency()    { return spiritualTendency; }
        public void setSpiritualTendency(String v) { spiritualTendency = v; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Root getters/setters
    // ═══════════════════════════════════════════════════════════════════════════

    public String getSeventhHouseSign()            { return seventhHouseSign; }
    public void setSeventhHouseSign(String v)      { seventhHouseSign = v; }
    public String getSeventhHouseLord()            { return seventhHouseLord; }
    public void setSeventhHouseLord(String v)      { seventhHouseLord = v; }
    public String getSeventhLordSign()             { return seventhLordSign; }
    public void setSeventhLordSign(String v)       { seventhLordSign = v; }
    public String getSeventhLordNakshatra()        { return seventhLordNakshatra; }
    public void setSeventhLordNakshatra(String v)  { seventhLordNakshatra = v; }
    public String getSeventhLordPada()             { return seventhLordPada; }
    public void setSeventhLordPada(String v)       { seventhLordPada = v; }
    public String getPlanetsIn7th()                { return planetsIn7th; }
    public void setPlanetsIn7th(String v)          { planetsIn7th = v; }
    public String getD9Lagna()                     { return d9Lagna; }
    public void setD9Lagna(String v)               { d9Lagna = v; }
    public String getD9VenusSign()                 { return d9VenusSign; }
    public void setD9VenusSign(String v)           { d9VenusSign = v; }
    public String getD9VenusDignity()              { return d9VenusDignity; }
    public void setD9VenusDignity(String v)        { d9VenusDignity = v; }
    public String getVenusNakshatra()              { return venusNakshatra; }
    public void setVenusNakshatra(String v)        { venusNakshatra = v; }
    public String getDarakaraka()                  { return darakaraka; }
    public void setDarakaraka(String v)            { darakaraka = v; }
    public String getUpapadaLagna()                { return upapadaLagna; }
    public void setUpapadaLagna(String v)          { upapadaLagna = v; }
    public String getKarakamsha()                  { return karakamsha; }
    public void setKarakamsha(String v)            { karakamsha = v; }
    public Appearance getAppearance()              { return appearance; }
    public void setAppearance(Appearance v)        { appearance = v; }
    public Figure getFigure()                      { return figure; }
    public void setFigure(Figure v)                { figure = v; }
    public Location getProbableLocation()          { return probableLocation; }
    public void setProbableLocation(Location v)    { probableLocation = v; }
    public Occupation getProbableOccupation()      { return probableOccupation; }
    public void setProbableOccupation(Occupation v){ probableOccupation = v; }
    public Personality getPersonality()            { return personality; }
    public void setPersonality(Personality v)      { personality = v; }
    public String getChatGptImagePrompt()          { return chatGptImagePrompt; }
    public void setChatGptImagePrompt(String v)    { chatGptImagePrompt = v; }
    public String getDallePrompt()                 { return dallePrompt; }
    public void setDallePrompt(String v)           { dallePrompt = v; }
    public String getMidJourneyPrompt()            { return midJourneyPrompt; }
    public void setMidJourneyPrompt(String v)      { midJourneyPrompt = v; }
    public String getConfidence()                  { return confidence; }
    public void setConfidence(String v)            { confidence = v; }
    public String getDisclaimer()                  { return disclaimer; }
    public void setDisclaimer(String v)            { disclaimer = v; }
}
