#!/usr/bin/env python3
"""
numerology_engine.py — Numerology & Cosmic Name-Tuning Engine

Calculates:
1. Life Path Number (from Date of Birth)
2. Destiny / Expression Number (from Full Name via Pythagorean & Chaldean gematria)
3. Soul Urge Number (vowels) & Personality Number (consonants)
4. Name Vibration Tuning & Harmonic Compatibility Assessment
5. Lucky Numbers, Lucky Colors, Lucky Days, and Planetary Associations
"""

import re

# Chaldean Gematria Map
CHALDEAN_MAP = {
    'A': 1, 'I': 1, 'J': 1, 'Q': 1, 'Y': 1,
    'B': 2, 'K': 2, 'R': 2,
    'C': 3, 'G': 3, 'L': 3, 'S': 3,
    'D': 4, 'M': 4, 'T': 4,
    'E': 5, 'H': 5, 'N': 5, 'X': 5,
    'U': 6, 'V': 6, 'W': 6,
    'O': 7, 'Z': 7,
    'F': 8, 'P': 8
}

# Pythagorean Map
PYTHAGOREAN_MAP = {
    'A': 1, 'B': 2, 'C': 3, 'D': 4, 'E': 5, 'F': 6, 'G': 7, 'H': 8, 'I': 9,
    'J': 1, 'K': 2, 'L': 3, 'M': 4, 'N': 5, 'O': 6, 'P': 7, 'Q': 8, 'R': 9,
    'S': 1, 'T': 2, 'U': 3, 'V': 4, 'W': 5, 'X': 6, 'Y': 7, 'Z': 8
}

NUMBER_ATTRIBUTES = {
    1: {"planet": "Sun", "energy": "Leadership, Pioneer, Originality, Executive Authority", "colors": ["Gold", "Yellow", "Orange"], "days": ["Sunday"]},
    2: {"planet": "Moon", "energy": "Diplomacy, Intuition, Harmony, Gentle Partnership", "colors": ["White", "Cream", "Silver"], "days": ["Monday"]},
    3: {"planet": "Jupiter", "energy": "Wisdom, Creative Expansion, Optimism, Higher Counsel", "colors": ["Yellow", "Saffron", "Royal Blue"], "days": ["Thursday"]},
    4: {"planet": "Rahu", "energy": "Structural Mastery, Unconventional Logic, Persistence, Innovation", "colors": ["Electric Blue", "Grey", "Brown"], "days": ["Saturday"]},
    5: {"planet": "Mercury", "energy": "Versatility, Commercial Intellect, Communication, Rapid Adaptation", "colors": ["Emerald Green", "Light Blue", "Turquoise"], "days": ["Wednesday"]},
    6: {"planet": "Venus", "energy": "Aesthetic Harmony, Luxury, Nurturing Magnetism, Deep Relationship", "colors": ["Pink", "Pastel Blue", "Diamond White"], "days": ["Friday"]},
    7: {"planet": "Ketu", "energy": "Mysticism, Analytical Depth, Intuition, Philosophical Research", "colors": ["Light Green", "White", "Violet"], "days": ["Monday", "Thursday"]},
    8: {"planet": "Saturn", "energy": "Endurance, Material Mastery, Big Infrastructure, Karmic Realization", "colors": ["Dark Blue", "Black", "Charcoal"], "days": ["Saturday"]},
    9: {"planet": "Mars", "energy": "Courage, Universal Humanitarianism, Dynamic Force, Righteous Action", "colors": ["Crimson Red", "Coral", "Maroon"], "days": ["Tuesday"]},
    11: {"planet": "Master Number 11", "energy": "Spiritual Illumination, High Intuition, Visionary Inspiration", "colors": ["Silver", "Violet"], "days": ["Monday"]},
    22: {"planet": "Master Number 22", "energy": "Master Builder, Manifesting Global Systems, Practical Genius", "colors": ["Gold", "Deep Navy"], "days": ["Saturday"]},
    33: {"planet": "Master Number 33", "energy": "Master Teacher, Universal Compassion, Spiritual Upliftment", "colors": ["Golden Yellow", "Emerald"], "days": ["Thursday"]}
}


def reduce_number(n, keep_master=True):
    """Reduces an integer to a single digit or master number (11, 22, 33)."""
    while n > 9:
        if keep_master and n in (11, 22, 33):
            return n
        n = sum(int(d) for d in str(n))
    return n


def compute_numerology(name, dob_str):
    """
    Computes Life Path, Destiny, Soul Urge, Personality numbers and harmonic tuning.
    """
    # 1. Life Path Number (DOB: YYYY-MM-DD)
    digits = [int(d) for d in re.sub(r'[^0-9]', '', dob_str)]
    life_path = reduce_number(sum(digits))

    # 2. Name Cleaning
    clean_name = re.sub(r'[^A-Z]', '', name.upper())

    # Pythagorean Destiny
    pyth_sum = sum(PYTHAGOREAN_MAP.get(c, 0) for c in clean_name)
    pyth_destiny = reduce_number(pyth_sum)

    # Chaldean Destiny (Traditional Indian commercial numerology)
    chald_sum = sum(CHALDEAN_MAP.get(c, 0) for c in clean_name)
    chald_destiny = reduce_number(chald_sum)

    # Soul Urge (Vowels: A, E, I, O, U)
    vowels = [c for c in clean_name if c in 'AEIOU']
    soul_urge = reduce_number(sum(PYTHAGOREAN_MAP.get(c, 0) for c in vowels)) if vowels else 1

    # Personality Number (Consonants)
    consonants = [c for c in clean_name if c not in 'AEIOU']
    personality = reduce_number(sum(PYTHAGOREAN_MAP.get(c, 0) for c in consonants)) if consonants else 1

    # Harmony Analysis between Life Path and Destiny
    # Friendly combinations: (1, 3, 5, 9), (2, 4, 8), (3, 6, 9), (1, 5, 6)
    friendly_pairs = {
        1: [1, 3, 5, 9], 2: [2, 4, 8], 3: [1, 3, 5, 9],
        4: [2, 4, 8, 1], 5: [1, 3, 5, 6], 6: [3, 5, 6, 9],
        7: [7, 1, 5], 8: [2, 4, 8], 9: [1, 3, 9, 6]
    }
    is_harmonious = chald_destiny in friendly_pairs.get(life_path, [life_path])

    tuning_advice = "Your name spelling resonates harmoniously with your Life Path vibration. No major letter modification is required."
    if not is_harmonious:
        tuning_advice = f"Your current Chaldean vibration ({chald_destiny}) has slight friction with your Life Path ({life_path}). Adjusting a vowel or repeating an auspicious letter to reach compound 33 or single digit {life_path} enhances commercial luck."

    attr = NUMBER_ATTRIBUTES.get(life_path, NUMBER_ATTRIBUTES[1])

    return {
        "fullName": name,
        "dateOfBirth": dob_str,
        "coreNumbers": {
            "lifePathNumber": life_path,
            "pythagoreanDestiny": pyth_destiny,
            "chaldeanDestiny": chald_destiny,
            "soulUrgeNumber": soul_urge,
            "personalityNumber": personality
        },
        "lifePathProfile": {
            "rulingArchetype": attr["energy"],
            "governingPlanet": attr["planet"],
            "luckyNumbers": [life_path, (life_path + 3) % 9 or 9, (life_path + 6) % 9 or 9],
            "luckyColors": attr["colors"],
            "luckyDays": attr["days"]
        },
        "nameVibrationTuning": {
            "isHarmonious": is_harmonious,
            "chaldeanScore": chald_destiny,
            "recommendation": tuning_advice
        }
    }


if __name__ == "__main__":
    res = compute_numerology("Shubham Vardaan", "1989-10-30")
    print("Numerology Test for Shubham Vardaan:")
    print("  Life Path:", res["coreNumbers"]["lifePathNumber"])
    print("  Chaldean Destiny:", res["coreNumbers"]["chaldeanDestiny"])
    print("  Governing Planet:", res["lifePathProfile"]["governingPlanet"])
    print("  Lucky Colors:", res["lifePathProfile"]["luckyColors"])
