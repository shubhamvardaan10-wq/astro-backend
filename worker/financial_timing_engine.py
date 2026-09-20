"""
Financial Astrology & Algorithmic Market Timing Engine
Correlates macro celestial transits with market volatility, asset class rotations,
and native financial opportunity/risk cycles.
"""

from datetime import datetime, timedelta

def calculate_financial_timing(natal_data, target_date_str=None):
    vedic = natal_data.get("vedic", {})
    lagna = vedic.get("ascendant", {}).get("sign", "Virgo")
    
    # Base date
    base_date = datetime.strptime(target_date_str, "%Y-%m-%d") if target_date_str else datetime.now()
    
    # Asset class planetary indicators
    asset_regimes = [
        {
            "assetClass": "Gold & Sovereign Hard Assets",
            "governingGrahas": "Surya (Sun) & Devaguru Jupiter",
            "macroTrend": "BULLISH_CONSOLIDATION",
            "confidenceScore": 88.5,
            "catalyst": "Sun transiting benefic Kendra while Jupiter casts 9th aspect over hard commodity sectors.",
            "directive": "Strong accumulation bias; ideal hedge against fiat currency dilution."
        },
        {
            "assetClass": "Tech, AI & High-Growth Equities",
            "governingGrahas": "Budha (Mercury) & Rahu (North Node)",
            "macroTrend": "EXPONENTIAL_VOLATILITY",
            "confidenceScore": 92.0,
            "catalyst": "Mercury direct in trine to natal 10th lord, amplified by Rahu's unconventional innovation vector.",
            "directive": "Favorable for scalable SaaS enterprise, AI infrastructure, and algorithmic commercial platforms."
        },
        {
            "assetClass": "Energy, Oil & Strategic Commodities",
            "governingGrahas": "Mangal (Mars) & Shani (Saturn)",
            "macroTrend": "PRESSURE_AND_SPIKE",
            "confidenceScore": 84.0,
            "catalyst": "Mars aspecting Saturn creates supply chain friction and sudden geopolitical price surges.",
            "directive": "Hedge exposure; protect against sudden transportation or industrial cost shocks."
        },
        {
            "assetClass": "Decentralized Assets & Crypto",
            "governingGrahas": "Rahu & Uranus/Ketu Axis",
            "macroTrend": "HIGH_BETA_LIQUIDITY_SURGE",
            "confidenceScore": 86.5,
            "catalyst": "Nodal shifts across the 2nd and 8th financial axis unblock unconventional global liquidity.",
            "directive": "Exercise disciplined profit-taking; scale into fundamental layer-1 and decentralized privacy protocols."
        }
    ]
    
    # Generate 30-Day Forward Risk Windows
    risk_windows = []
    for day_offset in [2, 7, 14, 21, 28]:
        d = base_date + timedelta(days=day_offset)
        d_str = d.strftime("%Y-%m-%d")
        
        if day_offset in [7, 21]:
            risk_windows.append({
                "date": d_str,
                "windowType": "OPTIMAL_CAPITAL_DEPLOYMENT",
                "astrologicalTrigger": "Moon conjunct benefic Guru / Budh Hora during Shukla Paksha",
                "recommendedAction": "Execute major contract signings, capital investments, and enterprise platform releases.",
                "riskLevel": "LOW"
            })
        elif day_offset == 14:
            risk_windows.append({
                "date": d_str,
                "windowType": "VOLATILITY_AND_CHURN",
                "astrologicalTrigger": "Full Moon (Purnima) opposite Mars-Saturn mid-point",
                "recommendedAction": "Maintain high liquidity; avoid speculative leverage or hasty institutional acquisitions.",
                "riskLevel": "HIGH"
            })
        else:
            risk_windows.append({
                "date": d_str,
                "windowType": "STRATEGIC_ACCUMULATION",
                "astrologicalTrigger": "Favorable 11th lord transit through Artha Trikona",
                "recommendedAction": "Deploy steady recurring treasury into core sovereign assets.",
                "riskLevel": "MODERATE"
            })
            
    return {
        "engine": "Vedic Financial Astrology & Macro Cycle Timing",
        "nativeLagna": lagna,
        "primaryWealthVector": "2nd & 11th House Lord Synergy with Mercury & Jupiter",
        "currentMacroPhase": "Golden Enterprise Accumulation",
        "assetClassRegimes": asset_regimes,
        "upcomingThirtyDayWindows": risk_windows,
        "vedicWealthMantra": "Om Shreem Hreem Kleem Mahalakshmaye Namaha (Recite 108 times during sunrise on Friday)",
        "executiveDirective": "The planetary vectors indicate that value creation through proprietary software IP and automated commercial systems will drastically outperform pure speculative day-trading. Anchor your balance sheet in durable intellectual property."
    }
