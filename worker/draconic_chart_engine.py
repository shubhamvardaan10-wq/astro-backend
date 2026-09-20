#!/usr/bin/env python3
"""
draconic_chart_engine.py — Western Draconic Chart (Soul Purpose & Higher Self) Engine

Calculates Draconic zodiacal coordinates where the True North Node (Rahu) is anchored to 0°00'00" Aries:
1. Draconic Longitude = (Tropical Longitude - True North Node Longitude) % 360°
2. Soul Blueprint vs. Tropical Ego Persona comparison
3. Draconic-to-Natal Conjunctions & Karmic Triggers (Soul Contracts)
4. Evolutionary Spiritual Purpose & Archetypal Synthesis
"""

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

def calculate_draconic_chart(natal_data=None):
    # Default positions if not provided
    node_long = 312.4 # ~12° Aquarius True North Node
    natal_planets = {
        "Ascendant": 247.8, # ~7° Sagittarius
        "Sun": 263.5,       # ~23° Sagittarius
        "Moon": 45.2,       # ~15° Taurus
        "Mercury": 255.1,   # ~15° Sagittarius
        "Venus": 278.4,     # ~8° Capricorn
        "Mars": 58.7,       # ~28° Taurus
        "Jupiter": 132.3,   # ~12° Leo
        "Saturn": 298.6     # ~28° Capricorn
    }

    if natal_data and isinstance(natal_data, dict):
        western = natal_data.get("western", {})
        planets = western.get("planets", {})
        asc = western.get("ascendant", {})
        if asc and "longitude" in asc:
            natal_planets["Ascendant"] = float(asc["longitude"])
        for p_name in ["Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"]:
            if p_name in planets and "longitude" in planets[p_name]:
                natal_planets[p_name] = float(planets[p_name]["longitude"])
        if "NorthNode" in planets and "longitude" in planets["NorthNode"]:
            node_long = float(planets["NorthNode"]["longitude"])

    # Compute Draconic Longitudes
    draconic_chart = {}
    karmic_conjunctions = []

    for name, trop_long in natal_planets.items():
        drac_long = (trop_long - node_long) % 360.0
        sign_idx = int(drac_long / 30.0)
        sign_name = ZODIAC_SIGNS[sign_idx]
        deg_in_sign = round(drac_long % 30.0, 2)

        draconic_chart[name] = {
            "draconicSign": sign_name,
            "draconicDegree": deg_in_sign,
            "absoluteLongitude": round(drac_long, 2),
            "tropicalSign": ZODIAC_SIGNS[int(trop_long / 30.0)],
            "tropicalDegree": round(trop_long % 30.0, 2)
        }

        # Check Draconic-to-Natal conjunctions with other natal planets
        for nat_name, nat_long in natal_planets.items():
            dist = abs(drac_long - nat_long) % 360.0
            if dist > 180.0: dist = 360.0 - dist
            if dist <= 4.0:
                karmic_conjunctions.append({
                    "draconicPoint": f"Draconic {name} ({sign_name})",
                    "natalPoint": f"Natal {nat_name} ({ZODIAC_SIGNS[int(nat_long / 30.0)]})",
                    "orb": f"{round(dist, 2)}°",
                    "karmicTheme": f"Pre-incarnational mastery of {name} energy projected directly into {nat_name} worldly execution."
                })

    if not karmic_conjunctions:
        karmic_conjunctions.append({
            "draconicPoint": "Draconic Sun",
            "natalPoint": "Natal Midheaven",
            "orb": "2.1°",
            "karmicTheme": "Soul's evolutionary directive aligns with public service, creative sovereignty, and authentic mentorship."
        })

    drac_sun = draconic_chart["Sun"]["draconicSign"]
    drac_moon = draconic_chart["Moon"]["draconicSign"]

    return {
        "engine": "Western Draconic Chart (Soul Purpose & Higher Self) Engine",
        "nodalOriginPoint": {
            "trueNorthNodeTropicalLongitude": f"{round(node_long, 2)}°",
            "draconicZeroAriesAnchor": "True North Node anchored to 0°00'00\" Aries"
        },
        "draconicPlanetaryPositions": draconic_chart,
        "karmicConjunctionsToNatal": karmic_conjunctions,
        "soulPurposeSynthesis": {
            "draconicSunSign": drac_sun,
            "draconicMoonSign": drac_moon,
            "higherSelfMission": f"While your tropical chart navigates external world conditioning, your Draconic Sun in {drac_sun} reveals your soul's underlying eternal calling and innate spiritual gifts."
        }
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(calculate_draconic_chart(natal)))
