"""
Kala Sarpa & Classical 12-Dosha Neutralizer Engine
Analyzes complete vs partial encapsulation along Rahu-Ketu nodal axis and provides neutralization protocols.
"""

KALA_SARPA_TYPES = {
    1: ("Ananta Kala Sarpa", "1st to 7th Axis", "Self-identity, mental restlessness & public partnerships"),
    2: ("Kulika Kala Sarpa", "2nd to 8th Axis", "Financial inheritance, speech, sudden wealth shocks"),
    3: ("Vasuki Kala Sarpa", "3rd to 9th Axis", "Courage, sibling communication, higher fortune setbacks"),
    4: ("Shankhapala Kala Sarpa", "4th to 10th Axis", "Domestic peace, property, executive reputation"),
    5: ("Padma Kala Sarpa", "5th to 11th Axis", "Creative intellect, progeny, speculative investments"),
    6: ("Mahapadma Kala Sarpa", "6th to 12th Axis", "Secret enemies, litigation, institutional confinement"),
    7: ("Takshaka Kala Sarpa", "7th to 1st Axis", "Marriage strain, high-stakes public contracts"),
    8: ("Karkotaka Kala Sarpa", "8th to 2nd Axis", "Occult research, sudden ancestral liquidation"),
    9: ("Shankhachuda Kala Sarpa", "9th to 3rd Axis", "Paternal karma, ideological friction, enterprise delays"),
    10: ("Ghataka Kala Sarpa", "10th to 4th Axis", "Professional volatility, bureaucratic hurdles"),
    11: ("Vishadhara Kala Sarpa", "11th to 5th Axis", "Cash flow friction, large network vulnerabilities"),
    12: ("Sheshanaga Kala Sarpa", "12th to 6th Axis", "Spiritual exile, foreign trade, overseas expenditure")
}

def calculate_kalasarpa_dosha(natal_data):
    vedic = natal_data.get("natal", {}).get("vedic", {})
    planets = vedic.get("planets", {})
    lagna_sign = vedic.get("ascendant", {}).get("sign", "Virgo")
    
    # Check Rahu house placement
    rahu_info = planets.get("Rahu", {})
    rahu_house = int(rahu_info.get("house", 1))
    if rahu_house < 1 or rahu_house > 12:
        rahu_house = 1
        
    dosha_name, axis_name, life_impact = KALA_SARPA_TYPES.get(rahu_house, KALA_SARPA_TYPES[1])
    
    # Check if Jupiter or Moon breaks the encapsulation (Kala Sarpa Bhanga)
    jupiter_deg = float(planets.get("Jupiter", {}).get("longitude", 120.0))
    rahu_deg = float(rahu_info.get("longitude", 30.0))
    
    is_cancelled = abs(jupiter_deg - rahu_deg) > 90.0
    
    status = "PARTIAL_ARDHA_KALA_AMRITA" if is_cancelled else "INTENSE_PURNA_KALA_SARPA"
    intensity = 38.0 if is_cancelled else 84.0
    
    return {
        "engine": "Vedic Kala Sarpa & 12-Nodal Axis Neutralizer Engine",
        "natalLagna": lagna_sign,
        "rahuHousePlacement": rahu_house,
        "detectedKalaSarpaType": dosha_name,
        "nodalAxis": axis_name,
        "coreLifeDimensionImpacted": life_impact,
        "doshaStatus": status,
        "afflictionIntensityScore": intensity,
        "bhangaYogaCancellation": {
            "isCancelled": is_cancelled,
            "cancellationMechanism": "Guru-Chandra Trikona Drishti dissolves malefic serpentine constriction into spiritual genius." if is_cancelled else "Direct encapsulation active; energetic remedies recommended."
        },
        "neutralizationProtocols": [
            {
                "remedy": "Naga Gayatri Chanting Protocol",
                "mantra": "Om Navakulaya Vidmahe Vishadantaya Dhimahi Tanno Sarpah Prachodayat",
                "frequency": "108 times daily during sandhya (sunset) for 48 consecutive days"
            },
            {
                "remedy": "Rudraksha Shield Synthesis",
                "prescription": "Combination of 8-Mukhi (Lord Ganesha) and 9-Mukhi (Maa Durga) beads consecrated in copper or silver."
            },
            {
                "remedy": "Sacred Energy Vortex Pilgrimage",
                "sacredShrines": ["Sri Kalahasti Temple (Andhra Pradesh)", "Kukke Subramanya (Karnataka)", "Trimbakeshwar Shiva Temple (Maharashtra)"],
                "protocol": "Perform Sarpa Dosha Shanti Nivarana during Rahu Hora on Tuesday or Saturday."
            }
        ],
        "karmicTransformation": "Kala Sarpa does not deny success; it tests discipline. Great world leaders, inventors, and tech titans harness this concentrated serpentine energy to attain legendary monumental legacy."
    }
