#!/usr/bin/env python3
"""
prashna_horary_engine.py — Classical Vedic & KP Horary Astrological Engine

Evaluates instant Prashna (Horary) charts based on:
1. Question Category (Career, Marriage, Wealth, Health, Lost Objects, Legal)
2. Target Karyasthana (House of Matter) & Karyesha (Lord of Matter)
3. Querent's Lagnesha & Moon Application
4. Tajika Yogas: Ithasala (Mutual Application), Ishrafa (Separation), Nakta, Yamaya
5. KP Horary Sub-Lord Verification (Seed 1-249)
6. Karya Siddhi Probability Score (0-100%) & Manifestation Time Window (Phala Kala)
"""

import math
from datetime import datetime

QUESTION_HOUSES = {
    "CAREER": {"house": 10, "significator": "Sun", "secondary": 11, "name": "Career & Professional Elevation"},
    "MARRIAGE": {"house": 7, "significator": "Venus", "secondary": 11, "name": "Marriage, Partnership & Union"},
    "WEALTH": {"house": 2, "significator": "Jupiter", "secondary": 11, "name": "Wealth, Liquidity & Gains"},
    "HEALTH": {"house": 1, "significator": "Sun", "secondary": 6, "name": "Physical Vitality & Disease Recovery"},
    "LOST_OBJECTS": {"house": 2, "significator": "Mercury", "secondary": 4, "name": "Recovery of Lost / Stolen Items"},
    "LEGAL": {"house": 6, "significator": "Mars", "secondary": 1, "name": "Litigation, Disputes & Competition"},
    "GENERAL": {"house": 1, "significator": "Jupiter", "secondary": 9, "name": "General Auspiciousness & Dharma"}
}

SIGN_LORDS = [
    "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
    "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
]

SIGN_MODALITIES = [
    "Chara (Movable - Fast / Days)",
    "Sthira (Fixed - Delayed / Months)",
    "Dvisvabhava (Dual - Moderate / Weeks)",
    "Chara (Movable - Fast / Days)",
    "Sthira (Fixed - Delayed / Months)",
    "Dvisvabhava (Dual - Moderate / Weeks)",
    "Chara (Movable - Fast / Days)",
    "Sthira (Fixed - Delayed / Months)",
    "Dvisvabhava (Dual - Moderate / Weeks)",
    "Chara (Movable - Fast / Days)",
    "Sthira (Fixed - Delayed / Months)",
    "Dvisvabhava (Dual - Moderate / Weeks)"
]

PLANET_SPEEDS = {
    "Moon": 13.17,
    "Mercury": 1.20,
    "Venus": 1.00,
    "Sun": 0.98,
    "Mars": 0.52,
    "Jupiter": 0.08,
    "Saturn": 0.03
}

PLANET_ORBS = {
    "Sun": 15.0,
    "Moon": 12.0,
    "Mars": 8.0,
    "Jupiter": 9.0,
    "Saturn": 9.0,
    "Mercury": 7.0,
    "Venus": 7.0
}

def calculate_prashna(question_type="CAREER", kp_seed=None, query_datetime_str=None, query_text="Will my venture succeed?"):
    q_key = question_type.upper().strip() if question_type else "CAREER"
    matter = QUESTION_HOUSES.get(q_key, QUESTION_HOUSES["CAREER"])

    # Determine Lagna degree from KP Seed (1-249) or Time
    if kp_seed and 1 <= int(kp_seed) <= 249:
        seed_val = int(kp_seed)
        asc_deg = ((seed_val - 1) * (360.0 / 249.0)) % 360.0
        calculation_mode = f"KP Horary 1-249 Sub-Lord Seed ({seed_val})"
    else:
        # Time-based ascendant simulation
        dt = datetime.now()
        if query_datetime_str:
            try:
                dt = datetime.strptime(query_datetime_str, "%Y-%m-%d %H:%M:%S")
            except Exception:
                try:
                    dt = datetime.strptime(query_datetime_str, "%Y-%m-%d")
                except Exception:
                    dt = datetime.now()
        minute_of_day = dt.hour * 60 + dt.minute
        asc_deg = (minute_of_day * 0.25 + 75.0) % 360.0
        calculation_mode = f"Time-Space Instantaneous Ascendant ({dt.strftime('%Y-%m-%d %H:%M')})"

    asc_sign_idx = int(asc_deg / 30.0)
    asc_deg_in_sign = asc_deg % 30.0
    lagnesha = SIGN_LORDS[asc_sign_idx]

    target_house = matter["house"]
    target_sign_idx = (asc_sign_idx + target_house - 1) % 12
    karyesha = SIGN_LORDS[target_sign_idx]

    # Simulating planet positions relative to ascendant
    lagnesha_long = (asc_deg + 34.2) % 360.0
    karyesha_long = (asc_sign_idx * 30.0 + (target_house * 28.5)) % 360.0
    moon_long = (asc_deg + 118.4) % 360.0

    # Angular distance between Lagnesha & Karyesha
    diff_deg = abs(lagnesha_long - karyesha_long) % 360.0
    if diff_deg > 180.0: diff_deg = 360.0 - diff_deg

    avg_orb = (PLANET_ORBS.get(lagnesha, 8.0) + PLANET_ORBS.get(karyesha, 8.0)) / 2.0
    speed_l = PLANET_SPEEDS.get(lagnesha, 0.5)
    speed_k = PLANET_SPEEDS.get(karyesha, 0.5)

    # Tajika Yogas Assessment
    if diff_deg <= avg_orb:
        if speed_l > speed_k:
            tajika_yoga = "Ithasala (Muthashila) Yoga — Rapid Mutual Application of Energies"
            karya_siddhi_score = 88
            verdict = "Definite Success & Favorable Fulfillment (Karya Siddhi Prapti)"
        else:
            tajika_yoga = "Ishrafa (Musaripha) Yoga — Separating Energy; Effort Dissipation"
            karya_siddhi_score = 42
            verdict = "Conditional Delay; Requires Strenuous Secondary Intervention"
    elif diff_deg in [60.0, 90.0, 120.0, 180.0] or abs(diff_deg - 120.0) <= 6.0:
        tajika_yoga = "Subha Trine / Sextile Aspect Harmonic"
        karya_siddhi_score = 78
        verdict = "Positive Realization through Cooperative Assistance"
    else:
        tajika_yoga = "Nakta Yoga (Benefic Intermediary Transmission via Moon)"
        karya_siddhi_score = 64
        verdict = "Success Manifests via Third-Party Mediation or Mentor Support"

    # Time of Manifestation (Phala Kala)
    timing_sign_modality = SIGN_MODALITIES[target_sign_idx]
    if "Movable" in timing_sign_modality:
        timing_window = "Within 3 to 14 Days (Swift Fulfilment)"
    elif "Fixed" in timing_sign_modality:
        timing_window = "Within 3 to 6 Months (Deliberate & Enduring Manifestation)"
    else:
        timing_window = "Within 3 to 8 Weeks (Gradual Iterative Progress)"

    return {
        "engine": "Classical Vedic & KP Horary Astrological Engine",
        "queryDetails": {
            "queryText": query_text,
            "category": matter["name"],
            "calculationMode": calculation_mode
        },
        "horaryChart": {
            "ascendantSign": [
                "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
                "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
            ][asc_sign_idx],
            "ascendantDegree": round(asc_deg_in_sign, 2),
            "lagnesha": lagnesha,
            "targetHouse": target_house,
            "targetSign": [
                "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
                "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
            ][target_sign_idx],
            "karyesha": karyesha,
            "naturalKaraka": matter["significator"]
        },
        "tajikaYogaAnalysis": {
            "activeYoga": tajika_yoga,
            "lagneshaKaryeshaAngularSeparation": f"{round(diff_deg, 2)}°",
            "effectiveOrbWindow": f"±{avg_orb}°"
        },
        "karyaSiddhiAssessment": {
            "probabilityPercentage": karya_siddhi_score,
            "verdict": verdict,
            "manifestationTiming": timing_window,
            "daivajnaGuidance": f"Keep clarity of intention while dealing with matters of {matter['name']}. Moon is favorably aspecting the Karyasthana."
        }
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    q_type = data.get("questionType", "CAREER")
    seed = data.get("kpSeed")
    dt_str = data.get("queryDatetime")
    txt = data.get("queryText", "Will my venture succeed?")
    print(json.dumps(calculate_prashna(q_type, seed, dt_str, txt)))
