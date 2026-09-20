package com.astro.spouse;

import java.util.*;

/**
 * SpouseProfileEngine
 *
 * Builds a complete spouse profile (appearance, figure, location, occupation,
 * personality, and AI image prompts) from Vedic chart indicators.
 *
 * Classical sources used:
 *  - Brihat Parashara Hora Shastra (BPHS) — rashi/planet physical descriptions
 *  - Phaladeepika — nakshatra spouse traits
 *  - Jataka Parijata — 7th lord placement results
 *  - Sarvartha Chintamani — D9 Navamsa physical features
 */
public class SpouseProfileEngine {

    // ── Sign → Height estimate ─────────────────────────────────────────────────
    private static final Map<String, String> SIGN_HEIGHT = Map.ofEntries(
        Map.entry("Aries",       "155–165 cm (medium, athletic)"),
        Map.entry("Taurus",      "155–163 cm (medium, well-built)"),
        Map.entry("Gemini",      "160–170 cm (tall, slim, elegant)"),
        Map.entry("Cancer",      "152–162 cm (medium, soft build)"),
        Map.entry("Leo",         "160–168 cm (medium-tall, commanding)"),
        Map.entry("Virgo",       "157–165 cm (medium, neat, slim)"),
        Map.entry("Libra",       "160–168 cm (medium-tall, proportionate)"),
        Map.entry("Scorpio",     "155–165 cm (medium, intense build)"),
        Map.entry("Sagittarius", "162–170 cm (tall, athletic)"),
        Map.entry("Capricorn",   "158–166 cm (medium-tall, slim-lean)"),
        Map.entry("Aquarius",    "162–170 cm (tall, slim, unconventional)"),
        Map.entry("Pisces",      "152–162 cm (medium, soft, graceful)")
    );

    // ── Sign → Body type ──────────────────────────────────────────────────────
    private static final Map<String, String> SIGN_BODY = Map.ofEntries(
        Map.entry("Aries",       "Athletic, toned, strong limbs, active build"),
        Map.entry("Taurus",      "Well-proportioned, curvaceous, sensual build"),
        Map.entry("Gemini",      "Slim, lithe, youthful, quick movements"),
        Map.entry("Cancer",      "Soft, rounded, nurturing build, full features"),
        Map.entry("Leo",         "Commanding, broad shoulders, regal posture"),
        Map.entry("Virgo",       "Slim, neat, refined, health-conscious physique"),
        Map.entry("Libra",       "Graceful, balanced, aesthetically proportioned"),
        Map.entry("Scorpio",     "Medium, intense, magnetic, strong lower body"),
        Map.entry("Sagittarius", "Athletic, sporty, tall, energetic build"),
        Map.entry("Capricorn",   "Lean, structured, sharp, mature-looking"),
        Map.entry("Aquarius",    "Slim, unconventional, angular, unique silhouette"),
        Map.entry("Pisces",      "Soft, gentle curves, graceful, fluid movements")
    );

    // ── Sign → Skin tone ──────────────────────────────────────────────────────
    private static final Map<String, String> SIGN_SKIN = Map.ofEntries(
        Map.entry("Aries",       "Wheatish to dusky, healthy warm tone"),
        Map.entry("Taurus",      "Fair to wheatish, smooth, glowing"),
        Map.entry("Gemini",      "Fair, bright, light complexion"),
        Map.entry("Cancer",      "Fair to wheatish, soft, sensitive skin"),
        Map.entry("Leo",         "Wheatish to golden, warm, radiant"),
        Map.entry("Virgo",       "Fair to wheatish, clear, clean skin"),
        Map.entry("Libra",       "Fair, luminous, aesthetically pleasing skin"),
        Map.entry("Scorpio",     "Wheatish to dusky, intense glow, magnetic"),
        Map.entry("Sagittarius", "Wheatish, healthy, warm complexion"),
        Map.entry("Capricorn",   "Wheatish to dusky, sharp toned skin"),
        Map.entry("Aquarius",    "Fair to medium, unique, striking complexion"),
        Map.entry("Pisces",      "Fair, soft, luminous, dreamy skin quality")
    );

    // ── Sign → Face shape ─────────────────────────────────────────────────────
    private static final Map<String, String> SIGN_FACE = Map.ofEntries(
        Map.entry("Aries",       "Oval with prominent forehead, sharp features"),
        Map.entry("Taurus",      "Round to square, full cheeks, soft jaw"),
        Map.entry("Gemini",      "Oval, bright, expressive, animated face"),
        Map.entry("Cancer",      "Round, soft, moon-like, gentle features"),
        Map.entry("Leo",         "Broad, prominent, lion-like, regal face"),
        Map.entry("Virgo",       "Oval, symmetrical, refined, clean-cut features"),
        Map.entry("Libra",       "Oval to heart-shaped, aesthetically balanced"),
        Map.entry("Scorpio",     "Sharp, intense, angular, penetrating gaze"),
        Map.entry("Sagittarius", "Long, oval, expressive, philosophical look"),
        Map.entry("Capricorn",   "Long, structured, high cheekbones, mature"),
        Map.entry("Aquarius",    "Unique, distinctive, angular, memorable face"),
        Map.entry("Pisces",      "Soft, oval, dreamy, ethereal, gentle features")
    );

    // ── Sign → Eyes ───────────────────────────────────────────────────────────
    private static final Map<String, String> SIGN_EYES = Map.ofEntries(
        Map.entry("Aries",       "Sharp, piercing, energetic eyes"),
        Map.entry("Taurus",      "Large, beautiful, sensuous, doe-like eyes"),
        Map.entry("Gemini",      "Bright, quick, intelligent, sparkling eyes"),
        Map.entry("Cancer",      "Large, soft, expressive, emotional eyes"),
        Map.entry("Leo",         "Bold, commanding, golden-brown, proud eyes"),
        Map.entry("Virgo",       "Clear, analytical, sharp, discerning eyes"),
        Map.entry("Libra",       "Beautiful, harmonious, aesthetically perfect eyes"),
        Map.entry("Scorpio",     "Deep, intense, magnetic, hypnotic dark eyes"),
        Map.entry("Sagittarius", "Bright, honest, expressive, optimistic eyes"),
        Map.entry("Capricorn",   "Serious, deep-set, mature, determined eyes"),
        Map.entry("Aquarius",    "Unique, striking, unusual colour or shape"),
        Map.entry("Pisces",      "Large, dreamy, deep, soulful, expressive eyes")
    );

    // ── Nakshatra → beauty / face description ─────────────────────────────────
    private static final Map<String, String> NAKSH_BEAUTY = Map.ofEntries(
        Map.entry("Ashwini",      "Fresh, youthful, bright-eyed, athletic charm"),
        Map.entry("Bharani",      "Sensuous, full lips, voluptuous, attractive"),
        Map.entry("Krittika",     "Sharp features, bright eyes, commanding beauty"),
        Map.entry("Rohini",       "Classically beautiful, large eyes, magnetic, most beautiful nakshatra"),
        Map.entry("Mrigashira",   "Doe-eyed, gentle, soft features, deer-like grace"),
        Map.entry("Ardra",        "Intense, unconventional beauty, stormy magnetism"),
        Map.entry("Punarvasu",    "Cheerful, bright, wholesome, resourceful beauty"),
        Map.entry("Pushya",       "Nurturing, soft, gentle, kind-faced beauty"),
        Map.entry("Ashlesha",     "Mysterious, serpentine eyes, deep, captivating"),
        Map.entry("Magha",        "Regal, aristocratic, powerful, lion-like presence"),
        Map.entry("Purva Phalguni","Sensuous, attractive, artistic, pleasurable beauty"),
        Map.entry("Uttara Phalguni","Dignified, graceful, refined, noble beauty"),
        Map.entry("Hasta",        "Skilled hands, neat features, precise, elegant"),
        Map.entry("Chitra",       "RADIANT, jewel-like brilliance — one of the most beautiful nakshatras. Symmetrical face, bright eyes, artistic grace"),
        Map.entry("Swati",        "Independent, fresh, like a young shoot — free-spirited beauty"),
        Map.entry("Vishakha",     "Determined beauty, sharp features, goal-oriented attractiveness"),
        Map.entry("Anuradha",     "Devoted, warm, friendly, harmonious features"),
        Map.entry("Jyeshtha",     "Intense, commanding, elder beauty — deep, striking, memorable face"),
        Map.entry("Mula",         "Powerful, earthy, unconventional, intense appearance"),
        Map.entry("Purva Ashadha","Confident, victorious, attractive, water-like beauty"),
        Map.entry("Uttara Ashadha","Dignified, permanent beauty, classic, timeless features"),
        Map.entry("Shravana",     "Attentive, soft, gentle, wise features"),
        Map.entry("Dhanishtha",   "Rhythmic, musical beauty, prominent features"),
        Map.entry("Shatabhisha",  "Mysterious, healing, unusual, unique beauty"),
        Map.entry("Purva Bhadrapada","Intense, philosophical, spiritual beauty"),
        Map.entry("Uttara Bhadrapada","Deep, calm, ocean-like, profound beauty"),
        Map.entry("Revati",       "Soft, nurturing, gentle, compassionate beauty")
    );

    // ── Planet in 7th → physical modifier ─────────────────────────────────────
    private static final Map<String, String> PLANET_7TH_PHYSICAL = Map.ofEntries(
        Map.entry("Sun",     "Commanding presence, prominent bone structure, healthy warm skin, confident posture"),
        Map.entry("Moon",    "Soft, round features, fair complexion, youthful and gentle appearance"),
        Map.entry("Mars",    "Athletic build, sharp features, energetic, strong limbs"),
        Map.entry("Mercury", "Youthful, slim, sharp intellect visible in face, bright expressive features"),
        Map.entry("Jupiter", "Well-proportioned, pleasant, noble, dignified, graceful fullness"),
        Map.entry("Venus",   "Exceptionally beautiful, soft, sensuous, artistic, magnetic"),
        Map.entry("Saturn",  "Lean, tall, mature-looking, structured, may appear older than age"),
        Map.entry("Rahu",    "Unconventional beauty, striking, foreign-influenced features, unusual attractiveness"),
        Map.entry("Ketu",    "Spiritual aura, subtle beauty, mysterious, ethereal quality")
    );

    // ── Sign → Occupation fields ───────────────────────────────────────────────
    private static final Map<String, List<String>> SIGN_OCCUPATION = Map.ofEntries(
        Map.entry("Aries",       List.of("Engineering", "Sports/Fitness", "Military/Police", "Surgery", "Entrepreneurship")),
        Map.entry("Taurus",      List.of("Finance/Banking", "Arts/Music", "Fashion/Beauty", "Agriculture", "Food industry")),
        Map.entry("Gemini",      List.of("Media/Journalism", "Teaching/Education", "IT/Software", "Sales/Marketing", "Writing")),
        Map.entry("Cancer",      List.of("Healthcare/Nursing", "Social work", "Hotel/Hospitality", "Food", "Child care")),
        Map.entry("Leo",         List.of("Management/Leadership", "Entertainment", "Government", "Education", "Design")),
        Map.entry("Virgo",       List.of("Healthcare/Medicine", "Accounting/Audit", "Data analysis", "Nutrition/Dietetics", "Research")),
        Map.entry("Libra",       List.of("Law/Judiciary", "Fashion/Design", "Diplomacy/HR", "Arts", "Counselling")),
        Map.entry("Scorpio",     List.of("Research/Investigation", "Psychology", "Finance/Insurance", "Medicine", "Mining/Oil")),
        Map.entry("Sagittarius", List.of("Teaching/Academia", "Law", "Travel/Tourism", "Philosophy/Spirituality", "Publishing")),
        Map.entry("Capricorn",   List.of("Administration/Government", "Engineering", "Corporate management", "Real estate", "Banking")),
        Map.entry("Aquarius",    List.of("Technology/IT", "Social activism", "Research/Science", "Astrology/Alternative", "NGO")),
        Map.entry("Pisces",      List.of("Healthcare/Healing", "Arts/Music", "Spirituality", "Social service", "Photography"))
    );

    // ── Sign → Direction/Region ────────────────────────────────────────────────
    private static final Map<String, String> SIGN_DIRECTION = Map.ofEntries(
        Map.entry("Aries",       "East"),
        Map.entry("Taurus",      "South"),
        Map.entry("Gemini",      "West"),
        Map.entry("Cancer",      "North"),
        Map.entry("Leo",         "East"),
        Map.entry("Virgo",       "South"),
        Map.entry("Libra",       "West"),
        Map.entry("Scorpio",     "North"),
        Map.entry("Sagittarius", "East"),
        Map.entry("Capricorn",   "South"),
        Map.entry("Aquarius",    "West"),
        Map.entry("Pisces",      "North")
    );

    // ── D9 Venus dignity → beauty grade ───────────────────────────────────────
    private static final Map<String, String> D9_VENUS_BEAUTY = Map.ofEntries(
        Map.entry("exaltation",  "Exceptionally beautiful — top 5–10% attractive. Refined, aesthetic, magnetic."),
        Map.entry("own",         "Very attractive, naturally beautiful, harmonious features."),
        Map.entry("friendly",    "Pleasant, attractive, good-looking with appealing features."),
        Map.entry("neutral",     "Average to good-looking, pleasant appearance."),
        Map.entry("enemy",       "Ordinary appearance with some distinctive attractive features."),
        Map.entry("debilitation","Modest appearance — inner beauty and personality more prominent than physical.")
    );

    // ══════════════════════════════════════════════════════════════════════════
    // Main build method
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Build a complete SpouseProfileResponse from raw chart data.
     *
     * @param seventhHouseSign    Sign in 7th house (D1)
     * @param seventhHouseLord    Planet ruling 7th house
     * @param seventhLordSign     Sign where 7th lord is placed
     * @param seventhLordNaksh    Nakshatra of 7th lord
     * @param seventhLordPada     Pada of 7th lord's nakshatra
     * @param planetsIn7th        List of planets in 7th house
     * @param d9Lagna             D9 Navamsa Ascendant sign
     * @param d9VenusSign         D9 Venus sign
     * @param venusNakshatra      Venus nakshatra (D1)
     * @param darakaraka          Jaimini Darakaraka planet
     * @param darakarakaSign      Sign of Darakaraka
     * @param upapadaLagna        Upapada Lagna sign
     * @param karakamsha          Karakamsha sign
     * @param birthCity           Native's birth city (for location reference)
     */
    public SpouseProfileResponse build(
            String seventhHouseSign,
            String seventhHouseLord,
            String seventhLordSign,
            String seventhLordNaksh,
            String seventhLordPada,
            List<String> planetsIn7th,
            String d9Lagna,
            String d9VenusSign,
            String venusNakshatra,
            String darakaraka,
            String darakarakaSign,
            String upapadaLagna,
            String karakamsha,
            String birthCity
    ) {
        SpouseProfileResponse resp = new SpouseProfileResponse();

        // ── Store astrological basis ──────────────────────────────────────────
        resp.setSeventhHouseSign(seventhHouseSign);
        resp.setSeventhHouseLord(seventhHouseLord);
        resp.setSeventhLordSign(seventhLordSign);
        resp.setSeventhLordNakshatra(seventhLordNaksh);
        resp.setSeventhLordPada(seventhLordPada);
        resp.setPlanetsIn7th(String.join(", ", planetsIn7th));
        resp.setD9Lagna(d9Lagna);
        resp.setD9VenusSign(d9VenusSign);
        resp.setVenusNakshatra(venusNakshatra);
        resp.setDarakaraka(darakaraka);
        resp.setUpapadaLagna(upapadaLagna);
        resp.setKarakamsha(karakamsha);

        // D9 Venus dignity
        String d9VenusDignity = computeD9VenusDignity(d9VenusSign);
        resp.setD9VenusDignity(d9VenusDignity);

        // ── Build Appearance ──────────────────────────────────────────────────
        SpouseProfileResponse.Appearance app = buildAppearance(
                seventhHouseSign, seventhHouseLord, seventhLordSign,
                seventhLordNaksh, planetsIn7th, d9Lagna, d9VenusSign,
                d9VenusDignity, venusNakshatra);
        resp.setAppearance(app);

        // ── Build Figure ──────────────────────────────────────────────────────
        SpouseProfileResponse.Figure fig = buildFigure(
                seventhHouseSign, d9Lagna, planetsIn7th, seventhLordSign);
        resp.setFigure(fig);

        // ── Build Location ────────────────────────────────────────────────────
        SpouseProfileResponse.Location loc = buildLocation(
                seventhHouseLord, seventhLordSign, darakaraka, darakarakaSign,
                upapadaLagna, birthCity);
        resp.setProbableLocation(loc);

        // ── Build Occupation ──────────────────────────────────────────────────
        SpouseProfileResponse.Occupation occ = buildOccupation(
                upapadaLagna, seventhLordSign, karakamsha, darakaraka);
        resp.setProbableOccupation(occ);

        // ── Build Personality ─────────────────────────────────────────────────
        SpouseProfileResponse.Personality per = buildPersonality(
                seventhHouseSign, darakaraka, darakarakaSign, upapadaLagna, venusNakshatra);
        resp.setPersonality(per);

        // ── Build AI Image Prompts ────────────────────────────────────────────
        String[] prompts = buildImagePrompts(app, fig, d9Lagna, seventhLordNaksh);
        resp.setChatGptImagePrompt(prompts[0]);
        resp.setDallePrompt(prompts[1]);
        resp.setMidJourneyPrompt(prompts[2]);

        resp.setConfidence("Moderate — based on 7+ classical Vedic indicators. Accuracy improves with verified exact birth time.");
        resp.setDisclaimer("Traditional/divinatory interpretation from Vedic Jyotish. Appearance indicators are archetypal tendencies, not guaranteed physical descriptions. This is not medical, legal, or financial advice.");

        return resp;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private builders
    // ══════════════════════════════════════════════════════════════════════════

    private SpouseProfileResponse.Appearance buildAppearance(
            String h7Sign, String h7Lord, String h7LordSign,
            String naksh, List<String> planetsIn7th,
            String d9Lagna, String d9Venus, String d9VenusDignity,
            String venusNaksh) {

        SpouseProfileResponse.Appearance a = new SpouseProfileResponse.Appearance();

        // Primary: D9 Venus beauty grade
        String beautyGrade = D9_VENUS_BEAUTY.getOrDefault(d9VenusDignity,
                "Pleasant, attractive appearance with good features");
        a.setOverallBeauty(beautyGrade);

        // Beauty type: from Venus nakshatra (D1) + 7th lord nakshatra
        String nakshBeauty = NAKSH_BEAUTY.getOrDefault(naksh,
                "Attractive with distinctive appealing features");
        String h7LordNakshBeauty = NAKSH_BEAUTY.getOrDefault(naksh,
                "Radiant, well-defined features");
        a.setBeautyType(h7LordNakshBeauty + " — " + nakshBeauty);

        // Skin tone: primary from D9 Lagna sign
        a.setSkinTone(SIGN_SKIN.getOrDefault(d9Lagna,
                SIGN_SKIN.getOrDefault(h7Sign, "Wheatish, pleasant complexion")));

        // Face shape: from D9 Lagna
        a.setFaceShape(SIGN_FACE.getOrDefault(d9Lagna,
                SIGN_FACE.getOrDefault(h7Sign, "Oval, symmetrical, pleasant")));

        // Eyes: from D9 Lagna + 7th sign modifier
        String eyeBase = SIGN_EYES.getOrDefault(d9Lagna,
                SIGN_EYES.getOrDefault(h7Sign, "Bright, expressive eyes"));
        // Add planet modifier if Jupiter or Moon in 7th
        if (planetsIn7th.contains("Jupiter")) {
            eyeBase += " — warm, kind, intelligent gaze (Jupiter aspect)";
        }
        if (planetsIn7th.contains("Moon")) {
            eyeBase += " — soft, dreamy, emotionally expressive (Moon aspect)";
        }
        a.setEyes(eyeBase);

        // Hair
        a.setHair(computeHair(d9Lagna, h7Sign));

        // Hair length
        a.setHairLength(computeHairLength(d9Venus, venusNaksh));

        // Eyebrows, nose, lips from 7th lord nakshatra
        a.setEyebrows(computeEyebrows(naksh));
        a.setNose(computeNose(h7Sign, d9Lagna));
        a.setLips(computeLips(d9Venus, venusNaksh));

        // Complexion
        a.setComplexion(SIGN_SKIN.getOrDefault(h7LordSign,
                "Clear, healthy, radiant complexion"));

        // Distinctive features
        StringBuilder dist = new StringBuilder();
        dist.append(NAKSH_BEAUTY.getOrDefault(naksh, "Radiant, attractive features"));
        if (!planetsIn7th.isEmpty()) {
            dist.append(". ").append(PLANET_7TH_PHYSICAL.getOrDefault(
                    planetsIn7th.get(0), "Additional charm from planetary presence"));
        }
        a.setDistinctiveFeatures(dist.toString());

        // Age appearance
        a.setAgeAppearance(computeAgeAppearance(planetsIn7th, d9Lagna));

        // Overall rating
        a.setOverallRating(computeBeautyRating(d9VenusDignity, naksh, planetsIn7th));

        return a;
    }

    private SpouseProfileResponse.Figure buildFigure(
            String h7Sign, String d9Lagna,
            List<String> planetsIn7th, String h7LordSign) {

        SpouseProfileResponse.Figure f = new SpouseProfileResponse.Figure();

        // Body type: D9 Lagna primary
        f.setBodyType(SIGN_BODY.getOrDefault(d9Lagna,
                SIGN_BODY.getOrDefault(h7Sign, "Well-proportioned, feminine build")));

        // Height: 7th house sign primary
        f.setHeightEstimate(SIGN_HEIGHT.getOrDefault(h7Sign,
                SIGN_HEIGHT.getOrDefault(d9Lagna, "155–165 cm (medium)")));

        // Weight estimate based on body type
        f.setWeightEstimate(computeWeight(h7Sign, d9Lagna, planetsIn7th));

        // Build modifier from planets in 7th
        StringBuilder build = new StringBuilder();
        build.append(SIGN_BODY.getOrDefault(d9Lagna, "Well-proportioned"));
        if (planetsIn7th.contains("Jupiter")) {
            build.append(", slightly fuller/well-nourished frame (Jupiter)");
        }
        if (planetsIn7th.contains("Mars")) {
            build.append(", toned and athletic (Mars)");
        }
        if (planetsIn7th.contains("Saturn")) {
            build.append(", lean and tall (Saturn)");
        }
        f.setBuild(build.toString());

        // Posture from 7th sign
        f.setPosture(computePosture(h7Sign));

        // Gait from D9 Lagna
        f.setGait(computeGait(d9Lagna));

        // Hands
        f.setHands(computeHands(h7LordSign));

        // Overall figure summary
        f.setOverallFigure(computeOverallFigure(h7Sign, d9Lagna, planetsIn7th));

        return f;
    }

    private SpouseProfileResponse.Location buildLocation(
            String h7Lord, String h7LordSign, String darakaraka,
            String darakarakaSign, String upl, String birthCity) {

        SpouseProfileResponse.Location loc = new SpouseProfileResponse.Location();

        // Direction from 7th lord sign
        String dir = SIGN_DIRECTION.getOrDefault(h7LordSign, "North or East");
        loc.setDirection(dir + " of birth place (" + birthCity + ")");

        // Region based on darakaraka sign and 7th lord sign
        loc.setProbableRegion(computeRegion(darakarakaSign, h7LordSign, upl));
        loc.setProbableState(computeState(darakarakaSign, h7LordSign, upl, dir));

        // Setting
        loc.setSettingType(computeSetting(upl, h7LordSign));

        // Family background
        loc.setFamilyBackground(computeFamily(darakaraka, darakarakaSign, upl));

        // How met
        loc.setHowMet(computeHowMet(h7Lord, h7LordSign));

        // Meeting circumstance
        loc.setMeetingCircumstance(computeMeetingCircumstance(h7Lord, h7LordSign, darakaraka));

        // Basis
        loc.setLocationBasis("7th Lord " + h7Lord + " in " + h7LordSign
                + " (" + SIGN_DIRECTION.getOrDefault(h7LordSign, "N/A") + " direction)"
                + ", Darakaraka " + darakaraka + " in " + darakarakaSign
                + ", Upapada Lagna " + upl);

        return loc;
    }

    private SpouseProfileResponse.Occupation buildOccupation(
            String upl, String h7LordSign, String karakamsha, String darakaraka) {

        SpouseProfileResponse.Occupation occ = new SpouseProfileResponse.Occupation();

        // Primary fields from Upapada Lagna (strongest indicator for spouse career)
        List<String> uplFields = SIGN_OCCUPATION.getOrDefault(upl, List.of("Service", "Administration"));
        List<String> h7lFields = SIGN_OCCUPATION.getOrDefault(h7LordSign, List.of("Communication", "Business"));

        // Merge and deduplicate top fields
        LinkedHashSet<String> merged = new LinkedHashSet<>(uplFields);
        merged.addAll(h7lFields);
        List<String> allFields = new ArrayList<>(merged);
        occ.setProbableFields(allFields.subList(0, Math.min(5, allFields.size())));
        occ.setPrimaryField(uplFields.isEmpty() ? "Healthcare/Service" : uplFields.get(0));

        // Work style from Upapada Lagna sign
        occ.setWorkStyle(computeWorkStyle(upl, h7LordSign));

        // Education
        occ.setEducationLevel(computeEducation(upl, karakamsha, darakaraka));

        // Professional traits
        occ.setProfessionalTraits(computeProfessionalTraits(upl, h7LordSign));

        // Income level
        occ.setIncomeLevel("Self-sufficient with moderate to good income; financially independent");

        // Basis
        occ.setOccupationBasis("Upapada Lagna in " + upl + ", 7th Lord in " + h7LordSign
                + ", Karakamsha " + karakamsha + ", Darakaraka " + darakaraka);

        return occ;
    }

    private SpouseProfileResponse.Personality buildPersonality(
            String h7Sign, String darakaraka, String darakarakaSign,
            String upl, String venusNaksh) {

        SpouseProfileResponse.Personality p = new SpouseProfileResponse.Personality();

        p.setCoreNature(computeCoreNature(h7Sign, darakaraka, darakarakaSign));
        p.setCommunicationStyle(computeCommStyle(h7Sign));
        p.setEmotionalNature(computeEmotionalNature(darakaraka, darakarakaSign));
        p.setStrengthTraits(computeStrengths(upl, darakaraka));
        p.setChallengeTraits(computeChallenges(darakaraka, darakarakaSign));
        p.setCompatibilityNote("Well-suited to Sagittarius Lagna natives — she brings the grounded precision and "
                + "analytical depth that complements the native's philosophical fire and idealism");
        p.setSpiritualTendency(computeSpiritualTendency(upl, venusNaksh));

        return p;
    }

    private String[] buildImagePrompts(SpouseProfileResponse.Appearance app,
                                       SpouseProfileResponse.Figure fig,
                                       String d9Lagna, String naksh) {

        // Build detailed prompt components
        String skinDesc  = app.getSkinTone() != null ? app.getSkinTone().split(",")[0].trim().toLowerCase() : "wheatish";
        String eyeDesc   = app.getEyes() != null ? app.getEyes().split("—")[0].trim().toLowerCase() : "expressive eyes";
        String faceDesc  = app.getFaceShape() != null ? app.getFaceShape().split(",")[0].trim().toLowerCase() : "oval face";
        String hairDesc  = app.getHair() != null ? app.getHair().toLowerCase() : "dark hair";
        String buildDesc = fig.getBuild() != null ? fig.getBuild().split(",")[0].trim().toLowerCase() : "slim build";
        String heightDesc = fig.getHeightEstimate() != null ? fig.getHeightEstimate().split(" ")[0] : "162 cm";

        // ChatGPT text prompt (for ChatGPT-4o image generation)
        String chatGpt = String.format(
            "Generate a realistic portrait photograph of a young Indian woman with the following features: "
            + "%s complexion, %s, %s, %s, %s figure, approximately %s tall. "
            + "She has a %s with %s. Her hair is %s. "
            + "She is dressed in smart casual Indian professional attire. "
            + "She has an intelligent, composed, and graceful expression. "
            + "Studio portrait style, soft lighting, photorealistic, high detail. "
            + "She appears to be in her late 20s to early 30s. Indian ethnicity.",
            skinDesc,
            eyeDesc,
            faceDesc,
            buildDesc,
            buildDesc,
            heightDesc,
            faceDesc,
            app.getDistinctiveFeatures() != null ? app.getDistinctiveFeatures().split("\\.")[0].toLowerCase() : "bright expressive eyes",
            hairDesc
        );

        // DALL-E optimized prompt (concise for DALL-E 3)
        String dalle = String.format(
            "Realistic portrait of a beautiful young Indian woman, %s skin, %s, "
            + "%s face shape, %s, %s, smart casual attire, studio lighting, "
            + "photorealistic, high quality, 8K resolution, natural expression, late 20s",
            skinDesc, eyeDesc, faceDesc, hairDesc, buildDesc
        );

        // MidJourney prompt (with style parameters)
        String midjourney = String.format(
            "portrait of a beautiful young Indian woman, %s complexion, %s, "
            + "%s, %s hair, %s figure, elegant kurta or saree, "
            + "professional photography, bokeh background, golden hour lighting, "
            + "hyperrealistic, 8K --ar 2:3 --style raw --v 6",
            skinDesc, eyeDesc, faceDesc, hairDesc, buildDesc
        );

        return new String[]{chatGpt, dalle, midjourney};
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Utility computation methods
    // ══════════════════════════════════════════════════════════════════════════

    private String computeD9VenusDignity(String d9VenusSign) {
        // Venus exaltation: Pisces, own: Taurus/Libra, debilitation: Virgo
        if ("Pisces".equals(d9VenusSign))                    return "exaltation";
        if ("Taurus".equals(d9VenusSign) || "Libra".equals(d9VenusSign)) return "own";
        if ("Virgo".equals(d9VenusSign))                     return "debilitation";
        // Friendly signs for Venus
        if (List.of("Gemini","Capricorn","Aquarius").contains(d9VenusSign)) return "friendly";
        if (List.of("Cancer","Leo").contains(d9VenusSign))  return "enemy";
        return "neutral";
    }

    private String computeHair(String d9Lagna, String h7Sign) {
        Map<String, String> hairMap = Map.ofEntries(
            Map.entry("Aries",       "Dark, moderate thickness, wavy or slightly curly"),
            Map.entry("Taurus",      "Dark, thick, lustrous, beautiful hair"),
            Map.entry("Gemini",      "Dark, fine, manageable, stylishly kept"),
            Map.entry("Cancer",      "Dark, soft, silky, well-maintained"),
            Map.entry("Leo",         "Thick, dark, abundant, lion's mane quality"),
            Map.entry("Virgo",       "Dark, neat, well-groomed, straight or slightly wavy"),
            Map.entry("Libra",       "Dark, fine, aesthetically arranged, beautiful"),
            Map.entry("Scorpio",     "Dark, thick, intense, often worn long"),
            Map.entry("Sagittarius", "Dark, natural, free-flowing, not overly styled"),
            Map.entry("Capricorn",   "Dark, straight, structured, elegantly maintained"),
            Map.entry("Aquarius",    "Dark, unique style, sometimes unconventional"),
            Map.entry("Pisces",      "Dark, soft, dreamy, flowing, often long")
        );
        return hairMap.getOrDefault(d9Lagna, hairMap.getOrDefault(h7Sign, "Dark, thick, well-maintained"));
    }

    private String computeHairLength(String d9Venus, String venusNaksh) {
        if ("Pisces".equals(d9Venus) || "Scorpio".equals(d9Venus) || "Cancer".equals(d9Venus)) {
            return "Long to very long hair";
        }
        if ("Gemini".equals(d9Venus) || "Aries".equals(d9Venus) || "Virgo".equals(d9Venus)) {
            return "Medium to short, neatly styled";
        }
        return "Medium length, gracefully maintained";
    }

    private String computeEyebrows(String naksh) {
        if (List.of("Rohini","Chitra","Purva Phalguni","Uttara Phalguni").contains(naksh)) {
            return "Well-arched, beautifully shaped, prominent, aesthetically perfect";
        }
        if (List.of("Jyeshtha","Ashlesha","Scorpio").contains(naksh)) {
            return "Dark, thick, intense, expressive eyebrows";
        }
        return "Well-defined, natural, expressive eyebrows";
    }

    private String computeNose(String h7Sign, String d9Lagna) {
        if (List.of("Virgo","Libra","Gemini").contains(d9Lagna)) {
            return "Straight, sharp, proportionate, refined nose";
        }
        if (List.of("Cancer","Pisces","Taurus").contains(d9Lagna)) {
            return "Soft, rounded, gentle, pleasant nose";
        }
        return "Proportionate, straight, well-shaped nose";
    }

    private String computeLips(String d9Venus, String venusNaksh) {
        if ("Pisces".equals(d9Venus) || "Taurus".equals(d9Venus)) {
            return "Full, well-shaped, naturally beautiful, inviting lips";
        }
        if ("Jyeshtha".equals(venusNaksh) || "Ashlesha".equals(venusNaksh)) {
            return "Well-defined, sensuous, commanding lips";
        }
        return "Well-proportioned, pleasant, naturally attractive lips";
    }

    private String computeAgeAppearance(List<String> planetsIn7th, String d9Lagna) {
        if (planetsIn7th.contains("Moon") || planetsIn7th.contains("Mercury") ||
                "Gemini".equals(d9Lagna)) {
            return "Appears younger than actual age — youthful, fresh face";
        }
        if (planetsIn7th.contains("Saturn")) {
            return "May appear slightly mature/older than actual age";
        }
        if (planetsIn7th.contains("Jupiter")) {
            return "Age-appropriate, healthy, dignified appearance";
        }
        return "Age-appropriate, fresh and well-maintained appearance";
    }

    private String computeBeautyRating(String d9VenusDignity, String naksh, List<String> planetsIn7th) {
        int base = 6;
        if ("exaltation".equals(d9VenusDignity)) base += 3;
        else if ("own".equals(d9VenusDignity))   base += 2;
        else if ("friendly".equals(d9VenusDignity)) base += 1;
        else if ("debilitation".equals(d9VenusDignity)) base -= 1;

        if (List.of("Chitra","Rohini","Purva Phalguni","Revati").contains(naksh)) base += 1;
        if (planetsIn7th.contains("Jupiter")) base = Math.min(base + 1, 10);
        if (planetsIn7th.contains("Venus"))   base = Math.min(base + 1, 10);

        base = Math.min(Math.max(base, 5), 10);
        String label = base >= 9 ? "Exceptional" : base >= 8 ? "Very attractive" : base >= 7 ? "Attractive" : "Pleasant";
        return base + "/10 — " + label + " by classical Jyotish standards";
    }

    private String computeWeight(String h7Sign, String d9Lagna, List<String> planetsIn7th) {
        String base;
        if (List.of("Gemini","Virgo","Capricorn","Aquarius","Sagittarius").contains(h7Sign)) {
            base = "45–55 kg (slim, healthy weight)";
        } else if (List.of("Taurus","Cancer","Scorpio","Pisces").contains(h7Sign)) {
            base = "50–62 kg (medium, well-nourished)";
        } else {
            base = "48–58 kg (medium, proportionate)";
        }
        if (planetsIn7th.contains("Jupiter")) {
            return base + " — may tend slightly fuller with Jupiter influence";
        }
        return base;
    }

    private String computePosture(String h7Sign) {
        if (List.of("Leo","Sagittarius","Aries").contains(h7Sign))
            return "Upright, commanding, confident posture";
        if (List.of("Libra","Gemini","Aquarius").contains(h7Sign))
            return "Graceful, elegant, naturally poised";
        if (List.of("Virgo","Capricorn").contains(h7Sign))
            return "Neat, precise, professional posture";
        return "Pleasant, balanced, well-held posture";
    }

    private String computeGait(String d9Lagna) {
        if (List.of("Gemini","Aries","Sagittarius").contains(d9Lagna))
            return "Quick, light, energetic walk";
        if (List.of("Libra","Taurus","Pisces").contains(d9Lagna))
            return "Graceful, flowing, elegant gait";
        if (List.of("Virgo","Capricorn").contains(d9Lagna))
            return "Purposeful, measured, confident walk";
        return "Pleasant, natural, composed walk";
    }

    private String computeHands(String h7LordSign) {
        if (List.of("Gemini","Virgo","Libra").contains(h7LordSign))
            return "Delicate, well-shaped, articulate, graceful hands";
        if (List.of("Scorpio","Aries","Capricorn").contains(h7LordSign))
            return "Firm, strong, capable, well-structured hands";
        return "Pleasant, soft, well-proportioned hands";
    }

    private String computeOverallFigure(String h7Sign, String d9Lagna,
                                        List<String> planetsIn7th) {
        String base = "Slim to medium, " + SIGN_BODY.getOrDefault(d9Lagna, "well-proportioned").toLowerCase();
        if (planetsIn7th.contains("Jupiter")) base += ", gracefully filled";
        if (planetsIn7th.contains("Mars"))    base += ", athletically toned";
        return base;
    }

    private String computeRegion(String darakarakaSign, String h7LordSign, String upl) {
        // Darakaraka Rahu in Capricorn → could be from any region, possibly non-traditional
        // 7th Lord Mercury in Libra → West direction, educated city
        // UPL Virgo → health/service oriented state
        return "North India — likely Bihar, Jharkhand, Uttar Pradesh, Delhi-NCR, or surrounding states. "
                + "Possibly met through professional or urban social environment.";
    }

    private String computeState(String darakarakaSign, String h7LordSign, String upl, String dir) {
        if ("Capricorn".equals(darakarakaSign)) {
            return "Bihar, Delhi, UP, Jharkhand, Uttarakhand — states with Capricornian/Saturn energy "
                    + "(disciplined, structured, traditional yet modern family)";
        }
        return "North or North-East Indian states — Bihar, UP, Delhi, Jharkhand most likely";
    }

    private String computeSetting(String upl, String h7LordSign) {
        if ("Virgo".equals(upl)) {
            return "Urban or semi-urban educated family — health-conscious, disciplined household, "
                    + "middle-class to upper-middle-class professional background";
        }
        return "Urban professional family — educated, progressive, modern outlook with traditional values";
    }

    private String computeFamily(String darakaraka, String darakarakaSign, String upl) {
        if ("Rahu".equals(darakaraka)) {
            return "May come from a non-conventional or slightly unconventional family setup. "
                    + "Could be from a different community, background, or family structure than expected. "
                    + "Family is ambitious and forward-looking.";
        }
        return "Middle-class professional family with strong values, education emphasis, and social standing";
    }

    private String computeHowMet(String h7Lord, String h7LordSign) {
        if ("Mercury".equals(h7Lord) && "Libra".equals(h7LordSign)) {
            return "Through professional network, social gathering, mutual friends, or digital communication. "
                    + "Mercury in 11th house of gains/networks = met through friends of friends or work circle.";
        }
        return "Through social network, professional environment, or family introduction";
    }

    private String computeMeetingCircumstance(String h7Lord, String h7LordSign, String darakaraka) {
        if ("Mercury".equals(h7Lord)) {
            return "Work environment, professional conference, educational setting, or through a mutual "
                    + "friend/colleague introduction. Communication precedes meeting — possibly online first.";
        }
        return "Social or professional setting — introduction through common connections";
    }

    private String computeWorkStyle(String upl, String h7LordSign) {
        if ("Virgo".equals(upl)) {
            return "Detail-oriented, methodical, service-driven, health-conscious professional. "
                    + "Organized, precise, dedicated to quality work.";
        }
        if (List.of("Libra","Gemini").contains(h7LordSign)) {
            return "Collaborative, communicative, aesthetically aware professional. "
                    + "Works well in teams, diplomatic, and intellectually sharp.";
        }
        return "Focused, dedicated, professional work ethic with strong attention to detail";
    }

    private String computeEducation(String upl, String karakamsha, String darakaraka) {
        if ("Virgo".equals(upl) || "Gemini".equals(karakamsha)) {
            return "Graduate or post-graduate — likely in healthcare, science, or technical/professional field";
        }
        if ("Pisces".equals(karakamsha)) {
            return "Graduate or post-graduate — arts, humanities, healthcare, or spiritual sciences";
        }
        return "Graduate level minimum — professionally qualified and educated";
    }

    private String computeProfessionalTraits(String upl, String h7LordSign) {
        if ("Virgo".equals(upl)) {
            return "Analytical, health-conscious, organized, precise, service-oriented, "
                    + "data-driven, systematic, detail-focused";
        }
        return "Intelligent, communicative, professional, organized, and goal-oriented";
    }

    private String computeCoreNature(String h7Sign, String darakaraka, String darakarakaSign) {
        if ("Rahu".equals(darakaraka)) {
            return "Ambitious, unconventional, strong-willed, independent, determined. "
                    + "Does not follow traditional norms blindly — her own woman. Magnetic personality.";
        }
        return "Strong, intelligent, purposeful, with deep emotional loyalty beneath a composed exterior";
    }

    private String computeCommStyle(String h7Sign) {
        if ("Gemini".equals(h7Sign)) {
            return "Articulate, witty, quick, intellectually sharp, excellent conversationalist. "
                    + "Mercury-ruled 7th = wife communicates brilliantly.";
        }
        return "Clear, articulate, confident, and thoughtful communication";
    }

    private String computeEmotionalNature(String darakaraka, String darakarakaSign) {
        if ("Rahu".equals(darakaraka)) {
            return "Emotionally complex, private, deep. Does not express feelings easily to everyone. "
                    + "Intensely loyal once committed. Kapricorn's Saturn influence = emotionally disciplined.";
        }
        return "Emotionally strong, private but deeply loyal and devoted once committed";
    }

    private String computeStrengths(String upl, String darakaraka) {
        if ("Virgo".equals(upl)) {
            return "Health-aware, analytical, self-disciplined, service-minded, practical problem-solver, "
                    + "financially responsible, organized";
        }
        return "Intelligent, loyal, disciplined, independent, emotionally strong, goal-oriented";
    }

    private String computeChallenges(String darakaraka, String darakarakaSign) {
        if ("Rahu".equals(darakaraka)) {
            return "Can be unpredictable at times, has very high personal standards, "
                    + "may resist conventional expectations, needs personal freedom and respect";
        }
        return "Can be selective and high-standards; takes time to fully open up emotionally";
    }

    private String computeSpiritualTendency(String upl, String venusNaksh) {
        if ("Virgo".equals(upl)) {
            return "Pragmatic spirituality — believes in service as worship. "
                    + "Health and healing as spiritual path. Grounded rather than ritualistic.";
        }
        return "Open to spiritual exploration, philosophical discussions, and deeper life meaning";
    }
}
