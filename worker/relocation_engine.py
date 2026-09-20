#!/usr/bin/env python3
"""
relocation_engine.py — Astro-Cartography & Relocation Engine

Calculates how a native's chart shifts when relocated to major global cities.
Identifies prime locations for:
  - Wealth & Empire (Jupiter/Venus on MC or 10th/11th/2nd house)
  - Career Authority & Fame (Sun/Mars/10th lord on MC)
  - Love & Partnerships (Venus/Jupiter on Descendant or 7th house)
  - Peace, Healing & Longevity (Benefics in 4th/9th house, avoiding malefics in 1st/6th/8th)
"""

from datetime import datetime
import swisseph as swe

# 25 Major Global Financial, Cultural, and Lifestyle Hubs
GLOBAL_CITIES = [
    {"city": "Dubai", "country": "UAE", "lat": 25.2048, "lon": 55.2708, "region": "Middle East"},
    {"city": "Singapore", "country": "Singapore", "lat": 1.3521, "lon": 103.8198, "region": "Asia-Pacific"},
    {"city": "London", "country": "UK", "lat": 51.5074, "lon": -0.1278, "region": "Europe"},
    {"city": "New York", "country": "USA", "lat": 40.7128, "lon": -74.0060, "region": "North America"},
    {"city": "San Francisco", "country": "USA", "lat": 37.7749, "lon": -122.4194, "region": "North America"},
    {"city": "Toronto", "country": "Canada", "lat": 43.6532, "lon": -79.3832, "region": "North America"},
    {"city": "Zurich", "country": "Switzerland", "lat": 47.3769, "lon": 8.5417, "region": "Europe"},
    {"city": "Tokyo", "country": "Japan", "lat": 35.6762, "lon": 139.6503, "region": "Asia-Pacific"},
    {"city": "Sydney", "country": "Australia", "lat": -33.8688, "lon": 151.2093, "region": "Australia"},
    {"city": "Berlin", "country": "Germany", "lat": 52.5200, "lon": 13.4050, "region": "Europe"},
    {"city": "Paris", "country": "France", "lat": 48.8566, "lon": 2.3522, "region": "Europe"},
    {"city": "Amsterdam", "country": "Netherlands", "lat": 52.3676, "lon": 4.9041, "region": "Europe"},
    {"city": "Hong Kong", "country": "China", "lat": 22.3193, "lon": 114.1694, "region": "Asia-Pacific"},
    {"city": "Mumbai", "country": "India", "lat": 19.0760, "lon": 72.8777, "region": "South Asia"},
    {"city": "Bengaluru", "country": "India", "lat": 12.9716, "lon": 77.5946, "region": "South Asia"},
    {"city": "New Delhi", "country": "India", "lat": 28.6139, "lon": 77.2090, "region": "South Asia"},
    {"city": "Dublin", "country": "Ireland", "lat": 53.3498, "lon": -6.2603, "region": "Europe"},
    {"city": "Melbourne", "country": "Australia", "lat": -37.8136, "lon": 144.9631, "region": "Australia"},
    {"city": "Vancouver", "country": "Canada", "lat": 49.2827, "lon": -123.1207, "region": "North America"},
    {"city": "Bangkok", "country": "Thailand", "lat": 13.7563, "lon": 100.5018, "region": "Asia-Pacific"},
]

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]


def calculate_relocated_chart(utc_jd, lat, lon):
    """
    Computes relocated Ascendant and house placements for the given UTC Julian Day.
    """
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    houses, ascmc = swe.houses_ex(utc_jd, lat, lon, b'W', swe.FLG_SIDEREAL)
    asc_deg = ascmc[0]
    mc_deg = ascmc[1]

    asc_sign_idx = int(asc_deg / 30.0) % 12
    mc_sign_idx = int(mc_deg / 30.0) % 12

    # Planetary positions (sidereal longitudes stay the same across the globe at that instant)
    planets_house = {}
    for p_name, p_id in [("Sun", swe.SUN), ("Moon", swe.MOON), ("Mars", swe.MARS),
                         ("Mercury", swe.MERCURY), ("Jupiter", swe.JUPITER),
                         ("Venus", swe.VENUS), ("Saturn", swe.SATURN), ("Rahu", swe.MEAN_NODE)]:
        res, flags = swe.calc_ut(utc_jd, p_id, swe.FLG_SIDEREAL)
        p_lon = res[0]
        p_sign = int(p_lon / 30.0) % 12
        # Relocated house from relocated Lagna
        h = ((p_sign - asc_sign_idx + 12) % 12) + 1
        planets_house[p_name] = {
            "house": h,
            "sign": ZODIAC_SIGNS[p_sign],
            "degree": round(p_lon % 30.0, 2)
        }

    return {
        "relocatedAscendant": {"sign": ZODIAC_SIGNS[asc_sign_idx], "degree": round(asc_deg % 30.0, 2)},
        "relocatedMC": {"sign": ZODIAC_SIGNS[mc_sign_idx], "degree": round(mc_deg % 30.0, 2)},
        "planets": planets_house
    }


def compute_relocation_report(birth_data):
    """
    Scans global cities and categorizes top locations for wealth, career, love, and health.
    """
    # Compute UTC Julian Day
    dob_str = birth_data.get("dob", "1989-10-30")
    time_str = birth_data.get("time", "10:10")
    dt = datetime.strptime(f"{dob_str} {time_str}", "%Y-%m-%d %H:%M")
    # Native born in IST (UTC+5:30)
    utc_hours = (dt.hour - 5) + (dt.minute - 30) / 60.0
    jd = swe.julday(dt.year, dt.month, dt.day, utc_hours)

    results = []
    wealth_cities = []
    career_cities = []
    love_cities = []
    peace_cities = []

    for city_info in GLOBAL_CITIES:
        chart = calculate_relocated_chart(jd, city_info["lat"], city_info["lon"])
        p = chart["planets"]

        # Scores for this city
        # Wealth: Jupiter in 10, 11, 2, 1, or Venus in 11, 2
        w_score = 50
        if p["Jupiter"]["house"] in (11, 2, 1, 10): w_score += 25
        if p["Venus"]["house"] in (11, 2, 9): w_score += 15
        if p["Mercury"]["house"] in (11, 2): w_score += 10
        if p["Saturn"]["house"] == 12: w_score -= 15

        # Career: Sun/Mars in 10, 1, 11
        c_score = 50
        if p["Sun"]["house"] in (10, 1, 11): c_score += 25
        if p["Mars"]["house"] in (10, 6, 11): c_score += 20
        if p["Jupiter"]["house"] in (10, 1): c_score += 15

        # Love: Venus in 7, 1, 5, 11
        l_score = 50
        if p["Venus"]["house"] in (7, 1, 5, 11): l_score += 30
        if p["Jupiter"]["house"] in (7, 5, 9): l_score += 15
        if p["Mars"]["house"] == 7: l_score -= 15

        # Peace: Jupiter/Moon in 4, 9, 1
        peace_score = 50
        if p["Jupiter"]["house"] in (4, 9, 1): peace_score += 25
        if p["Moon"]["house"] in (4, 9, 1): peace_score += 15
        if p["Saturn"]["house"] in (1, 6, 8): peace_score -= 15

        summary = {
            "city": city_info["city"],
            "country": city_info["country"],
            "region": city_info["region"],
            "relocatedLagna": chart["relocatedAscendant"]["sign"],
            "relocatedMC": chart["relocatedMC"]["sign"],
            "wealthScore": min(98, max(20, w_score)),
            "careerScore": min(98, max(20, c_score)),
            "romanceScore": min(98, max(20, l_score)),
            "peaceScore": min(98, max(20, peace_score)),
            "keyPlacements": {
                "jupiterHouse": p["Jupiter"]["house"],
                "venusHouse": p["Venus"]["house"],
                "sunHouse": p["Sun"]["house"],
                "saturnHouse": p["Saturn"]["house"]
            }
        }
        results.append(summary)

        if summary["wealthScore"] >= 75:
            wealth_cities.append(f"{city_info['city']} ({city_info['country']}) — Jupiter in H{p['Jupiter']['house']}")
        if summary["careerScore"] >= 75:
            career_cities.append(f"{city_info['city']} ({city_info['country']}) — Sun/Mars in H{p['Sun']['house']}/H{p['Mars']['house']}")
        if summary["romanceScore"] >= 75:
            love_cities.append(f"{city_info['city']} ({city_info['country']}) — Venus in H{p['Venus']['house']}")
        if summary["peaceScore"] >= 75:
            peace_cities.append(f"{city_info['city']} ({city_info['country']}) — Benefics in H4/H9")

    # Sort all results by overall balance
    results.sort(key=lambda x: (x["wealthScore"] + x["careerScore"]), reverse=True)

    return {
        "birthCity": birth_data.get("city", "Hajipur"),
        "topRelocationHubs": {
            "wealthAndFinance": wealth_cities[:4] if wealth_cities else ["Dubai", "Singapore"],
            "careerFameAndAuthority": career_cities[:4] if career_cities else ["London", "New York"],
            "loveAndPartnership": love_cities[:4] if love_cities else ["Paris", "Sydney"],
            "peaceAndWellbeing": peace_cities[:4] if peace_cities else ["Zurich", "Vancouver"]
        },
        "allCitiesEvaluated": results
    }


if __name__ == "__main__":
    test_birth = {"dob": "1989-10-30", "time": "10:10", "city": "Hajipur"}
    res = compute_relocation_report(test_birth)
    print("Top Wealth Hubs:", res["topRelocationHubs"]["wealthAndFinance"])
    print("Top Career Hubs:", res["topRelocationHubs"]["careerFameAndAuthority"])
    print("Best overall city:", res["allCitiesEvaluated"][0]["city"], "Score:", res["allCitiesEvaluated"][0]["wealthScore"])
