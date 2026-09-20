#!/usr/bin/env python3
"""
progressions_directions_engine.py — Western Secondary Progressions & Solar Arc Directions

Calculates:
1. Secondary Progressions (Day for a year: 1 ephemeris day after birth = 1 tropical year of life)
2. Solar Arc Directions (All planets and angles directed by Sun's daily arc)
3. Progressed Moon monthly shifts (~1°/month, major trigger of life phases)
4. Exact aspect contacts between Progressed/Directed planets and Natal points
"""

import math
from datetime import datetime

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

def calculate_progressions_and_directions(natal_data, target_date_str=None):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    asc = vedic.get("ascendant", {})
    asc_long = float(asc.get("longitude", 240.0))

    # Parse birth date
    dob_str = natal_data.get("dob", "1990-01-15")
    try:
        birth_dt = datetime.strptime(dob_str, "%Y-%m-%d")
    except Exception:
        birth_dt = datetime(1990, 1, 15)

    if target_date_str:
        try:
            target_dt = datetime.strptime(target_date_str, "%Y-%m-%d")
        except Exception:
            target_dt = datetime.now()
    else:
        target_dt = datetime.now()

    days_lived = max(0, (target_dt - birth_dt).days)
    years_lived = days_lived / 365.25

    # 1. Solar Arc: Daily motion of Sun is approx 0.9856 degrees/day.
    # In Solar Arc Directions, every planet moves by (years_lived * 0.9856)°
    solar_arc_delta = (years_lived * 0.9856) % 360.0

    solar_arc_planets = {}
    secondary_progressed = {}
    eligible_planets = ["Sun", "Moon", "Mercury", "Venus", "Mars", "Jupiter", "Saturn"]

    daily_speeds = {
        "Sun": 0.9856,
        "Moon": 13.176,
        "Mercury": 1.25,
        "Venus": 1.15,
        "Mars": 0.524,
        "Jupiter": 0.083,
        "Saturn": 0.033
    }

    # 2. Secondary Progressions:
    # 1 year of life = 1 day of planetary motion
    # Progressed Moon moves 13.176° per year = ~1.1° per month!
    for p in eligible_planets:
        base_long = float(planets.get(p, {}).get("longitude", 0.0))

        # Solar Arc directed longitude
        sa_long = (base_long + solar_arc_delta) % 360.0
        sa_sign_idx = int(sa_long / 30.0) % 12
        solar_arc_planets[p] = {
            "name": p,
            "directedLongitude": round(sa_long, 2),
            "sign": ZODIAC_SIGNS[sa_sign_idx],
            "degreeInSign": round(sa_long % 30.0, 2)
        }

        # Secondary progressed longitude (years_lived * daily_speed)
        sp_delta = (years_lived * daily_speeds.get(p, 1.0)) % 360.0
        sp_long = (base_long + sp_delta) % 360.0
        sp_sign_idx = int(sp_long / 30.0) % 12
        secondary_progressed[p] = {
            "name": p,
            "progressedLongitude": round(sp_long, 2),
            "sign": ZODIAC_SIGNS[sp_sign_idx],
            "degreeInSign": round(sp_long % 30.0, 2),
            "annualMotionRate": f"{daily_speeds.get(p, 1.0):.2f}°/year"
        }

    # Directed Ascendant & Midheaven
    directed_asc = (asc_long + solar_arc_delta) % 360.0
    directed_mc = (directed_asc + 270.0) % 360.0

    # 3. Detect major aspect triggers (within 1.5 degree tight orb)
    active_triggers = []
    for p_name, sp_data in secondary_progressed.items():
        sp_l = sp_data["progressedLongitude"]
        for nat_name in eligible_planets:
            nat_l = float(planets.get(nat_name, {}).get("longitude", 0.0))
            diff = abs(sp_l - nat_l) % 360.0
            if diff > 180.0: diff = 360.0 - diff

            for asp_deg, asp_label in [(0.0, "Conjunction"), (60.0, "Sextile"), (90.0, "Square"), (120.0, "Trine"), (180.0, "Opposition")]:
                orb = abs(diff - asp_deg)
                if orb <= 1.5:
                    active_triggers.append({
                        "progressedPlanet": p_name,
                        "natalPlanet": nat_name,
                        "aspect": asp_label,
                        "orbDegrees": round(orb, 2),
                        "significance": f"Progressed {p_name} forms {asp_label} to Natal {nat_name}: major activation of {p_name.lower()} archetypes."
                    })

    # Progressed Moon Phase (Sun vs Moon progression)
    sp_sun_l = secondary_progressed["Sun"]["progressedLongitude"]
    sp_moon_l = secondary_progressed["Moon"]["progressedLongitude"]
    prog_moon_phase_angle = (sp_moon_l - sp_sun_l) % 360.0
    if prog_moon_phase_angle < 45: phase_name = "Progressed New Moon (Seeds & New Cycle)"
    elif prog_moon_phase_angle < 90: phase_name = "Progressed Crescent Moon (Initial Momentum)"
    elif prog_moon_phase_angle < 135: phase_name = "Progressed First Quarter Moon (Action & Challenge)"
    elif prog_moon_phase_angle < 180: phase_name = "Progressed Gibbous Moon (Refinement)"
    elif prog_moon_phase_angle < 225: phase_name = "Progressed Full Moon (Culmination & Illumination)"
    elif prog_moon_phase_angle < 270: phase_name = "Progressed Disseminating Moon (Sharing Wisdom)"
    elif prog_moon_phase_angle < 315: phase_name = "Progressed Last Quarter Moon (Reorientation)"
    else: phase_name = "Progressed Balsamic Moon (Release & Completion)"

    return {
        "engine": "Western Secondary Progressions & Solar Arc Directions Engine",
        "currentAge": round(years_lived, 2),
        "targetAnalysisDate": target_dt.strftime("%Y-%m-%d"),
        "progressedMoonPhase": {
            "phase": phase_name,
            "phaseAngle": round(prog_moon_phase_angle, 2),
            "significance": "Governs the 29.5-year overarching emotional and developmental life chapter."
        },
        "secondaryProgressedPlanets": secondary_progressed,
        "solarArcDirectedPoints": {
            "directedAscendant": {
                "sign": ZODIAC_SIGNS[int(directed_asc / 30.0) % 12],
                "degree": round(directed_asc % 30.0, 2)
            },
            "directedMidheaven": {
                "sign": ZODIAC_SIGNS[int(directed_mc / 30.0) % 12],
                "degree": round(directed_mc % 30.0, 2)
            },
            "directedPlanets": solar_arc_planets
        },
        "activeMilestoneAspectTriggers": active_triggers[:6],
        "predictiveSynthesis": f"At age {round(years_lived, 1)}, the {phase_name} signals a prime period for structural consolidation and long-term goal setting."
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    td = data.get("targetDate")
    print(json.dumps(calculate_progressions_and_directions(natal, td)))
