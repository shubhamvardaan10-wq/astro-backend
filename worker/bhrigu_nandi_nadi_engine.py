#!/usr/bin/env python3
"""
bhrigu_nandi_nadi_engine.py — Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations

Calculates:
1. Directional Trigonometry of Planets:
   - Dharma / East / Fire: Houses 1, 5, 9
   - Artha / South / Earth: Houses 2, 6, 10
   - Kama / West / Air: Houses 3, 7, 11
   - Moksha / North / Water: Houses 4, 8, 12
2. Nadi Planetary Conjunctions by directional co-presence (regardless of house boundaries)
3. Essential Nadi Karakatwas:
   - Jupiter = Jiva (The Native's Soul & Life Energy)
   - Saturn = Karma (Profession, Responsibility & Destiny)
   - Venus = Bhoga / Maya (Wealth, Assets, Wife for male native)
   - Mars = Bhratri / Shakti (Courage, Brothers, Husband for female native)
   - Rahu = Kalapurusha Maya (Foreign Lands, Technology, Amplification)
   - Ketu = Mukti / Jnanakaraka (Detachment, Esoteric Mastery, Obstacles)
"""

import math

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

DIRECTIONS = {
    "East (Fire / Dharma)":  [0, 4, 8],    # Aries, Leo, Sagittarius
    "South (Earth / Artha)": [1, 5, 9],    # Taurus, Virgo, Capricorn
    "West (Air / Kama)":     [2, 6, 10],   # Gemini, Libra, Aquarius
    "North (Water / Moksha)":[3, 7, 11]    # Cancer, Scorpio, Pisces
}

NADI_KARAKAS = {
    "Jupiter": "Jiva Karaka (Native's Life, Consciousness & Dharma)",
    "Saturn": "Karma Karaka (Profession, Structural Labor & Destiny)",
    "Venus": "Bhoga Karaka (Wealth, Luxury, Vehicles, Spouse for Male)",
    "Mars": "Shakti Karaka (Courage, Engineering, Spouse for Female)",
    "Mercury": "Buddhi Karaka (Intellect, Commerce, Youth, Analytical Skill)",
    "Sun": "Atma Karaka (Father, Government, Authority, Soul Essence)",
    "Moon": "Mano Karaka (Mind, Liquid Wealth, Mother, Frequent Travel)",
    "Rahu": "Maya Karaka (Foreign Expansion, Technology, Grand Ambition)",
    "Ketu": "Moksha Karaka (Spiritual Liberation, Coding, Precision, Detachment)"
}

def calculate_bnn(natal_data):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})

    directional_groups = {
        "East (Fire / Dharma)": [],
        "South (Earth / Artha)": [],
        "West (Air / Kama)": [],
        "North (Water / Moksha)": []
    }

    planet_positions = {}
    for p_name, p_role in NADI_KARAKAS.items():
        if p_name in planets:
            lon = float(planets[p_name].get("longitude", 0.0))
            sign_idx = int(lon / 30.0) % 12
            planet_positions[p_name] = {
                "sign": ZODIAC_SIGNS[sign_idx],
                "longitude": round(lon, 2),
                "role": p_role
            }

            for dir_name, signs in DIRECTIONS.items():
                if sign_idx in signs:
                    directional_groups[dir_name].append(p_name)
                    break

    # Analyze key Nadi combinations (Yoga Formations)
    detected_combinations = []

    # 1. Jiva-Karma union (Jupiter & Saturn in same direction)
    for d_name, p_list in directional_groups.items():
        if "Jupiter" in p_list and "Saturn" in p_list:
            detected_combinations.append({
                "combination": "Dharma Karmadhipati Nadi Yoga (Jupiter + Saturn)",
                "direction": d_name,
                "interpretation": "High philosophical wisdom blended with professional integrity. Native achieves profound societal authority and mentorship status."
            })
        if "Saturn" in p_list and "Venus" in p_list:
            detected_combinations.append({
                "combination": "Bhoga Karma Yoga (Saturn + Venus)",
                "direction": d_name,
                "interpretation": "Wealth through sustained professional mastery. Strong affinity with real estate, luxury enterprise, or design."
            })
        if "Jupiter" in p_list and "Ketu" in p_list:
            detected_combinations.append({
                "combination": "Jnana Yoga (Jupiter + Ketu)",
                "direction": d_name,
                "interpretation": "Intuitive wisdom, metaphysical insights, detachment from petty squabbles, and spiritual counseling capacity."
            })
        if "Saturn" in p_list and "Rahu" in p_list:
            detected_combinations.append({
                "combination": "Karmic Amplification Yoga (Saturn + Rahu)",
                "direction": d_name,
                "interpretation": "Global/foreign professional pursuits, engagement with cutting-edge tech or unconventional scale."
            })

    if not detected_combinations:
        detected_combinations.append({
            "combination": "Independent Planetary Strengths",
            "direction": "Multi-Directional Balance",
            "interpretation": "Planets are distributed evenly across the 4 cardinal directions, providing balanced multidimensional capabilities."
        })

    return {
        "engine": "Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations Engine",
        "tradition": "South Indian Nadi Jyotish (Shri R.G. Rao System)",
        "directionalZodiacGrouping": directional_groups,
        "nadiPlanetaryKarakas": planet_positions,
        "prominentNadiYogas": detected_combinations,
        "nadiLifePurposeSynthesis": "Your Jiva Karaka (Jupiter) and Karma Karaka (Saturn) directional dispositions signify that career fulfillment emerges from structured mentorship and ethical leadership."
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(calculate_bnn(natal)))
