#!/usr/bin/env python3
"""
tara_bala_engine.py — Navatara Chakra & Daily Tara Bala / Chandra Bala Engine

Evaluates lunar transit compatibility against the birth chart:
1. 9 Taras across 3 Paryayas (27 Nakshatras):
   Janma, Sampat, Vipat, Kshema, Pratyak, Sadhana, Naidhana (Vadha), Mitra, Parama Mitra
2. Chandra Bala: House distance of transit Moon from natal Moon (1, 3, 6, 7, 10, 11 favorable; 8th is Chandra Ashtama)
3. Daily Actionability Metric: Green (Auspicious / Go), Yellow (Moderate), Red (Caution / Avoid)
"""

from datetime import datetime, timedelta

NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu",
    "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
    "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
    "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
    "Uttara Bhadrapada", "Revati"
]

TARA_DEFINITIONS = [
    {"name": "Janma", "nature": "Neutral / Identity Focus", "auspicious": True, "code": "YELLOW"},
    {"name": "Sampat", "nature": "Wealth & Material Gain", "auspicious": True, "code": "GREEN"},
    {"name": "Vipat", "nature": "Unexpected Obstacles & Losses", "auspicious": False, "code": "RED"},
    {"name": "Kshema", "nature": "Well-Being & Safe Progression", "auspicious": True, "code": "GREEN"},
    {"name": "Pratyak", "nature": "Opposition & Interpersonal Friction", "auspicious": False, "code": "RED"},
    {"name": "Sadhana", "nature": "Achievement & Peak Execution", "auspicious": True, "code": "GREEN"},
    {"name": "Naidhana (Vadha)", "nature": "Severe Destruction / Critical Danger", "auspicious": False, "code": "RED"},
    {"name": "Mitra", "nature": "Harmonious Alliance & Friendship", "auspicious": True, "code": "GREEN"},
    {"name": "Parama Mitra", "nature": "Supreme Divine Assistance & Breakthroughs", "auspicious": True, "code": "GREEN"}
]

RASHIS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

FAVORABLE_CHANDRA_HOUSES = [1, 3, 6, 7, 10, 11]

def calculate_tara_bala(natal_data=None, target_month_str="2026-10"):
    natal_nak_idx = 3 # Default Rohini
    natal_rashi_idx = 1 # Taurus

    if natal_data and isinstance(natal_data, dict):
        vedic = natal_data.get("vedic", {})
        planets = vedic.get("planets", {})
        moon = planets.get("Moon", {})
        if moon:
            natal_rashi_idx = int(moon.get("signIndex", 1))
            long = float(moon.get("longitude", 45.0))
            natal_nak_idx = int(long / 13.333333) % 27

    natal_nak_name = NAKSHATRAS[natal_nak_idx]
    natal_rashi_name = RASHIS[natal_rashi_idx]

    # Generate 14-day projection starting from target month or now
    try:
        start_date = datetime.strptime(target_month_str + "-01" if len(target_month_str) == 7 else target_month_str, "%Y-%m-%d")
    except Exception:
        start_date = datetime.now()

    daily_calendar = []
    # Simulating moon traversing ~1 nakshatra per day
    for day_offset in range(14):
        curr_dt = start_date + timedelta(days=day_offset)
        # Moon transit nakshatra
        transit_nak_idx = (natal_nak_idx + day_offset * 1) % 27
        transit_nak_name = NAKSHATRAS[transit_nak_idx]

        # Tara calculation: (transit_idx - birth_idx) % 9
        tara_idx = (transit_nak_idx - natal_nak_idx) % 9
        tara_info = TARA_DEFINITIONS[tara_idx]

        # Chandra Bala: transit house from natal moon
        transit_rashi_idx = int(transit_nak_idx * 13.333333 / 30.0) % 12
        chandra_house = ((transit_rashi_idx - natal_rashi_idx) % 12) + 1
        chandra_favorable = chandra_house in FAVORABLE_CHANDRA_HOUSES
        is_chandra_ashtama = (chandra_house == 8)

        # Combined status code
        if is_chandra_ashtama or tara_info["code"] == "RED":
            status = "RED (Caution / Defer High-Risk Deeds)"
            verdict = "Avoid high-value signing or medical elections; practice contemplative routine."
        elif tara_info["code"] == "GREEN" and chandra_favorable:
            status = "GREEN (Peak Auspicious Alignment)"
            verdict = "Optimal for launching contracts, investments, and major personal milestones."
        else:
            status = "YELLOW (Neutral / Moderate Execution)"
            verdict = "Routine activities proceed smoothly; exercise normal operational diligence."

        daily_calendar.append({
            "date": curr_dt.strftime("%Y-%m-%d"),
            "transitMoonNakshatra": transit_nak_name,
            "tara": {
                "taraName": tara_info["name"],
                "taraSignificance": tara_info["nature"],
                "paryaya": f"Paryaya {(transit_nak_idx // 9) + 1}"
            },
            "chandraBala": {
                "transitSign": RASHIS[transit_rashi_idx],
                "houseFromNatalMoon": chandra_house,
                "isChandraAshtama": is_chandra_ashtama,
                "isHouseFavorable": chandra_favorable
            },
            "overallDailyRating": status,
            "actionGuidance": verdict
        })

    return {
        "engine": "Navatara Chakra & Daily Tara Bala / Chandra Bala Engine",
        "natalMoonAnchor": {
            "janmaNakshatra": natal_nak_name,
            "janmaRashi": natal_rashi_name
        },
        "totalDaysEvaluated": len(daily_calendar),
        "favorableDaysCount": sum(1 for d in daily_calendar if "GREEN" in d["overallDailyRating"]),
        "cautionDaysCount": sum(1 for d in daily_calendar if "RED" in d["overallDailyRating"]),
        "dailyForecastTimeline": daily_calendar
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    t_month = data.get("targetMonth", "2026-10")
    print(json.dumps(calculate_tara_bala(natal, t_month)))
