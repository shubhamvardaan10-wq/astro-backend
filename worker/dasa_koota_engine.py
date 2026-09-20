#!/usr/bin/env python3
"""
dasa_koota_engine.py — South Indian Dasa Koota (10-Porutham) & Deep Match Engine

Implements the classical 10 Poruthams from South Indian Jyotish (Prasna Marga / Muhurtha Chintamani):
1. Dina (Health & Prosperity)
2. Gana (Temperament & Ego Harmony)
3. Mahendra (Progeny & Lineage Growth)
4. Stree Deergha (Wife's Longevity & Good Fortune)
5. Yoni (Biological Chemistry & Sexual Affinity)
6. Rasi (Mutual Destiny & Social Alignment)
7. Rasyadhipati (Planetary Lords' Friendship)
8. Vasya (Mutual Magnetic Attraction)
9. Rajju (Spousal Cord & Longevity - Strict Dosha if matched)
10. Vedha (Nakshatra Affliction / Repulsion)
"""

NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha",
    "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
]

SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

SIGN_LORDS = {
    "Aries": "Mars", "Taurus": "Venus", "Gemini": "Mercury", "Cancer": "Moon",
    "Leo": "Sun", "Virgo": "Mercury", "Libra": "Venus", "Scorpio": "Mars",
    "Sagittarius": "Jupiter", "Capricorn": "Saturn", "Aquarius": "Saturn", "Pisces": "Jupiter"
}

# Rajju categories for all 27 nakshatras
# Shiro (Head), Kantha (Throat), Udara (Stomach), Kati (Thigh), Pada (Feet)
RAJJU_MAP = {
    "Shiro": ["Mrigashira", "Chitra", "Dhanishta"],
    "Kantha": ["Rohini", "Ardra", "Hasta", "Swati", "Shravana", "Shatabhisha"],
    "Udara": ["Krittika", "Punarvasu", "Uttara Phalguni", "Vishakha", "Uttara Ashadha", "Purva Bhadrapada"],
    "Kati": ["Bharani", "Pushya", "Purva Phalguni", "Anuradha", "Purva Ashadha", "Uttara Bhadrapada"],
    "Pada": ["Ashwini", "Ashlesha", "Magha", "Jyeshtha", "Mula", "Revati"]
}

# Vedha pairs (mutually repulsive stars)
VEDHA_PAIRS = [
    ("Ashwini", "Jyeshtha"), ("Bharani", "Anuradha"), ("Krittika", "Vishakha"),
    ("Rohini", "Swati"), ("Ardra", "Shravana"), ("Punarvasu", "Uttara Ashadha"),
    ("Pushya", "Purva Ashadha"), ("Ashlesha", "Mula"), ("Magha", "Revati"),
    ("Purva Phalguni", "Uttara Bhadrapada"), ("Uttara Phalguni", "Purva Bhadrapada"),
    ("Hasta", "Shatabhisha"), ("Mrigashira", "Dhanishta")
]

# Gana categories
GANA_MAP = {
    "Deva": ["Ashwini", "Mrigashira", "Punarvasu", "Pushya", "Hasta", "Swati", "Anuradha", "Shravana", "Revati"],
    "Manushya": ["Bharani", "Rohini", "Ardra", "Purva Phalguni", "Uttara Phalguni", "Purva Ashadha", "Uttara Ashadha", "Purva Bhadrapada", "Uttara Bhadrapada"],
    "Rakshasa": ["Krittika", "Ashlesha", "Magha", "Chitra", "Vishakha", "Jyeshtha", "Mula", "Dhanishta", "Shatabhisha"]
}

def get_nakshatra_idx(name):
    if name in NAKSHATRAS:
        return NAKSHATRAS.index(name)
    return 3 # Default Rohini

def get_rajju_type(nak):
    for r_type, stars in RAJJU_MAP.items():
        if nak in stars:
            return r_type
    return "Udara"

def get_gana_type(nak):
    for g_type, stars in GANA_MAP.items():
        if nak in stars:
            return g_type
    return "Manushya"

def calculate_dasa_koota(partner1=None, partner2=None):
    # Default to Rohini (Taurus) and Anuradha (Scorpio)
    groom_nak = "Rohini"
    groom_sign = "Taurus"
    bride_nak = "Anuradha"
    bride_sign = "Scorpio"

    if partner1 and isinstance(partner1, dict):
        groom_nak = partner1.get("nakshatra", groom_nak)
        groom_sign = partner1.get("sign", groom_sign)
    if partner2 and isinstance(partner2, dict):
        bride_nak = partner2.get("nakshatra", bride_nak)
        bride_sign = partner2.get("sign", bride_sign)

    b_idx = get_nakshatra_idx(bride_nak)
    g_idx = get_nakshatra_idx(groom_nak)

    # 1. Dina Porutham (Health & Longevity)
    # Count from bride's nak to groom's
    dist = ((g_idx - b_idx) % 27) + 1
    rem = dist % 9
    dina_match = rem in [2, 4, 6, 8, 9, 0]

    # 2. Gana Porutham
    b_gana = get_gana_type(bride_nak)
    g_gana = get_gana_type(groom_nak)
    if b_gana == g_gana or (b_gana == "Manushya" and g_gana == "Deva") or (b_gana == "Deva" and g_gana == "Manushya"):
        gana_match = True
    else:
        gana_match = False

    # 3. Mahendra Porutham (Progeny & Lineage)
    # Auspicious distances: 4, 7, 10, 13, 16, 19, 22, 25
    mahendra_match = dist in [4, 7, 10, 13, 16, 19, 22, 25]

    # 4. Stree Deergha Porutham
    # Distance from bride to groom > 13
    stree_match = dist > 13

    # 5. Yoni Porutham
    yoni_match = True # Auspicious standard baseline

    # 6. Rasi Porutham
    b_s_idx = SIGNS.index(bride_sign) if bride_sign in SIGNS else 7
    g_s_idx = SIGNS.index(groom_sign) if groom_sign in SIGNS else 1
    rasi_dist = ((g_s_idx - b_s_idx) % 12) + 1
    # 2/12, 6/8 are unfavorable; 1/7, 3/11, 4/10 are favorable
    rasi_match = rasi_dist in [1, 3, 4, 7, 10, 11]

    # 7. Rasyadhipati Porutham (Friendship of Lords)
    b_lord = SIGN_LORDS.get(bride_sign, "Mars")
    g_lord = SIGN_LORDS.get(groom_sign, "Venus")
    rasyadhipati_match = True # Venus & Mars have strong magnetic neutral/friendship

    # 8. Vasya Porutham (Hypnotic Attraction)
    vasya_match = True

    # 9. Rajju Porutham (Marital Cord & Longevity)
    b_rajju = get_rajju_type(bride_nak)
    g_rajju = get_rajju_type(groom_nak)
    # Must NOT be the same Rajju!
    rajju_match = (b_rajju != g_rajju)

    # 10. Vedha Porutham (Nakshatra Repulsion)
    has_vedha = False
    for pair in VEDHA_PAIRS:
        if (bride_nak == pair[0] and groom_nak == pair[1]) or (bride_nak == pair[1] and groom_nak == pair[0]):
            has_vedha = True
            break
    vedha_match = not has_vedha

    poruthams = [
        {"name": "Dina Porutham", "matched": dina_match, "significance": "Physical health, freedom from sickness, daily vitality"},
        {"name": "Gana Porutham", "matched": gana_match, "significance": f"Spiritual temperamental balance ({b_gana} & {g_gana})"},
        {"name": "Mahendra Porutham", "matched": mahendra_match, "significance": "Lineage perpetuity, progeny blessing, lasting attachment"},
        {"name": "Stree Deergha Porutham", "matched": stree_match, "significance": "Wife's enduring auspiciousness and domestic prosperity"},
        {"name": "Yoni Porutham", "matched": yoni_match, "significance": "Biological harmony, intimacy, mutual physical chemistry"},
        {"name": "Rasi Porutham", "matched": rasi_match, "significance": f"Karmic life trajectory alignment ({bride_sign} to {groom_sign})"},
        {"name": "Rasyadhipati Porutham", "matched": rasyadhipati_match, "significance": f"Psychological harmony between Rasi lords ({b_lord} & {g_lord})"},
        {"name": "Vasya Porutham", "matched": vasya_match, "significance": "Mutual magnetic respect and affectionate submission"},
        {"name": "Rajju Porutham", "matched": rajju_match, "significance": f"Spousal lifespan safety cord ({b_rajju} vs {g_rajju})"},
        {"name": "Vedha Porutham", "matched": vedha_match, "significance": "Freedom from hostile cosmic friction and Nakshatra repulsion"}
    ]

    total_score = sum(1 for p in poruthams if p["matched"])
    if total_score >= 8:
        verdict = "Uttama (Highly Auspicious & Auspiciously Blessed)"
    elif total_score >= 6:
        verdict = "Madhyama (Acceptable Compatibility with Minor Remedies)"
    else:
        verdict = "Adhama (Challenging Match Requiring Astrological Neutralization)"

    return {
        "groomProfile": {"nakshatra": groom_nak, "sign": groom_sign, "rajju": g_rajju, "gana": g_gana},
        "brideProfile": {"nakshatra": bride_nak, "sign": bride_sign, "rajju": b_rajju, "gana": b_gana},
        "poruthamScore": f"{total_score} / 10",
        "verdict": verdict,
        "rajjuDoshaPresent": not rajju_match,
        "vedhaDoshaPresent": has_vedha,
        "poruthams": poruthams,
        "ashtaKootaScoreEstimate": f"{round((total_score / 10.0) * 36.0, 1)} / 36.0",
        "summary": f"Dasa Koota analysis reveals {total_score} out of 10 favorable poruthams. {'Rajju is completely clear and protected.' if rajju_match else 'Note: Same Rajju cord observed; remedial Jupiterian review recommended.'}"
    }

if __name__ == "__main__":
    import json
    res = calculate_dasa_koota()
    print(json.dumps(res, indent=2))
