#!/usr/bin/env python3
"""
famous_charts_engine.py — Famous & Historic Horoscopes Database & Search Engine

Curated repository of verified historical and celebrity horoscopes with
astrological yoga classification and keyword search.
"""

FAMOUS_HOROSCOPES = [
    {
        "id": "einstein",
        "name": "Albert Einstein",
        "category": "Theoretical Physics / Genius",
        "dob": "1879-03-14",
        "time": "11:30",
        "city": "Ulm, Germany",
        "lagna": "Gemini",
        "moonSign": "Sagittarius",
        "notableYogas": ["Budhaditya Yoga (Mercury + Sun in Pisces)", "Neecha Bhanga Raja Yoga", "Malavya Yoga"],
        "astrologicalHighlights": "Exalted Venus combined with Mercury in the 10th house bestowed supreme mathematical imagination and spacetime conceptualization."
    },
    {
        "id": "jobs",
        "name": "Steve Jobs",
        "category": "Technology Pioneer / Visionary",
        "dob": "1955-02-24",
        "time": "19:15",
        "city": "San Francisco, USA",
        "lagna": "Leo",
        "moonSign": "Pisces",
        "notableYogas": ["Simha Lagna with Mars in 9th (Bhagyadhipati)", "Amala Yoga", "Kesar Yoga"],
        "astrologicalHighlights": "Sun in Aquarius in the 7th house aspecting Leo Ascendant gave fierce product design perfectionism and revolutionary consumer computing aesthetics."
    },
    {
        "id": "musk",
        "name": "Elon Musk",
        "category": "Industrialist / Multi-Planetary Tech",
        "dob": "1971-06-28",
        "time": "06:30",
        "city": "Pretoria, South Africa",
        "lagna": "Gemini",
        "moonSign": "Leo",
        "notableYogas": ["Gajakesari Yoga", "Ruchaka Yoga (Exalted Mars in 8th)", "Budhaditya Yoga"],
        "astrologicalHighlights": "Mars exalted in Capricorn in the 8th house imparts relentless engineering endurance, rocketry drive, and tolerance for extreme industrial risk."
    },
    {
        "id": "gandhi",
        "name": "Mahatma Gandhi",
        "category": "Spiritual Statesman / Non-Violence",
        "dob": "1869-10-02",
        "time": "07:12",
        "city": "Porbandar, India",
        "lagna": "Libra",
        "moonSign": "Leo",
        "notableYogas": ["Malavya Pancha Mahapurusha Yoga", "Gajakesari Yoga", "Bhadra Yoga"],
        "astrologicalHighlights": "Ascendant Lord Venus in 1st house in Libra gave sublime moral conviction, voluntary simplicity, and magnetic global mass influence."
    },
    {
        "id": "curie",
        "name": "Marie Curie",
        "category": "Pioneering Scientist / Nobel Laureate",
        "dob": "1867-11-07",
        "time": "12:00",
        "city": "Warsaw, Poland",
        "lagna": "Capricorn",
        "moonSign": "Pisces",
        "notableYogas": ["Sasa Yoga (Saturn in Scorpio 11th)", "Dharma Karmadhipati Yoga"],
        "astrologicalHighlights": "Saturn and Sun in deep trine conferred monumental scientific persistence in discovering Polonium and Radium despite severe radiation adversity."
    }
]

def search_famous_charts(query="", category="ALL", yoga=""):
    q = query.lower().strip()
    cat = category.upper().strip()
    y_term = yoga.lower().strip()

    matches = []
    for chart in FAMOUS_HOROSCOPES:
        if q and not (q in chart["name"].lower() or q in chart["astrologicalHighlights"].lower() or q in chart["lagna"].lower()):
            continue
        if cat != "ALL" and cat not in chart["category"].upper():
            continue
        if y_term and not any(y_term in y.lower() for y in chart["notableYogas"]):
            continue
        matches.append(chart)

    return {
        "engine": "Famous & Historic Horoscopes Database & Search Engine",
        "totalDatabaseEntries": len(FAMOUS_HOROSCOPES),
        "matchesFound": len(matches),
        "results": matches
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    q = data.get("query", "")
    c = data.get("category", "ALL")
    y = data.get("yoga", "")
    print(json.dumps(search_famous_charts(q, c, y)))
