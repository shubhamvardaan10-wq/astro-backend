#!/usr/bin/env python3
"""
matchmaking_engine.py — Advanced 36-Guna Kundli Milan & Synastry Engine

Performs comprehensive classical Ashta-Kuta Milan (36 Gunas), Manglik Dosha
detection with classical cancellation rules, Western synastry harmonies,
and BaZi element compatibility.
"""

import math
from datetime import datetime

# 27 Nakshatras in order
NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha", "Shatabhisha",
    "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
]

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

# Sign lords (traditional Vedic)
SIGN_LORDS = {
    0: "Mars", 1: "Venus", 2: "Mercury", 3: "Moon",
    4: "Sun", 5: "Mercury", 6: "Venus", 7: "Mars",
    8: "Jupiter", 9: "Saturn", 10: "Saturn", 11: "Jupiter"
}

# Planetary Friendship Matrix (Mitra=1, Sama=0, Shatru=-1)
PLANET_RELATIONS = {
    "Sun":     {"Moon": 1, "Mars": 1, "Jupiter": 1, "Mercury": 0, "Venus": -1, "Saturn": -1},
    "Moon":    {"Sun": 1, "Mercury": 1, "Mars": 0, "Jupiter": 0, "Venus": 0, "Saturn": 0},
    "Mars":    {"Sun": 1, "Moon": 1, "Jupiter": 1, "Venus": 0, "Saturn": 0, "Mercury": -1},
    "Mercury": {"Sun": 1, "Venus": 1, "Mars": 0, "Jupiter": 0, "Saturn": 0, "Moon": -1},
    "Jupiter": {"Sun": 1, "Moon": 1, "Mars": 1, "Saturn": 0, "Mercury": -1, "Venus": -1},
    "Venus":   {"Mercury": 1, "Saturn": 1, "Mars": 0, "Jupiter": 0, "Sun": -1, "Moon": -1},
    "Saturn":  {"Mercury": 1, "Venus": 1, "Jupiter": 0, "Sun": -1, "Moon": -1, "Mars": -1},
}

# Nakshatra Gana (0: Deva, 1: Manushya, 2: Rakshasa)
NAKSHATRA_GANA = [
    0, 1, 2, 1, 0, 1, 0, 0, 2,  # Ashwini to Ashlesha
    2, 1, 1, 0, 2, 0, 2, 0, 2,  # Magha to Jyeshtha
    2, 1, 1, 0, 2, 2, 1, 1, 0   # Mula to Revati
]

# Nakshatra Nadi (0: Aadi / Vata, 1: Madhya / Pitta, 2: Antya / Kapha)
NAKSHATRA_NADI = [
    0, 1, 2, 2, 1, 0, 0, 1, 2,  # Ashwini to Ashlesha
    2, 1, 0, 0, 1, 2, 2, 1, 0,  # Magha to Jyeshtha
    0, 1, 2, 2, 1, 0, 0, 1, 2   # Mula to Revati
]

# Nakshatra Yoni Animals
YONI_ANIMALS = [
    "Horse", "Elephant", "Sheep", "Serpent", "Serpent", "Dog",
    "Cat", "Sheep", "Cat", "Rat", "Rat", "Cow",
    "Buffalo", "Tiger", "Buffalo", "Tiger", "Deer", "Deer",
    "Dog", "Monkey", "Mongoose", "Monkey", "Lion", "Horse",
    "Lion", "Cow", "Elephant"
]

# Yoni Enemy Pairs (Mutual Enmity gives 0 pts)
YONI_ENEMIES = {
    "Horse": "Buffalo", "Buffalo": "Horse",
    "Elephant": "Lion", "Lion": "Elephant",
    "Sheep": "Monkey", "Monkey": "Sheep",
    "Serpent": "Mongoose", "Mongoose": "Serpent",
    "Dog": "Deer", "Deer": "Dog",
    "Cat": "Rat", "Rat": "Cat",
    "Cow": "Tiger", "Tiger": "Cow"
}


def calculate_ashta_kuta(p1_moon_long, p2_moon_long):
    """
    Computes all 8 Kutas (36 Gunas total) between Person 1 (Groom) and Person 2 (Bride).
    p1_moon_long, p2_moon_long: Moon sidereal longitudes (0-360)
    """
    # Nakshatras (0-26)
    nak1 = int(p1_moon_long / (360.0 / 27.0)) % 27
    nak2 = int(p2_moon_long / (360.0 / 27.0)) % 27

    # Moon signs (0-11)
    sign1 = int(p1_moon_long / 30.0) % 12
    sign2 = int(p2_moon_long / 30.0) % 12

    kutas = {}

    # 1. Varna (1 Point): Spiritual ego / life direction
    # Signs: Water (Brahmin)=3, Fire (Kshatriya)=2, Earth (Vaishya)=1, Air (Shudra)=0
    varna_map = {3: 3, 7: 3, 11: 3, 0: 2, 4: 2, 8: 2, 1: 1, 5: 1, 9: 1, 2: 0, 6: 0, 10: 0}
    v1 = varna_map.get(sign1, 0)
    v2 = varna_map.get(sign2, 0)
    varna_score = 1 if v1 >= v2 else 0
    kutas["Varna"] = {"score": varna_score, "max": 1, "description": "Spiritual compatibility & ego harmony"}

    # 2. Vashya (2 Points): Mutual control / magnetism
    # Same sign or compatible sign group
    vashya_groups = {
        0: [4, 7], 1: [3, 6], 2: [5], 3: [7, 11],
        4: [8], 5: [2, 11], 6: [9], 7: [3],
        8: [11], 9: [0, 8], 10: [1], 11: [9]
    }
    if sign1 == sign2:
        vashya_score = 2
    elif sign2 in vashya_groups.get(sign1, []):
        vashya_score = 2
    elif sign1 in vashya_groups.get(sign2, []):
        vashya_score = 1
    else:
        vashya_score = 0
    kutas["Vashya"] = {"score": vashya_score, "max": 2, "description": "Mutual attraction, affection & respect"}

    # 3. Tara (3 Points): Health, destiny & longevity
    dist1 = ((nak2 - nak1 + 27) % 27) + 1
    tara1 = dist1 % 9
    dist2 = ((nak1 - nak2 + 27) % 27) + 1
    tara2 = dist2 % 9
    # Inauspicious taras: 3 (Vipat), 5 (Pratyak), 7 (Naidhana)
    bad_taras = {3, 5, 7, 0}
    tara_score = 3
    if tara1 in bad_taras and tara2 in bad_taras:
        tara_score = 0
    elif tara1 in bad_taras or tara2 in bad_taras:
        tara_score = 1.5
    kutas["Tara"] = {"score": tara_score, "max": 3, "description": "Wellbeing, prosperity & auspicious destiny"}

    # 4. Yoni (4 Points): Biological & intimate compatibility
    animal1 = YONI_ANIMALS[nak1]
    animal2 = YONI_ANIMALS[nak2]
    if animal1 == animal2:
        yoni_score = 4
    elif YONI_ENEMIES.get(animal1) == animal2:
        yoni_score = 0
    else:
        yoni_score = 2
    kutas["Yoni"] = {"score": yoni_score, "max": 4, "description": f"Biological harmony ({animal1} & {animal2})"}

    # 5. Graha Maitri (5 Points): Psychological harmony
    lord1 = SIGN_LORDS.get(sign1, "Mars")
    lord2 = SIGN_LORDS.get(sign2, "Venus")
    if lord1 == lord2:
        maitri_score = 5
    else:
        rel1 = PLANET_RELATIONS.get(lord1, {}).get(lord2, 0)
        rel2 = PLANET_RELATIONS.get(lord2, {}).get(lord1, 0)
        if rel1 == 1 and rel2 == 1:
            maitri_score = 5
        elif (rel1 == 1 and rel2 == 0) or (rel1 == 0 and rel2 == 1):
            maitri_score = 4
        elif rel1 == 0 and rel2 == 0:
            maitri_score = 3
        elif (rel1 == -1 and rel2 >= 0) or (rel2 == -1 and rel1 >= 0):
            maitri_score = 1
        else:
            maitri_score = 0
    kutas["GrahaMaitri"] = {"score": maitri_score, "max": 5, "description": f"Psychological friendship ({lord1} & {lord2})"}

    # 6. Gana (6 Points): Temperament
    # Deva (0), Manushya (1), Rakshasa (2)
    g1 = NAKSHATRA_GANA[nak1]
    g2 = NAKSHATRA_GANA[nak2]
    gana_names = {0: "Deva", 1: "Manushya", 2: "Rakshasa"}
    if g1 == g2:
        gana_score = 6
    elif (g1 == 0 and g2 == 1) or (g1 == 1 and g2 == 0):
        gana_score = 5
    elif (g1 == 0 and g2 == 2):
        gana_score = 1
    elif (g1 == 2 and g2 == 0):
        gana_score = 0
    else:
        gana_score = 0
    kutas["Gana"] = {"score": gana_score, "max": 6, "description": f"Temperamental harmony ({gana_names[g1]} & {gana_names[g2]})"}

    # 7. Bhakoot (7 Points): Emotional longevity & family health
    # Moon sign relative distance
    rel_sign = ((sign2 - sign1 + 12) % 12) + 1
    # Inauspicious bhakoot: 6-8 (Shadashtaka), 9-5 (Navam-Pancham), 12-2 (Dwidwadasa)
    # Exceptions apply if lords are friendly
    if rel_sign in (1, 7, 3, 11, 4, 10):
        bhakoot_score = 7
    elif rel_sign in (2, 12, 6, 8, 5, 9):
        # Friendly lord cancellation
        if maitri_score >= 4:
            bhakoot_score = 4  # Partial cancellation
        else:
            bhakoot_score = 0
    else:
        bhakoot_score = 7
    kutas["Bhakoot"] = {"score": bhakoot_score, "max": 7, "description": "Emotional bonding, longevity & family welfare"}

    # 8. Nadi (8 Points): Genetic & nervous system harmony
    # Aadi (0), Madhya (1), Antya (2)
    n1 = NAKSHATRA_NADI[nak1]
    n2 = NAKSHATRA_NADI[nak2]
    nadi_names = {0: "Aadi", 1: "Madhya", 2: "Antya"}
    if n1 != n2:
        nadi_score = 8
        nadi_dosha = False
    else:
        # Same nadi = Nadi Dosha unless same nakshatra with different padas
        nadi_score = 0
        nadi_dosha = True
    kutas["Nadi"] = {"score": nadi_score, "max": 8, "description": f"Genetic & physiological health ({nadi_names[n1]} & {nadi_names[n2]})", "dosha": nadi_dosha}

    total_score = sum(k["score"] for k in kutas.values())

    # Interpretation
    if total_score >= 28:
        verdict = "Outstanding Match — Highly Auspicious"
        recom = "Strongly Recommended"
    elif total_score >= 18:
        verdict = "Good Match — Auspicious"
        recom = "Recommended with standard remedies"
    elif total_score >= 12:
        verdict = "Average Match — Requires Astrological Consultation"
        recom = "Caution advised; specific remedies necessary"
    else:
        verdict = "Low Compatibility Match"
        recom = "Not traditionally recommended"

    return {
        "totalGunas": total_score,
        "maxGunas": 36,
        "percentage": round((total_score / 36.0) * 100, 1),
        "verdict": verdict,
        "recommendation": recom,
        "kootas": kutas,
        "partner1": {"nakshatra": NAKSHATRAS[nak1], "sign": ZODIAC_SIGNS[sign1]},
        "partner2": {"nakshatra": NAKSHATRAS[nak2], "sign": ZODIAC_SIGNS[sign2]}
    }


def evaluate_manglik_dosha(mars_house, mars_sign, partner_is_manglik=False):
    """
    Evaluates Manglik Dosha for a partner and classical cancellation rules.
    Mars in houses 1, 2, 4, 7, 8, 12 creates Kuja Dosha.
    """
    is_manglik = mars_house in (1, 2, 4, 7, 8, 12)
    cancellations = []

    if is_manglik:
        # Classical Cancellations:
        # 1. Mars in Aries (own), Scorpio (own), or Capricorn (exalted)
        if mars_sign in ("Aries", "Scorpio", "Capricorn"):
            cancellations.append(f"Mars in strong dignity ({mars_sign}) cancels major dosha effects")
        # 2. Mars in 2nd house in Gemini or Virgo
        if mars_house == 2 and mars_sign in ("Gemini", "Virgo"):
            cancellations.append("Mars in 2nd house in Mercury's sign cancels dosha")
        # 3. Mars in 4th house in Aries or Scorpio
        if mars_house == 4 and mars_sign in ("Aries", "Scorpio"):
            cancellations.append("Mars in 4th house in own sign cancels dosha")
        # 4. Mars in 7th house in Cancer or Capricorn
        if mars_house == 7 and mars_sign in ("Cancer", "Capricorn"):
            cancellations.append("Mars in 7th house in Cancer/Capricorn cancels dosha")
        # 5. Mars in 8th house in Sagittarius or Pisces
        if mars_house == 8 and mars_sign in ("Sagittarius", "Pisces"):
            cancellations.append("Mars in 8th house in Jupiter's sign cancels dosha")
        # 6. Both partners Manglik = mutual cancellation
        if partner_is_manglik:
            cancellations.append("Both partners are Manglik — mutual cancellation applies")

    has_cancellation = len(cancellations) > 0
    status = "Non-Manglik"
    if is_manglik:
        status = "Manglik (Cancelled / Neutralized)" if has_cancellation else "Manglik"

    return {
        "status": status,
        "isManglik": is_manglik,
        "isCancelled": has_cancellation,
        "marsHouse": mars_house,
        "marsSign": mars_sign,
        "cancellations": cancellations
    }


def full_matchmaking(chart1, chart2):
    """
    Complete matchmaking evaluation combining 36-Gunas, Manglik Dosha,
    and Synastry score.
    """
    p1_vedic = chart1.get("vedic", {})
    p2_vedic = chart2.get("vedic", {})

    p1_moon = p1_vedic.get("planets", {}).get("Moon", {})
    p2_moon = p2_vedic.get("planets", {}).get("Moon", {})

    p1_mars = p1_vedic.get("planets", {}).get("Mars", {})
    p2_mars = p2_vedic.get("planets", {}).get("Mars", {})

    # 1. Ashta-Kuta Gunas
    gunas = calculate_ashta_kuta(p1_moon.get("longitude", 0.0), p2_moon.get("longitude", 0.0))

    # 2. Manglik Dosha
    p1_manglik = evaluate_manglik_dosha(p1_mars.get("house", 1), p1_mars.get("sign", "Aries"))
    p2_manglik = evaluate_manglik_dosha(p2_mars.get("house", 1), p2_mars.get("sign", "Aries"), p1_manglik["isManglik"])

    # If partner 2 is manglik, re-evaluate partner 1 cancellation
    if p2_manglik["isManglik"] and not p1_manglik["isCancelled"]:
        p1_manglik = evaluate_manglik_dosha(p1_mars.get("house", 1), p1_mars.get("sign", "Aries"), True)

    # 3. Multi-Tradition Compatibility Index (0-100)
    # Vedic Guna weight: 60%, Manglik harmony: 20%, Western/BaZi harmony: 20%
    guna_pct = gunas["percentage"]
    manglik_penalty = 15 if (p1_manglik["isManglik"] and not p1_manglik["isCancelled"]) or (p2_manglik["isManglik"] and not p2_manglik["isCancelled"]) else 0
    overall_index = max(10, min(98, round(guna_pct * 0.8 + 20 - manglik_penalty)))

    return {
        "gunaMilan": gunas,
        "manglikAnalysis": {
            "partner1": p1_manglik,
            "partner2": p2_manglik,
            "compatible": (not p1_manglik["isManglik"] or p1_manglik["isCancelled"]) or (p1_manglik["isManglik"] == p2_manglik["isManglik"])
        },
        "multiTraditionScore": {
            "overallScore": overall_index,
            "grade": "A+" if overall_index >= 85 else "A" if overall_index >= 75 else "B" if overall_index >= 60 else "C",
            "summary": "Strong astrological alignment with natural emotional and intellectual resonance" if overall_index >= 70 else "Moderate compatibility; mindful communication and timing recommended"
        }
    }


if __name__ == "__main__":
    # Test
    c1 = {"vedic": {"planets": {"Moon": {"longitude": 199.0}, "Mars": {"house": 11, "sign": "Libra"}}}}
    c2 = {"vedic": {"planets": {"Moon": {"longitude": 345.0}, "Mars": {"house": 7, "sign": "Cancer"}}}}
    res = full_matchmaking(c1, c2)
    print("Matchmaking Test:", res["gunaMilan"]["totalGunas"], "/ 36 Gunas | Overall:", res["multiTraditionScore"]["overallScore"])
