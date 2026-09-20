#!/usr/bin/env python3
"""
namakaran_engine.py — Vedic Baby Name & Nama-Karan Generator

Calculates exact Moon Nakshatra & Pada and determines the 4 auspicious
starting phonetics/syllables (Aksharas) according to classical Jyotish.
Generates a curated catalog of modern, meaningful names starting with those syllables.
"""

from datetime import datetime
import swisseph as swe

# 27 Nakshatras with their 4 Pada starting syllables (Brihat Jataka & Muhurta Chintamani)
NAKSHATRA_SYLLABLES = [
    {"name": "Ashwini", "padas": ["Chu", "Che", "Cho", "La"]},
    {"name": "Bharani", "padas": ["Lee", "Lu", "Le", "Lo"]},
    {"name": "Krittika", "padas": ["A", "Ee", "U", "Ea"]},
    {"name": "Rohini", "padas": ["O", "Va", "Vi", "Vu"]},
    {"name": "Mrigashira", "padas": ["Ve", "Vo", "Ka", "Kee"]},
    {"name": "Ardra", "padas": ["Ku", "Gha", "Ng", "Chha"]},
    {"name": "Punarvasu", "padas": ["Ke", "Ko", "Ha", "Hee"]},
    {"name": "Pushya", "padas": ["Hu", "He", "Ho", "Da"]},
    {"name": "Ashlesha", "padas": ["Dee", "Du", "De", "Do"]},
    {"name": "Magha", "padas": ["Ma", "Mee", "Mu", "Me"]},
    {"name": "Purva Phalguni", "padas": ["Mo", "Ta", "Tee", "Too"]},
    {"name": "Uttara Phalguni", "padas": ["Te", "To", "Paa", "Pee"]},
    {"name": "Hasta", "padas": ["Pu", "Sha", "Na", "Tha"]},
    {"name": "Chitra", "padas": ["Pe", "Po", "Ra", "Ree"]},
    {"name": "Swati", "padas": ["Ru", "Re", "Ro", "Taa"]},
    {"name": "Vishakha", "padas": ["Tee", "Tue", "Tey", "Too"]},
    {"name": "Anuradha", "padas": ["Na", "Nee", "Nu", "Ne"]},
    {"name": "Jyeshtha", "padas": ["No", "Ya", "Yee", "Yu"]},
    {"name": "Mula", "padas": ["Ye", "Yo", "Bha", "Bhee"]},
    {"name": "Purva Ashadha", "padas": ["Bhoo", "Dha", "Pha", "Dhad"]},
    {"name": "Uttara Ashadha", "padas": ["Bhe", "Bho", "Ja", "Jee"]},
    {"name": "Shravana", "padas": ["Khee", "Khoo", "Khe", "Kho"]},
    {"name": "Dhanishtha", "padas": ["Gaa", "Gee", "Gu", "Ge"]},
    {"name": "Shatabhisha", "padas": ["Go", "Saa", "See", "Su"]},
    {"name": "Purva Bhadrapada", "padas": ["Se", "So", "Daa", "Dee"]},
    {"name": "Uttara Bhadrapada", "padas": ["Du", "Shyam", "Jha", "Jna"]},
    {"name": "Revati", "padas": ["De", "Do", "Chaa", "Chee"]}
]

# Sample curated baby name repository mapped by initial syllables
NAME_DATABASE = {
    "A": [
        {"name": "Aarav", "gender": "Boy", "meaning": "Peaceful, musical sound"},
        {"name": "Advait", "gender": "Boy", "meaning": "Unique, non-dual, free from duality"},
        {"name": "Ananya", "gender": "Girl", "meaning": "Matchless, unique, graceful"},
        {"name": "Arya", "gender": "Unisex", "meaning": "Noble, honorable, Goddess Durga"}
    ],
    "Chu": [
        {"name": "Chunmay", "gender": "Boy", "meaning": "Supreme consciousness, wise"},
        {"name": "Chulbuli", "gender": "Girl", "meaning": "Playful, charming"}
    ],
    "Che": [
        {"name": "Chetan", "gender": "Boy", "meaning": "Consciousness, vitality, soul"},
        {"name": "Chetana", "gender": "Girl", "meaning": "Awakened awareness, intelligent"}
    ],
    "Cho": [
        {"name": "Choksha", "gender": "Boy", "meaning": "Pure, radiant, honest"},
        {"name": "Chokshi", "gender": "Girl", "meaning": "Pure and immaculate"}
    ],
    "La": [
        {"name": "Laksh", "gender": "Boy", "meaning": "Destination, target, aim"},
        {"name": "Lavanya", "gender": "Girl", "meaning": "Grace, beauty, charm"}
    ],
    "Va": [
        {"name": "Varun", "gender": "Boy", "meaning": "Lord of cosmic waters and truth"},
        {"name": "Vanya", "gender": "Girl", "meaning": "Gracious gift of God, forest deity"}
    ],
    "Vi": [
        {"name": "Vivaan", "gender": "Boy", "meaning": "Full of life, rays of the morning sun"},
        {"name": "Vidhi", "gender": "Girl", "meaning": "Destiny, sacred order, Goddess Saraswati"}
    ],
    "Ra": [
        {"name": "Reyansh", "gender": "Boy", "meaning": "Ray of celestial light, Lord Vishnu"},
        {"name": "Radhika", "gender": "Girl", "meaning": "Prosperous, beloved of Krishna"}
    ],
    "Ree": [
        {"name": "Riaan", "gender": "Boy", "meaning": "Little king, royal grace"},
        {"name": "Riya", "gender": "Girl", "meaning": "Graceful singer, Goddess Lakshmi"}
    ],
    "Pe": [
        {"name": "Peyush", "gender": "Boy", "meaning": "Celestial nectar, sweet elixir"},
        {"name": "Perina", "gender": "Girl", "meaning": "Fairy-like, delicate and noble"}
    ],
    "Po": [
        {"name": "Poochan", "gender": "Boy", "meaning": "Nourisher, Sun God"},
        {"name": "Pooja", "gender": "Girl", "meaning": "Sacred worship, reverent offering"}
    ]
}


def compute_namakaran(dob_str, time_str, gender="unspecified"):
    """
    Computes Nakshatra & Pada and returns auspicious syllables & name suggestions.
    """
    dt = datetime.strptime(f"{dob_str} {time_str}", "%Y-%m-%d %H:%M")
    utc_hours = (dt.hour - 5) + (dt.minute - 30) / 60.0
    jd = swe.julday(dt.year, dt.month, dt.day, utc_hours)

    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    res, flags = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)
    moon_lon = res[0]

    # Nakshatra calculation
    nak_span = 360.0 / 27.0  # 13° 20'
    pada_span = nak_span / 4.0  # 3° 20'

    nak_idx = int(moon_lon / nak_span) % 27
    deg_in_nak = moon_lon % nak_span
    pada = int(deg_in_nak / pada_span) + 1

    nak_data = NAKSHATRA_SYLLABLES[nak_idx]
    nak_name = nak_data["name"]
    primary_syllable = nak_data["padas"][pada - 1]
    all_padas_syllables = nak_data["padas"]

    # Gather matching names
    suggested = []
    # Primary pada syllable
    if primary_syllable in NAME_DATABASE:
        suggested.extend(NAME_DATABASE[primary_syllable])
    # Fallback to general nakshatra syllables if needed
    for syl in all_padas_syllables:
        if syl in NAME_DATABASE and syl != primary_syllable:
            suggested.extend(NAME_DATABASE[syl])

    # Fallback generic modern names if specific syllable is rare
    if len(suggested) < 3:
        suggested.extend([
            {"name": f"{primary_syllable}arav", "gender": "Boy", "meaning": f"Harmonious start with {primary_syllable}"},
            {"name": f"{primary_syllable}anya", "gender": "Girl", "meaning": f"Graceful melody of {primary_syllable}"},
            {"name": f"{primary_syllable}eesh", "gender": "Boy", "meaning": f"Lord of {nak_name}"},
            {"name": f"{primary_syllable}ita", "gender": "Girl", "meaning": f"Beloved of {nak_name}"}
        ])

    return {
        "birthDateTime": f"{dob_str} {time_str}",
        "moonLongitude": round(moon_lon, 3),
        "nakshatra": nak_name,
        "pada": pada,
        "sacredStartingSyllable": primary_syllable,
        "allPadaSyllables": {
            "pada1": all_padas_syllables[0],
            "pada2": all_padas_syllables[1],
            "pada3": all_padas_syllables[2],
            "pada4": all_padas_syllables[3],
        },
        "nameRecommendations": suggested,
        "astrologicalSignificance": f"Naming a child with '{primary_syllable}' activates the beneficial lunar vibration of {nak_name} Nakshatra Pada {pada}, promoting health, memory retention, and social harmony."
    }


if __name__ == "__main__":
    res = compute_namakaran("1989-10-30", "10:10", "Boy")
    print("Namakaran Result:")
    print("  Nakshatra:", res["nakshatra"], "Pada:", res["pada"])
    print("  Primary Syllable:", res["sacredStartingSyllable"])
    print("  Suggested Names:", len(res["nameRecommendations"]))
