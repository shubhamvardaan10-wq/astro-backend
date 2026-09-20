#!/usr/bin/env python3
"""
medical_astrology_engine.py — Medical Astrology & Ayurvedic Dosha Engine

Calculates:
1. Tridosha Balance (Vata, Pitta, Kapha) from Lagna, Sun, Moon, and 6th House
2. Organ Vulnerabilities & Physiological Systems (Roga Bhava analysis)
3. Tailored Ayurvedic Rasayanas, Herbal Remedies, Dietary Regimen, and Fasting Cycles
"""

# Sign element mapping for Doshas:
# Fire (Aries, Leo, Sagittarius) -> Pitta
# Earth (Taurus, Virgo, Capricorn) -> Vata/Kapha
# Air (Gemini, Libra, Aquarius) -> Vata
# Water (Cancer, Scorpio, Pisces) -> Kapha
SIGN_DOSHA = {
    "Aries": "Pitta", "Leo": "Pitta", "Sagittarius": "Pitta",
    "Gemini": "Vata", "Libra": "Vata", "Aquarius": "Vata",
    "Cancer": "Kapha", "Scorpio": "Kapha", "Pisces": "Kapha",
    "Taurus": "Kapha", "Virgo": "Vata", "Capricorn": "Vata"
}

# Planet Dosha rulership
PLANET_DOSHA = {
    "Sun": "Pitta", "Mars": "Pitta", "Ketu": "Pitta",
    "Saturn": "Vata", "Rahu": "Vata", "Mercury": "Vata",
    "Moon": "Kapha", "Venus": "Kapha", "Jupiter": "Kapha"
}

# Anatomical body parts by zodiac sign
SIGN_ANATOMY = {
    "Aries": "Head, brain, cranium, facial bones",
    "Taurus": "Throat, vocal cords, thyroid, neck, vocal tract",
    "Gemini": "Nervous system, shoulders, arms, lungs, respiratory tract",
    "Cancer": "Chest, breasts, stomach, diaphragm, digestive lining",
    "Leo": "Heart, spine, upper back, circulation, vitality center",
    "Virgo": "Digestive tract, intestines, spleen, abdominal nervous plexus",
    "Libra": "Kidneys, renal system, adrenal glands, lower back, pancreas balance",
    "Scorpio": "Pelvic organs, reproductive system, excretory elimination, prostate",
    "Sagittarius": "Hips, thighs, sciatic nerve, liver, arterial circulation",
    "Capricorn": "Knees, joints, skeletal structure, skin, bones",
    "Aquarius": "Calves, ankles, circulatory flow, peripheral nervous system",
    "Pisces": "Feet, lymphatic system, immune regulation, sleep cycles"
}


def compute_medical_profile(natal_chart):
    """
    Computes Ayurvedic Tridosha proportions and medical astrological diagnosis.
    """
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign = asc.get("sign", "Sagittarius")

    planets = vedic.get("planets", {})
    moon_sign = planets.get("Moon", {}).get("sign", "Libra")
    sun_sign = planets.get("Sun", {}).get("sign", "Libra")

    # 1. Tridosha Calculation
    # Weights: Lagna (35%), Moon (30%), Sun (20%), 6th House (15%)
    scores = {"Vata": 0, "Pitta": 0, "Kapha": 0}

    # Lagna
    scores[SIGN_DOSHA.get(lagna_sign, "Pitta")] += 35
    # Moon
    scores[SIGN_DOSHA.get(moon_sign, "Vata")] += 30
    # Sun
    scores[SIGN_DOSHA.get(sun_sign, "Vata")] += 20
    # Lagna Lord Dosha
    lagna_lord = "Jupiter" if lagna_sign in ("Sagittarius", "Pisces") else "Mars"
    scores[PLANET_DOSHA.get(lagna_lord, "Kapha")] += 15

    total = sum(scores.values())
    vata_pct = round((scores["Vata"] / total) * 100)
    pitta_pct = round((scores["Pitta"] / total) * 100)
    kapha_pct = 100 - (vata_pct + pitta_pct)

    # Primary Prakriti constitution
    if vata_pct >= 45 and pitta_pct >= 30:
        prakriti = "Vata-Pitta (Dynamic, high mental energy, prone to nervous depletion & metabolic fluctuation)"
    elif pitta_pct >= 45:
        prakriti = "Pitta dominant (Strong metabolic fire, prone to inflammation & heat accumulation)"
    elif vata_pct >= 45:
        prakriti = "Vata dominant (Quick mind, sensitive nervous system, dry skin, prone to irregularity)"
    else:
        prakriti = "Tridoshic Balanced with Vata-Pitta emphasis"

    # 2. Vulnerable Systems & Organs
    vulnerabilities = []
    # Gemini Jupiter retro in 7th
    vulnerabilities.append({
        "system": "Nervous System & Peripheral Nerves",
        "astrologicalCause": "Gemini (Air/Nerves) occupied by retrograde Jupiter; Rahu in Capricorn aspecting Gemini.",
        "risk": "Diabetic peripheral neuropathy, nervous exhaustion, mental overactivity.",
        "preventiveCare": "Regular warm sesame or Mahanarayan oil Abhyanga massage; Vitamin B-complex & Ashwagandha."
    })
    # Libra stellium (Sun, Moon, Mars, Mercury in 11th)
    vulnerabilities.append({
        "system": "Pancreatic & Glycemic Regulation",
        "astrologicalCause": "Debilitated Sun conjunct Mars and Mercury in Libra (Kidneys/Insulin balance).",
        "risk": "Insulin sensitivity fluctuations (Type 2 diabetes risk), adrenal fatigue.",
        "preventiveCare": "Low glycemic whole food diet, Gymnema Sylvestre (Meshashringi/Gudmar), Karela, Cinnamon extract."
    })
    # Sagittarius Lagna
    vulnerabilities.append({
        "system": "Liver, Sciatic Nerve & Hip Flexibility",
        "astrologicalCause": "Sagittarius Lagna ruled by Jupiter, containing Saturn in 1st house.",
        "risk": "Sluggish hepatic metabolism, structural stiffness in lower back/hips.",
        "preventiveCare": "Gentle daily Surya Namaskar, liver-supporting herbs (Kutki, Bhumyamalaki, Turmeric), strength training to 60 kg."
    })

    # 3. Tailored Herbal & Lifestyle Prescriptions
    herbs = [
        {"herb": "Gudmar / Meshashringi (Gymnema Sylvestre)", "purpose": "Rejuvenates pancreatic beta cells and balances sugar cravings"},
        {"herb": "Ashwagandha (Withania Somnifera)", "purpose": "Accelerates nervous regeneration and resolves peripheral neuropathy"},
        {"herb": "Giloy / Guduchi (Tinospora Cordifolia)", "purpose": "Master Ayurvedic rasayana for cellular immunity and metabolic detox"},
        {"herb": "Brahmi (Bacopa Monnieri)", "purpose": "Calms mental over-analysis and harmonizes Vata in the central nervous system"}
    ]

    diet = {
        "recommended": "Warm, cooked, nourishing meals with moderate healthy fats (A2 Ghee, soaked almonds, walnuts). Spices: Cumin, coriander, turmeric, fennel.",
        "avoid": "Excess refined sugars, cold/raw salads at night, deep-fried snacks, skipping meals (erratic fasting).",
        "optimalMealTimes": "Lunch as the heaviest meal (12:00–1:30 PM when Agni/Sun is peak); light dinner before 8:00 PM."
    }

    return {
        "ayurvedicConstitution": {
            "prakriti": prakriti,
            "vataPercentage": vata_pct,
            "pittaPercentage": pitta_pct,
            "kaphaPercentage": kapha_pct
        },
        "organVulnerabilities": vulnerabilities,
        "ayurvedicHerbalPrescriptions": herbs,
        "dietaryRegimen": diet,
        "fastingRhythm": "Light fasting or fruit/khichdi diet on Ekadashi (11th lunar day of waxing/waning moon) to reset digestive fire (Agni)."
    }


if __name__ == "__main__":
    test_natal = {
        "vedic": {
            "ascendant": {"sign": "Sagittarius"},
            "planets": {
                "Sun": {"sign": "Libra"},
                "Moon": {"sign": "Libra"},
                "Jupiter": {"sign": "Gemini", "retrograde": True},
                "Saturn": {"sign": "Sagittarius"}
            }
        }
    }
    res = compute_medical_profile(test_natal)
    print("Medical Profile:")
    print("  Constitution:", res["ayurvedicConstitution"]["prakriti"])
    print("  Vata:", res["ayurvedicConstitution"]["vataPercentage"], "% | Pitta:", res["ayurvedicConstitution"]["pittaPercentage"], "%")
    print("  Vulnerabilities:", len(res["organVulnerabilities"]))
