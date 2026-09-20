"""
Astrocartography & Relocation Matrix Engine
Calculates planetary angular alignments (Ascendant, Midheaven/MC, Descendant, IC)
across major global cities and identifies top power destinations for wealth, love, and vitality.
"""

import math

GLOBAL_CITIES = [
    {"name": "London", "country": "United Kingdom", "lat": 51.5074, "lon": -0.1278},
    {"name": "New York", "country": "United States", "lat": 40.7128, "lon": -74.0060},
    {"name": "San Francisco", "country": "United States", "lat": 37.7749, "lon": -122.4194},
    {"name": "Tokyo", "country": "Japan", "lat": 35.6762, "lon": 139.6503},
    {"name": "Dubai", "country": "United Arab Emirates", "lat": 25.2048, "lon": 55.2708},
    {"name": "Singapore", "country": "Singapore", "lat": 1.3521, "lon": 103.8198},
    {"name": "Paris", "country": "France", "lat": 48.8566, "lon": 2.3522},
    {"name": "Berlin", "country": "Germany", "lat": 52.5200, "lon": 13.4050},
    {"name": "Zurich", "country": "Switzerland", "lat": 47.3769, "lon": 8.5417},
    {"name": "Sydney", "country": "Australia", "lat": -33.8688, "lon": 151.2093},
    {"name": "Mumbai", "country": "India", "lat": 19.0760, "lon": 72.8777},
    {"name": "Bengaluru", "country": "India", "lat": 12.9716, "lon": 77.5946},
    {"name": "Toronto", "country": "Canada", "lat": 43.6532, "lon": -79.3832},
    {"name": "Austin", "country": "United States", "lat": 30.2672, "lon": -97.7431},
    {"name": "Hong Kong", "country": "Hong Kong", "lat": 22.3193, "lon": 114.1694},
    {"name": "Amsterdam", "country": "Netherlands", "lat": 52.3676, "lon": 4.9041},
    {"name": "Riyadh", "country": "Saudi Arabia", "lat": 24.7136, "lon": 46.6753},
    {"name": "Bali", "country": "Indonesia", "lat": -8.3405, "lon": 115.0920},
    {"name": "Cape Town", "country": "South Africa", "lat": -33.9249, "lon": 18.4241},
    {"name": "Kyoto", "country": "Japan", "lat": 35.0116, "lon": 135.7681}
]

def calculate_relocation_matrix(natal_data):
    """
    Computes angular power lines for natal planets relative to global longitudes.
    """
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    
    # Get planetary longitudes (default degrees if missing)
    sun_lon = planets.get("Sun", {}).get("longitude", 54.2)
    moon_lon = planets.get("Moon", {}).get("longitude", 282.5)
    jupiter_lon = planets.get("Jupiter", {}).get("longitude", 72.8)
    venus_lon = planets.get("Venus", {}).get("longitude", 15.4)
    saturn_lon = planets.get("Saturn", {}).get("longitude", 298.1)
    mercury_lon = planets.get("Mercury", {}).get("longitude", 35.9)
    
    scored_cities = []
    
    for city in GLOBAL_CITIES:
        lon = city["lon"]
        lat = city["lat"]
        
        # Approximate relocated RAMC / MC based on city longitude
        relocated_mc = (jupiter_lon + lon * 1.05 + 180.0) % 360.0
        
        # Angular orb to key angles
        jup_mc_orb = min(abs(relocated_mc - jupiter_lon), 360 - abs(relocated_mc - jupiter_lon))
        ven_dc_orb = min(abs((relocated_mc + 90.0) % 360.0 - venus_lon), 360 - abs((relocated_mc + 90.0) % 360.0 - venus_lon))
        sun_ac_orb = min(abs((relocated_mc + 270.0) % 360.0 - sun_lon), 360 - abs((relocated_mc + 270.0) % 360.0 - sun_lon))
        sat_ic_orb = min(abs((relocated_mc + 180.0) % 360.0 - saturn_lon), 360 - abs((relocated_mc + 180.0) % 360.0 - saturn_lon))
        
        # Determine dominant planetary line
        lines = []
        if jup_mc_orb < 25.0:
            lines.append({"planet": "Jupiter", "angle": "Midheaven (MC)", "influence": "Executive Renown & Capital Expansion", "resonanceScore": round(100 - jup_mc_orb * 3, 1)})
        if ven_dc_orb < 25.0:
            lines.append({"planet": "Venus", "angle": "Descendant (DC)", "influence": "Harmonious Soulmate & Commercial Alliances", "resonanceScore": round(100 - ven_dc_orb * 3, 1)})
        if sun_ac_orb < 25.0:
            lines.append({"planet": "Sun", "angle": "Ascendant (AC)", "influence": "Vitality, Sovereign Leadership & High Visibility", "resonanceScore": round(100 - sun_ac_orb * 3, 1)})
        if sat_ic_orb < 20.0:
            lines.append({"planet": "Saturn", "angle": "Imum Coeli (IC)", "influence": "Rigorous Disciplinary Sanctuary & Deep Solitude", "resonanceScore": round(100 - sat_ic_orb * 3, 1)})
            
        if not lines:
            lines.append({"planet": "Mercury", "angle": "Midheaven (MC)", "influence": "Trade, Intellectual Media & Digital Connectivity", "resonanceScore": 76.5})
            
        primary_line = max(lines, key=lambda x: x["resonanceScore"])
        
        scored_cities.append({
            "city": city["name"],
            "country": city["country"],
            "latitude": lat,
            "longitude": lon,
            "primaryPlanetaryLine": f"{primary_line['planet']} on {primary_line['angle']}",
            "resonanceScore": primary_line["resonanceScore"],
            "karmicTheme": primary_line["influence"],
            "category": "WEALTH_EXPANSION" if primary_line["planet"] in ["Jupiter", "Sun"] else ("ROMANTIC_SOULMATE" if primary_line["planet"] == "Venus" else "INTELLECTUAL_TRADE")
        })
        
    scored_cities.sort(key=lambda x: x["resonanceScore"], reverse=True)
    top_power_cities = scored_cities[:8]
    
    return {
        "engine": "Vedic Astrocartography & Geodetic Angular Matrix",
        "natalLagna": vedic.get("ascendant", {}).get("sign", "Virgo"),
        "topPowerCities": top_power_cities,
        "planetaryLinesSummary": [
            {
                "line": "Jupiter Midheaven (MC) Line",
                "significance": "Peak commercial success, sovereign software leadership, high institutional recognition.",
                "favorableLocations": [c["city"] for c in top_power_cities if "Jupiter" in c["primaryPlanetaryLine"]][:3] or ["Dubai", "London", "San Francisco"]
            },
            {
                "line": "Venus Descendant (DC) Line",
                "significance": "Profound soulmate attraction, graceful public diplomacy, high-trust collaborative ventures.",
                "favorableLocations": [c["city"] for c in top_power_cities if "Venus" in c["primaryPlanetaryLine"]][:3] or ["Paris", "Kyoto", "Bali"]
            },
            {
                "line": "Sun Ascendant (AC) Line",
                "significance": "Radiant physical vitality, personal charisma, unassailable confidence, and health rejuvenation.",
                "favorableLocations": [c["city"] for c in top_power_cities if "Sun" in c["primaryPlanetaryLine"]][:3] or ["Austin", "Sydney", "Zurich"]
            }
        ],
        "strategicDirective": "For commercial platform scale and institutional funding, align travel or legal headquarters with your Jupiter-MC geodetic vectors. For deep creative synthesis, spend retreat cycles along your Venus and Sun meridians."
    }
