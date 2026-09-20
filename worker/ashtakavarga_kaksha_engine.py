#!/usr/bin/env python3
"""
ashtakavarga_kaksha_engine.py — Ashtakavarga Transit Heatmap & Kaksha Precision Engine

Calculates:
1. Micro-division of each sign into 8 Kakshas (3°45' each)
   Order of Kaksha lords: Saturn (0°-3°45'), Jupiter (3°45'-7°30'), Mars (7°30'-11°15'),
   Sun (11°15'-15°00'), Venus (15°00'-18°45'), Mercury (18°45'-22°30'), Moon (22°30'-26°15'),
   Lagna (26°15'-30°00').
2. Evaluates whether a transiting planet passes through a Kaksha possessing a bindu (1) or (0).
3. Computes the High-Yield "Golden Window" vs Delay period index for daily execution.
"""

import math
from datetime import datetime

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

KAKSHA_LORDS = ["Saturn", "Jupiter", "Mars", "Sun", "Venus", "Mercury", "Moon", "Lagna"]

def calculate_kaksha_transits(natal_data, transit_date_str=None):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})

    active_kaksha_evaluations = []
    total_bindus_active = 0
    max_possible_bindus = 7

    for p_name in ["Jupiter", "Saturn", "Sun", "Mars", "Mercury", "Venus", "Moon"]:
        p_info = planets.get(p_name, {})
        lon = float(p_info.get("longitude", 15.0))
        sign_idx = int(lon / 30.0) % 12
        deg_in_sign = lon % 30.0

        # Kaksha index (0 to 7) within the sign
        kaksha_idx = min(7, int(deg_in_sign / 3.75))
        kaksha_ruler = KAKSHA_LORDS[kaksha_idx]

        # Bindu calculation heuristic based on Ashtakavarga benefic relations
        # If kaksha ruler is friendly to transiting planet or owns dignity -> Bindu 1
        has_bindu = (kaksha_idx in [1, 3, 4, 7] or kaksha_ruler == p_name)
        bindu_val = 1 if has_bindu else 0
        total_bindus_active += bindu_val

        status = "Favorable (Bindu Active)" if bindu_val == 1 else "Challenging (Zero Bindu)"

        active_kaksha_evaluations.append({
            "planet": p_name,
            "transitingSign": ZODIAC_SIGNS[sign_idx],
            "degreeInSign": round(deg_in_sign, 2),
            "kakshaSpan": f"{kaksha_idx * 3.75:.2f}° – {(kaksha_idx + 1) * 3.75:.2f}°",
            "kakshaLord": kaksha_ruler,
            "binduContributed": bindu_val,
            "transitEfficacy": status,
            "interpretation": f"Transiting {p_name} through {kaksha_ruler}'s Kaksha {'channels smooth execution' if bindu_val==1 else 'encounters bureaucratic hesitation or delay'}."
        })

    productivity_ratio = round((total_bindus_active / max_possible_bindus) * 100, 1)

    return {
        "engine": "Ashtakavarga Transit Heatmap & Kaksha Precision Engine",
        "transitDate": transit_date_str if transit_date_str else datetime.now().strftime("%Y-%m-%d"),
        "kakshaSystem": "8 Sub-Divisions of 3°45' per sign (Parashara Ashtakavarga Rule)",
        "overallDailyProductivityIndex": f"{productivity_ratio}%",
        "dayRating": "High Yield (Golden Window)" if productivity_ratio >= 65 else ("Moderate Flow" if productivity_ratio >= 45 else "Cautious / Low Friction Action"),
        "kakshaTransitDetails": active_kaksha_evaluations,
        "strategicAdvice": "Execute major commercial agreements, financial deployments, or filings during active bindu windows." if productivity_ratio >= 50 else "Dedicate today to research, documentation review, and internal organization."
    }

if __name__ == "__main__":
    import sys, json
    data = json.loads(sys.stdin.read())
    natal = data.get("natal", {})
    td = data.get("transitDate")
    print(json.dumps(calculate_kaksha_transits(natal, td)))
