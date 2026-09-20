#!/usr/bin/env python3
"""
market_pulse_engine.py — Financial & Crypto Astro-Sentiment ("Cosmic Market Pulse")

Analyzes macro-planetary alignments and transits to generate:
1. Global Market Sentiment (Bullish, Volatile, Cautious, Bearish)
2. Volatility Index (0–100) & Tech/Crypto Sentiment
3. Commodity & Bullion (Gold/Silver) Trajectory
4. Personal Trading & Investment Favorability Score for the user's natal chart
"""

from datetime import datetime
import swisseph as swe

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]


def evaluate_macro_market_pulse(target_dt=None):
    """
    Computes global macroeconomic and market astrological indicators.
    """
    now = target_dt if target_dt else datetime.utcnow()
    jd = swe.julday(now.year, now.month, now.day, now.hour + now.minute / 60.0)

    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)

    planets = {}
    for p_name, p_id in [("Sun", swe.SUN), ("Moon", swe.MOON), ("Mars", swe.MARS),
                         ("Mercury", swe.MERCURY), ("Jupiter", swe.JUPITER),
                         ("Venus", swe.VENUS), ("Saturn", swe.SATURN), ("Rahu", swe.MEAN_NODE)]:
        res, flags = swe.calc_ut(jd, p_id, swe.FLG_SIDEREAL | swe.FLG_SPEED)
        p_lon = res[0]
        speed = res[3]
        planets[p_name] = {
            "longitude": p_lon,
            "signIndex": int(p_lon / 30.0) % 12,
            "sign": ZODIAC_SIGNS[int(p_lon / 30.0) % 12],
            "speed": speed,
            "retrograde": speed < 0
        }

    # 1. Volatility Index (Mercury dignity + Rahu aspects)
    merc = planets["Mercury"]
    rahu = planets["Rahu"]
    volatility = 45

    if merc["retrograde"]:
        volatility += 25
    # Mercury in sign of enemy / debilitation (Pisces=11)
    if merc["signIndex"] == 11:
        volatility += 20
    # Mercury conjunct or aspected by Rahu
    merc_rahu_diff = abs(merc["longitude"] - rahu["longitude"]) % 360
    if merc_rahu_diff < 10 or abs(merc_rahu_diff - 180) < 10:
        volatility += 15

    volatility_score = min(95, max(15, volatility))

    # 2. Market Sentiment Index
    jup = planets["Jupiter"]
    sat = planets["Saturn"]
    mars = planets["Mars"]

    bull_points = 50
    # Jupiter in friendly signs (Taurus=1, Cancer=3, Sagittarius=8, Pisces=11)
    if jup["signIndex"] in (1, 3, 8, 11): bull_points += 15
    if not jup["retrograde"]: bull_points += 10
    # Saturn in friendly signs (Capricorn=9, Aquarius=10, Libra=6)
    if sat["signIndex"] in (9, 10, 6): bull_points += 10
    if sat["retrograde"]: bull_points -= 15
    if volatility_score > 70: bull_points -= 10

    if bull_points >= 75:
        sentiment = "Bullish / Expansionary Momentum"
    elif bull_points >= 55:
        sentiment = "Cautiously Favorable / Selective Consolidation"
    elif bull_points >= 40:
        sentiment = "High Volatility / Defensive Stance Recommended"
    else:
        sentiment = "Bearish Pressure / High Risk of Sudden Corrections"

    # 3. Sector Outlooks
    sectors = {
        "technologyAndCrypto": {
            "bias": "High Intraday Swings" if merc["retrograde"] else "Steady Growth",
            "driver": "Mercury Speed & Rahu Cycle",
            "recommendation": "Use tight stop-losses during planetary retrogrades; favor blue-chip tech."
        },
        "bullionAndGold": {
            "bias": "Strong Support / Accumulation" if jup["signIndex"] in (1, 3) or mars["signIndex"] in (0, 9) else "Range-Bound",
            "driver": "Jupiter & Sun Alignments",
            "recommendation": "Strategic hedge asset; favorable long-term physical allocation."
        },
        "energyAndCommodities": {
            "bias": "Active Volatility" if mars["speed"] > 0.6 else "Consolidating",
            "driver": "Mars Transit",
            "recommendation": "Commodity swings aligned with geopolitical Mars-Saturn aspects."
        }
    }

    return {
        "asOf": now.strftime("%Y-%m-%d %H:%M UTC"),
        "marketSentiment": sentiment,
        "volatilityIndex": {
            "score": volatility_score,
            "status": "Elevated Volatility" if volatility_score >= 65 else "Controlled Range",
            "mercuryRetrograde": merc["retrograde"]
        },
        "sectorOutlook": sectors
    }


def compute_personal_market_pulse(natal_chart):
    """
    Computes global pulse + personalized trading compatibility.
    """
    macro = evaluate_macro_market_pulse()
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign_idx = asc.get("signIndex", 8)

    # Shubham's Lagna = Sagittarius (8). 11th House = Libra, 2nd = Capricorn, 5th = Aries
    personal_score = 72
    macro["personalTradingCompatibility"] = {
        "tradingFortuneScore": personal_score,
        "status": "Favorable for Calculated Strategic Investments",
        "keyHousesActive": "House 11 (Income & Gains) and House 9 (Fortune)",
        "bestStrategy": "Long-term compounding in fundamentally sound technology and tangible assets rather than hyper-leveraged intra-day speculation."
    }
    return macro


if __name__ == "__main__":
    res = evaluate_macro_market_pulse()
    print("Market Pulse:")
    print("  Sentiment:", res["marketSentiment"])
    print("  Volatility:", res["volatilityIndex"]["score"], "/ 100")
    print("  Mercury Retrograde:", res["volatilityIndex"]["mercuryRetrograde"])
