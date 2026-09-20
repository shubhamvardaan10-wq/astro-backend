"""
Panch-Pakshi Shastra: Ancient Tamil Siddhar 5-Bird Chronobiology Engine
Computes native ruling bird and real-time 5-activity diurnal rhythm (Ruling, Eating, Walking, Sleeping, Dying).
"""
from datetime import datetime

# 27 Nakshatras to 5 Birds mapping (Shukla Paksha cycle)
BIRD_NAMES = ["Vulture (Valloor)", "Owl (Aandhai)", "Crow (Kaakai)", "Cock (Kozhi)", "Peacock (Mayil)"]

BIRDS_DATA = {
    "Vulture (Valloor)": {
        "element": "Ether (Akasha)",
        "rulingGraha": "Sun & Jupiter",
        "powerQuality": "Strategic Command & High-Altitude Vision",
        "gemstoneResonance": "Ruby / Yellow Sapphire"
    },
    "Owl (Aandhai)": {
        "element": "Water (Jala)",
        "rulingGraha": "Moon & Venus",
        "powerQuality": "Deep Intuitive Clairvoyance & Night Strategy",
        "gemstoneResonance": "Pearl / Diamond"
    },
    "Crow (Kaakai)": {
        "element": "Earth (Prithvi)",
        "rulingGraha": "Saturn & Mercury",
        "powerQuality": "Unstoppable Resilience & Material Manifestation",
        "gemstoneResonance": "Blue Sapphire / Emerald"
    },
    "Cock (Kozhi)": {
        "element": "Fire (Agni)",
        "rulingGraha": "Mars (Mangal)",
        "powerQuality": "Rapid Tactical Execution & Courageous Conquest",
        "gemstoneResonance": "Red Coral"
    },
    "Peacock (Mayil)": {
        "element": "Air (Vayu)",
        "rulingGraha": "Rahu & Ketu",
        "powerQuality": "Charismatic Hypnotic Magnetism & Global Influence",
        "gemstoneResonance": "Hessonite Garnet"
    }
}

def calculate_panch_pakshi(req_data):
    vedic = req_data.get("natal", {}).get("vedic", {})
    lagna_sign = vedic.get("ascendant", {}).get("sign", "Virgo")
    
    # Nakshatra index or hash to bird
    bird_idx = 0  # Default Vulture for high-tier visionary
    if lagna_sign in ["Aries", "Leo", "Sagittarius"]:
        bird_idx = 0  # Vulture
    elif lagna_sign in ["Cancer", "Scorpio", "Pisces"]:
        bird_idx = 1  # Owl
    elif lagna_sign in ["Taurus", "Virgo", "Capricorn"]:
        bird_idx = 2  # Crow
    elif lagna_sign in ["Gemini", "Libra", "Aquarius"]:
        bird_idx = 4  # Peacock
        
    bird_name = BIRD_NAMES[bird_idx]
    bird_meta = BIRDS_DATA[bird_name]
    
    # 5 Diurnal Activity Intervals (Daytime 06:00 AM - 06:00 PM broken into 5 equal yamas of 2h 24m)
    activities_cycle = [
        {"activity": "RULING (Arasu)", "powerPercentage": 100, "influence": "Peak sovereign dominion. Sign high-stakes contracts, launch products, enter high-stakes negotiations.", "auspiciousness": "SUPREME_EXCELLENCE"},
        {"activity": "EATING (Oon)", "powerPercentage": 80, "influence": "Deep focus and metabolic assimilation. Ideal for deep software engineering, financial modeling, and strategy meetings.", "auspiciousness": "VERY_FAVORABLE"},
        {"activity": "WALKING (Nadai)", "powerPercentage": 50, "influence": "Medium velocity flow. Good for travel, routine communication, administrative follow-up.", "auspiciousness": "NEUTRAL_OPERATIONAL"},
        {"activity": "SLEEPING (Thuyil)", "powerPercentage": 20, "influence": "Reduced vital energy. Avoid commitments; dedicate time to restorative contemplation or meditation.", "auspiciousness": "CAUTION_REST"},
        {"activity": "DYING (Chaavu)", "powerPercentage": 0, "influence": "Vulnerability vector. Complete avoidance of conflict, trading, or major decisions; practice total mindfulness.", "auspiciousness": "STRICT_AVOIDANCE"}
    ]
    
    # Schedule intervals
    time_windows = [
        ("06:00 AM - 08:24 AM", 0),
        ("08:24 AM - 10:48 AM", 1),
        ("10:48 AM - 01:12 PM", 2),
        ("01:12 PM - 03:36 PM", 3),
        ("03:36 PM - 06:00 PM", 4)
    ]
    
    schedule = []
    for time_span, act_idx in time_windows:
        act = activities_cycle[(act_idx + bird_idx) % 5]
        schedule.append({
            "yamaInterval": time_span,
            "activityState": act["activity"],
            "powerPercentage": act["powerPercentage"],
            "karmicDirective": act["influence"],
            "auspiciousTier": act["auspiciousness"]
        })
        
    peak_ruling_window = next((s for s in schedule if "RULING" in s["activityState"]), schedule[0])
    
    return {
        "engine": "Tamil Siddhar Panch-Pakshi Shastra (Five-Bird Chronobiology Engine)",
        "nativeRulingBird": bird_name,
        "birdElementalGovernance": bird_meta["element"],
        "governingGrahaAlliance": bird_meta["rulingGraha"],
        "powerVirtue": bird_meta["powerQuality"],
        "gemstoneTuning": bird_meta["gemstoneResonance"],
        "dailyDiurnalYamaSchedule": schedule,
        "goldenExecutionWindow": peak_ruling_window["yamaInterval"],
        "siddharWarning": "Never initiate confrontation or execute capital transactions during the 'Dying (Chaavu)' yama. Maximize the 'Ruling (Arasu)' yama for transformative commercial breakthroughs."
    }
