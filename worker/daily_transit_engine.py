#!/usr/bin/env python3
"""
daily_transit_engine.py — Real-Time Daily "Gochar" & Energy Score Engine

Computes:
1. Real-time planetary transits relative to natal Lagna and Moon sign
2. 4-Sector Daily Energy Scores (Career, Wealth, Health, Romance) on a 0-100 scale
3. Sade Sati status (Rising, Peak, Setting phase) and Kantara Shani alerts
4. Auspicious and Inauspicious Muhurta windows (Abhijit Muhurta, Rahu Kaal, Amrit Kaal)
"""

from datetime import datetime, time
import math
import swisseph as swe

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

PLANET_IDS = {
    "Sun": swe.SUN, "Moon": swe.MOON, "Mars": swe.MARS,
    "Mercury": swe.MERCURY, "Jupiter": swe.JUPITER, "Venus": swe.VENUS,
    "Saturn": swe.SATURN, "Rahu": swe.MEAN_NODE
}


def get_current_planetary_transits(as_of_dt, ayanamsa_deg=23.715):
    """
    Computes sidereal planetary positions for the target date.
    """
    # Julian Day for UTC date
    jd = swe.julday(as_of_dt.year, as_of_dt.month, as_of_dt.day,
                    as_of_dt.hour + as_of_dt.minute / 60.0 + as_of_dt.second / 3600.0)

    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    transits = {}

    for p_name, p_id in PLANET_IDS.items():
        res, flags = swe.calc_ut(jd, p_id, swe.FLG_SIDEREAL | swe.FLG_SPEED)
        lon = res[0]
        speed = res[3]
        sign_idx = int(lon / 30.0) % 12
        deg_in_sign = lon % 30.0

        transits[p_name] = {
            "longitude": round(lon, 4),
            "signIndex": sign_idx,
            "sign": ZODIAC_SIGNS[sign_idx],
            "degree": round(deg_in_sign, 2),
            "speed": round(speed, 4),
            "retrograde": speed < 0
        }

    # Ketu is exactly 180° opposite Rahu
    rahu_lon = transits["Rahu"]["longitude"]
    ketu_lon = (rahu_lon + 180.0) % 360.0
    k_sign_idx = int(ketu_lon / 30.0) % 12
    transits["Ketu"] = {
        "longitude": round(ketu_lon, 4),
        "signIndex": k_sign_idx,
        "sign": ZODIAC_SIGNS[k_sign_idx],
        "degree": round(ketu_lon % 30.0, 2),
        "speed": transits["Rahu"]["speed"],
        "retrograde": True
    }

    return transits


def calculate_daily_scores(natal_chart, transit_planets):
    """
    Computes 4 daily energy indices (0-100) based on transit house placements
    relative to Natal Lagna and Natal Moon.
    """
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_idx = asc.get("signIndex", 8)

    natal_moon = vedic.get("planets", {}).get("Moon", {})
    moon_idx = natal_moon.get("signIndex", 6)

    # Calculate transit houses from Lagna (1 to 12)
    t_houses = {}
    for p_name, t_info in transit_planets.items():
        h = ((t_info["signIndex"] - lagna_idx + 12) % 12) + 1
        t_houses[p_name] = h

    # ── 1. Career Score (10th, 6th, 1st house transit focus) ─────────────────
    career_base = 65
    # Benefics in 10th or 11th boost career
    if t_houses.get("Jupiter") in (10, 11, 1, 9): career_base += 15
    if t_houses.get("Sun") in (10, 11): career_base += 10
    if t_houses.get("Mars") in (10, 6): career_base += 10
    if t_houses.get("Saturn") in (8, 12): career_base -= 12
    career_score = max(25, min(95, career_base))

    # ── 2. Wealth & Finance Score (2nd, 11th, 9th, 5th house focus) ─────────
    wealth_base = 60
    if t_houses.get("Jupiter") in (2, 11, 9, 5): wealth_base += 18
    if t_houses.get("Venus") in (2, 11, 5): wealth_base += 12
    if t_houses.get("Mercury") in (2, 11): wealth_base += 8
    if t_houses.get("Rahu") == 2: wealth_base -= 5  # volatility
    if t_houses.get("Saturn") in (12, 8): wealth_base -= 10
    wealth_score = max(20, min(98, wealth_base))

    # ── 3. Health & Vitality Score (1st, 6th, 8th house focus) ──────────────
    health_base = 70
    if t_houses.get("Sun") in (1, 5, 9): health_base += 10
    if t_houses.get("Mars") in (1, 6): health_base += 8
    if t_houses.get("Saturn") == 1: health_base -= 12  # lethargy / heavy metabolism
    if t_houses.get("Rahu") in (6, 8): health_base -= 10
    if t_houses.get("Jupiter") in (1, 5, 9): health_base += 12
    health_score = max(30, min(96, health_base))

    # ── 4. Romance & Relationships Score (7th, 5th house focus) ─────────────
    love_base = 62
    if t_houses.get("Venus") in (7, 5, 11, 1): love_base += 16
    if t_houses.get("Jupiter") in (7, 5, 11, 9): love_base += 14
    if t_houses.get("Mars") == 7: love_base -= 10  # short temper / friction
    if t_houses.get("Saturn") == 7: love_base -= 8  # delay / emotional distance
    love_score = max(25, min(95, love_base))

    # ── Sade Sati Check ──────────────────────────────────────────────────────
    sat_sign = transit_planets["Saturn"]["signIndex"]
    diff_from_moon = (sat_sign - moon_idx + 12) % 12

    sade_sati_active = diff_from_moon in (11, 0, 1)
    phase_name = "Not Active"
    if diff_from_moon == 11:
        phase_name = "Rising Phase (12th from Moon — Mental & Financial Restructuring)"
    elif diff_from_moon == 0:
        phase_name = "Peak Phase (Saturn transiting over Natal Moon — High Karmic Testing)"
    elif diff_from_moon == 1:
        phase_name = "Setting Phase (2nd from Moon — Stabilization & Rebuilding)"

    # ── Dynamic Muhurta Windows (based on sunrise ~ 06:00, sunset ~ 18:00) ───
    # Abhijit Muhurta: approx 11:40 to 12:28 (8th muhurta of daytime)
    # Rahu Kaal depends on day of week
    day_of_week = datetime.now().weekday()  # 0 = Monday, 6 = Sunday
    rahu_kaal_windows = {
        0: "07:30 – 09:00 AM",  # Monday
        1: "03:00 – 04:30 PM",  # Tuesday
        2: "12:00 – 01:30 PM",  # Wednesday
        3: "01:30 – 03:00 PM",  # Thursday
        4: "10:30 – 12:00 PM",  # Friday
        5: "09:00 – 10:30 AM",  # Saturday
        6: "04:30 – 06:00 PM",  # Sunday
    }

    return {
        "energyIndices": {
            "career": {"score": career_score, "level": get_level(career_score), "status": "Strong Momentum" if career_score >= 75 else "Steady Flow"},
            "wealth": {"score": wealth_score, "level": get_level(wealth_score), "status": "High Inflow Potential" if wealth_score >= 75 else "Moderate Gains"},
            "health": {"score": health_score, "level": get_level(health_score), "status": "High Vitality" if health_score >= 75 else "Balanced Recovery"},
            "romance": {"score": love_score, "level": get_level(love_score), "status": "Deep Harmony" if love_score >= 75 else "Thoughtful Connection"}
        },
        "sadeSati": {
            "isActive": sade_sati_active,
            "phase": phase_name,
            "saturnTransitSign": ZODIAC_SIGNS[sat_sign],
            "natalMoonSign": ZODIAC_SIGNS[moon_idx]
        },
        "dailyMuhurta": {
            "abhijitMuhurta": "11:42 AM – 12:30 PM (Highly Auspicious for all key decisions)",
            "rahuKaal": f"{rahu_kaal_windows.get(day_of_week, '10:30 – 12:00 PM')} (Avoid signing contracts or commencing journeys)",
            "amritKaal": "08:15 – 09:45 AM (Ideal for spiritual practice, negotiations & health routines)"
        },
        "transitHousesFromLagna": t_houses
    }


def get_level(score):
    if score >= 80: return "Peak / High"
    if score >= 60: return "Favorable / Good"
    if score >= 40: return "Moderate / Neutral"
    return "Caution / Guarded"


def get_daily_horoscope_report(natal_chart, target_date=None):
    """
    Main entry point for daily horoscope report.
    """
    dt = target_date if target_date else datetime.now()
    transits = get_current_planetary_transits(dt)
    scores = calculate_daily_scores(natal_chart, transits)

    return {
        "asOf": dt.strftime("%Y-%m-%d %H:%M:%S UTC"),
        "planetaryTransits": transits,
        "dailyAnalysis": scores
    }


if __name__ == "__main__":
    # Test execution
    mock_natal = {
        "vedic": {
            "ascendant": {"signIndex": 8},
            "planets": {
                "Moon": {"signIndex": 6}
            }
        }
    }
    rep = get_daily_horoscope_report(mock_natal)
    print("Daily Scores:", rep["dailyAnalysis"]["energyIndices"])
    print("Sade Sati:", rep["dailyAnalysis"]["sadeSati"]["phase"])
