#!/usr/bin/env python3
"""
event_muhurta_engine.py — Auspicious Life Event Muhurta Scheduler

Scans upcoming dates over a given horizon (1 to 12 months) and identifies
golden astrological timing windows for key milestones:
  - Vivaha Muhurta (Marriage ceremony)
  - Griha Pravesh & Property Purchase (Real estate / home entry)
  - Vyapar Arambh (Business / startup launch / contract signing)
  - Vahan Kharid (Vehicle acquisition)
  - Medical Treatment & Surgery Commencement
"""

from datetime import datetime, timedelta
import swisseph as swe

AUSPICIOUS_NAKSHATRAS = {
    "marriage": ["Rohini", "Mrigashira", "Magha", "Uttara Phalguni", "Hasta", "Swati", "Anuradha", "Uttara Ashadha", "Uttara Bhadrapada", "Revati"],
    "property": ["Rohini", "Mrigashira", "Punarvasu", "Pushya", "Uttara Phalguni", "Hasta", "Chitra", "Anuradha", "Uttara Ashadha", "Shravana", "Uttara Bhadrapada", "Revati"],
    "business": ["Ashwini", "Rohini", "Pushya", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Anuradha", "Shravana", "Dhanishtha", "Revati"],
    "vehicle": ["Ashwini", "Rohini", "Punarvasu", "Pushya", "Hasta", "Swati", "Shravana", "Dhanishtha", "Shatabhisha", "Revati"],
    "medical": ["Ashwini", "Rohini", "Mrigashira", "Punarvasu", "Pushya", "Hasta", "Chitra", "Swati", "Anuradha", "Shravana", "Shatabhisha", "Revati"]
}

NAKSHATRAS = [
    "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra",
    "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni",
    "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha",
    "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishtha", "Shatabhisha",
    "Purva Bhadrapada", "Uttara Bhadrapada", "Revati"
]

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]


def evaluate_date_muhurta(dt, event_type):
    """Evaluates a single day for the specified event type."""
    jd = swe.julday(dt.year, dt.month, dt.day, 12.0)
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)

    # Moon longitude
    res, flags = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)
    moon_lon = res[0]
    nak_idx = int(moon_lon / (360.0 / 27.0)) % 27
    nak_name = NAKSHATRAS[nak_idx]
    moon_sign_idx = int(moon_lon / 30.0) % 12

    # Sun longitude (for Tithi)
    s_res, s_flags = swe.calc_ut(jd, swe.SUN, swe.FLG_SIDEREAL)
    sun_lon = s_res[0]

    # Tithi (1 to 30)
    diff = (moon_lon - sun_lon + 360.0) % 360.0
    tithi_num = int(diff / 12.0) + 1  # 1-15 Shukla, 16-30 Krishna

    # Day of week
    weekday = dt.weekday()  # 0 = Mon, 6 = Sun
    weekday_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]

    # Inauspicious Tithi check (Rikta Tithis: 4, 9, 14, 19, 24, 29, and Amavasya 30)
    is_rikta = tithi_num in (4, 9, 14, 19, 24, 29, 30)
    # Panchak check (Moon in Aquarius/Pisces: signs 10, 11)
    is_panchak = moon_sign_idx in (10, 11)

    target_naks = AUSPICIOUS_NAKSHATRAS.get(event_type, AUSPICIOUS_NAKSHATRAS["business"])
    is_favorable_nak = nak_name in target_naks

    # Scoring
    score = 50
    if is_favorable_nak: score += 25
    if not is_rikta: score += 15
    if not is_panchak: score += 10
    if weekday in (0, 2, 3, 4): score += 10  # Mon, Wed, Thu, Fri generally auspicious
    if weekday == 1 and event_type != "medical": score -= 15 # Tuesday caution except surgery

    if score >= 85:
        grade = "Gold (Superlative Auspiciousness)"
    elif score >= 70:
        grade = "Silver (Highly Favorable)"
    else:
        grade = "Neutral / Standard"

    return {
        "date": dt.strftime("%Y-%m-%d"),
        "dayOfWeek": weekday_names[weekday],
        "nakshatra": nak_name,
        "moonSign": ZODIAC_SIGNS[moon_sign_idx],
        "tithi": f"Tithi {tithi_num} ({'Shukla Paksha' if tithi_num <= 15 else 'Krishna Paksha'})",
        "muhurtaScore": score,
        "qualityGrade": grade,
        "optimalHours": "11:45 AM – 12:35 PM (Abhijit Muhurta)" if weekday != 2 else "09:30 AM – 11:00 AM (Amrit Kaal)",
        "isRecommended": score >= 75
    }


def find_top_muhurtas(event_type="marriage", days_ahead=90, max_results=6):
    """
    Scans days_ahead starting from today to find the top auspicious Muhurta dates.
    """
    start = datetime.now() + timedelta(days=1)
    favorable = []

    for i in range(days_ahead):
        cur_dt = start + timedelta(days=i)
        eval_res = evaluate_date_muhurta(cur_dt, event_type)
        if eval_res["isRecommended"]:
            favorable.append(eval_res)

    # Sort by score
    favorable.sort(key=lambda x: x["muhurtaScore"], reverse=True)
    selected = favorable[:max_results]

    event_labels = {
        "marriage": "Vivaha Muhurta (Marriage & Wedding Ceremonies)",
        "property": "Griha Pravesh & Real Estate Purchase",
        "business": "Vyapar Arambh (Company Launch & Contract Signing)",
        "vehicle": "Vahan Kharid (Vehicle Acquisition)",
        "medical": "Medical Treatment & Surgery Commencement"
    }

    return {
        "eventType": event_type,
        "eventDescription": event_labels.get(event_type, "Auspicious Event"),
        "scanWindow": f"Next {days_ahead} days",
        "totalAuspiciousDatesFound": len(favorable),
        "topRecommendedMuhurtas": selected,
        "generalGuidelines": "Ensure key agreements or rituals are commenced during the specified optimal hours to align with the Abhijit or Amrit windows."
    }


if __name__ == "__main__":
    res = find_top_muhurtas("marriage", 90, 4)
    print("Muhurta Engine Test:")
    print("  Event:", res["eventDescription"])
    print("  Top dates found:", len(res["topRecommendedMuhurtas"]))
    if res["topRecommendedMuhurtas"]:
        print("  Top date:", res["topRecommendedMuhurtas"][0]["date"], "-", res["topRecommendedMuhurtas"][0]["qualityGrade"])
