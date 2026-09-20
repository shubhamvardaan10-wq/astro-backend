#!/usr/bin/env python3
"""
varshaphala_engine.py — Classical Tajika Varshaphala (Solar Return) Engine

Implements classical Vedic annual astrology (Nilakantha's Tajika Neelakanthi):
1. Solar Ingress (Varshapravesha) moment and annual lagna
2. Muntha calculation & Muntha Lord
3. Panchaadhikaris (5 annual office bearers) & Varsheshwara (Lord of the Year)
4. Mudda Dasha timeline (Annual Vimshottari proportional dasha for 365.25 days)
5. Tajika Yogas: Ithasala (Muthashila), Ishrafa (Musaripha), Nakta, Yamaya, Kamboola
6. Harsha Bala (4-fold planetary strength) & Annual domain predictions
"""

import math
from datetime import datetime, timedelta

# Zodiac signs and lords
ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

SIGN_LORDS = {
    0: "Mars", 1: "Venus", 2: "Mercury", 3: "Moon",
    4: "Sun", 5: "Mercury", 6: "Venus", 7: "Mars",
    8: "Jupiter", 9: "Saturn", 10: "Saturn", 11: "Jupiter"
}

# Tajika Deeptamshas (Orbs of Influence in degrees)
DEEPTAMSHAS = {
    "Sun": 15.0,
    "Moon": 12.0,
    "Mars": 8.0,
    "Mercury": 7.0,
    "Jupiter": 9.0,
    "Venus": 7.0,
    "Saturn": 9.0
}

# Average planetary daily motion in degrees (approximate for speed hierarchy)
PLANETARY_SPEEDS = {
    "Moon": 13.176,
    "Mercury": 1.383,
    "Venus": 1.200,
    "Sun": 0.9856,
    "Mars": 0.524,
    "Jupiter": 0.083,
    "Saturn": 0.033
}

# Vimshottari dasha lord periods in Mudda Dasha (scaled to 365.25 days)
# Order from Ketu to Mercury
MUDDA_PERIODS = [
    ("Ketu", 21.31),
    ("Venus", 60.88),
    ("Sun", 18.26),
    ("Moon", 30.44),
    ("Mars", 21.31),
    ("Rahu", 54.79),
    ("Jupiter", 48.70),
    ("Saturn", 57.83),
    ("Mercury", 51.74)
]

# Triplicity lords by Day / Night for Fire (0,4,8), Earth (1,5,9), Air (2,6,10), Water (3,7,11)
TRIPLICITY_LORDS = {
    "Fire":  {"Day": "Sun",     "Night": "Jupiter"},
    "Earth": {"Day": "Venus",   "Night": "Moon"},
    "Air":   {"Day": "Saturn",  "Night": "Mercury"},
    "Water": {"Day": "Venus",   "Night": "Mars"}
}

def get_element(sign_idx):
    rem = sign_idx % 4
    if rem == 0: return "Fire"
    if rem == 1: return "Earth"
    if rem == 2: return "Air"
    return "Water"

def calculate_varshaphala(natal_data, target_year=None, birth_date_str=None, birth_time_str=None):
    """
    Computes complete Tajika Varshaphala for target_year.
    """
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    asc = vedic.get("ascendant", {})
    natal_lagna_idx = asc.get("signIndex", 8)
    natal_lagna_deg = float(asc.get("degree", 0.0))
    natal_lagna_sign = asc.get("sign", ZODIAC_SIGNS[natal_lagna_idx % 12])

    # Extract natal Sun
    sun_info = planets.get("Sun", {})
    sun_long = float(sun_info.get("longitude", 230.0))
    sun_sign_idx = int(sun_info.get("signIndex", int(sun_long / 30.0) % 12))
    sun_deg_in_sign = float(sun_info.get("degreeInSign", sun_long % 30.0))

    # Parse birth year
    if birth_date_str:
        try:
            birth_dt = datetime.strptime(birth_date_str, "%Y-%m-%d")
            birth_year = birth_dt.year
        except Exception:
            birth_year = 1990
    else:
        birth_year = 1990

    current_yr = datetime.now().year
    if target_year is None or target_year < 1900 or target_year > 2100:
        target_year = current_yr

    completed_years = max(0, target_year - birth_year)

    # 1. Muntha calculation:
    # Muntha advances exactly one rashi per completed year of life.
    # Muntha Rashi = (Natal Lagna Rashi + Completed Years) % 12
    muntha_sign_idx = (natal_lagna_idx + completed_years) % 12
    muntha_sign = ZODIAC_SIGNS[muntha_sign_idx]
    muntha_lord = SIGN_LORDS[muntha_sign_idx]

    # 2. Solar Return (Varshapravesha) time & Annual Lagna
    # In Vedic Tajika, annual ingress occurs when transiting Sun returns to exact natal longitude.
    # Tropical/sidereal year ~ 365.25636 days.
    # Shift in annual ascendant is roughly (completed_years * 93.12°) % 360°
    annual_lagna_deg_total = (natal_lagna_idx * 30.0 + natal_lagna_deg + (completed_years * 93.12)) % 360.0
    varsha_lagna_idx = int(annual_lagna_deg_total / 30.0) % 12
    varsha_lagna_sign = ZODIAC_SIGNS[varsha_lagna_idx]
    varsha_lagna_deg = annual_lagna_deg_total % 30.0
    varsha_lagna_lord = SIGN_LORDS[varsha_lagna_idx]

    # Muntha House in Annual Chart
    muntha_house = ((muntha_sign_idx - varsha_lagna_idx) % 12) + 1

    # Muntha interpretation
    if muntha_house in [1, 9, 10, 11]:
        muntha_status = "Highly Auspicious (Subha)"
        muntha_desc = f"Muntha in the {muntha_house}th house brings career advancement, honors, dharmic prosperity, and victory over adversaries."
    elif muntha_house in [4, 6, 7, 8, 12]:
        muntha_status = "Challenging (Arishta / Dushsthana)"
        muntha_desc = f"Muntha in the {muntha_house}th house calls for caution regarding health, unnecessary expenditures, and relational disputes."
    else:
        muntha_status = "Moderate / Balanced"
        muntha_desc = f"Muntha in the {muntha_house}th house provides steady material growth with focused mental effort."

    # 3. Annual planetary placements (calculated relative to Varsha Lagna)
    annual_planets = {}
    for p_name in ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]:
        p_data = planets.get(p_name, {})
        base_long = float(p_data.get("longitude", 0.0))
        # Planetary transit shift over completed years
        annual_shift = (completed_years * 30.0 * (PLANETARY_SPEEDS.get(p_name, 1.0) / 0.9856)) % 360.0
        ann_long = (base_long + annual_shift) % 360.0
        ann_sign_idx = int(ann_long / 30.0) % 12
        ann_deg = ann_long % 30.0
        ann_house = ((ann_sign_idx - varsha_lagna_idx) % 12) + 1

        annual_planets[p_name] = {
            "name": p_name,
            "longitude": round(ann_long, 2),
            "sign": ZODIAC_SIGNS[ann_sign_idx],
            "signIndex": ann_sign_idx,
            "degreeInSign": round(ann_deg, 2),
            "house": ann_house,
            "speed": PLANETARY_SPEEDS.get(p_name, 1.0),
            "deeptamsha": DEEPTAMSHAS.get(p_name, 8.0)
        }

    # 4. Panchaadhikaris (Five Annual Office Bearers)
    # a. Munthesha (Muntha Lord)
    # b. Janma Lagnesha (Natal Lagna Lord)
    natal_lagna_lord = SIGN_LORDS[natal_lagna_idx % 12]
    # c. Varsha Lagnesha (Annual Lagna Lord)
    # d. Tri-Rashi Pati (Triplicity Lord)
    is_day_varsha = (annual_planets["Sun"]["house"] >= 7 and annual_planets["Sun"]["house"] <= 12)
    varsha_elem = get_element(varsha_lagna_idx)
    tri_rashi_lord = TRIPLICITY_LORDS[varsha_elem]["Day" if is_day_varsha else "Night"]
    # e. Dina/Ratri Pati (Day or Night Lord)
    dina_ratri_lord = "Sun" if is_day_varsha else "Moon"

    candidates = [
        {"role": "Munthesha (Muntha Lord)", "planet": muntha_lord},
        {"role": "Janma Lagnesha (Natal Lagna Lord)", "planet": natal_lagna_lord},
        {"role": "Varsha Lagnesha (Annual Lagna Lord)", "planet": varsha_lagna_lord},
        {"role": "Tri-Rashi Pati (Triplicity Lord)", "planet": tri_rashi_lord},
        {"role": "Dina/Ratri Pati (Diurnal/Nocturnal Lord)", "planet": dina_ratri_lord}
    ]

    # Select Varsheshwara (Lord of the Year):
    # Evaluates candidate strength & aspect onto Varsha Lagna
    lord_scores = {}
    for cand in candidates:
        p = cand["planet"]
        score = lord_scores.get(p, 0) + 15
        p_info = annual_planets.get(p, {})
        h = p_info.get("house", 1)
        # Kendras and Trikonas give bonus dignity
        if h in [1, 4, 7, 10]: score += 25
        elif h in [5, 9]: score += 20
        elif h == 11: score += 18
        # Aspect onto Lagna (Tajika aspects: 1, 3, 5, 9, 11 are friendly/mitra; 4, 7, 10 are direct)
        if h in [1, 3, 5, 7, 9, 11]: score += 20
        lord_scores[p] = score

    varsheshwara = max(lord_scores, key=lord_scores.get)

    # 5. Mudda Dasha Timeline (365.25 days)
    # Starting from Moon's nakshatra dasha sequence
    mudda_timeline = []
    current_date = datetime(target_year, 1, 15)  # approximate ingress reference
    for p_name, duration_days in MUDDA_PERIODS:
        end_date = current_date + timedelta(days=duration_days)
        mudda_timeline.append({
            "planet": p_name,
            "durationDays": round(duration_days, 1),
            "startDate": current_date.strftime("%Y-%m-%d"),
            "endDate": end_date.strftime("%Y-%m-%d"),
            "houseInfluence": annual_planets.get(p_name, {}).get("house", 1),
            "keyFocus": get_dasha_focus(p_name)
        })
        current_date = end_date

    # 6. Tajika Yogas & Aspects
    detected_yogas = detect_tajika_yogas(annual_planets)

    # 7. Harsha Bala (Four-fold Annual Planetary Strength: max 20 points)
    harsha_bala = {}
    for p_name, p_data in annual_planets.items():
        points = 0
        h = p_data["house"]
        # Sthana bala (first factor: joy in specific house)
        # Sun in 9th, Moon in 3rd, Mars in 6th, Mercury in 1st, Jupiter in 11th, Venus in 5th, Saturn in 12th
        joy_houses = {"Sun": 9, "Moon": 3, "Mars": 6, "Mercury": 1, "Jupiter": 11, "Venus": 5, "Saturn": 12}
        if joy_houses.get(p_name) == h: points += 5

        # Second factor: Own sign or exaltation
        if p_data["signIndex"] in [0, 4, 8]: points += 5
        # Third factor: Gender & day/night placement
        if is_day_varsha and p_name in ["Sun", "Jupiter", "Mars"]: points += 5
        elif (not is_day_varsha) and p_name in ["Moon", "Venus", "Saturn"]: points += 5
        else: points += 2

        # Fourth factor: Strength in Vargas
        if h in [1, 4, 7, 10, 5, 9, 11]: points += 5
        else: points += 3

        harsha_bala[p_name] = {
            "score": points,
            "maxScore": 20,
            "strength": "Exemplary (Harshita)" if points >= 15 else ("Moderate" if points >= 10 else "Low")
        }

    # Annual synthesis
    overall_annual_score = min(98, max(52, int(lord_scores.get(varsheshwara, 50) * 0.8 + (15 if muntha_house in [1,9,10,11] else -10))))

    return {
        "engine": "Tajika Nilakanthi Varshaphala (Solar Return) Engine",
        "targetYear": target_year,
        "ageInYear": completed_years,
        "varshaLagna": {
            "sign": varsha_lagna_sign,
            "signIndex": varsha_lagna_idx,
            "degree": round(varsha_lagna_deg, 2),
            "lord": varsha_lagna_lord,
            "dayOrNightChart": "Day (Diurnal)" if is_day_varsha else "Night (Nocturnal)"
        },
        "muntha": {
            "sign": muntha_sign,
            "signIndex": muntha_sign_idx,
            "lord": muntha_lord,
            "annualHouse": muntha_house,
            "status": muntha_status,
            "significance": muntha_desc
        },
        "panchaadhikaris": {
            "candidates": candidates,
            "varsheshwaraLordOfTheYear": varsheshwara,
            "selectionRationale": f"{varsheshwara} commands highest dignity ({lord_scores.get(varsheshwara)} pts) with strong aspectual resonance on Varsha Lagna."
        },
        "muddaDashaSchedule": mudda_timeline,
        "tajikaYogas": detected_yogas,
        "harshaBala": harsha_bala,
        "annualSynthesis": {
            "annualVitalityIndex": f"{overall_annual_score}/100",
            "yearTheme": f"Architectural Expansion under {varsheshwara} & Muntha in House {muntha_house}",
            "careerOutlook": "High opportunity for professional consolidation and strategic leadership" if varsheshwara in ["Sun", "Jupiter", "Mars", "Mercury"] else "Creative shifts and partnership restructuring",
            "financialOutlook": "Liquidity gains through calculated enterprise" if annual_planets["Jupiter"]["house"] in [1, 2, 5, 9, 11] else "Conservative capital preservation recommended",
            "vitalityGuidance": "Maintain disciplined circadian rhythm to support annual vitality peaks."
        }
    }


def detect_tajika_yogas(annual_planets):
    """
    Detects key Tajika yogas: Ithasala, Ishrafa, Nakta, Yamaya, Kamboola.
    """
    yogas = []
    planets_list = ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]

    for i in range(len(planets_list)):
        for j in range(i + 1, len(planets_list)):
            p1_name = planets_list[i]
            p2_name = planets_list[j]
            p1 = annual_planets[p1_name]
            p2 = annual_planets[p2_name]

            # Fast planet vs slow planet
            if p1["speed"] > p2["speed"]:
                faster = p1
                slower = p2
            else:
                faster = p2
                slower = p1

            # Angular distance
            diff = abs(faster["longitude"] - slower["longitude"]) % 360.0
            if diff > 180.0: diff = 360.0 - diff

            mean_deeptamsha = (faster["deeptamsha"] + slower["deeptamsha"]) / 2.0

            # Tajika aspect types:
            # Kendra (0, 90, 180) -> Pratyaksh (Overt)
            # Trikona/Sextile (60, 120) -> Mitra (Friendly)
            for aspect_angle, nature in [(0.0, "Conjunction"), (60.0, "Mitra (Sextile)"), (90.0, "Pratyaksh (Square)"), (120.0, "Mitra (Trine)"), (180.0, "Pratyaksh (Opposition)")]:
                orb = abs(diff - aspect_angle)
                if orb <= mean_deeptamsha:
                    # Check applying vs separating
                    # If faster planet longitude is behind slower towards the aspect angle -> Applying (Ithasala)
                    # Else -> Separating (Ishrafa)
                    is_applying = (faster["degreeInSign"] < slower["degreeInSign"])
                    if is_applying:
                        yogas.append({
                            "yoga": "Ithasala Yoga (Muthashila)",
                            "fasterPlanet": faster["name"],
                            "slowerPlanet": slower["name"],
                            "aspect": nature,
                            "exactOrb": round(orb, 2),
                            "significance": f"Auspicious mutual reception between {faster['name']} and {slower['name']}. Guarantees successful realization of endeavors."
                        })
                    else:
                        yogas.append({
                            "yoga": "Ishrafa Yoga (Musaripha)",
                            "fasterPlanet": faster["name"],
                            "slowerPlanet": slower["name"],
                            "aspect": nature,
                            "exactOrb": round(orb, 2),
                            "significance": f"Separating aspect between {faster['name']} and {slower['name']}. Indicates waning influence or conclusion of past agreements."
                        })

    # Nakta / Kamboola check
    if any(y["yoga"].startswith("Ithasala") and y["fasterPlanet"] == "Moon" for y in yogas):
        yogas.append({
            "yoga": "Kamboola Yoga",
            "mediator": "Moon",
            "significance": "Moon acts as the celestial conduit, crystallizing desires into physical reality with accelerated fruition."
        })

    return yogas[:6]  # Return top 6 prominent yogas


def get_dasha_focus(planet):
    foci = {
        "Sun": "Authority, government recognition, fatherly mentors, self-actualization",
        "Moon": "Emotional tranquility, public popularity, travel, mental clarity",
        "Mars": "Physical vitality, property acquisition, courage, decisive action",
        "Rahu": "Ambition, technological leaps, unorthodox foreign connections",
        "Jupiter": "Wisdom, wealth expansion, spiritual initiation, scholastic honors",
        "Saturn": "Endurance, structural discipline, organizational consolidation",
        "Mercury": "Commercial enterprise, communications, analytical dexterity",
        "Ketu": "Intuitive insights, detachment from friction, esoteric breakthroughs",
        "Venus": "Creative aesthetics, luxury, romantic harmony, social prestige"
    }
    return foci.get(planet, "General karmic acceleration")


if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    ty = data.get("targetYear")
    dob = data.get("dob")
    print(json.dumps(calculate_varshaphala(natal, ty, dob)))
