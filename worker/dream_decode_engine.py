#!/usr/bin/env python3
"""
dream_decode_engine.py — Astro-Swapna Shastra & Dream Decoding Engine

Decodes user dreams using classical Vedic Swapna Shastra, Swiss Ephemeris lunar timing,
and Jungian archetypal psychology:
 1. Analyzes dream text for classical Vedic omens (Serpents, Sacred Waters, Temples, Flight, Gold).
 2. Evaluates the Lunar Tithi, Nakshatra, and Moon dignity for the dream date.
 3. Projects the Manifestation Window based on the Prahar (quarter of night).
 4. Delivers an Auspiciousness Rating (0–100%) and prescribed Vedic Swapna Mantras.
"""

from datetime import datetime
import json
import re
import swisseph as swe

SWAPNA_ARCHETYPES = {
    "serpent": {
        "keywords": ["snake", "serpent", "cobra", "naga", "python", "viper"],
        "planet": "Rahu & Ketu (Kundalini Shakti)",
        "meaning": "Awakening of dormant psychic potential, sudden unexpected transformation, secret knowledge revelation, or spiritual protection.",
        "nature": "Highly Auspicious if calm or golden; Cautionary if aggressive."
    },
    "water": {
        "keywords": ["river", "ocean", "sea", "lake", "water", "swimming", "rain", "fountain"],
        "planet": "Moon (Chandra) & Varuna",
        "meaning": "Purification of the emotional body, subconscious psychic reset, dissolution of past karmic grime, and high intuitive flow.",
        "nature": "Auspicious. Clear water indicates pure spiritual blessing."
    },
    "gold_and_temple": {
        "keywords": ["gold", "temple", "deity", "idol", "altar", "crown", "palace", "treasure"],
        "planet": "Jupiter (Guru) & Sun (Surya)",
        "meaning": "Royal favor, major financial expansion, divine grace (*Ishvara Kripa*), and lasting elevation of worldly status.",
        "nature": "Superlative Golden Omen (Param Shubh)."
    },
    "flight": {
        "keywords": ["fly", "flying", "floating", "sky", "clouds", "wings", "soaring"],
        "planet": "Mercury (Budh) & Ketu",
        "meaning": "Transcending worldly constraints, intellectual liberation, elevated perspective over complex problems, and rapid career acceleration.",
        "nature": "Very Favorable."
    },
    "mountain": {
        "keywords": ["mountain", "cliff", "hill", "peak", "summit", "rock"],
        "planet": "Saturn (Shani) & Shiva",
        "meaning": "Ascending to unshakeable worldly authority through disciplined endurance; conquering a challenging long-term milestone.",
        "nature": "Auspicious milestone indicator."
    },
    "fire_or_light": {
        "keywords": ["fire", "flame", "sun", "lamp", "torch", "light", "lightning"],
        "planet": "Mars (Mangal) & Agni",
        "meaning": "Purification of internal drive, incineration of obstacles, dynamic motivation, and righteous victory over competition.",
        "nature": "Dynamic & Energizing."
    }
}


def decode_dream_swapna_shastra(dream_text, dream_date_str=None, prahar="brahma_muhurta"):
    """
    Decodes dreams using Vedic Swapna Shastra and Swiss Ephemeris.
    """
    if not dream_date_str:
        dt = datetime.now()
    else:
        try:
            dt = datetime.strptime(dream_date_str, "%Y-%m-%d")
        except Exception:
            dt = datetime.now()

    # 1. Ephemeris Lunar Calculation
    jd = swe.julday(dt.year, dt.month, dt.day, 4.0)  # 4:00 AM standard dream time
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    m_res, _ = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)
    s_res, _ = swe.calc_ut(jd, swe.SUN, swe.FLG_SIDEREAL)

    moon_lon = m_res[0]
    sun_lon = s_res[0]
    nak_idx = int(moon_lon / (360.0 / 27.0)) % 27
    tithi_num = int(((moon_lon - sun_lon + 360.0) % 360.0) / 12.0) + 1

    nakshatras = [
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
        "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
        "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
        "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha", "Shatabhisha",
        "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
    ]
    current_nakshatra = nakshatras[nak_idx]

    # 2. Timing Manifestation Window based on Prahar
    prahar_map = {
        "brahma_muhurta": {
            "timeWindow": "03:30 AM – 06:00 AM (4th Prahar)",
            "manifestationTimeline": "Rapid Manifestation: 10 to 30 Days",
            "accuracyMultiplier": 0.95
        },
        "midnight": {
            "timeWindow": "12:00 AM – 03:00 AM (3rd Prahar)",
            "manifestationTimeline": "Medium Term: 1 to 3 Months",
            "accuracyMultiplier": 0.85
        },
        "early_night": {
            "timeWindow": "09:00 PM – 12:00 AM (1st & 2nd Prahar)",
            "manifestationTimeline": "Long Range: 6 Months to 1 Year",
            "accuracyMultiplier": 0.70
        }
    }
    prahar_info = prahar_map.get(prahar.lower(), prahar_map["brahma_muhurta"])

    # 3. Text NLP & Archetype Extraction
    clean_text = dream_text.lower()
    matched_archetypes = []
    base_auspiciousness = 75

    for key, data in SWAPNA_ARCHETYPES.items():
        for kw in data["keywords"]:
            if re.search(r'\b' + re.escape(kw) + r'\b', clean_text):
                matched_archetypes.append({
                    "symbol": key.replace("_", " ").title(),
                    "detectedKeyword": kw,
                    "planetaryRuler": data["planet"],
                    "esotericMeaning": data["meaning"],
                    "omenNature": data["nature"]
                })
                base_auspiciousness += 5
                break

    # If no specific keyword matched, provide universal archetypal reading
    if not matched_archetypes:
        matched_archetypes.append({
            "symbol": "Cosmic Transformation & Journey",
            "detectedKeyword": "General Dreamscape",
            "planetaryRuler": "Moon & Ketu",
            "esotericMeaning": "Internal realignment of subconscious memory patterns; processing unresolved karmic impressions (*Samskaras*).",
            "omenNature": "Favorable Psychological Cleansing"
        })

    auspiciousness_score = min(98, max(60, base_auspiciousness))

    # Classical Protective / Activating Mantra
    mantra = "Om Namo Bhagavate Vasudevaya (Chant 11 times upon waking to anchor auspicious visions)"

    return {
        "dreamSummary": dream_text[:140] + ("..." if len(dream_text) > 140 else ""),
        "astrologicalTiming": {
            "dreamDate": dt.strftime("%Y-%m-%d"),
            "lunarTithi": f"Tithi {tithi_num} ({'Shukla Paksha' if tithi_num <= 15 else 'Krishna Paksha'})",
            "moonNakshatra": current_nakshatra,
            "praharWindow": prahar_info["timeWindow"],
            "projectedManifestation": prahar_info["manifestationTimeline"]
        },
        "auspiciousnessIndex": auspiciousness_score,
        "omenGrade": "Golden Omen (Parama Shubh)" if auspiciousness_score >= 85 else "Auspicious Transformation",
        "detectedArchetypes": matched_archetypes,
        "swapnaInterpretation": (
            f"According to classical Swapna Shastra, dreams occurring under {current_nakshatra} Nakshatra "
            f"during {prahar_info['timeWindow']} carry extraordinary predictive weight. The symbols in your dream "
            f"signal an impending cycle of rapid expansion and internal breakthrough."
        ),
        "sacredRemedyOrMantra": mantra
    }


if __name__ == "__main__":
    test_dream = "I saw a golden cobra swimming in clear ocean water under bright sunlight."
    res = decode_dream_swapna_shastra(test_dream, "2026-09-19", "brahma_muhurta")
    print("Dream Decoding Engine Test:")
    print("  Tithi:", res["astrologicalTiming"]["lunarTithi"])
    print("  Nakshatra:", res["astrologicalTiming"]["moonNakshatra"])
    print("  Manifestation:", res["astrologicalTiming"]["projectedManifestation"])
    print("  Auspiciousness:", res["auspiciousnessIndex"])
    print("  Matched Archetypes:", len(res["detectedArchetypes"]))
