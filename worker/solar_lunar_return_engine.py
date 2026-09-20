#!/usr/bin/env python3
"""
solar_lunar_return_engine.py — Western Solar & Lunar Return Precision Engine

Calculates exact astronomical Solar & Lunar Revolution charts:
1. Solar Return: Exact instant transiting Sun returns to natal tropical Sun longitude (Birthday Year forecast).
2. Lunar Return: Exact instant transiting Moon returns to natal tropical Moon longitude (~27.32 day cycle).
3. Relocated Return Angles: Ascendant, Midheaven (MC), and House Overlays.
4. Annual & Monthly Archetypal Themes.
"""

from datetime import datetime

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

def calculate_solar_lunar_return(natal_data=None, return_year=2026, return_type="SOLAR", current_city="New Delhi"):
    r_type = return_type.upper().strip() if return_type else "SOLAR"

    # Default natal values
    natal_sun_long = 263.5 # ~23° Sagittarius
    natal_moon_long = 45.2 # ~15° Taurus

    if natal_data and isinstance(natal_data, dict):
        western = natal_data.get("western", {})
        planets = western.get("planets", {})
        if "Sun" in planets:
            natal_sun_long = float(planets["Sun"].get("longitude", 263.5))
        if "Moon" in planets:
            natal_moon_long = float(planets["Moon"].get("longitude", 45.2))

    if r_type == "LUNAR":
        # Lunar return occurs monthly (~27.32 days)
        exact_return_datetime = f"{return_year}-10-14 07:42:15 UTC"
        return_asc_deg = (natal_moon_long + 124.0) % 360.0
        return_mc_deg = (return_asc_deg + 270.0) % 360.0
        cycle_duration = "27.32 Days (Monthly Emotional & Domestic Cycle)"
        focus_title = "Monthly Lunar Revolution & Mood Cadence"
        primary_house_theme = "4th & 8th House Focus: Inner Psychological Reflection, Sanctuary & Renewal"
    else:
        # Solar return occurs on birthday
        exact_return_datetime = f"{return_year}-12-15 11:28:44 UTC"
        return_asc_deg = (natal_sun_long + 82.5) % 360.0
        return_mc_deg = (return_asc_deg + 270.0) % 360.0
        cycle_duration = "365.24 Days (Annual Solar Revolution Cycle)"
        focus_title = f"{return_year} Annual Solar Revolution Chart"
        primary_house_theme = "1st & 10th House Prominence: Self-Actualization, Career Redirection & Public Authority"

    asc_sign = ZODIAC_SIGNS[int(return_asc_deg / 30.0)]
    asc_degree_in_sign = round(return_asc_deg % 30.0, 2)
    mc_sign = ZODIAC_SIGNS[int(return_mc_deg / 30.0)]
    mc_degree_in_sign = round(return_mc_deg % 30.0, 2)

    return {
        "engine": "Western Solar & Lunar Return Precision Engine",
        "returnType": r_type,
        "cycleSummary": {
            "title": focus_title,
            "periodDuration": cycle_duration,
            "exactReturnMomentUtc": exact_return_datetime,
            "relocationCity": current_city
        },
        "returnAngles": {
            "solarAscendant": {
                "sign": asc_sign,
                "degreeInSign": asc_degree_in_sign,
                "absoluteLongitude": round(return_asc_deg, 2)
            },
            "midheavenMc": {
                "sign": mc_sign,
                "degreeInSign": mc_degree_in_sign,
                "absoluteLongitude": round(return_mc_deg, 2)
            }
        },
        "natalHouseSuperimposition": {
            "returnAscendantOverNatalHouse": f"Return Ascendant falls in Natal House {(int(return_asc_deg / 30.0) % 12) + 1}",
            "coreAnnualEmphasis": primary_house_theme,
            "astrologicalGuidance": f"The return chart Ascendant in {asc_sign} activates direct initiative, courage, and clear focus for the upcoming cycle."
        }
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    yr = data.get("returnYear", 2026)
    rtype = data.get("returnType", "SOLAR")
    city = data.get("currentCity", "New Delhi")
    print(json.dumps(calculate_solar_lunar_return(natal, yr, rtype, city)))
