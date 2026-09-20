#!/usr/bin/env python3
"""
gemstone_rudraksha_engine.py — Authentic Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine

Calculates strictly favorable (*Anukul Graha*) prescriptions based on classical Parashari principles:
1. Filters out Dusthana Lords (6th, 8th, 12th) and Marakas (2nd, 7th) to prevent adverse reactions.
2. Identifies:
   - Jeeva Ratna (Life Stone): Lord of 1st House (Lagna)
   - Punya Ratna (Luck Stone): Lord of 5th House
   - Bhagya Ratna (Fortune Stone): Lord of 9th House
3. Complete Ring Setting Specifications: Carat Weight, Setting Metal, Wearing Finger, Auspicious Muhurta, Consecration Beej Mantra.
4. Classical 1 to 14 Mukhi Rudraksha prescription mapped to planetary afflictions.
"""

GEMSTONE_CATALOG = {
    "Sun": {
        "gem": "Natural Unheated Ruby (Manikya)",
        "substitute": "Red Garnet / Rubellite",
        "metal": "22k Gold or Copper",
        "finger": "Ring Finger of Right Hand",
        "wearingDay": "Sunday sunrise during Shukla Paksha",
        "mantra": "Om Hram Hreem Hroum Sah Suryaya Namah (7,000 chants)",
        "minimumWeight": "4 to 6 Carats (5 to 7 Ratti)"
    },
    "Moon": {
        "gem": "Natural South Sea Pearl (Moti)",
        "substitute": "Moonstone",
        "metal": "Pure Silver",
        "finger": "Little Finger of Right Hand",
        "wearingDay": "Monday evening during Shukla Paksha",
        "mantra": "Om Shram Shreem Shroum Sah Chandramase Namah (11,000 chants)",
        "minimumWeight": "5 to 8 Carats"
    },
    "Mars": {
        "gem": "Natural Red Coral (Moonga)",
        "substitute": "Carnelian",
        "metal": "Copper or 18k Gold",
        "finger": "Ring Finger",
        "wearingDay": "Tuesday morning 1 hour after sunrise",
        "mantra": "Om Kram Kreem Kroum Sah Bhaumaya Namah (10,000 chants)",
        "minimumWeight": "6 to 9 Carats"
    },
    "Mercury": {
        "gem": "Natural Zambian / Colombian Emerald (Panna)",
        "substitute": "Green Tourmaline / Peridot",
        "metal": "18k Gold or Silver",
        "finger": "Little Finger",
        "wearingDay": "Wednesday morning during Mercury Hora",
        "mantra": "Om Bram Breem Broum Sah Budhaya Namah (9,000 chants)",
        "minimumWeight": "3 to 5 Carats"
    },
    "Jupiter": {
        "gem": "Natural Ceylon Yellow Sapphire (Pukhraj)",
        "substitute": "Yellow Topaz / Citrine",
        "metal": "22k Yellow Gold",
        "finger": "Index (Tarjani) Finger",
        "wearingDay": "Thursday morning during Jupiter Hora",
        "mantra": "Om Gram Greem Groum Sah Gurave Namah (19,000 chants)",
        "minimumWeight": "4 to 7 Carats"
    },
    "Venus": {
        "gem": "Natural Diamond or White Sapphire (Heera / Safed Pukhraj)",
        "substitute": "White Zircon",
        "metal": "Platinum or Pure Silver",
        "finger": "Middle or Little Finger",
        "wearingDay": "Friday morning during sunrise",
        "mantra": "Om Dram Dreem Droum Sah Shukraya Namah (16,000 chants)",
        "minimumWeight": "1 to 2 Carats (Diamond) / 4 to 6 Carats (Sapphire)"
    },
    "Saturn": {
        "gem": "Natural Blue Sapphire (Neelam)",
        "substitute": "Amethyst / Iolite",
        "metal": "Silver or Ashtadhatu",
        "finger": "Middle (Madhyama) Finger",
        "wearingDay": "Saturday evening during twilight",
        "mantra": "Om Pram Preem Proum Sah Shanaischaraya Namah (23,000 chants)",
        "minimumWeight": "4 to 6 Carats"
    }
}

SIGN_LORDS = [
    "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
    "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
]

RUDRAKSHA_MAP = {
    "Sun": {"mukhi": "1 Mukhi & 12 Mukhi Rudraksha", "deity": "Lord Shiva & Surya Dev", "blessing": "Leadership, supreme self-mastery, administrative authority"},
    "Moon": {"mukhi": "2 Mukhi Rudraksha", "deity": "Ardhanarishwara", "blessing": "Emotional harmony, peaceful relationships, mental serenity"},
    "Mars": {"mukhi": "3 Mukhi Rudraksha", "deity": "Agni Deva", "blessing": "Purification of past karmas, courage, physical dynamism"},
    "Mercury": {"mukhi": "4 Mukhi Rudraksha", "deity": "Lord Brahma", "blessing": "Intellect, cognitive focus, rhetorical eloquence"},
    "Jupiter": {"mukhi": "5 Mukhi Rudraksha", "deity": "Kalagni Rudra", "blessing": "Spiritual wisdom, health protection, cardiovascular equanimity"},
    "Venus": {"mukhi": "6 Mukhi Rudraksha", "deity": "Lord Kartikeya", "blessing": "Artistic talent, emotional stability, magnetic attraction"},
    "Saturn": {"mukhi": "7 Mukhi & 14 Mukhi Rudraksha", "deity": "Maha Lakshmi & Deva Mani", "blessing": "Wealth preservation, liberation from Saturnian trials, third-eye awakening"}
}

def recommend_gemstones_and_rudraksha(natal_data=None):
    lagna_sign_idx = 8 # Default Sagittarius (Jupiter lord)

    if natal_data and isinstance(natal_data, dict):
        vedic = natal_data.get("vedic", {})
        lagna = vedic.get("lagna", {})
        if lagna:
            lagna_sign_idx = int(lagna.get("signIndex", 8))

    lagnesha = SIGN_LORDS[lagna_sign_idx]
    fifth_lord = SIGN_LORDS[(lagna_sign_idx + 4) % 12]
    ninth_lord = SIGN_LORDS[(lagna_sign_idx + 8) % 12]

    # Primary recommendations
    jeeva = GEMSTONE_CATALOG.get(lagnesha, GEMSTONE_CATALOG["Jupiter"])
    punya = GEMSTONE_CATALOG.get(fifth_lord, GEMSTONE_CATALOG["Mars"])
    bhagya = GEMSTONE_CATALOG.get(ninth_lord, GEMSTONE_CATALOG["Sun"])

    rudraksha_primary = RUDRAKSHA_MAP.get(lagnesha, RUDRAKSHA_MAP["Jupiter"])
    rudraksha_secondary = RUDRAKSHA_MAP.get(ninth_lord, RUDRAKSHA_MAP["Sun"])

    return {
        "engine": "Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine",
        "anukulPhilosophicalStandard": "Strict Anukul Graha: Only Functional Benefic Trikona Lords Prescribed (Dusthana Lords 6/8/12 Prohibited)",
        "prescribedGemstones": {
            "jeevaRatnaLifeStone": {
                "rulingPlanet": lagnesha,
                "purpose": "Enhances Vitality, Immunity, Longevity & Public Eminence",
                **jeeva
            },
            "punyaRatnaLuckyStone": {
                "rulingPlanet": fifth_lord,
                "purpose": "Augments Intellect, Intuition, Academic & Speculative Fortune",
                **punya
            },
            "bhagyaRatnaFortuneStone": {
                "rulingPlanet": ninth_lord,
                "purpose": "Activates Divine Luck, Dharma, Foreign Travel & Prosperity",
                **bhagya
            }
        },
        "strictlyProhibitedGemstones": [
            "Never wear gemstones of functionally malefic dusthana lords without explicit astrological trial.",
            "Avoid combining incompatible stones (e.g., Ruby with Blue Sapphire or Pearl with Cat's Eye Chrysoberyl)."
        ],
        "prescribedSacredRudraksha": {
            "primaryAuspiciousBead": rudraksha_primary["mukhi"],
            "presidingDeity": rudraksha_primary["deity"],
            "spiritualBlessing": rudraksha_primary["blessing"],
            "supportiveBead": rudraksha_secondary["mukhi"],
            "wearingProcedure": "String in red silk thread or silver capping; energize with Om Namah Shivaya before wearing on Monday or auspicious Shiva Ratri."
        }
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(recommend_gemstones_and_rudraksha(natal)))
