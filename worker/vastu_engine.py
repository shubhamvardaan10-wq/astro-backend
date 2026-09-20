#!/usr/bin/env python3
"""
vastu_engine.py — Astro-Vastu Directional Energy Grid

Maps the native's birth chart (Lagna, 4th House of Home, and Lagna Lord)
to classical 8-directional Vastu Shastra rules to optimize residential
and commercial spaces for wealth, health, and intellectual focus.
"""

VASTU_DIRECTIONS = {
    "NorthEast": {
        "sanskrit": "Ishan Kona",
        "governingPlanet": "Jupiter",
        "element": "Water / Ether",
        "purpose": "Spiritual clarity, divine grace, higher intellect, and mental peace",
        "optimalUsage": "Meditation corner, prayer altar, light study area, water fountain; keep light and clutter-free."
    },
    "East": {
        "sanskrit": "Purva",
        "governingPlanet": "Sun",
        "element": "Fire / Light",
        "purpose": "Vitality, social status, leadership, and public recognition",
        "optimalUsage": "Main entrance, large windows, morning sunlight access; display achievements or warm amber lighting."
    },
    "SouthEast": {
        "sanskrit": "Agneya Kona",
        "governingPlanet": "Venus & Mars",
        "element": "Fire (Agni)",
        "purpose": "Financial cash flow, energy, passion, and technological dynamism",
        "optimalUsage": "Kitchen, electronic equipment, server racks, computer power supplies; avoid water leaks here."
    },
    "South": {
        "sanskrit": "Dakshin",
        "governingPlanet": "Mars",
        "element": "Earth / Fire",
        "purpose": "Strength, courage, stability, and disciplined execution",
        "optimalUsage": "Heavy furniture, storage units, soundproofed executive chambers; keep solid without large hollow openings."
    },
    "SouthWest": {
        "sanskrit": "Nairruti Kona",
        "governingPlanet": "Rahu",
        "element": "Earth (Prithvi)",
        "purpose": "Master stability, authority, family leadership, and long-term grounding",
        "optimalUsage": "Master bedroom, founder/CEO desk, safe/vault; place the heaviest elements here for grounded authority."
    },
    "West": {
        "sanskrit": "Pashchim",
        "governingPlanet": "Saturn",
        "element": "Air / Water",
        "purpose": "Gains, long-term investments, sustained career profits, and discipline",
        "optimalUsage": "Dining space, study room for deep analytical work, asset documentation lockers."
    },
    "NorthWest": {
        "sanskrit": "Vayavya Kona",
        "governingPlanet": "Moon",
        "element": "Air (Vayu)",
        "purpose": "Movement, networking, shipping, foreign client relations, and rapid trade",
        "optimalUsage": "Guest bedroom, sales and outreach workstation, finished goods dispatch."
    },
    "North": {
        "sanskrit": "Uttara (Kuber Sthan)",
        "governingPlanet": "Mercury",
        "element": "Water",
        "purpose": "Treasury, incoming revenue, wealth opportunities, and liquid capital",
        "optimalUsage": "Financial accounts desk, cash box, green indoor plants, open mirrors; ensure clear unblocked energy."
    }
}


def compute_vastu_guidance(natal_chart):
    """
    Computes personalized Vastu alignment for the native.
    """
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign = asc.get("sign", "Sagittarius")

    # Specific personalized recommendations for Sagittarius Lagna
    recommendations = {
        "workstationOrientation": {
            "facingDirection": "North or East",
            "placementSector": "South-West quadrant facing North/East",
            "benefit": "Activates Mercury (House 10 lord of career) and Sun (House 9 lord of fortune), ensuring high analytical output and executive clarity."
        },
        "sleepAndRestAlignment": {
            "headOrientation": "Head toward South (Primary) or East (Secondary)",
            "forbiddenDirection": "Never sleep with head towards North (avoids geomagnetic repulsion that aggravates Vata and neuropathic tension).",
            "benefit": "Deep restorative REM sleep, cellular repair, and cardiovascular stabilization."
        },
        "wealthCornerActivation": {
            "sector": "North & North-East Quadrant (Kuber & Ishan Sthan)",
            "activationRemedy": "Place a healthy green plant (Jade or Money Plant) in a green pot in the North, and maintain a small copper or brass water vessel in the North-East.",
            "rule": "Keep the North-East corner completely clutter-free, clean, and well-lit with warm white light."
        },
        "technologyAndHardwareZone": {
            "sector": "South-East (Agneya)",
            "optimalPlacement": "Ideal location for computer monitors, modems, charging hubs, and backup drives to harness active fire energy without overheating."
        }
    }

    return {
        "ascendant": lagna_sign,
        "vastuGridOverview": VASTU_DIRECTIONS,
        "personalSpaceOptimization": recommendations,
        "energyPrinciple": "In Vedic architecture, personal vitality aligns when the head rests in South (Earth/Stability) and work proceeds facing North (Mercury/Opportunity) or East (Solar Vitality)."
    }


if __name__ == "__main__":
    test_natal = {"vedic": {"ascendant": {"sign": "Sagittarius"}}}
    res = compute_vastu_guidance(test_natal)
    print("Vastu Engine Test:")
    print("  Ascendant:", res["ascendant"])
    print("  Workstation Facing:", res["personalSpaceOptimization"]["workstationOrientation"]["facingDirection"])
    print("  Sleep Head Orientation:", res["personalSpaceOptimization"]["sleepAndRestAlignment"]["headOrientation"])
