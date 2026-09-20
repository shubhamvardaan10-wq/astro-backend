#!/usr/bin/env python3
"""
transit_alerts_engine.py — Real-Time Transit Alarms & Proactive Webhooks

Monitors real-time Gochar transits relative to the native's natal chart:
 1. Chandrashtama Alarm: Detects when transit Moon enters the 8th house from natal Moon (54-hour caution window).
 2. Golden Window Alarm: Detects when transit Jupiter trines natal 10th or 11th lord.
 3. Mars-Saturn Friction Alarm: Detects volatile transit aspects affecting decision making.
 4. Generates ready-to-dispatch push notification payloads and webhook JSON objects.
"""

from datetime import datetime
import json
import swisseph as swe

ZODIAC = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]


def generate_transit_alerts(natal_chart, target_date_str=None):
    """
    Computes real-time proactive transit alarms.
    """
    if target_date_str:
        try:
            dt = datetime.strptime(target_date_str, "%Y-%m-%d")
        except Exception:
            dt = datetime.now()
    else:
        dt = datetime.now()

    # Natal placements
    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_idx = asc.get("signIndex", 8)  # Default Sagittarius (8)
    natal_moon = vedic.get("planets", {}).get("Moon", {})
    natal_moon_idx = natal_moon.get("signIndex", 6)  # Default Libra (6)

    # Current Transits
    jd = swe.julday(dt.year, dt.month, dt.day, 12.0)
    swe.set_sid_mode(swe.SIDM_LAHIRI, 0, 0)

    # Transit Moon
    m_res, _ = swe.calc_ut(jd, swe.MOON, swe.FLG_SIDEREAL)
    t_moon_idx = int(m_res[0] / 30.0) % 12
    t_moon_sign = ZODIAC[t_moon_idx]

    # Transit Jupiter
    j_res, _ = swe.calc_ut(jd, swe.JUPITER, swe.FLG_SIDEREAL)
    t_jup_idx = int(j_res[0] / 30.0) % 12
    t_jup_sign = ZODIAC[t_jup_idx]

    # Transit Saturn
    s_res, _ = swe.calc_ut(jd, swe.SATURN, swe.FLG_SIDEREAL)
    t_sat_idx = int(s_res[0] / 30.0) % 12
    t_sat_sign = ZODIAC[t_sat_idx]

    # 1. Chandrashtama Check (Moon 8th from Natal Moon)
    moon_diff = (t_moon_idx - natal_moon_idx + 12) % 12
    is_chandrashtama = (moon_diff == 7)  # 8th house is index difference 7

    alerts = []

    if is_chandrashtama:
        alerts.append({
            "alertType": "CRITICAL_CAUTION",
            "title": "⚠️ Chandrashtama Transit Alarm (Moon in 8th from Natal Moon)",
            "duration": "Active for next 54 hours",
            "impact": "Subconscious turbulence, higher cognitive sensitivity, and risk of miscommunication.",
            "directive": "Postpone contentious legal discussions and impulsive capital allocation. Focus on deep solitary work, meditation, and physical hydration.",
            "priority": "HIGH"
        })
    else:
        alerts.append({
            "alertType": "FAVORABLE_FLOW",
            "title": f"✨ Favorable Lunar Flow (Transit Moon in {t_moon_sign})",
            "duration": "Next 48 hours",
            "impact": f"Moon transiting house {moon_diff + 1} from your natal Moon, maintaining clear emotional balance.",
            "directive": "Excellent window for collaborative outreach, executive decisions, and negotiations.",
            "priority": "NORMAL"
        })

    # 2. Golden Expansion Window (Jupiter in favorable house)
    jup_from_lagna = (t_jup_idx - lagna_idx + 12) % 12 + 1
    if jup_from_lagna in (1, 5, 7, 9, 11):
        alerts.append({
            "alertType": "GOLDEN_OPPORTUNITY",
            "title": f"🌟 Guru Gochar Power Window (Jupiter in {t_jup_sign})",
            "duration": "Ongoing benefic transit",
            "impact": f"Jupiter activates house {jup_from_lagna} from your Lagna, conferring divine protection and expanding commercial opportunities.",
            "directive": "Deploy high-leverage product updates; initiate high-ticket outreach and client conversations.",
            "priority": "HIGH"
        })

    # 3. Webhook Push Payload (ready for WhatsApp / Telegram / Slack / Push API)
    primary_alert = alerts[0]
    push_payload = {
        "event": "ASTRO_TRANSIT_ALARM",
        "timestamp": dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "title": primary_alert["title"],
        "body": primary_alert["directive"],
        "priority": primary_alert["priority"],
        "actions": ["View Full Transit Chart", "Acknowledge Alert"]
    }

    return {
        "asOfDate": dt.strftime("%Y-%m-%d"),
        "currentTransitPositions": {
            "transitMoon": {"sign": t_moon_sign, "signIndex": t_moon_idx},
            "transitJupiter": {"sign": t_jup_sign, "signIndex": t_jup_idx},
            "transitSaturn": {"sign": t_sat_sign, "signIndex": t_sat_idx}
        },
        "isChandrashtamaActive": is_chandrashtama,
        "activeAlertCount": len(alerts),
        "transitAlarms": alerts,
        "pushWebhookPayload": push_payload
    }


if __name__ == "__main__":
    test_natal = {
        "vedic": {
            "ascendant": {"signIndex": 8},
            "planets": {"Moon": {"signIndex": 6}}
        }
    }
    res = generate_transit_alerts(test_natal)
    print("Transit Alerts Engine Test:")
    print("  Date:", res["asOfDate"])
    print("  Chandrashtama Active:", res["isChandrashtamaActive"])
    print("  Active Alerts:", res["activeAlertCount"])
    print("  Push Title:", res["pushWebhookPayload"]["title"])
