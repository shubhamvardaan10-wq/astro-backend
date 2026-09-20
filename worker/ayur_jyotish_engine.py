"""
Ayur-Jyotish: Medical Astrology & Tridosha Constitution Engine
Synthesizes classical Vedic astrology with Charaka Samhita Ayurvedic diagnostics.
"""

def calculate_ayur_jyotish(natal_data):
    vedic = natal_data.get("vedic", {})
    lagna_sign = vedic.get("ascendant", {}).get("sign", "Virgo")
    
    # Elemental & Dosha mapping
    # Fire (Aries, Leo, Sag) -> Pitta
    # Earth (Taurus, Virgo, Cap) -> Vata/Kapha
    # Air (Gemini, Libra, Aqua) -> Vata
    # Water (Cancer, Scorpio, Pisces) -> Kapha
    
    if lagna_sign in ["Aries", "Leo", "Sagittarius"]:
        pitta, vata, kapha = 55.0, 25.0, 20.0
        primary_dosha = "Pitta Dominant (Agni / Fire & Metabolic Drive)"
    elif lagna_sign in ["Gemini", "Libra", "Aquarius"]:
        pitta, vata, kapha = 20.0, 60.0, 20.0
        primary_dosha = "Vata Dominant (Prana / Air, Nervous System & Cognitive Agility)"
    elif lagna_sign in ["Cancer", "Scorpio", "Pisces"]:
        pitta, vata, kapha = 25.0, 20.0, 55.0
        primary_dosha = "Kapha Dominant (Ojas / Water & Structural Endurance)"
    else: # Earth signs: Taurus, Virgo, Capricorn
        pitta, vata, kapha = 30.0, 45.0, 25.0
        primary_dosha = "Vata-Pitta Prakriti (Analytical Precision & Nervous Stamina)"

    return {
        "engine": "Ayur-Jyotish (Classical Medical Astrology & Charaka Samhita)",
        "constitutionalPrakriti": primary_dosha,
        "doshaDistribution": {
            "vataPercentage": vata,
            "pittaPercentage": pitta,
            "kaphaPercentage": kapha
        },
        "biologicalSystemsVulnerabilityHeatmap": [
            {
                "system": "Central Nervous System & Cognitive Synapses",
                "governingGraha": "Mercury (Budha)",
                "vulnerabilityLevel": "MODERATE_ELEVATED",
                "warningSigns": "Mental exhaustion, eye strain from screens, fragmented sleep cycles during intense release sprints.",
                "protectiveAction": "Implement digital sunset at 9:30 PM; practice Nadi Shodhana pranayama (alternate nostril breathing)."
            },
            {
                "system": "Digestive Fire (Jatharagni) & Gut Microbiome",
                "governingGraha": "Sun (Surya) & Mars",
                "vulnerabilityLevel": "HEALTHY_ROBUST",
                "warningSigns": "Mild acidity when meals are delayed past 1:30 PM.",
                "protectiveAction": "Sip warm cumin-coriander-fennel (CCF) tea throughout deep work sessions."
            },
            {
                "system": "Musculoskeletal & Spinal Posture",
                "governingGraha": "Saturn (Shani)",
                "vulnerabilityLevel": "STABLE",
                "warningSigns": "Lower lumbar rigidity from prolonged ergonomic sitting.",
                "protectiveAction": "Incorporate standing desk intervals and daily Suryanamaskar stretches."
            }
        ],
        "circadianDinacharyaSchedule": {
            "brahmaMuhurtaWaking": "05:15 AM - 05:45 AM (Vata hour of supreme mental clarity)",
            "digestivePeakMeal": "12:00 PM - 01:30 PM (Sun Hora: Maximum enzymatic digestion)",
            "creativeIntellectualSprint": "09:00 AM - 12:00 PM (Mercury-Jupiter Hora: Peak algorithmic flow)",
            "restorativeSleepWindow": "10:15 PM (Pitta cleansing phase begins at 10:00 PM)"
        },
        "rasayanaAdaptogenPrescriptions": [
            {
                "herbName": "Brahmi (Bacopa Monnieri)",
                "planetaryTarget": "Mercury & Moon",
                "purpose": "Enhances cerebral synapse velocity, memory retention, and shields the nervous system from burnout.",
                "dosageProtocol": "500mg with warm organic milk or ghee before sleep."
            },
            {
                "herbName": "Ashwagandha (Withania Somnifera)",
                "planetaryTarget": "Saturn & Mars",
                "purpose": "Calms excess Vata, builds deep physical vitality (*Ojas*), and lowers cortisol.",
                "dosageProtocol": "1/2 teaspoon organic root powder in warm water in the evening."
            },
            {
                "herbName": "Holy Basil (Tulsi) Infusion",
                "planetaryTarget": "Sun & Jupiter",
                "purpose": "Purifies respiratory channels and fosters sattvic mental equanimity.",
                "dosageProtocol": "Steep fresh or dried leaves in hot water as a morning tea."
            }
        ],
        "ayurvedicDietaryGuidance": "Emphasize warm, grounding, cooked foods with healthy fats (ghee, olive oil). Minimize cold, raw, or iced beverages which extinguish your digestive fire."
    }
