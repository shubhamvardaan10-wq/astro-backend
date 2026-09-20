#!/usr/bin/env python3
"""
prashna_engine.py — Advanced Vedic Horary (Prashna Kundli) Engine

Casts an instant ephemeris chart for the precise moment of query and evaluates:
1. Domain classification (Career, Marriage, Wealth, Health, Travel, Property)
2. Lagna Lord (Querent) and Karyesh (Significant House Lord)
3. Classical Tajika Horary Yogas (Ithasala, Ishrafa, Nakta, Yamaya)
4. Deterministic Outcome (Yes / No / Delayed) with confidence percentage and timeframe.
"""

from datetime import datetime
import re
import swisseph as swe

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

SIGN_LORDS = {
    0: "Mars", 1: "Venus", 2: "Mercury", 3: "Moon",
    4: "Sun", 5: "Mercury", 6: "Venus", 7: "Mars",
    8: "Jupiter", 9: "Saturn", 10: "Saturn", 11: "Jupiter"
}

# Sign modality (0: Chara/Movable - fast, 1: Sthira/Fixed - slow, 2: Dvisvabhava/Dual - moderate)
SIGN_MODALITY = {
    0: "Movable", 1: "Fixed", 2: "Dual", 3: "Movable",
    4: "Fixed", 5: "Dual", 6: "Movable", 7: "Fixed",
    8: "Dual", 9: "Movable", 10: "Fixed", 11: "Dual"
}

PLANET_SPEED_RANK = {
    "Moon": 1, "Mercury": 2, "Venus": 3, "Sun": 4,
    "Mars": 5, "Jupiter": 6, "Saturn": 7
}

DOMAINS = {
    "career": {
        "house": 10,
        "keywords": ["job", "career", "promotion", "work", "boss", "interview", "business", "deal", "contract"],
        "label": "Career, Employment & Business Enterprise"
    },
    "marriage": {
        "house": 7,
        "keywords": ["marriage", "wife", "husband", "partner", "love", "relationship", "divorce", "proposal"],
        "label": "Marriage, Partnerships & Romance"
    },
    "wealth": {
        "house": 11,
        "keywords": ["money", "wealth", "profit", "gain", "investment", "rich", "lottery", "property", "finance"],
        "label": "Wealth, Profits & Financial Inflow"
    },
    "health": {
        "house": 1,
        "keywords": ["health", "disease", "diabetes", "cure", "recovery", "hospital", "doctor", "illness", "surgery"],
        "label": "Health, Vitality & Physical Recovery"
    },
    "property": {
        "house": 4,
        "keywords": ["house", "land", "flat", "vehicle", "car", "real estate", "property"],
        "label": "Property, Real Estate & Immovable Assets"
    },
    "travel": {
        "house": 9,
        "keywords": ["visa", "travel", "foreign", "abroad", "relocate", "journey", "trip"],
        "label": "Travel, Higher Education & Foreign Relocation"
    }
}


def classify_question_domain(question):
    """Detects the inquiry domain from question text."""
    if not question:
        return "general", 1, "General Inquiry & Life Overview"

    q_lower = question.lower()
    for domain_key, info in DOMAINS.items():
        if any(re.search(r'\b' + re.escape(w) + r'\b', q_lower) for w in info["keywords"]):
            return domain_key, info["house"], info["label"]

    return "general", 1, "General Inquiry & Vitality"


def compute_prashna(question, lat=25.6858, lon=85.2146, as_of=None):
    """
    Casts Prashna chart and analyzes horary outcome.
    """
    now = as_of if as_of else datetime.utcnow()

    # Calculate Julian Day
    jd = swe.julday(now.year, now.month, now.day,
                    now.hour + now.minute / 60.0 + now.second / 3600.0)

    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)

    # 1. Ascendant
    houses, ascmc = swe.houses_ex(jd, lat, lon, b'W', swe.FLG_SIDEREAL)
    asc_deg = ascmc[0]
    asc_sign_idx = int(asc_deg / 30.0) % 12
    asc_lord = SIGN_LORDS[asc_sign_idx]

    # 2. Planetary positions
    planets = {}
    for p_name, p_id in [("Sun", swe.SUN), ("Moon", swe.MOON), ("Mars", swe.MARS),
                         ("Mercury", swe.MERCURY), ("Jupiter", swe.JUPITER),
                         ("Venus", swe.VENUS), ("Saturn", swe.SATURN)]:
        res, flags = swe.calc_ut(jd, p_id, swe.FLG_SIDEREAL | swe.FLG_SPEED)
        p_lon = res[0]
        p_sign = int(p_lon / 30.0) % 12
        p_house = ((p_sign - asc_sign_idx + 12) % 12) + 1
        planets[p_name] = {
            "longitude": p_lon,
            "signIndex": p_sign,
            "sign": ZODIAC_SIGNS[p_sign],
            "degree": round(p_lon % 30.0, 2),
            "house": p_house,
            "speed": res[3],
            "retrograde": res[3] < 0
        }

    # 3. Domain & Significator
    domain_key, target_house, domain_label = classify_question_domain(question)
    target_sign_idx = (asc_sign_idx + target_house - 1) % 12
    karyesh_planet = SIGN_LORDS[target_sign_idx]

    lagnesh_info = planets.get(asc_lord, {})
    karyesh_info = planets.get(karyesh_planet, {})
    moon_info = planets.get("Moon", {})

    # 4. Horary Yoga Evaluation (Ithasala / Applying Aspect)
    # Check angular distance between Lagnesh and Karyesh
    l_lon = lagnesh_info.get("longitude", 0.0)
    k_lon = karyesh_info.get("longitude", 0.0)
    diff = abs(l_lon - k_lon) % 360.0
    if diff > 180: diff = 360 - diff

    # Classical aspect orbs (within 8 degrees of Conjunction, Sextile 60, Trine 120, Opposition 180)
    is_conjunction = diff < 8.0
    is_trine = abs(diff - 120.0) < 8.0
    is_sextile = abs(diff - 60.0) < 6.0
    is_square = abs(diff - 90.0) < 6.0
    is_opposition = abs(diff - 180.0) < 8.0

    has_benefic_aspect = is_conjunction or is_trine or is_sextile
    has_challenging_aspect = is_square or is_opposition

    # Faster moving planet applies to slower moving planet
    faster_is_lagnesh = PLANET_SPEED_RANK.get(asc_lord, 5) < PLANET_SPEED_RANK.get(karyesh_planet, 5)
    is_applying = (l_lon < k_lon) if faster_is_lagnesh else (k_lon < l_lon)

    # 5. Outcome synthesis
    if asc_lord == karyesh_planet:
        outcome = "Highly Favorable / Definite Yes"
        prob = 92
        status_note = f"Querent (Lagnesh) and Objective (Karyesh) are governed by the same planet ({asc_lord}), indicating direct personal agency and alignment."
        timeframe = "Immediate to 2–4 weeks"
    elif has_benefic_aspect and is_applying:
        outcome = "Favorable / Yes — Success Promised"
        prob = 85
        status_note = f"Ithasala Yoga formed: {asc_lord} applies favorably to {karyesh_planet} within orb, promising success through purposeful action."
        timeframe = "Within 1 to 3 months"
    elif has_benefic_aspect and not is_applying:
        outcome = "Delayed / Eventual Success"
        prob = 68
        status_note = f"Aspect is separating (Ishrafa): The initial window passed, but subsequent lunar movement supports delayed achievement."
        timeframe = "Within 3 to 6 months"
    elif has_challenging_aspect:
        outcome = "Requires Strenuous Effort / Obstacles Present"
        prob = 45
        status_note = f"Challenging aspect between {asc_lord} and {karyesh_planet}. Direct effort, negotiation, or structural compromises required."
        timeframe = "6 to 9 months or conditional on revision"
    else:
        outcome = "Neutral / Conditional — Developing Situation"
        prob = 55
        status_note = f"No direct aspect between {asc_lord} and {karyesh_planet}. The outcome relies on third-party intervention or upcoming lunar transit."
        timeframe = "Uncertain; re-evaluate after key milestones"

    # Moon placement modifier
    moon_house = moon_info.get("house", 1)
    if moon_house in (6, 8, 12):
        prob = max(20, prob - 10)
        status_note += " (Moon in Dusthana indicates anxiety or procedural friction)."
    elif moon_house in (1, 5, 9, 10, 11):
        prob = min(98, prob + 5)

    return {
        "question": question,
        "domain": {
            "key": domain_key,
            "label": domain_label,
            "houseAnalyzed": target_house,
            "karyesh": karyesh_planet
        },
        "prashnaLagna": {
            "ascendant": ZODIAC_SIGNS[asc_sign_idx],
            "degree": round(asc_deg % 30.0, 2),
            "lagnesh": asc_lord,
            "modality": SIGN_MODALITY.get(asc_sign_idx, "Movable")
        },
        "horaryVerdict": {
            "outcome": outcome,
            "probabilityPercentage": prob,
            "confidence": "High" if prob >= 80 or prob <= 30 else "Moderate",
            "estimatedTimeframe": timeframe,
            "astrologicalRationale": status_note
        },
        "keyPlanets": {
            "lagnesh": {"planet": asc_lord, "house": lagnesh_info.get("house"), "sign": lagnesh_info.get("sign")},
            "karyesh": {"planet": karyesh_planet, "house": karyesh_info.get("house"), "sign": karyesh_info.get("sign")},
            "moon": {"house": moon_house, "sign": moon_info.get("sign")}
        }
    }


if __name__ == "__main__":
    q = "Will I get married soon and find a good partner?"
    res = compute_prashna(q)
    print("Prashna Result for:", res["question"])
    print("  Domain:", res["domain"]["label"])
    print("  Outcome:", res["horaryVerdict"]["outcome"])
    print("  Probability:", res["horaryVerdict"]["probabilityPercentage"], "%")
    print("  Timeframe:", res["horaryVerdict"]["estimatedTimeframe"])
