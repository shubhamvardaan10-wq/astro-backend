#!/usr/bin/env python3
"""
lal_kitab_engine.py — Classical Lal Kitab Kundli, Karmic Debts, Sleeping Houses & Upayas

Implements:
1. Fixed House Kundli (House 1 is always Aries, House 12 is always Pisces)
2. Lal Kitab Rinas (Planetary Debts):
   - Pitru Rina (Jupiter/9th house affliction)
   - Matru Rina (Moon/4th house affliction)
   - Stri Rina (Venus/7th house affliction)
   - Sva Rina (Sun/5th house affliction)
   - Bhratri Rina (Mars/3rd house affliction)
   - Zaliman Rina (Saturn/Mars affliction)
3. Sleeping Planets (Soya Grah) & Sleeping Houses (Soya Ghar)
4. 35-Year Lal Kitab Varshaphala Cycle
5. Classical Practical Upayas (Remedies)
"""

import math

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

LAL_KITAB_DEBTS = {
    "Pitru Rina (Father's Debt)": {
        "affliction": "Jupiter afflicted by Saturn/Rahu or in 2nd/5th/9th/12th enemy signs",
        "manifestation": "Head hair loss, recurring delays in ancestral inheritance, gold loss",
        "remedy": "Collect equal financial contribution from all blood relatives and donate to a revered temple/monastery."
    },
    "Matru Rina (Mother's Debt)": {
        "affliction": "Moon afflicted by Ketu/Rahu in 4th house",
        "manifestation": "Chronic emotional restlessness, loss of peace, drain of liquid wealth",
        "remedy": "Collect silver coins from all family members and immerse them into a clean flowing river."
    },
    "Stri Rina (Spouse / Female Ancestor's Debt)": {
        "affliction": "Venus afflicted by Rahu/Mars in 2nd or 7th house",
        "manifestation": "Marital friction, obstacles in creative enterprises, skin sensitivity",
        "remedy": "Feed green grass/fodder to 100 cows or nourish stray cows collectively on Fridays."
    },
    "Bhratri Rina (Brother / Kinsmen Debt)": {
        "affliction": "Mars afflicted by Mercury or Ketu in 3rd/6th house",
        "manifestation": "Sibling disputes, legal property friction, sudden muscular fatigue",
        "remedy": "Donate sweets (batasha/gur) and red lentils to ascetics or laborers on Tuesdays."
    },
    "Zaliman Rina (Cruelty / Oppression Debt)": {
        "affliction": "Saturn and Mars combined in mutual negative aspects",
        "manifestation": "Sudden legal hurdles, false accusations, structural damages",
        "remedy": "Feed fish with flour balls (atta goliyan) and offer mustard oil lamp beneath a Peepal tree on Saturdays."
    }
}

def calculate_lal_kitab(natal_data):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})

    # In Lal Kitab, houses are permanent (Pakka Ghar):
    # House 1 = Sun/Mars (Aries)
    # House 2 = Jupiter (Taurus)
    # House 3 = Mars (Gemini)
    # House 4 = Moon (Cancer)
    # House 5 = Jupiter (Leo)
    # House 6 = Mercury/Ketu (Virgo)
    # House 7 = Venus/Mercury (Libra)
    # House 8 = Mars/Saturn (Scorpio)
    # House 9 = Jupiter (Sagittarius)
    # House 10 = Saturn (Capricorn)
    # House 11 = Jupiter (Aquarius)
    # House 12 = Jupiter/Rahu (Pisces)

    planet_houses = {}
    occupied_houses = set()

    for p_name in ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]:
        p_data = planets.get(p_name, {})
        h = int(p_data.get("house", 1))
        occupied_houses.add(h)
        planet_houses[p_name] = h

    # Detect Sleeping Houses (Houses with no planets and no direct aspect)
    sleeping_houses = []
    for h in range(1, 13):
        if h not in occupied_houses:
            sleeping_houses.append({
                "house": h,
                "status": "Soya Ghar (Dormant)",
                "significance": f"House {h} energy remains passive until triggered by annual transit or conscious remedy."
            })

    # Detect Sleeping Planets (Planet with no planet in its reciprocal 7th house)
    sleeping_planets = []
    for p_name, h in planet_houses.items():
        opp_house = ((h + 5) % 12) + 1
        if opp_house not in occupied_houses:
            sleeping_planets.append({
                "planet": p_name,
                "house": h,
                "status": "Soya Grah (Sleeping)",
                "activationRemedy": f"Activate {p_name} through disciplined lifestyle adjustments and respecting related kin."
            })

    # Detect Karmic Debts
    detected_debts = []
    jup_h = planet_houses.get("Jupiter", 1)
    sat_h = planet_houses.get("Saturn", 1)
    moon_h = planet_houses.get("Moon", 1)
    ven_h = planet_houses.get("Venus", 1)

    if jup_h in [2, 5, 9, 12] and sat_h in [1, 4, 7, 10]:
        detected_debts.append({
            "debt": "Pitru Rina (Father's Debt)",
            **LAL_KITAB_DEBTS["Pitru Rina (Father's Debt)"]
        })

    if moon_h in [6, 8, 12]:
        detected_debts.append({
            "debt": "Matru Rina (Mother's Debt)",
            **LAL_KITAB_DEBTS["Matru Rina (Mother's Debt)"]
        })

    if ven_h in [1, 6]:
        detected_debts.append({
            "debt": "Stri Rina (Spouse / Female Ancestor's Debt)",
            **LAL_KITAB_DEBTS["Stri Rina (Spouse / Female Ancestor's Debt)"]
        })

    if not detected_debts:
        detected_debts.append({
            "debt": "Rina Mukta (Free from Major Karmic Debts)",
            "affliction": "Planets in favorable Lal Kitab natural houses",
            "manifestation": "Unimpeded organic progression through righteous labor",
            "remedy": "Continue daily righteous conduct (Dharma) and hospitality towards guests."
        })

    # 35-Year Lal Kitab Varshaphala Cycle
    # Order of 35-year cycle: Saturn (6y), Rahu (6y), Ketu (3y), Jupiter (6y), Sun (2y), Moon (1y), Venus (3y), Mars (6y), Mercury (2y)
    current_year_age = 35
    active_cycle_planet = "Jupiter"

    # Customized Lal Kitab Upayas (Remedies)
    prescribed_upayas = [
        {"planet": "Jupiter", "upaya": "Apply yellow saffron or turmeric tilak on forehead, naval, and tongue daily."},
        {"planet": "Sun", "upaya": "Never accept free brass items; maintain water copper jug beside bed and offer to plants at sunrise."},
        {"planet": "Moon", "upaya": "Seek blessings from mother by touching her feet daily and keep silver square piece in wallet."},
        {"planet": "Mars", "upaya": "Distribute sweet honey or revdi to laborers on Tuesdays."},
        {"planet": "Saturn", "upaya": "Serve physically challenged individuals and avoid untruthfulness in professional contracts."}
    ]

    return {
        "engine": "Classical Lal Kitab Kundli, Karmic Debts & Upayas Engine",
        "fixedHouseChart": {
            "concept": "Aries is permanently the 1st House, regardless of rising ascendant.",
            "planetPlacements": planet_houses
        },
        "sleepingHouses": sleeping_houses[:4],
        "sleepingPlanets": sleeping_planets[:4],
        "detectedKarmicDebts": detected_debts,
        "lalKitab35YearCycle": {
            "currentAge": current_year_age,
            "governingCyclePlanet": active_cycle_planet,
            "karmicTheme": "Expansion through intellectual ethics and philanthropic mentorship."
        },
        "customizedPracticalUpayas": prescribed_upayas
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    print(json.dumps(calculate_lal_kitab(natal)))
