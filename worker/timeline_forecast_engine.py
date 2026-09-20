#!/usr/bin/env python3
"""
timeline_forecast_engine.py — High-Granularity Monthly Life Event Timing & Scoring Engine

Synthesizes:
1. Real-time planetary transits (Gochara via Swiss Ephemeris)
2. Natal Lagna, Moon sign, and house rulerships
3. Aspectual transit triggers (Jupiter, Saturn, Rahu, Ketu, Mars)
4. Domain-specific scoring (0-100) for Career, Wealth, Love/Marriage, and Health
5. Categorizes each month into Peak Opportunities, Steady Progress, or Caution Windows
"""

import math
from datetime import datetime, timedelta
import swisseph as swe

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

SIGN_LORDS = {
    0: "Mars", 1: "Venus", 2: "Mercury", 3: "Moon",
    4: "Sun", 5: "Mercury", 6: "Venus", 7: "Mars",
    8: "Jupiter", 9: "Saturn", 10: "Saturn", 11: "Jupiter"
}

def get_house(planet_sign_idx, lagna_sign_idx):
    """Calculates whole-sign house (1 to 12) from reference sign."""
    return ((planet_sign_idx - lagna_sign_idx + 12) % 12) + 1

def compute_monthly_forecast(natal_chart, horizon_months=12, start_year=None, start_month=None):
    """
    Computes month-by-month trajectory scoring for the specified horizon (12-36 months).
    """
    now = datetime.now()
    cur_year = start_year if start_year else now.year
    cur_month = start_month if start_month else now.month

    # Extract natal reference points
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_raw = asc.get("signIndex", 8)
    # Handle both 1-based (from Java) and 0-based indexing
    lagna_idx = ((lagna_raw - 1) % 12) if (isinstance(lagna_raw, int) and lagna_raw > 11) else (lagna_raw % 12)
    lagna_name = asc.get("sign") or ZODIAC_SIGNS[lagna_idx]

    planets = vedic.get("planets", {})
    natal_moon = planets.get("Moon", {})
    moon_raw = natal_moon.get("signIndex", 6)
    moon_idx = ((moon_raw - 1) % 12) if (isinstance(moon_raw, int) and moon_raw > 11) else (moon_raw % 12)
    moon_name = natal_moon.get("sign") or ZODIAC_SIGNS[moon_idx]

    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)

    timeline = []
    peak_windows = []
    caution_windows = []

    for m in range(horizon_months):
        target_month = (cur_month - 1 + m) % 12 + 1
        target_year = cur_year + (cur_month - 1 + m) // 12
        month_label = f"{target_year:04d}-{target_month:02d}"

        # Mid-month calculation date
        jd = swe.julday(target_year, target_month, 15, 12.0)

        # Calculate transiting planetary positions
        def calc_planet(pid):
            res, _ = swe.calc_ut(jd, pid, swe.FLG_SIDEREAL)
            idx = int(res[0] / 30.0) % 12
            return idx, res[0] % 30.0

        jup_idx, jup_deg = calc_planet(swe.JUPITER)
        sat_idx, sat_deg = calc_planet(swe.SATURN)
        rah_idx, rah_deg = calc_planet(swe.TRUE_NODE)
        ket_idx = (rah_idx + 6) % 12
        mar_idx, mar_deg = calc_planet(swe.MARS)
        sun_idx, sun_deg = calc_planet(swe.SUN)
        ven_idx, ven_deg = calc_planet(swe.VENUS)
        mer_idx, mer_deg = calc_planet(swe.MERCURY)
        moo_idx, moo_deg = calc_planet(swe.MOON)

        # House placements relative to Lagna
        jup_h_lagna = get_house(jup_idx, lagna_idx)
        sat_h_lagna = get_house(sat_idx, lagna_idx)
        rah_h_lagna = get_house(rah_idx, lagna_idx)
        mar_h_lagna = get_house(mar_idx, lagna_idx)
        ven_h_lagna = get_house(ven_idx, lagna_idx)
        sun_h_lagna = get_house(sun_idx, lagna_idx)

        # House placements relative to Moon
        jup_h_moon = get_house(jup_idx, moon_idx)
        sat_h_moon = get_house(sat_idx, moon_idx)
        moo_h_moon = get_house(moo_idx, moon_idx)

        # ── 1. Career Score (0-100) ──────────────────────────────────────────
        c_score = 65
        c_triggers = []
        # Jupiter influencing 10th or 1st house
        if jup_h_lagna in (10, 1) or jup_h_lagna in (2, 6): # 5th/9th trine aspect on 10th
            c_score += 20
            c_triggers.append(f"Benefic Jupiter transiting house {jup_h_lagna} boosts professional status and executive authority.")
        # Sun or Mars in 10th house gives Digbala
        if sun_h_lagna == 10:
            c_score += 15
            c_triggers.append("Sun transits 10th house of Karma & Governance (peak directional strength).")
        if mar_h_lagna == 10:
            c_score += 12
            c_triggers.append("Mars transits 10th house providing bold leadership and execution drive.")
        # Saturn in 11th house of gains or 10th house
        if sat_h_lagna == 11:
            c_score += 10
            c_triggers.append("Saturn in 11th house supports structured institutional recognition.")
        elif sat_h_lagna == 8:
            c_score -= 12
            c_triggers.append("Saturn transiting 8th house suggests organizational restructuring and patience.")
        career_score = min(98, max(30, c_score))

        # ── 2. Wealth & Finance Score (0-100) ─────────────────────────────────
        w_score = 62
        w_triggers = []
        if jup_h_lagna in (2, 11, 9) or jup_h_moon in (2, 11, 9):
            w_score += 24
            w_triggers.append("Jupiter activates 2nd/11th Dhana & Labha houses (prime capital accumulation window).")
        if ven_h_lagna in (2, 11, 5, 9):
            w_score += 12
            w_triggers.append("Venus brings liquid financial gains and commercial prosperity.")
        if sat_h_lagna == 11:
            w_score += 14
            w_triggers.append("Saturn in 11th house anchors long-term recurring revenue streams.")
        if rah_h_lagna in (11, 2):
            w_score += 8
            w_triggers.append("Rahu in financial houses triggers unconventional windfall opportunities.")
        elif sat_h_lagna == 12 or mar_h_lagna == 12:
            w_score -= 15
            w_triggers.append("12th house transit flags elevated capital expenditures and asset reallocation.")
        wealth_score = min(99, max(30, w_score))

        # ── 3. Love & Relationship Score (0-100) ─────────────────────────────
        l_score = 60
        l_triggers = []
        # Jupiter aspecting or occupying 7th house (Partnership)
        if jup_h_lagna == 7 or jup_h_lagna in (1, 3, 11): # 1st, 3rd, 11th cast aspects on 7th
            l_score += 26
            l_triggers.append("Guru Drishti (Jupiter aspect) illuminates the 7th house of marriage and sacred union.")
        if ven_h_lagna in (1, 5, 7, 9):
            l_score += 18
            l_triggers.append("Venus transit in auspicious Kendra/Trikona fosters romantic harmony and mutual empathy.")
        if sat_h_lagna == 7:
            l_score -= 10
            l_triggers.append("Saturn in 7th house emphasizes duty, patience, and realistic relationship agreements.")
        love_score = min(96, max(30, l_score))

        # ── 4. Health & Vitality Score (0-100) ────────────────────────────────
        h_score = 75
        h_triggers = []
        if jup_h_lagna in (1, 5, 9):
            h_score += 15
            h_triggers.append("Jupiter supports strong vital immunity and cellular regeneration.")
        if sat_h_lagna in (6, 8) or sat_h_moon in (1, 8):
            h_score -= 14
            h_triggers.append("Saturn transit prompts disciplined metabolic and nervous system recuperation.")
        if mar_h_lagna == 8:
            h_score -= 15
            h_triggers.append("Mars in 8th house suggests avoiding physical over-exertion or reckless driving.")
        # Chandrashtama (Moon in 8th from Moon)
        if moo_h_moon == 8:
            h_score -= 10
            h_triggers.append("Chandrashtama cycle during the month; prioritize emotional equilibrium and restorative sleep.")
        health_score = min(95, max(35, h_score))

        # Composite monthly average
        overall = round((career_score * 0.35 + wealth_score * 0.30 + love_score * 0.20 + health_score * 0.15), 1)

        # Classification
        if overall >= 80:
            status = "PEAK_OPPORTUNITY"
            verdict = "Exceptional golden window for strategic leaps, major agreements, and bold personal initiatives."
            peak_windows.append(month_label)
        elif overall >= 68:
            status = "STEADY_PROGRESS"
            verdict = "Constructive momentum and steady forward consolidation across personal and professional goals."
        elif overall >= 55:
            status = "NEUTRAL_TRANSITION"
            verdict = "Balanced transition period; ideal for planning, self-care, and internal preparation."
        else:
            status = "CAUTION_REQUIRED"
            verdict = "Caution period: defer high-stakes financial speculations and emphasize health boundaries."
            caution_windows.append(month_label)

        timeline.append({
            "month": month_label,
            "overallScore": overall,
            "status": status,
            "domainScores": {
                "career": career_score,
                "wealth": wealth_score,
                "loveAndMarriage": love_score,
                "healthAndVitality": health_score
            },
            "majorTransits": {
                "jupiter": f"{ZODIAC_SIGNS[jup_idx]} (House {jup_h_lagna})",
                "saturn": f"{ZODIAC_SIGNS[sat_idx]} (House {sat_h_lagna})",
                "rahu": f"{ZODIAC_SIGNS[rah_idx]} (House {rah_h_lagna})",
                "ketu": f"{ZODIAC_SIGNS[ket_idx]}",
                "mars": f"{ZODIAC_SIGNS[mar_idx]}"
            },
            "verdict": verdict,
            "keyTriggers": c_triggers + w_triggers + l_triggers + h_triggers
        })

    # Summary synthesis
    return {
        "engine": "Astro-Backend Life Event Timing & Monthly Scoring Engine",
        "horizonMonths": horizon_months,
        "natalReference": {
            "ascendant": lagna_name,
            "natalMoonSign": moon_name
        },
        "executiveSummary": {
            "topPeakWindows": peak_windows[:3] if peak_windows else ["Upcoming quarter steady progress"],
            "cautionPeriods": caution_windows[:2] if caution_windows else ["None — smooth planetary flow throughout"],
            "yearlyAverageScores": {
                "career": round(sum(m["domainScores"]["career"] for m in timeline) / len(timeline), 1),
                "wealth": round(sum(m["domainScores"]["wealth"] for m in timeline) / len(timeline), 1),
                "love": round(sum(m["domainScores"]["loveAndMarriage"] for m in timeline) / len(timeline), 1),
                "health": round(sum(m["domainScores"]["healthAndVitality"] for m in timeline) / len(timeline), 1)
            }
        },
        "monthlyForecast": timeline
    }

if __name__ == "__main__":
    import json
    sample_natal = {
        "vedic": {
            "ascendant": {"sign": "Sagittarius", "signIndex": 8},
            "planets": {
                "Moon": {"sign": "Libra", "signIndex": 6},
                "Sun": {"sign": "Sagittarius", "signIndex": 8}
            }
        }
    }
    res = compute_monthly_forecast(sample_natal, 12)
    print(json.dumps(res, indent=2))
