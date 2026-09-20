#!/usr/bin/env python3
"""
lo_shu_engine.py — Classical Astro-Numerology & Lo Shu Magic Square Engine

Integrates ancient Vedic Graha Numerology with the 3x3 Lo Shu Magic Square:
    4 (Rahu)    | 9 (Mars)    | 2 (Moon)
    3 (Jupiter) | 5 (Mercury) | 7 (Ketu)
    8 (Saturn)  | 1 (Sun)     | 6 (Venus)

1. Core Numbers:
   - Mulank (Driver / Psychic Number): Day of Birth
   - Bhagyank (Conductor / Destiny Number): Total Date Sum (D+M+Y)
   - Kua Number (Feng Shui Energetic Direction)
2. Lo Shu Grid Digit Mapping (Frequency Counts 1-9)
3. 8 Planes & Arrows of Strength / Weakness:
   - Mental Plane (4-9-2), Emotional/Soul Plane (3-5-7), Practical Plane (8-1-6)
   - Thought (4-3-8), Willpower (9-5-1), Action (2-7-6)
   - Golden Arrow of Fortune (4-5-6), Earth/Prosperity Arrow (2-5-8)
4. Missing Number Cures & Harmonization Remedies
"""

from collections import Counter

GRAHA_NUMBERS = {
    1: {"graha": "Sun", "quality": "Leadership, Independence, Individual Will", "element": "Water / Career"},
    2: {"graha": "Moon", "quality": "Emotional Sensitivity, Intuition, Partnership", "element": "Earth / Relationships"},
    3: {"graha": "Jupiter", "quality": "Wisdom, Intellect, Academic Mastery", "element": "Wood / Growth"},
    4: {"graha": "Rahu", "quality": "Practical Planning, Structure, Logic", "element": "Wood / Wealth"},
    5: {"graha": "Mercury", "quality": "Balance, Communication, Adaptability, Pivot", "element": "Earth / Core Center"},
    6: {"graha": "Venus", "quality": "Luxury, Harmony, Domestic Bliss, Family", "element": "Metal / Helpful Friends"},
    7: {"graha": "Ketu", "quality": "Spirituality, Philosophical Analysis, Research", "element": "Metal / Children"},
    8: {"graha": "Saturn", "quality": "Endurance, Organization, Karmic Labor", "element": "Earth / Knowledge"},
    9: {"graha": "Mars", "quality": "Courage, Passion, Ambition, Recognition", "element": "Fire / Fame"}
}

MISSING_REMEDIES = {
    1: "Drink water from copper vessel; offer Arghya to rising Sun daily.",
    2: "Wear silver ring or pearl; respect mother and water elements.",
    3: "Wear yellow/saffron; maintain healthy plants and honor teachers/elders.",
    4: "Wear a wooden wristband; organize workspaces with meticulous symmetry.",
    5: "Place a green bamboo plant or brass bell in the center of home/workspace.",
    6: "Apply natural sandalwood or rose fragrance; wear light pastel attire on Fridays.",
    7: "Meditate in solitude; feed stray dogs and practice analytical writing.",
    8: "Cultivate strict physical discipline; feed birds and avoid procrastination.",
    9: "Wear red thread on right wrist; exercise daily and pursue courageous goals."
}

def reduce_to_single_digit(n):
    while n > 9:
        n = sum(int(d) for d in str(n))
    return n

def calculate_lo_shu(dob_str="1990-12-15", gender="MALE"):
    clean_digits = [int(c) for c in dob_str if c.isdigit() and c != '0']

    # Date parsing
    parts = dob_str.split("-")
    day_val = int(parts[2]) if len(parts) >= 3 else 15
    month_val = int(parts[1]) if len(parts) >= 2 else 12
    year_val = int(parts[0]) if len(parts) >= 1 else 1990

    # 1. Mulank (Driver)
    mulank = reduce_to_single_digit(day_val)

    # 2. Bhagyank (Conductor)
    total_sum = sum(int(c) for c in f"{year_val:04d}{month_val:02d}{day_val:02d}")
    bhagyank = reduce_to_single_digit(total_sum)

    # 3. Kua Number
    year_sum = reduce_to_single_digit(sum(int(c) for c in str(year_val)))
    if gender.upper() == "FEMALE":
        kua = reduce_to_single_digit(year_sum + 4)
    else:
        kua = reduce_to_single_digit(11 - year_sum if (11 - year_sum) > 0 else 2)

    # All numbers to populate grid: DOB digits + Mulank + Bhagyank + Kua
    all_grid_digits = clean_digits + [mulank, bhagyank, kua]
    counts = Counter(all_grid_digits)

    # Grid mapping
    grid = {
        "topRow": {"4_Rahu": counts[4], "9_Mars": counts[9], "2_Moon": counts[2]},
        "middleRow": {"3_Jupiter": counts[3], "5_Mercury": counts[5], "7_Ketu": counts[7]},
        "bottomRow": {"8_Saturn": counts[8], "1_Sun": counts[1], "6_Venus": counts[6]}
    }

    # Evaluate Arrows of Strength
    arrows = []
    if counts[4] and counts[3] and counts[8]: arrows.append("Arrow of Thought (4-3-8) — Superior Strategic Vision")
    if counts[9] and counts[5] and counts[1]: arrows.append("Arrow of Willpower (9-5-1) — Iron Determination & Resilience")
    if counts[2] and counts[7] and counts[6]: arrows.append("Arrow of Action (2-7-6) — Relentless Pragmatic Execution")
    if counts[4] and counts[5] and counts[6]: arrows.append("Arrow of Golden Fortune (4-5-6) — Material Prosperity & Ease")
    if counts[2] and counts[5] and counts[8]: arrows.append("Arrow of Earth Stability (2-5-8) — Property, Real Estate & Solidity")
    if counts[3] and counts[5] and counts[7]: arrows.append("Arrow of Mystical Spirituality (3-5-7) — Deep Intuitive Perception")

    # Missing numbers
    missing = [num for num in range(1, 10) if counts[num] == 0]
    remedies = [{"number": m, "governingGraha": GRAHA_NUMBERS[m]["graha"], "remedy": MISSING_REMEDIES[m]} for m in missing]

    return {
        "engine": "Classical Astro-Numerology & Lo Shu Magic Square Engine",
        "coreVedicNumerology": {
            "birthDate": dob_str,
            "mulankDriverNumber": mulank,
            "mulankGraha": GRAHA_NUMBERS[mulank]["graha"],
            "bhagyankDestinyNumber": bhagyank,
            "bhagyankGraha": GRAHA_NUMBERS[bhagyank]["graha"],
            "kuaNumber": kua
        },
        "loShuGridFrequencies": grid,
        "activeArrowsOfStrength": arrows if arrows else ["Balanced Decentralized Distribution"],
        "missingNumbers": missing,
        "remedialCures": remedies
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    dob = data.get("dob", "1990-12-15")
    g = data.get("gender", "MALE")
    print(json.dumps(calculate_lo_shu(dob, g)))
