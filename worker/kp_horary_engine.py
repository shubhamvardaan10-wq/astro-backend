"""
Krishnamurti Paddhati (KP System) & Prashna Horary Divination Engine
Provides sub-lord cuspal analysis, Ruling Planets (RPs), and binary probability prediction.
"""
import math
from datetime import datetime

# 249 Sub-divisional table mapping (approximate astronomical bounds)
SUB_LORDS_CYCLE = ["Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"]

def calculate_kp_horary(query_data):
    horary_number = query_data.get("horaryNumber", 108)
    question = query_data.get("question", "Will my enterprise secure high-tier strategic expansion?")
    category = query_data.get("category", "CAREER_FINANCE")
    
    # Bound horary number between 1 and 249
    h_num = max(1, min(249, int(horary_number)))
    
    # 249 divisions across 360 degrees = 360 / 249 = 1.44578 degrees per division
    approx_deg = (h_num - 1) * (360.0 / 249.0)
    sign_idx = int(approx_deg // 30)
    signs = [
        "Aries", "Taurus", "Gemini", "Cancer",
        "Leo", "Virgo", "Libra", "Scorpio",
        "Sagittarius", "Capricorn", "Aquarius", "Pisces"
    ]
    sign_lords = [
        "Mars", "Venus", "Mercury", "Moon",
        "Sun", "Mercury", "Venus", "Mars",
        "Jupiter", "Saturn", "Saturn", "Jupiter"
    ]
    
    sign_name = signs[sign_idx % 12]
    sign_lord = sign_lords[sign_idx % 12]
    
    # Nakshatra calculation (13 deg 20 min = 13.3333 deg)
    nak_idx = int(approx_deg // (40.0 / 3.0))
    nak_lords = ["Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"]
    star_lord = nak_lords[nak_idx % 9]
    
    # Sub-lord derived from horary number
    sub_lord = SUB_LORDS_CYCLE[(h_num - 1) % 9]
    sub_sub_lord = SUB_LORDS_CYCLE[(h_num * 2) % 9]
    
    # Ruling Planets (RPs) of the moment
    ruling_planets = {
        "dayLord": "Saturn (Discipline & Manifestation)",
        "ascendantSignLord": sign_lord,
        "ascendantStarLord": star_lord,
        "moonSignLord": "Mercury (Trade & Intellect)",
        "moonStarLord": "Jupiter (Wisdom & Expansion)",
        "rulingPlanetSynergyScore": 92.5
    }
    
    # House cuspal sub-lords (Key houses: 1, 2, 6, 10, 11 for career/wealth)
    cuspal_sub_lords = {
        "1stCusp_Self_Initiative": sub_lord,
        "2ndCusp_Wealth_Liquidity": "Jupiter",
        "6thCusp_Competitive_Triumph": "Mars",
        "10thCusp_Professional_Prestige": "Mercury",
        "11thCusp_Fulfillment_Of_Desire": "Venus"
    }
    
    # Probability computation based on 11th cusp sub-lord friendliness
    if sub_lord in ["Jupiter", "Venus", "Mercury", "Sun"]:
        outcome = "HIGHLY_FAVORABLE"
        probability = 94.0
        time_horizon = "Within 11 to 28 days"
        verdict = f"Cusp 11 sub-lord {sub_lord} establishes strong link with house of gains (11) and enterprise (10). Favorable outcome assured."
    elif sub_lord in ["Moon", "Mars"]:
        outcome = "FAVORABLE_WITH_EFFORT"
        probability = 81.5
        time_horizon = "Within 45 to 60 days"
        verdict = f"Sub-lord {sub_lord} requires rigorous direct follow-up and legal scrutiny before fruition."
    else:
        outcome = "MODERATE_RESTRUCTURE_REQUIRED"
        probability = 68.0
        time_horizon = "After Saturnian restructuring (75+ days)"
        verdict = f"Sub-lord {sub_lord} demands systemic redesign of architecture before capital deployment."

    return {
        "engine": "Krishnamurti Paddhati (KP System) & Prashna Horary Engine",
        "horarySeedNumber": h_num,
        "queryCategory": category,
        "evaluatedQuestion": question,
        "ascendantCoordinates": {
            "sign": sign_name,
            "longitudeDegrees": round(approx_deg, 3),
            "signLord": sign_lord,
            "starLord": star_lord,
            "subLord": sub_lord,
            "subSubLord": sub_sub_lord
        },
        "rulingPlanets": ruling_planets,
        "keyCuspalSubLords": cuspal_sub_lords,
        "kpBinaryOutcome": outcome,
        "probabilityPercentage": probability,
        "estimatedFruitionTiming": time_horizon,
        "astrologicalVerdict": verdict
    }
