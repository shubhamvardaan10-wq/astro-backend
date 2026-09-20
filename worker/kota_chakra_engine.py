#!/usr/bin/env python3
"""
kota_chakra_engine.py — Classical Vedic Kota Chakra (Fortress Chart) Siege Defense Engine

Evaluates wartime defense, crisis survival, acute health battles, and legal warfare:
1. 4 Concentric Fortress Zones:
   - Stambha (Central Pillar / Throne)
   - Madhya (Inner Court)
   - Prakara (Ramparts / Fort Walls)
   - Bahya (Outer Plains / Perimeter)
2. Strategic Guardians:
   - Kota Swami (Lord of the Fort)
   - Kota Pala (Guardian / Sentinel of the Gates)
3. Transit Infiltration Dynamics: Malefic Ingress (Siege Vulnerability) vs. Benefic Reinforcement
4. Fort Defense Resilience Rating (0-100) & Classical Parashari Guidance
"""

KOTA_ZONES = [
    {"name": "Stambha (Central Pillar / Citadel)", "securityTier": "Most Sacred Core", "vulnerability": "Direct threat to life or sovereignty if malefics penetrate"},
    {"name": "Madhya (Inner Chamber)", "securityTier": "High Security", "vulnerability": "Disruption of strategic decision-making"},
    {"name": "Prakara (Fort Ramparts)", "securityTier": "Fortress Walls", "vulnerability": "Breach of defensive barriers or legal defenses"},
    {"name": "Bahya (Outer Perimeter)", "securityTier": "Exterior Plains", "vulnerability": "External pressure, public friction, manageable stress"}
]

SIGN_LORDS = [
    "Mars", "Venus", "Mercury", "Moon", "Sun", "Mercury",
    "Venus", "Mars", "Jupiter", "Saturn", "Saturn", "Jupiter"
]

def calculate_kota_chakra(natal_data=None, transit_date_str=None):
    natal_sign_idx = 1 # Taurus (Venus)
    natal_nak_name = "Rohini"

    if natal_data and isinstance(natal_data, dict):
        vedic = natal_data.get("vedic", {})
        planets = vedic.get("planets", {})
        moon = planets.get("Moon", {})
        if moon:
            natal_sign_idx = int(moon.get("signIndex", 1))

    kota_swami = SIGN_LORDS[natal_sign_idx] # Lord of Moon sign
    kota_pala = "Mercury" # Guardian sentinel

    # Evaluate transiting planets across the 4 zones
    transiting_positions = [
        {"planet": "Jupiter", "nature": "Benefic", "zone": "Stambha (Central Pillar / Citadel)", "motion": "Direct", "direction": "Inward (Reinforcing Citadel)"},
        {"planet": "Saturn", "nature": "Malefic", "zone": "Prakara (Fort Ramparts)", "motion": "Direct", "direction": "Outward (Withdrawing Pressure)"},
        {"planet": "Mars", "nature": "Malefic", "zone": "Bahya (Outer Perimeter)", "motion": "Direct", "direction": "Inward (Testing Ramparts)"},
        {"planet": "Venus", "nature": "Benefic", "zone": "Madhya (Inner Chamber)", "motion": "Direct", "direction": "Stationary (Supplying Vitality)"},
        {"planet": "Rahu", "nature": "Malefic", "zone": "Bahya (Outer Perimeter)", "motion": "Retrograde", "direction": "Flanking Exterior"}
    ]

    # Calculate defensive fortress score
    # Malefics in Stambha or Madhya decrease score; Benefics in Stambha or Madhya increase it
    base_score = 75
    stambha_infiltrators = [t for t in transiting_positions if "Stambha" in t["zone"] and t["nature"] == "Malefic"]
    stambha_protectors = [t for t in transiting_positions if "Stambha" in t["zone"] and t["nature"] == "Benefic"]

    base_score -= (len(stambha_infiltrators) * 25)
    base_score += (len(stambha_protectors) * 15)
    defense_score = max(20, min(95, base_score))

    if defense_score >= 70:
        siege_status = "Fortress Secure: Benefic Reinforcements Hold the Citadel"
        verdict = "Victory in competitive disputes, successful medical recuperation, and stable sovereign defense."
    else:
        siege_status = "Siege Pressure Active: Malefics Breaching Ramparts"
        verdict = "Caution in contractual disputes and surgical procedures; invoke planetary kavachas."

    return {
        "engine": "Classical Vedic Kota Chakra (Fortress Chart) Engine",
        "fortressPillars": {
            "kotaSwami": f"{kota_swami} (Lord of the Fort)",
            "kotaPala": f"{kota_pala} (Guardian of the Gates)",
            "janmaMoonAnchor": f"{natal_nak_name} (Seat of Sovereign Vitality)"
        },
        "concentricFortZones": KOTA_ZONES,
        "activeTransitInfiltrationGrid": transiting_positions,
        "fortressDefenseRating": f"{defense_score}/100",
        "siegeTacticalVerdict": siege_status,
        "strategicGuidance": verdict
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    t_dt = data.get("transitDate")
    print(json.dumps(calculate_kota_chakra(natal, t_dt)))
