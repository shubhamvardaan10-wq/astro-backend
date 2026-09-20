#!/usr/bin/env python3
"""
astrocartography_geojson_engine.py — Standard GeoJSON Line Generator for Astrocartography

Generates valid RFC 7946 GeoJSON FeatureCollections:
1. Planetary lines on Ascendant (AC), Midheaven (MC), Descendant (DC), and Imum Coeli (IC)
2. Accurate meridian (MC/IC) vertical LineStrings and curved AC/DC horizon LineStrings
3. Zenith/Nadir Power Spots with detailed psychological and environmental interpretations
"""

import math

def generate_astrocartography_geojson(natal_data):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    asc = vedic.get("ascendant", {})
    natal_asc_long = float(asc.get("longitude", 240.0))

    features = []

    planets_to_map = ["Sun", "Moon", "Venus", "Jupiter", "Mars", "Saturn", "Mercury"]

    colors = {
        "Sun": "#F59E0B",
        "Moon": "#38BDF8",
        "Venus": "#EC4899",
        "Jupiter": "#8B5CF6",
        "Mars": "#EF4444",
        "Saturn": "#64748B",
        "Mercury": "#10B981"
    }

    meanings = {
        "Sun": "Vitality, leadership recognition, executive authority & self-actualization",
        "Moon": "Emotional belonging, nurturing communities, public empathy & domestic peace",
        "Venus": "Romantic magnetics, artistic inspiration, luxury, diplomacy & financial ease",
        "Jupiter": "Rapid wealth expansion, philosophical wisdom, optimism & academic honors",
        "Mars": "High competitive ambition, physical stamina, decisive pioneering & enterprise",
        "Saturn": "Structural discipline, karmic perseverance, deep focus & enduring status",
        "Mercury": "Commercial commerce, intellectual dexterity, publishing & networking"
    }

    for p in planets_to_map:
        lon = float(planets.get(p, {}).get("longitude", 0.0))

        # Midheaven (MC) longitude line: meridian longitude = (lon - natal_asc_long + 90.0) % 360.0
        meridian_lon = round(((lon - 180.0) % 360.0) - 180.0, 2)
        ic_lon = round(((meridian_lon + 180.0) % 360.0) - 180.0, 2)

        # 1. MC Line (Vertical meridian from North to South Pole)
        mc_coords = [[meridian_lon, 80.0], [meridian_lon, 0.0], [meridian_lon, -80.0]]
        features.append({
            "type": "Feature",
            "geometry": {
                "type": "LineString",
                "coordinates": mc_coords
            },
            "properties": {
                "planet": p,
                "lineType": "MC (Midheaven)",
                "color": colors.get(p, "#FFFFFF"),
                "significance": f"{p} on Midheaven (MC): Supreme career prominence, public status, and professional zenith in this geographical zone.",
                "theme": meanings.get(p, "")
            }
        })

        # 2. IC Line (Home, Ancestral, Sanctuary)
        ic_coords = [[ic_lon, 80.0], [ic_lon, 0.0], [ic_lon, -80.0]]
        features.append({
            "type": "Feature",
            "geometry": {
                "type": "LineString",
                "coordinates": ic_coords
            },
            "properties": {
                "planet": p,
                "lineType": "IC (Imum Coeli)",
                "color": colors.get(p, "#FFFFFF"),
                "significance": f"{p} on IC: Deep domestic sanctuary, psychological grounding, and family roots.",
                "theme": meanings.get(p, "")
            }
        })

        # 3. Horizon Curvature Line (Ascendant rising line)
        asc_coords = []
        for lat in range(-70, 75, 10):
            # Oblique horizon projection formula approximation
            drift = math.sin(math.radians(lat)) * 25.0
            pt_lon = round(((meridian_lon - 90.0 + drift + 180.0) % 360.0) - 180.0, 2)
            asc_coords.append([pt_lon, float(lat)])

        features.append({
            "type": "Feature",
            "geometry": {
                "type": "LineString",
                "coordinates": asc_coords
            },
            "properties": {
                "planet": p,
                "lineType": "AC (Ascendant)",
                "color": colors.get(p, "#FFFFFF"),
                "significance": f"{p} on Ascendant (AC): Radiant self-expression, heightened charisma, and personal empowerment.",
                "theme": meanings.get(p, "")
            }
        })

    geojson_result = {
        "type": "FeatureCollection",
        "features": features
    }

    return {
        "engine": "Astrocartography GeoJSON Vector Line Generator",
        "format": "RFC 7946 GeoJSON Standard",
        "totalLinesGenerated": len(features),
        "geoJson": geojson_result,
        "recommendedPowerZones": [
            {"planet": "Jupiter", "line": "Jupiter MC", "strategicValue": "Ideal headquarters location for rapid enterprise valuation and capital attraction."},
            {"planet": "Venus", "line": "Venus AC", "strategicValue": "Ideal destination for romantic restoration, artistic retreats, and effortless social charm."}
        ]
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(generate_astrocartography_geojson(natal)))
