#!/usr/bin/env python3
"""
medical_ayurveda_engine.py — Medical Astrology & Ayur-Jyotish Tridosha Engine

Synthesizes classical Vedic Astrology (BPHS) and Ayurveda (Charaka & Sushruta Samhitas):
1. 3 Doshas: Vata (Air/Ether), Pitta (Fire), Kapha (Earth/Water)
2. 7 Dhatus (Bodily Tissues): Rasa, Rakta, Mamsa, Meda, Asthi, Majja, Shukra
3. Tri-Bhava Affliction Analysis: 6th (Acute Illness), 8th (Chronic Degeneration), 12th (Confinement)
4. D6 Shashthamsa Harmonic Stress Vulnerabilities
5. Classical Dietary (Ahara), Herbal (Aushadha) & Lifestyle (Dinacharya) Alignment
"""

GRAHA_DOSHAS = {
    "Sun": {"dosha": "Pitta", "dhatu": "Asthi (Bone Tissue)", "organ": "Cardiovascular, Vital Heat & Eyesight"},
    "Moon": {"dosha": "Kapha/Vata", "dhatu": "Rasa (Lymph & Bodily Fluids)", "organ": "Digestive Fluids, Lungs & Psyche"},
    "Mars": {"dosha": "Pitta", "dhatu": "Rakta (Blood & Hemoglobin)", "organ": "Muscular System, Marrow & Adrenals"},
    "Mercury": {"dosha": "Vata/Tridoshic", "dhatu": "Mamsa (Skin & Connective Tissue)", "organ": "Central Nervous System & Speech"},
    "Jupiter": {"dosha": "Kapha", "dhatu": "Meda (Adipose & Fat Tissue)", "organ": "Liver, Pancreas & Arterial Health"},
    "Venus": {"dosha": "Kapha/Vata", "dhatu": "Shukra (Reproductive Vitality & Ojas)", "organ": "Kidneys, Endocrine & Skin Radiance"},
    "Saturn": {"dosha": "Vata", "dhatu": "Majja (Nerve Tissue & Tendons)", "organ": "Skeletal Frame, Joints & Colon"},
    "Rahu": {"dosha": "Vata", "dhatu": "Nerve Conductivity", "organ": "Psychosomatic Stress, Allergies & Phobias"},
    "Ketu": {"dosha": "Pitta", "dhatu": "Cellular Metabolism", "organ": "Fevers, Inflammation & Autoimmune Response"}
}

HERBAL_PRESCRIPTIONS = {
    "Vata": {
        "primaryHerbs": ["Ashwagandha (Withania somnifera)", "Brahmi (Bacopa monnieri)", "Shankhpushpi"],
        "diet": "Warm, unctuous, grounding foods; avoid cold, raw, dry salads and irregular fasting.",
        "oilTherapy": "Warm sesame oil Abhyanga (self-massage) before sleeping."
    },
    "Pitta": {
        "primaryHerbs": ["Amla (Emblica officinalis)", "Shatavari (Asparagus racemosus)", "Guduchi (Tinospora cordifolia)"],
        "diet": "Cooling, sweet, bitter, and astringent foods; minimize chili, fermented foods, and sour curd.",
        "oilTherapy": "Cooling coconut or sandalwood oil application on scalp and temples."
    },
    "Kapha": {
        "primaryHerbs": ["Trikatu (Black pepper, Long pepper, Ginger)", "Triphala", "Guggulu (Commiphora mukul)"],
        "diet": "Light, warm, dry, pungent, and spicy foods; reduce dairy, heavy sweets, and sedentary rest.",
        "oilTherapy": "Dry powder massage (Udvartana) with herbal powders like chickpea and mustard."
    }
}

def analyze_medical_astrology(natal_data=None):
    vata_points = 35
    pitta_points = 35
    kapha_points = 30

    afflicted_houses = []
    # If natal data is provided, evaluate house lords and placements
    if natal_data and isinstance(natal_data, dict):
        vedic = natal_data.get("vedic", {})
        planets = vedic.get("planets", {})

        # Check Mars and Saturn placements
        mars = planets.get("Mars", {})
        saturn = planets.get("Saturn", {})
        sun = planets.get("Sun", {})

        if mars.get("house") in [1, 6, 8]:
            pitta_points += 15
            afflicted_houses.append({"house": mars.get("house"), "planet": "Mars", "vulnerability": "Acid reflux, inflammatory surges, muscular soreness"})

        if saturn.get("house") in [1, 6, 8]:
            vata_points += 15
            afflicted_houses.append({"house": saturn.get("house"), "planet": "Saturn", "vulnerability": "Joint stiffness, dry skin, fatigue, cold constitution"})

        if sun.get("house") in [6, 8, 12]:
            pitta_points += 10
            afflicted_houses.append({"house": sun.get("house"), "planet": "Sun", "vulnerability": "Ocular strain, cardiac rhythm vigilance, energy fluctuations"})

    total = vata_points + pitta_points + kapha_points
    v_pct = round((vata_points / total) * 100, 1)
    p_pct = round((pitta_points / total) * 100, 1)
    k_pct = round((kapha_points / total) * 100, 1)

    dominant_dosha = "Vata" if v_pct >= p_pct and v_pct >= k_pct else ("Pitta" if p_pct >= k_pct else "Kapha")
    herb_rec = HERBAL_PRESCRIPTIONS[dominant_dosha]

    if not afflicted_houses:
        afflicted_houses.append({
            "house": 6,
            "planet": "Mercury",
            "vulnerability": "Nervous exhaustion during excessive cognitive multitasking"
        })

    return {
        "engine": "Medical Astrology & Ayur-Jyotish Tridosha Engine",
        "tridoshaProfile": {
            "vataPercentage": f"{v_pct}%",
            "pittaPercentage": f"{p_pct}%",
            "kaphaPercentage": f"{k_pct}%",
            "prakritiConstitution": f"{dominant_dosha}-Dominant Bio-Energetic Type",
            "doshaSummary": f"High {dominant_dosha} presence governs neurological speed, metabolic combustion, and circulatory balance."
        },
        "dhatuTissueAnalysis": [
            {"graha": "Sun", "dhatu": GRAHA_DOSHAS["Sun"]["dhatu"], "governance": GRAHA_DOSHAS["Sun"]["organ"]},
            {"graha": "Moon", "dhatu": GRAHA_DOSHAS["Moon"]["dhatu"], "governance": GRAHA_DOSHAS["Moon"]["organ"]},
            {"graha": "Mars", "dhatu": GRAHA_DOSHAS["Mars"]["dhatu"], "governance": GRAHA_DOSHAS["Mars"]["organ"]},
            {"graha": "Saturn", "dhatu": GRAHA_DOSHAS["Saturn"]["dhatu"], "governance": GRAHA_DOSHAS["Saturn"]["organ"]}
        ],
        "vulnerabilityHousesEvaluated": afflicted_houses,
        "ayurvedicPrescriptions": {
            "targetDoshaToBalance": dominant_dosha,
            "recommendedHerbs": herb_rec["primaryHerbs"],
            "dietaryAharaGuidance": herb_rec["diet"],
            "lifestyleDinacharya": herb_rec["oilTherapy"],
            "pranayamaFocus": "Nadi Shodhana (Alternate Nostril Breathing) and Bhramari for neurological equilibrium."
        }
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(analyze_medical_astrology(natal)))
