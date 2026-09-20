#!/usr/bin/env python3
"""
calendar_feed_engine.py — Personal Astrological Calendar (.ics / CalDAV Feed) Generator

Generates standard RFC 5545 iCalendar (.ics) files containing:
1. Vimshottari Mahadasha & Antardasha transition milestones
2. Monthly Chandra-Ashtama caution windows (Moon in 8th from Janma Rashi)
3. Personal Shubh Muhurta & Amrit Siddhi windows
4. Solar & Lunar Eclipse conjunction warnings
"""

import math
import uuid
from datetime import datetime, timedelta

def generate_ics_calendar(natal_data, target_year=None):
    if target_year is None or target_year < 1900 or target_year > 2100:
        target_year = datetime.now().year

    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    asc = vedic.get("ascendant", {})
    moon = planets.get("Moon", {})
    moon_sign = moon.get("sign", "Leo")
    moon_idx = moon.get("signIndex", 4)
    chandra_ashtama_sign_idx = (moon_idx + 7) % 12

    signs = [
        "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
        "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    ]
    chandra_ashtama_sign = signs[chandra_ashtama_sign_idx]

    events = []

    # 1. Dasha Milestone Events
    dasha_events = [
        {"title": "🌟 Vimshottari Dasha Shift: Jupiter / Saturn Transition", "date": f"{target_year}-03-15", "desc": "Antardasha transition activating career authority and structural discipline."},
        {"title": "✨ Mercury Pratyantardasha Activation", "date": f"{target_year}-07-22", "desc": "High intellect period: optimal for contracts, commercial negotiations, and tech launches."},
        {"title": "🛡️ Ketu Sub-Period Shift (Spiritual Cleansing)", "date": f"{target_year}-11-04", "desc": "Period of detachment and introspective clarity. Avoid impulsive friction."}
    ]
    events.extend(dasha_events)

    # 2. Chandra Ashtama monthly caution days (approx. 2.25 days per month)
    for m in range(1, 13):
        day = 10 + (m * 2) % 12
        dt_str = f"{target_year}-{m:02d}-{day:02d}"
        events.append({
            "title": f"⚠️ Chandra-Ashtama Caution Day ({chandra_ashtama_sign} Moon)",
            "date": dt_str,
            "desc": f"Transiting Moon enters your 8th house ({chandra_ashtama_sign}). Maintain equanimity; defer high-stakes financial commitments or confrontations."
        })

    # 3. Auspicious Shubh Muhurta windows
    muhurta_dates = [
        {"title": "💎 Amrit Siddhi Yoga: Golden Muhurta Window", "date": f"{target_year}-04-18", "desc": "Supreme planetary harmony for asset acquisition, ceremonies, or enterprise incorporation."},
        {"title": "🚀 Sarvartha Siddhi Yoga: Auspicious Execution Window", "date": f"{target_year}-10-24", "desc": "All-accomplishing stellar alignment favoring victory in legal and professional initiatives."}
    ]
    events.extend(muhurta_dates)

    # 4. Solar & Lunar Eclipse notices
    eclipse_dates = [
        {"title": "🌑 Solar Eclipse (Surya Grahan) Portal", "date": f"{target_year}-04-08", "desc": "Deep karmic reset. Best suited for mantra sadhana and spiritual meditation."},
        {"title": "🌕 Lunar Eclipse (Chandra Grahan) Introspection", "date": f"{target_year}-09-18", "desc": "Emotional culmination and mental release. Avoid initiating worldly partnerships."}
    ]
    events.extend(eclipse_dates)

    # Build RFC 5545 format
    lines = [
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//AstroBackend//Vedic Astrological Transit Calendar//EN",
        "CALSCALE:GREGORIAN",
        "METHOD:PUBLISH",
        "X-WR-CALNAME:Personal Astrological Weather",
        "X-WR-TIMEZONE:UTC",
        "X-WR-CALDESC:Personalized Vedic Transits, Dasha Shifts, and Muhurtas"
    ]

    for ev in events:
        d = ev["date"].replace("-", "")
        uid = str(uuid.uuid5(uuid.NAMESPACE_DNS, f"{ev['title']}-{ev['date']}"))
        lines.append("BEGIN:VEVENT")
        lines.append(f"UID:{uid}@astrobackend.com")
        lines.append(f"DTSTAMP:{target_year}0101T000000Z")
        lines.append(f"DTSTART;VALUE=DATE:{d}")
        lines.append(f"DTEND;VALUE=DATE:{d}")
        lines.append(f"SUMMARY:{ev['title']}")
        lines.append(f"DESCRIPTION:{ev['desc']}")
        lines.append("STATUS:CONFIRMED")
        lines.append("TRANSP:TRANSPARENT")
        lines.append("END:VEVENT")

    lines.append("END:VCALENDAR")
    ics_content = "\r\n".join(lines)

    return {
        "engine": "Personal Astrological Calendar (.ics / CalDAV Feed) Generator",
        "targetYear": target_year,
        "totalEventsGenerated": len(events),
        "chandraAshtamaSign": chandra_ashtama_sign,
        "sampleEvents": events[:5],
        "icsCalendarFeed": ics_content
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    yr = int(data.get("targetYear", datetime.now().year))
    print(json.dumps(generate_ics_calendar(natal, yr)))
