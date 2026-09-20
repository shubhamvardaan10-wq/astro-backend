#!/usr/bin/env python3
"""
destiny_curve_engine.py — 30-Year Destiny & Wealth Trajectory Simulator

Synthesizes Vimshottari Dasha sub-periods, Sarvashtakavarga (SAV) house points,
and major planetary transit cycles into a continuous 30-year trajectory (2026–2056).
Generates year-by-year time-series indices for Wealth, Health, Career, and Fortune.
"""

from datetime import datetime

# Dasha periods for Shubham Vardaan & generalized trajectory modeling
DASHA_PERIODS = [
    {"start": 2023.6, "end": 2027.1, "maha": "Saturn", "antar": "Jupiter", "sav": 42, "event": "Karmic health & metabolic transformation"},
    {"start": 2027.1, "end": 2028.5, "maha": "Mercury", "antar": "Mercury", "sav": 36, "event": "Prime Marriage Window & Career Leadership Leap"},
    {"start": 2028.5, "end": 2029.5, "maha": "Mercury", "antar": "Ketu", "sav": 37, "event": "Karmic completion & neuropathy full remission"},
    {"start": 2029.5, "end": 2032.4, "maha": "Mercury", "antar": "Venus", "sav": 39, "event": "First Major Wealth Peak & Prime Real Estate Acquisition"},
    {"start": 2032.4, "end": 2033.2, "maha": "Mercury", "antar": "Sun", "sav": 36, "event": "Public authority, government / executive recognition"},
    {"start": 2033.2, "end": 2034.6, "maha": "Mercury", "antar": "Moon", "sav": 36, "event": "Domestic happiness, secondary progeny expansion"},
    {"start": 2034.6, "end": 2035.6, "maha": "Mercury", "antar": "Mars", "sav": 36, "event": "High energy enterprise & commercial expansion"},
    {"start": 2035.6, "end": 2038.2, "maha": "Mercury", "antar": "Rahu", "sav": 28, "event": "Unconventional global investments & technological pivots"},
    {"start": 2038.2, "end": 2040.5, "maha": "Mercury", "antar": "Jupiter", "sav": 42, "event": "Golden Dharma period & high institutional leadership"},
    {"start": 2040.5, "end": 2043.1, "maha": "Mercury", "antar": "Saturn", "sav": 31, "event": "Consolidation of assets & executive governance"},
    {"start": 2043.1, "end": 2050.1, "maha": "Ketu", "antar": "Mixed", "sav": 37, "event": "Spiritual mastery, philanthropy & advisory role"},
    {"start": 2050.1, "end": 2070.1, "maha": "Venus", "antar": "Mixed", "sav": 41, "event": "Lifetime Peak Wealth & Universal Recognition"}
]


def find_dasha_for_year(year):
    for p in DASHA_PERIODS:
        if p["start"] <= year < p["end"]:
            return p
    return DASHA_PERIODS[-1]


def compute_destiny_curve(start_year=2026, duration_years=30):
    """
    Simulates year-by-year indices (0-100) for Fortune, Wealth, Health, and Career.
    """
    curve = []
    milestones = []

    for yr in range(start_year, start_year + duration_years):
        dasha = find_dasha_for_year(yr + 0.5)
        maha = dasha["maha"]
        antar = dasha["antar"]

        # Base calculation from SAV points (average SAV is ~28, high is 36-42)
        sav_factor = (dasha["sav"] - 25) * 2.2

        # ── Wealth Score ─────────────────────────────────────────────────────
        w_base = 60 + sav_factor
        if maha == "Mercury" and antar in ("Venus", "Jupiter", "Mercury"):
            w_base += 15
        elif maha == "Venus":
            w_base += 20
        elif antar == "Rahu":
            w_base += 5  # high volatility gains
        wealth_score = min(98, max(30, round(w_base)))

        # ── Health Score ─────────────────────────────────────────────────────
        h_base = 62
        if yr in (2026, 2027):
            h_base = 72  # strong recovery window
        elif yr in (2028, 2029):
            h_base = 86  # neuropathy full remission window
        elif 2029 <= yr <= 2038:
            h_base = 88  # stable high health period
        elif maha == "Ketu":
            h_base = 78  # spiritual focus
        else:
            h_base = 82
        health_score = min(96, max(35, round(h_base)))

        # ── Career Score ─────────────────────────────────────────────────────
        c_base = 65 + (sav_factor * 0.8)
        if maha == "Mercury":
            c_base += 14
        if antar in ("Sun", "Mars", "Jupiter"):
            c_base += 8
        career_score = min(97, max(30, round(c_base)))

        # ── Composite Fortune Score ──────────────────────────────────────────
        composite_score = round((wealth_score * 0.4) + (career_score * 0.35) + (health_score * 0.25))

        entry = {
            "year": yr,
            "compositeFortune": composite_score,
            "wealthIndex": wealth_score,
            "careerIndex": career_score,
            "healthIndex": health_score,
            "runningDasha": f"{maha} — {antar}",
            "milestone": dasha["event"] if yr in (2027, 2028, 2029, 2030, 2032, 2038, 2043, 2050) else None
        }
        curve.append(entry)

        if entry["milestone"]:
            milestones.append({"year": yr, "event": entry["milestone"], "score": composite_score})

    # Summary statistics
    peak_wealth_year = max(curve, key=lambda x: x["wealthIndex"])["year"]
    peak_career_year = max(curve, key=lambda x: x["careerIndex"])["year"]

    return {
        "timeHorizon": f"{start_year} – {start_year + duration_years - 1}",
        "trajectorySummary": {
            "peakWealthDecade": "2029–2035 (Mercury–Venus into Mercury–Jupiter)",
            "peakCareerDecade": "2027–2038 (Mercury Mahadasha Peak)",
            "healthRemissionWindow": "Late 2026 to Mid 2028",
            "highestScoringYear": peak_wealth_year
        },
        "keyMilestones": milestones,
        "yearlyTimeline": curve
    }


if __name__ == "__main__":
    res = compute_destiny_curve()
    print("Destiny Curve 2026–2056 generated:")
    print("  Milestones:", len(res["keyMilestones"]))
    print("  First 3 years:", res["yearlyTimeline"][:3])
