#!/usr/bin/env python3
"""
jaimini_engine.py — Jaimini Chara Dasha & Karakamsha Soul Blueprint Engine

Implements classical Maharishi Jaimini Upadesha Sutras:
1. 7/8 Chara Karakas (Atmakaraka AK, Amatyakaraka AmK, Bhratri BK, Matri MK, Putra PK, Gnati GK, Dara DK)
2. Karakamsha & Swamsha Soul Blueprint (Navamsha placement of Atmakaraka)
3. 12th from Karakamsha: Ishta Devata (Deity of Liberation & Higher Calling)
4. Arudha Padas: Arudha Lagna (AL - Maya/Perception), Upapada (UL - Marriage), Rajya Pada (A10 - Status)
5. Classical Sign-Based Chara Dasha Sequence & Dual Direction (Direct/Reverse Progression)
"""

import math
from datetime import datetime, timedelta

SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

SIGN_LORDS = {
    "Aries": "Mars", "Taurus": "Venus", "Gemini": "Mercury", "Cancer": "Moon",
    "Leo": "Sun", "Virgo": "Mercury", "Libra": "Venus", "Scorpio": "Mars",
    "Sagittarius": "Jupiter", "Capricorn": "Saturn", "Aquarius": "Saturn", "Pisces": "Jupiter"
}

# Jaimini direct vs reverse signs for Dasha order
# Aries, Taurus, Gemini, Libra, Scorpio, Sagittarius are forward; others reverse
DIRECT_SIGNS = {"Aries", "Taurus", "Gemini", "Libra", "Scorpio", "Sagittarius"}

ISHTA_DEITIES = {
    "Sun": "Shiva / Rama",
    "Moon": "Gauri / Krishna",
    "Mars": "Kartikeya / Hanuman",
    "Mercury": "Maha Vishnu / Narayana",
    "Jupiter": "Samba Sadashiva / Guru",
    "Venus": "Maha Lakshmi / Annapurna",
    "Saturn": "Bhairava / Kurma",
    "Rahu": "Maa Durga / Varaha",
    "Ketu": "Lord Ganesha / Matsya"
}

def calculate_jaimini_details(natal_data=None):
    # Default planetary longitudes within signs for reference
    # Degrees in sign (0° to 30°)
    planet_degrees = {
        "Sun": 23.4,
        "Moon": 15.2,
        "Mars": 28.7,
        "Mercury": 18.9,
        "Jupiter": 12.3,
        "Venus": 27.6,
        "Saturn": 8.5
    }
    planet_signs = {
        "Sun": "Sagittarius",
        "Moon": "Taurus",
        "Mars": "Taurus",
        "Mercury": "Sagittarius",
        "Jupiter": "Leo",
        "Venus": "Capricorn",
        "Saturn": "Capricorn"
    }
    lagna_sign = "Sagittarius"

    if natal_data and isinstance(natal_data, dict):
        vedic = natal_data.get("vedic", {})
        planets = vedic.get("planets", {})
        asc = vedic.get("ascendant", {})
        if asc and "sign" in asc:
            lagna_sign = asc["sign"]
        for p in ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]:
            if p in planets:
                p_info = planets[p]
                if "degreeInSign" in p_info:
                    planet_degrees[p] = float(p_info["degreeInSign"])
                elif "longitude" in p_info:
                    planet_degrees[p] = round(float(p_info["longitude"]) % 30.0, 2)
                if "sign" in p_info:
                    planet_signs[p] = p_info["sign"]

    # 1. Determine 7 Chara Karakas by descending degree order
    sorted_by_degree = sorted(planet_degrees.items(), key=lambda x: x[1], reverse=True)
    karaka_titles = ["Atmakaraka (AK)", "Amatyakaraka (AmK)", "Bhratrikaraka (BK)", 
                     "Matrikaraka (MK)", "Putrakaraka (PK)", "Gnatikaraka (GK)", "Darakaraka (DK)"]

    seven_karakas = {}
    for i, (p_name, deg) in enumerate(sorted_by_degree[:7]):
        title = karaka_titles[i]
        seven_karakas[title] = {
            "planet": p_name,
            "degreeInSign": f"{deg}°",
            "sign": planet_signs[p_name]
        }

    atmakaraka_planet = sorted_by_degree[0][0]
    amatyakaraka_planet = sorted_by_degree[1][0]
    darakaraka_planet = sorted_by_degree[6][0]

    # 2. Karakamsha (D9 Navamsha sign of Atmakaraka)
    ak_sign = planet_signs[atmakaraka_planet]
    ak_deg = planet_degrees[atmakaraka_planet]
    navamsha_pada = int(ak_deg / (30.0 / 9.0)) # 0 to 8
    sign_idx = SIGNS.index(ak_sign)
    # Fire signs start from Aries, Earth from Capricorn, Air from Libra, Water from Cancer
    element_starts = {0: 0, 1: 9, 2: 6, 3: 3}
    start_idx = element_starts[sign_idx % 4]
    karakamsha_sign_idx = (start_idx + navamsha_pada) % 12
    karakamsha_sign = SIGNS[karakamsha_sign_idx]

    # 3. 12th from Karakamsha: Ishta Devata
    ishta_house_idx = (karakamsha_sign_idx + 11) % 12 # 12th house (0-indexed +11)
    ishta_sign = SIGNS[ishta_house_idx]
    ishta_lord = SIGN_LORDS[ishta_sign]
    ishta_deity = ISHTA_DEITIES.get(ishta_lord, "Maha Vishnu")

    # 4. Arudha Padas (AL, UL, A10, A11)
    lagna_idx = SIGNS.index(lagna_sign) if lagna_sign in SIGNS else 8
    lagna_lord = SIGN_LORDS[SIGNS[lagna_idx]]
    lord_sign = planet_signs.get(lagna_lord, "Leo")
    lord_idx = SIGNS.index(lord_sign)
    dist_l = (lord_idx - lagna_idx) % 12
    al_idx = (lord_idx + dist_l) % 12
    # Jaimini exception: if AL lands in 1st or 7th from house, move 10th
    if al_idx in [lagna_idx, (lagna_idx + 6) % 12]:
        al_idx = (al_idx + 9) % 12
    al_sign = SIGNS[al_idx]

    # Upapada Lagna (UL) - from 12th house
    h12_idx = (lagna_idx + 11) % 12
    h12_lord = SIGN_LORDS[SIGNS[h12_idx]]
    h12_lord_sign = planet_signs.get(h12_lord, "Scorpio")
    h12_lord_idx = SIGNS.index(h12_lord_sign)
    dist_12 = (h12_lord_idx - h12_idx) % 12
    ul_idx = (h12_lord_idx + dist_12) % 12
    if ul_idx in [h12_idx, (h12_idx + 6) % 12]:
        ul_idx = (ul_idx + 9) % 12
    ul_sign = SIGNS[ul_idx]

    # Rajya Pada (A10)
    h10_idx = (lagna_idx + 9) % 12
    h10_lord = SIGN_LORDS[SIGNS[h10_idx]]
    h10_lord_sign = planet_signs.get(h10_lord, "Taurus")
    h10_lord_idx = SIGNS.index(h10_lord_sign)
    dist_10 = (h10_lord_idx - h10_idx) % 12
    a10_idx = (h10_lord_idx + dist_10) % 12
    a10_sign = SIGNS[a10_idx]

    # 5. Jaimini Chara Dasha Timeline (12 Signs starting from Lagna)
    is_direct = lagna_sign in DIRECT_SIGNS
    chara_dasha = []
    current_year = 1990

    for step in range(12):
        s_idx = (lagna_idx + step) % 12 if is_direct else (lagna_idx - step) % 12
        r_sign = SIGNS[s_idx]
        r_lord = SIGN_LORDS[r_sign]
        r_lord_sign = planet_signs.get(r_lord, "Aries")
        r_lord_idx = SIGNS.index(r_lord_sign)

        # Classical Jaimini duration: count to lord (minus 1), or 12 if lord in own sign
        if r_sign in DIRECT_SIGNS:
            duration = (r_lord_idx - s_idx) % 12
        else:
            duration = (s_idx - r_lord_idx) % 12
        if duration == 0: duration = 12

        chara_dasha.append({
            "sign": r_sign,
            "lord": r_lord,
            "durationYears": duration,
            "startYear": current_year,
            "endYear": current_year + duration,
            "period": f"{current_year} – {current_year + duration}"
        })
        current_year += duration

    return {
        "atmakaraka": {
            "planet": atmakaraka_planet,
            "degree": f"{planet_degrees[atmakaraka_planet]}°",
            "sign": planet_signs[atmakaraka_planet],
            "significance": "Soul King: Supreme significator of divine destiny, innate desire, and spiritual liberation."
        },
        "amatyakaraka": {
            "planet": amatyakaraka_planet,
            "significance": "Prime Minister: Key driver of intellect, professional career path, and worldly achievements."
        },
        "darakaraka": {
            "planet": darakaraka_planet,
            "significance": "Spouse & Union: Karmic relationship mirror and business partnership indicator."
        },
        "sevenKarakas": seven_karakas,
        "karakamsha": {
            "karakamshaSign": karakamsha_sign,
            "navamshaPada": navamsha_pada + 1,
            "interpretation": f"Atmakaraka {atmakaraka_planet} in {karakamsha_sign} Navamsha reveals a soul attuned to wisdom, spiritual mentorship, and unshakeable inner rectitude."
        },
        "ishtaDevata": {
            "twelfthFromKarakamshaSign": ishta_sign,
            "twelfthLord": ishta_lord,
            "recommendedDeity": ishta_deity,
            "liberationGuidance": f"Devotion to {ishta_deity} purifies ancestral residue and accelerates spiritual moksha."
        },
        "arudhaPadas": {
            "arudhaLagna_AL": {"sign": al_sign, "theme": "Public Image, Societal Status & Material Aura"},
            "upapadaLagna_UL": {"sign": ul_sign, "theme": "Spousal Longevity, Marital Sanctity & Domestic Harmony"},
            "rajyaPada_A10": {"sign": a10_sign, "theme": "Professional Power, Hierarchy & Executive Recognition"}
        },
        "charaDashaTimeline": chara_dasha,
        "progressionDirection": "Direct (Zodiacal)" if is_direct else "Reverse (Anti-Zodiacal)"
    }

if __name__ == "__main__":
    import json
    res = calculate_jaimini_details()
    print(json.dumps(res, indent=2))
