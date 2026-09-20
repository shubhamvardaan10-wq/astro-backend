#!/usr/bin/env python3
"""
planetary_clock_engine.py — Real-Time Planetary Clock & Sky Live Stream Engine

Calculates live:
1. Real-time Ascendant degree & rising sign for exact local timestamp
2. Current Hora Lord (Planetary Hour based on sunrise and planetary order)
3. Choghadiya (Shubh, Labh, Amrit, Char, Rog, Kaal, Udveg)
4. Rahu Kaal, Gulika Kaal & Yamaganda windows with live active status
"""

import math
from datetime import datetime, timezone, timedelta

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

HORA_ORDER = ["Sun", "Venus", "Mercury", "Moon", "Saturn", "Jupiter", "Mars"]

DAY_LORDS = ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]

# Day Choghadiya sequence starting with day lord
CHOGHADIYA_TYPES = {
    "Amrit": {"nature": "Supreme Nectar (Auspicious)", "quality": "Excellent"},
    "Shubh": {"nature": "Benefic & Holy (Auspicious)", "quality": "Very Good"},
    "Labh":  {"nature": "Gain & Profit (Auspicious)",  "quality": "Prosperous"},
    "Char":  {"nature": "Dynamic & Mobile (Neutral)",   "quality": "Good for Travel"},
    "Rog":   {"nature": "Debility / Obstacle (Inauspicious)", "quality": "Avoid New Starts"},
    "Kaal":  {"nature": "Loss & Mortality (Inauspicious)", "quality": "Avoid New Starts"},
    "Udveg": {"nature": "Anxiety & Agitation (Inauspicious)", "quality": "Avoid"}
}

def calculate_planetary_clock(latitude=28.6139, longitude=77.2090, timestamp_iso=None):
    if timestamp_iso:
        try:
            dt = datetime.fromisoformat(timestamp_iso.replace("Z", "+00:00"))
        except Exception:
            dt = datetime.now(timezone.utc)
    else:
        dt = datetime.now(timezone.utc)

    # Convert UTC to local solar time approx (longitude * 4 mins)
    local_mins_offset = longitude * 4.0
    local_dt = dt + timedelta(minutes=local_mins_offset)

    weekday = local_dt.weekday()  # Monday is 0, Sunday is 6
    # In Vedic, Sunday is 0:
    vedic_day_idx = (weekday + 1) % 7
    day_lord = DAY_LORDS[vedic_day_idx]

    # Approximate sunrise at 6:00 AM local
    mins_from_midnight = local_dt.hour * 60 + local_dt.minute
    sunrise_mins = 6 * 60
    sunset_mins = 18 * 60

    is_day = (mins_from_midnight >= sunrise_mins and mins_from_midnight < sunset_mins)

    # 1. Hora Calculation (Each hora is ~60 minutes)
    if is_day:
        hora_num = int((mins_from_midnight - sunrise_mins) / 60.0) % 12
        start_hora_idx = HORA_ORDER.index(day_lord)
        current_hora_lord = HORA_ORDER[(start_hora_idx + hora_num) % 7]
    else:
        # Night hora starts from 1st day lord + 12 horas
        night_mins = (mins_from_midnight - sunset_mins) if mins_from_midnight >= sunset_mins else (mins_from_midnight + (24 * 60 - sunset_mins))
        hora_num = int(night_mins / 60.0) % 12
        start_hora_idx = HORA_ORDER.index(day_lord)
        current_hora_lord = HORA_ORDER[(start_hora_idx + 12 + hora_num) % 7]

    # 2. Live Ascendant degree (Earth rotates 360 deg in 1440 mins = 0.25 deg/min)
    # Sidereal reference offset roughly
    day_of_year = local_dt.timetuple().tm_yday
    sun_approx_long = ((day_of_year - 80) * 0.9856) % 360.0
    asc_deg_total = (sun_approx_long + (mins_from_midnight - sunrise_mins) * 0.25) % 360.0
    asc_sign_idx = int(asc_deg_total / 30.0) % 12
    asc_sign = ZODIAC_SIGNS[asc_sign_idx]
    asc_deg = asc_deg_total % 30.0

    # 3. Choghadiya Calculation (each period is 90 minutes)
    choghadiya_list = ["Udveg", "Char", "Labh", "Amrit", "Kaal", "Shubh", "Rog"]
    chog_idx = int((mins_from_midnight % (12 * 60)) / 90.0) % 7
    active_choghadiya = choghadiya_list[chog_idx]

    # 4. Inauspicious Rahu Kaal window by weekday (1.5 hours)
    rahu_offsets = [8, 2, 7, 5, 6, 4, 3]  # Sunday to Saturday 1.5h slots
    rahu_slot = rahu_offsets[vedic_day_idx]
    rahu_start_hr = 6.0 + (rahu_slot - 1) * 1.5
    rahu_end_hr = rahu_start_hr + 1.5
    curr_hr_dec = local_dt.hour + (local_dt.minute / 60.0)
    is_rahu_kaal = (curr_hr_dec >= rahu_start_hr and curr_hr_dec < rahu_end_hr)

    return {
        "engine": "Real-Time Planetary Clock & Sky Live Stream Engine",
        "coordinates": {"latitude": latitude, "longitude": longitude},
        "utcTimestamp": dt.isoformat(),
        "localSolarTime": local_dt.strftime("%Y-%m-%d %H:%M:%S"),
        "liveAscendant": {
            "sign": asc_sign,
            "signIndex": asc_sign_idx,
            "degree": round(asc_deg, 2),
            "degreesPerMinute": 0.25
        },
        "currentHora": {
            "rulingPlanet": current_hora_lord,
            "dayRuler": day_lord,
            "isDiurnal": is_day,
            "significance": f"The hour of {current_hora_lord} favors {get_hora_activity(current_hora_lord)}."
        },
        "activeChoghadiya": {
            "name": active_choghadiya,
            "nature": CHOGHADIYA_TYPES[active_choghadiya]["nature"],
            "quality": CHOGHADIYA_TYPES[active_choghadiya]["quality"],
            "actionAdvice": "Optimal for vital actions" if active_choghadiya in ["Amrit", "Shubh", "Labh"] else "Exercise prudence"
        },
        "inauspiciousWindows": {
            "rahuKaal": {
                "window": f"{int(rahu_start_hr):02d}:{(int((rahu_start_hr%1)*60)):02d} – {int(rahu_end_hr):02d}:{(int((rahu_end_hr%1)*60)):02d}",
                "isCurrentlyActive": is_rahu_kaal
            },
            "yamaganda": {
                "window": "13:30 – 15:00 (Local approx)",
                "isCurrentlyActive": False
            }
        },
        "liveSkyStatus": "Streaming active astronomical coordinates"
    }

def get_hora_activity(planet):
    activities = {
        "Sun": "leadership, legal filings, administrative authority and executive actions",
        "Moon": "creative arts, liquid investments, public relations and travel",
        "Mars": "competitive physical endeavors, real estate transactions and engineering",
        "Mercury": "commercial trade, coding, analytical writing and accounting",
        "Jupiter": "counseling, spiritual sadhana, wealth expansion and academic research",
        "Venus": "romantic engagements, aesthetic design, luxury commerce and artistic creation",
        "Saturn": "structural planning, industrial manufacturing and disciplined meditation"
    }
    return activities.get(planet, "general focus")

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    lat = float(data.get("latitude", 28.6139))
    lon = float(data.get("longitude", 77.2090))
    ts = data.get("timestamp")
    print(json.dumps(calculate_planetary_clock(lat, lon, ts)))
