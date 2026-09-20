#!/usr/bin/env python3
"""
daily_digest_engine.py — Automated Morning Astro-Briefing Generator

Generates a hyper-personalized, executive 60-second morning briefing:
1. Cosmic Weather Score (0-100) & Day Theme
2. Golden Hour (Abhijit/Amrit Muhurta) & Caution Window (Rahu Kaal)
3. Transit Highlight (Moon & Key Planet alignments)
4. Power Color, Power Direction, Lucky Number & Daily Affirmation
5. Micro-Ritual / Remedial Action
6. Pre-rendered push notification text & HTML widget card
"""

from datetime import datetime
import swisseph as swe

ZODIAC_SIGNS = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

RAHU_KAAL_WINDOWS = {
    0: "07:30 – 09:00 AM",  # Mon
    1: "03:00 – 04:30 PM",  # Tue
    2: "12:00 – 01:30 PM",  # Wed
    3: "01:30 – 03:00 PM",  # Thu
    4: "10:30 – 12:00 PM",  # Fri
    5: "09:00 – 10:30 AM",  # Sat
    6: "04:30 – 06:00 PM"   # Sun
}

DAILY_THEMES = [
    "Executive Strategy & High-Stakes Negotiations",
    "Creative Synthesis & Intellectual Breakthroughs",
    "Commercial Scaling & Financial Organization",
    "Calm Reflection, Internal Grounding & Reset",
    "Dynamic Action & Decisive System Architecture",
    "Networking, Key Alliances & Collaborative Expansion",
    "Deep Focus & Mastery over Complex Problems"
]

COLORS_BY_DAY = {
    0: "Pearl White / Cream",
    1: "Crimson Red / Coral",
    2: "Emerald Green",
    3: "Golden Yellow / Saffron",
    4: "Diamond White / Soft Pink",
    5: "Dark Navy / Royal Blue",
    6: "Deep Amber / Gold"
}

AFFIRMATIONS = [
    "My mind is clear, disciplined, and magnetic to expansive opportunities.",
    "I execute with unshakeable focus and sovereign authority today.",
    "Every challenge is transformed into strategic leverage through wisdom.",
    "Cosmic intelligence guides my speech, decisions, and investments.",
    "I build lasting value with calm grace and intellectual precision."
]


def generate_daily_digest(natal_chart, target_date_str=None):
    """
    Generates a personalized morning digest briefing.
    """
    if target_date_str:
        try:
            dt = datetime.strptime(target_date_str, "%Y-%m-%d")
        except Exception:
            dt = datetime.now()
    else:
        dt = datetime.now()

    weekday = dt.weekday()
    day_name = dt.strftime("%A")
    formatted_date = dt.strftime("%B %d, %Y")

    # Natal info
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign = asc.get("sign", "Sagittarius")
    lagna_idx = asc.get("signIndex", 8)

    # Current Moon transit calculation
    jd = swe.julday(dt.year, dt.month, dt.day, 12.0)
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)
    m_res, _ = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL | swe.FLG_SPEED)
    moon_lon = m_res[0]
    moon_sign_idx = int(moon_lon / 30.0) % 12
    moon_sign = ZODIAC_SIGNS[moon_sign_idx]
    moon_deg = round(moon_lon % 30.0, 1)

    # Transit house from Lagna
    transit_house = ((moon_sign_idx - lagna_idx + 12) % 12) + 1

    # Base score & theme
    base_scores = {1: 88, 2: 82, 3: 84, 4: 76, 5: 91, 6: 74, 7: 85, 8: 68, 9: 94, 10: 92, 11: 95, 12: 70}
    cosmic_score = base_scores.get(transit_house, 82)
    # Day variation modifier
    cosmic_score = max(55, min(98, cosmic_score + ((dt.day * 3) % 7) - 3))

    theme_idx = (dt.day + dt.month + transit_house) % len(DAILY_THEMES)
    day_theme = DAILY_THEMES[theme_idx]

    rahu_window = RAHU_KAAL_WINDOWS.get(weekday, "10:30 – 12:00 PM")
    golden_window = "11:45 AM – 12:35 PM (Abhijit Muhurta — Peak Solar Alignment)"
    amrit_window = "07:30 AM – 09:00 AM (Amrit Kaal — Ideal for Strategy & Meditation)"

    power_color = COLORS_BY_DAY.get(weekday, "Golden Saffron")
    power_direction = "North-East (Ishan)" if weekday in (0, 3) else ("East" if weekday in (1, 6) else "North")
    lucky_num = ((dt.day + weekday) % 9) + 1
    affirmation = AFFIRMATIONS[(dt.day) % len(AFFIRMATIONS)]

    transit_highlight = f"Transit Moon is transiting your {transit_house}th house in {moon_sign} ({moon_deg}°). "
    if transit_house in (1, 5, 9):
        transit_highlight += "This activates a Dharma Trikona alignment, bringing high mental clarity, creative inspiration, and instinctive good judgment."
    elif transit_house in (2, 10, 11):
        transit_highlight += "This energizes your Artha/Karma sectors, ideal for business deals, client discussions, and strategic monetization."
    elif transit_house in (3, 7):
        transit_highlight += "This sparks dynamic partnerships, persuasive communications, and decisive collaborative moves."
    else:
        transit_highlight += "This signals internal consolidation, deep research, and restorative discipline. Protect your focus from external noise."

    micro_ritual = "Offer fresh water in a copper vessel facing East at sunrise; practice 5 minutes of mindful Pranayama to ground nervous energy."

    # Mobile / Push summary text (ready for WhatsApp / SMS / Push)
    briefing_text = (
        f"☀️ Astro-Briefing for {day_name}, {formatted_date}\n"
        f"⚡ Cosmic Weather Score: {cosmic_score}/100 ({day_theme})\n"
        f"✨ Golden Hour: {golden_window}\n"
        f"⚠️ Caution Window (Rahu Kaal): {rahu_window}\n"
        f"🎯 Power Alignment: Facing {power_direction} | Lucky Color: {power_color} | Number: {lucky_num}\n"
        f"💡 Focus: {transit_highlight}\n"
        f"🧘 Micro-Ritual: {micro_ritual}"
    )

    # HTML Summary Card for Dashboards / Email
    html_card = f"""
    <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 580px; border-radius: 14px; background: linear-gradient(135deg, #0d1117 0%, #161b22 100%); color: #c9d1d9; padding: 24px; border: 1px solid #30363d; box-shadow: 0 8px 24px rgba(0,0,0,0.4);">
      <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #21262d; padding-bottom: 14px; margin-bottom: 18px;">
        <div>
          <h2 style="margin: 0; font-size: 20px; color: #58a6ff;">Daily Astro-Briefing</h2>
          <div style="font-size: 13px; color: #8b949e;">{day_name}, {formatted_date} • Lagna: {lagna_sign}</div>
        </div>
        <div style="text-align: right;">
          <div style="font-size: 26px; font-weight: 700; color: {'#3fb950' if cosmic_score >= 80 else '#d29922'};">{cosmic_score}<span style="font-size: 14px; font-weight: 400; color: #8b949e;">/100</span></div>
          <div style="font-size: 11px; text-transform: uppercase; letter-spacing: 0.5px; color: #8b949e;">Cosmic Weather</div>
        </div>
      </div>
      <div style="background: rgba(56, 139, 253, 0.1); border-left: 3px solid #58a6ff; padding: 10px 14px; border-radius: 4px; margin-bottom: 16px; font-size: 14px; font-weight: 500; color: #f0f6fc;">
        Theme: {day_theme}
      </div>
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 16px;">
        <div style="background: #21262d; padding: 12px; border-radius: 8px;">
          <div style="font-size: 11px; color: #3fb950; font-weight: 600; text-transform: uppercase;">✨ Golden Hour</div>
          <div style="font-size: 13px; font-weight: 600; color: #f0f6fc; margin-top: 4px;">11:45 AM – 12:35 PM</div>
          <div style="font-size: 11px; color: #8b949e;">Abhijit Muhurta</div>
        </div>
        <div style="background: #21262d; padding: 12px; border-radius: 8px;">
          <div style="font-size: 11px; color: #f85149; font-weight: 600; text-transform: uppercase;">⚠️ Caution Window</div>
          <div style="font-size: 13px; font-weight: 600; color: #f0f6fc; margin-top: 4px;">{rahu_window}</div>
          <div style="font-size: 11px; color: #8b949e;">Rahu Kaal (Avoid start of pacts)</div>
        </div>
      </div>
      <div style="background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 14px; margin-bottom: 16px; font-size: 13px; line-height: 1.5;">
        <strong style="color: #79c0ff;">Cosmic Trigger:</strong> {transit_highlight}
      </div>
      <div style="display: flex; justify-content: space-between; font-size: 12px; color: #8b949e; border-top: 1px solid #21262d; padding-top: 12px;">
        <span>🎨 Power Color: <strong style="color: #f0f6fc;">{power_color}</strong></span>
        <span>🧭 Direction: <strong style="color: #f0f6fc;">{power_direction}</strong></span>
        <span>🔢 Lucky #: <strong style="color: #f0f6fc;">{lucky_num}</strong></span>
      </div>
    </div>
    """

    return {
        "date": dt.strftime("%Y-%m-%d"),
        "dayName": day_name,
        "cosmicWeatherScore": cosmic_score,
        "dayTheme": day_theme,
        "timingWindows": {
            "goldenHour": golden_window,
            "amritKaal": amrit_window,
            "cautionHourRahuKaal": rahu_window
        },
        "dailyEnergyAlignments": {
            "powerColor": power_color,
            "powerDirection": power_direction,
            "luckyNumber": lucky_num,
            "affirmation": affirmation
        },
        "transitHighlight": {
            "transitMoonSign": moon_sign,
            "transitHouseFromLagna": transit_house,
            "description": transit_highlight
        },
        "microRitual": micro_ritual,
        "briefingCardText": briefing_text,
        "htmlSummaryCard": html_card.strip()
    }


if __name__ == "__main__":
    test_natal = {
        "vedic": {
            "ascendant": {"sign": "Sagittarius", "signIndex": 8}
        }
    }
    res = generate_daily_digest(test_natal)
    print("Daily Digest Test:")
    print("  Date:", res["date"], f"({res['dayName']})")
    print("  Cosmic Score:", res["cosmicWeatherScore"], "/ 100")
    print("  Day Theme:", res["dayTheme"])
    print("  Rahu Kaal:", res["timingWindows"]["cautionHourRahuKaal"])
    print("  Lucky Number:", res["dailyEnergyAlignments"]["luckyNumber"])
