#!/usr/bin/env python3
"""
remedies_engine.py — Vedic Remedial Prescription Engine

Calculates:
1. Functional Benefics vs. Functional Malefics based on Lagna (Ascendant)
2. Prescribed Gemstones (Life Stone, Fortune Stone, Career Stone) with carats, metal, finger, day
3. Rudraksha Mukhi Recommendations
4. Authentic Vedic & Tantric Beej Mantras with repetition counts
5. Daan (Charity & Donations) for afflicted planets and dusthana pacification
"""

# Functional nature of planets for each of the 12 Ascendants (BPHS rules)
LAGNA_PLANETARY_NATURE = {
    "Aries": {
        "benefics": ["Jupiter", "Sun", "Mars"],
        "malefics": ["Mercury", "Venus", "Saturn"],
        "neutral": ["Moon"],
        "lifeStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"},
        "fortuneStone": {"planet": "Jupiter", "gem": "Yellow Sapphire (Pukhraj)", "carats": "4–6 Ratti", "metal": "Gold / Brass", "finger": "Index Finger", "day": "Thursday Morning"},
        "careerStone": {"planet": "Sun", "gem": "Ruby (Manikya)", "carats": "3–5 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Sunday Morning"}
    },
    "Taurus": {
        "benefics": ["Saturn", "Mercury", "Venus"],
        "malefics": ["Jupiter", "Moon", "Mars"],
        "neutral": ["Sun"],
        "lifeStone": {"planet": "Venus", "gem": "Diamond / White Sapphire", "carats": "1–2 Carats", "metal": "Platinum / Silver", "finger": "Middle / Little Finger", "day": "Friday Morning"},
        "fortuneStone": {"planet": "Saturn", "gem": "Blue Sapphire / Amethyst", "carats": "4–6 Ratti", "metal": "Silver / Panchadhatu", "finger": "Middle Finger", "day": "Saturday Evening"},
        "careerStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold / Silver", "finger": "Little Finger", "day": "Wednesday Morning"}
    },
    "Gemini": {
        "benefics": ["Venus", "Mercury"],
        "malefics": ["Mars", "Jupiter", "Sun"],
        "neutral": ["Saturn", "Moon"],
        "lifeStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold / Silver", "finger": "Little Finger", "day": "Wednesday Morning"},
        "fortuneStone": {"planet": "Saturn", "gem": "Blue Sapphire / Blue Topaz", "carats": "4–6 Ratti", "metal": "Silver / Panchadhatu", "finger": "Middle Finger", "day": "Saturday Evening"},
        "careerStone": {"planet": "Venus", "gem": "White Zircon / Opal", "carats": "5–7 Ratti", "metal": "Silver", "finger": "Ring / Little Finger", "day": "Friday Morning"}
    },
    "Cancer": {
        "benefics": ["Mars", "Jupiter", "Moon"],
        "malefics": ["Mercury", "Venus", "Saturn"],
        "neutral": ["Sun"],
        "lifeStone": {"planet": "Moon", "gem": "Natural Pearl (Moti)", "carats": "5–7 Ratti", "metal": "Silver", "finger": "Little Finger", "day": "Monday Evening"},
        "fortuneStone": {"planet": "Jupiter", "gem": "Yellow Sapphire (Pukhraj)", "carats": "4–6 Ratti", "metal": "Gold", "finger": "Index Finger", "day": "Thursday Morning"},
        "careerStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"}
    },
    "Leo": {
        "benefics": ["Mars", "Jupiter", "Sun"],
        "malefics": ["Mercury", "Venus", "Saturn"],
        "neutral": ["Moon"],
        "lifeStone": {"planet": "Sun", "gem": "Ruby (Manikya)", "carats": "3–5 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Sunday Morning"},
        "fortuneStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"},
        "careerStone": {"planet": "Jupiter", "gem": "Yellow Sapphire (Pukhraj)", "carats": "4–6 Ratti", "metal": "Gold", "finger": "Index Finger", "day": "Thursday Morning"}
    },
    "Virgo": {
        "benefics": ["Venus", "Mercury"],
        "malefics": ["Mars", "Jupiter", "Moon"],
        "neutral": ["Saturn", "Sun"],
        "lifeStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold / Silver", "finger": "Little Finger", "day": "Wednesday Morning"},
        "fortuneStone": {"planet": "Venus", "gem": "White Sapphire / Diamond", "carats": "1–2 Carats", "metal": "Platinum / Silver", "finger": "Middle / Little Finger", "day": "Friday Morning"},
        "careerStone": {"planet": "Saturn", "gem": "Blue Sapphire (Neelam)", "carats": "4–6 Ratti", "metal": "Silver", "finger": "Middle Finger", "day": "Saturday Evening"}
    },
    "Libra": {
        "benefics": ["Saturn", "Mercury", "Venus"],
        "malefics": ["Jupiter", "Sun", "Mars"],
        "neutral": ["Moon"],
        "lifeStone": {"planet": "Venus", "gem": "Diamond / Opal", "carats": "1–2 Carats (Diamond) or 6–8 Ratti (Opal)", "metal": "Silver / Platinum", "finger": "Middle / Ring Finger", "day": "Friday Morning"},
        "fortuneStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold / Silver", "finger": "Little Finger", "day": "Wednesday Morning"},
        "careerStone": {"planet": "Saturn", "gem": "Blue Sapphire (Neelam) / Amethyst", "carats": "4–6 Ratti", "metal": "Silver / Iron", "finger": "Middle Finger", "day": "Saturday Evening"}
    },
    "Scorpio": {
        "benefics": ["Jupiter", "Moon", "Sun"],
        "malefics": ["Mercury", "Venus", "Saturn"],
        "neutral": ["Mars"],
        "lifeStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"},
        "fortuneStone": {"planet": "Moon", "gem": "Natural Pearl (Moti)", "carats": "5–7 Ratti", "metal": "Silver", "finger": "Little Finger", "day": "Monday Evening"},
        "careerStone": {"planet": "Sun", "gem": "Ruby (Manikya)", "carats": "3–5 Ratti", "metal": "Gold", "finger": "Ring Finger", "day": "Sunday Morning"}
    },
    "Sagittarius": {
        "benefics": ["Mars", "Sun", "Jupiter"],
        "malefics": ["Venus", "Mercury", "Saturn"],
        "neutral": ["Moon"],
        "lifeStone": {"planet": "Jupiter", "gem": "Yellow Sapphire (Pukhraj)", "carats": "4–6 Ratti", "metal": "Gold / Panchadhatu", "finger": "Index Finger", "day": "Thursday Morning at Sunrise"},
        "fortuneStone": {"planet": "Sun", "gem": "Ruby (Manikya)", "carats": "3–5 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Sunday Morning at Sunrise"},
        "careerStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"}
    },
    "Capricorn": {
        "benefics": ["Venus", "Mercury", "Saturn"],
        "malefics": ["Mars", "Jupiter", "Moon"],
        "neutral": ["Sun"],
        "lifeStone": {"planet": "Saturn", "gem": "Blue Sapphire (Neelam) / Blue Topaz", "carats": "4–6 Ratti", "metal": "Silver / Iron", "finger": "Middle Finger", "day": "Saturday Evening"},
        "fortuneStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold / Silver", "finger": "Little Finger", "day": "Wednesday Morning"},
        "careerStone": {"planet": "Venus", "gem": "Diamond / Opal", "carats": "1–2 Carats", "metal": "Silver", "finger": "Middle / Ring Finger", "day": "Friday Morning"}
    },
    "Aquarius": {
        "benefics": ["Venus", "Saturn"],
        "malefics": ["Jupiter", "Moon", "Mars"],
        "neutral": ["Sun", "Mercury"],
        "lifeStone": {"planet": "Saturn", "gem": "Blue Sapphire (Neelam) / Iolite", "carats": "4–6 Ratti", "metal": "Silver", "finger": "Middle Finger", "day": "Saturday Evening"},
        "fortuneStone": {"planet": "Venus", "gem": "Diamond / White Zircon", "carats": "1–2 Carats", "metal": "Platinum / Silver", "finger": "Middle / Ring Finger", "day": "Friday Morning"},
        "careerStone": {"planet": "Mercury", "gem": "Emerald (Panna)", "carats": "4–6 Ratti", "metal": "Gold", "finger": "Little Finger", "day": "Wednesday Morning"}
    },
    "Pisces": {
        "benefics": ["Moon", "Mars", "Jupiter"],
        "malefics": ["Sun", "Venus", "Mercury"],
        "neutral": ["Saturn"],
        "lifeStone": {"planet": "Jupiter", "gem": "Yellow Sapphire (Pukhraj)", "carats": "4–6 Ratti", "metal": "Gold", "finger": "Index Finger", "day": "Thursday Morning"},
        "fortuneStone": {"planet": "Mars", "gem": "Red Coral (Moonga)", "carats": "6–8 Ratti", "metal": "Gold / Copper", "finger": "Ring Finger", "day": "Tuesday Morning"},
        "careerStone": {"planet": "Moon", "gem": "Natural Pearl (Moti)", "carats": "5–7 Ratti", "metal": "Silver", "finger": "Little Finger", "day": "Monday Evening"}
    }
}

# Authentic Vedic Beej Mantras and Chanting Protocols
BEEJ_MANTRAS = {
    "Sun":     {"mantra": "ॐ ह्रां ह्रीं ह्रौं सः सूर्याय नमः", "translit": "Om Hraam Hreem Hroum Sah Sooryaya Namah", "target": "7,000 chants", "day": "Sunday Morning"},
    "Moon":    {"mantra": "ॐ श्रां श्रीं श्रौं सः चन्द्रमसे नमः", "translit": "Om Shraam Shreem Shroum Sah Chandramase Namah", "target": "11,000 chants", "day": "Monday Evening"},
    "Mars":    {"mantra": "ॐ क्रां क्रीं क्रौं सः भौमाय नमः", "translit": "Om Kraam Kreem Kroum Sah Bhaumaya Namah", "target": "10,000 chants", "day": "Tuesday Morning"},
    "Mercury": {"mantra": "ॐ ब्रां ब्रीं प्रौं सः बुधाय नमः", "translit": "Om Braam Breem Broum Sah Budhaya Namah", "target": "9,000 chants", "day": "Wednesday Morning"},
    "Jupiter": {"mantra": "ॐ ग्रां ग्रीं ग्रौं सः गुरवे नमः", "translit": "Om Graam Greem Groum Sah Gurave Namah", "target": "19,000 chants", "day": "Thursday Morning"},
    "Venus":   {"mantra": "ॐ द्रां द्रीं द्रौं सः शुक्राय नमः", "translit": "Om Draam Dreem Droum Sah Shukraya Namah", "target": "16,000 chants", "day": "Friday Morning"},
    "Saturn":  {"mantra": "ॐ प्रां प्रीं प्रौं सः शनैश्चराय नमः", "translit": "Om Praam Preem Proum Sah Shanaishcharaya Namah", "target": "23,000 chants", "day": "Saturday Evening"},
    "Rahu":    {"mantra": "ॐ भ्रां भ्रीं भ्रौं सः राहवे नमः", "translit": "Om Bhraam Bhreem Bhroum Sah Rahave Namah", "target": "18,000 chants", "day": "Saturday Night"},
    "Ketu":    {"mantra": "ॐ स्रां स्रीं स्रौं सः केतवे नमः", "translit": "Om Sraam Sreem Sroum Sah Ketave Namah", "target": "17,000 chants", "day": "Tuesday Night"}
}

# Rudraksha Mukhi Association
RUDRAKSHA_GUIDE = {
    "Sun":     {"mukhi": "1 or 12 Mukhi", "benefits": "Leadership, health, vitality, self-confidence, heart health"},
    "Moon":    {"mukhi": "2 Mukhi", "benefits": "Emotional balance, peace of mind, family harmony, relieves anxiety"},
    "Mars":    {"mukhi": "3 Mukhi", "benefits": "Courage, overcoming lethargy, vitality, digestive strength"},
    "Mercury": {"mukhi": "4 Mukhi", "benefits": "Intellect, memory, communication, public speaking, nervous system"},
    "Jupiter": {"mukhi": "5 Mukhi", "benefits": "Wisdom, spiritual growth, liver health, optimism, divine grace"},
    "Venus":   {"mukhi": "6 Mukhi", "benefits": "Artistic grace, marital bliss, attraction, reproductive wellness"},
    "Saturn":  {"mukhi": "7 or 14 Mukhi", "benefits": "Overcoming obstacles, financial stability, discipline, bone health"},
    "Rahu":    {"mukhi": "8 Mukhi", "benefits": "Removes unseen blocks, protection from deception, sudden wealth"},
    "Ketu":    {"mukhi": "9 Mukhi", "benefits": "Spiritual liberation, courage, protection against fear, intuitive awakening"}
}

# Daan (Charity) Recommendations
DAAN_ITEMS = {
    "Sun":     {"items": "Wheat, copper utensils, jaggery (gur), red cloth, rubies", "day": "Sunday"},
    "Moon":    {"items": "Rice, milk, silver coins, white cloth, camphor, curd", "day": "Monday"},
    "Mars":    {"items": "Red lentils (masoor dal), copper, red flowers, sweet roti", "day": "Tuesday"},
    "Mercury": {"items": "Green moong dal, green vegetables, bronze utensils, stationery", "day": "Wednesday"},
    "Jupiter": {"items": "Chana dal, turmeric (haldi), yellow cloth, religious scriptures", "day": "Thursday"},
    "Venus":   {"items": "White silk cloth, mishri (sugar candy), perfume, silver, ghee", "day": "Friday"},
    "Saturn":  {"items": "Mustard oil, black sesame seeds (til), iron pan, black blanket", "day": "Saturday"},
    "Rahu":    {"items": "Urad dal, blue/black cloth, coconut, electric goods to technicians", "day": "Saturday"},
    "Ketu":    {"items": "Seven-grain mix (sapta dhanya), blanket, mustard seeds, feed street dogs", "day": "Tuesday/Saturday"}
}


def compute_remedies(natal_chart):
    """
    Generates personalized remedial prescriptions for the given natal chart.
    """
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign = asc.get("sign", "Sagittarius")

    rules = LAGNA_PLANETARY_NATURE.get(lagna_sign, LAGNA_PLANETARY_NATURE["Sagittarius"])

    # Gemstone Prescriptions
    gemstones = {
        "lifeStone": rules["lifeStone"],
        "fortuneStone": rules["fortuneStone"],
        "careerStone": rules["careerStone"],
        "caution": "Never wear gemstones of functional malefics without consulting a qualified Jyotishi. Gemstones act as amplifiers."
    }

    # Rudraksha Recommendations: prioritize Lagna lord + Atmakaraka / key benefic
    lagna_planet = rules["lifeStone"]["planet"]
    rudrakshas = [
        {"planet": lagna_planet, "role": "Primary Life Alignment", **RUDRAKSHA_GUIDE.get(lagna_planet, {})},
        {"planet": rules["fortuneStone"]["planet"], "role": "Fortune & Prosperity", **RUDRAKSHA_GUIDE.get(rules["fortuneStone"]["planet"], {})}
    ]

    # Mantras for Benefics (strengthening) and Malefics (pacification)
    mantra_prescriptions = []
    for b in rules["benefics"][:2]:
        mantra_prescriptions.append({
            "planet": b,
            "type": "Strength & Empowerment",
            **BEEJ_MANTRAS.get(b, {})
        })
    for m in rules["malefics"][:2]:
        mantra_prescriptions.append({
            "planet": m,
            "type": "Pacification & Peace",
            **BEEJ_MANTRAS.get(m, {})
        })

    # Daan (Charity) Recommendations for functional malefics
    daan_prescriptions = []
    for m in rules["malefics"][:3]:
        daan_prescriptions.append({
            "planet": m,
            "purpose": f"Mitigates {m}'s challenging influence for {lagna_sign} Lagna",
            **DAAN_ITEMS.get(m, {})
        })

    return {
        "ascendant": lagna_sign,
        "planetaryClassification": {
            "functionalBenefics": rules["benefics"],
            "functionalMalefics": rules["malefics"],
            "neutral": rules["neutral"]
        },
        "gemstones": gemstones,
        "rudraksha": rudrakshas,
        "mantras": mantra_prescriptions,
        "charityDaan": daan_prescriptions
    }


if __name__ == "__main__":
    mock_natal = {"vedic": {"ascendant": {"sign": "Sagittarius"}}}
    res = compute_remedies(mock_natal)
    print("Remedies for", res["ascendant"], ":")
    print("  Life Stone:", res["gemstones"]["lifeStone"]["gem"])
    print("  Fortune Stone:", res["gemstones"]["fortuneStone"]["gem"])
    print("  Benefics:", res["planetaryClassification"]["functionalBenefics"])
