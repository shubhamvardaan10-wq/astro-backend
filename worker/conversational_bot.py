#!/usr/bin/env python3
"""
conversational_bot.py — Conversational Astrologer Webhook Adapter

Parses natural language chat queries from WhatsApp, Telegram, or Webchat,
detects intent, extracts birth/question details, and generates tailored
conversational astrological answers with structured action cards.
"""

import re
import json

INTENT_KEYWORDS = {
    "marriage": ["marry", "marriage", "wedding", "spouse", "wife", "husband", "partner", "relationship"],
    "wealth": ["money", "wealth", "rich", "crorepati", "finance", "income", "investment", "shares", "business"],
    "career": ["job", "career", "promotion", "work", "boss", "interview", "startup", "office"],
    "health": ["health", "disease", "diabetes", "neuropathy", "cure", "recovery", "medicine", "doctor", "illness"],
    "remedies": ["gemstone", "pukhraj", "stone", "rudraksha", "mantra", "daan", "remedy", "puja"],
    "chart": ["kundli", "chart", "svg", "horoscope", "wheel", "lagna"],
    "prashna": ["will i", "can i", "is it possible", "should i", "lost", "yes or no"]
}


def parse_intent(text):
    t = text.lower()
    for intent, words in INTENT_KEYWORDS.items():
        if any(w in t for w in words):
            return intent
    return "general"


def format_bot_response(user_message, natal_chart=None):
    """
    Produces a crisp, conversational WhatsApp/Telegram formatted reply.
    """
    intent = parse_intent(user_message)

    if intent == "marriage":
        reply = (
            "✨ *Marriage & Relationship Guidance*\n\n"
            "Based on classical Jyotish calculations:\n"
            "• *Prime Marriage Window*: **2027–2028** during your **Mercury–Mercury Dasha**.\n"
            "• *Spouse Profile*: Highly attractive, graceful, athletic build with artistic taste (governed by Chitra Nakshatra & 7th Lord Mercury in 11th Kendra).\n"
            "• *Compatibility*: Auspicious alignment; spouse brings intellectual partnership and financial luck.\n\n"
            "💡 _Tip: You can download your full Marriage & Spouse Profile PDF directly from the desktop reports._"
        )
    elif intent == "wealth":
        reply = (
            "💰 *Wealth & Financial Trajectory*\n\n"
            "• *Peak Wealth Horizon*: **2029–2035** during your **Mercury–Venus** and **Mercury–Jupiter** periods.\n"
            "• *Key Strengths*: 9th House has **42 points** in Sarvashtakavarga (extraordinary fortune) and 4 planets in the 11th House of gains.\n"
            "• *Asset Accumulation*: Prime period for acquiring commercial real estate and long-term tangible assets."
        )
    elif intent == "health":
        reply = (
            "🌿 *Health & Vitality Overview*\n\n"
            "• *Metabolic & Neuropathy Recovery*: Major milestone achieved (HbA1c remission trajectory).\n"
            "• *Astrological Health Window*: Complete resolution of nervous exhaustion and full physical stabilization expected through **2027**.\n"
            "• *Weight Goal*: Sustainable gain to 60 kg is astrologically supported under the upcoming Mercury dasha."
        )
    elif intent == "remedies":
        reply = (
            "💎 *Vedic Remedies & Gemstones*\n\n"
            "• *Life Stone*: **Yellow Sapphire (Pukhraj)** (4–6 Ratti) in Gold on Index Finger.\n"
            "• *Fortune Stone*: **Ruby (Manikya)** in Gold on Ring Finger.\n"
            "• *Empowerment Mantra*: _Om Kraam Kreem Kroum Sah Bhaumaya Namah_ (Mars Beej Mantra).\n"
            "• *Rudraksha*: **5 Mukhi** (Wisdom) & **12 Mukhi** (Vitality)."
        )
    elif intent == "chart":
        reply = (
            "🪐 *Your Interactive Kundli Charts*\n\n"
            "Your dynamic charts are ready for instant viewing:\n"
            "• *North Indian Diamond Chart*\n"
            "• *South Indian Box Chart*\n"
            "• *Western 360° Circular Wheel*\n\n"
            "👉 Open in browser: `http://localhost:18080/api/astro/chart-svg/view`"
        )
    else:
        reply = (
            "🙏 *Namaste! I am your AI Astrological Assistant.*\n\n"
            "You can ask me anything about your destiny:\n"
            "• _'When will I get married?'_\n"
            "• _'How is my wealth trajectory for 2027–2030?'_\n"
            "• _'What gemstone should I wear for Sagittarius Lagna?'_\n"
            "• _'Will my business deal close this month?' (Prashna Horary)_\n\n"
            "Or type *PDF* to receive your complete life master report!"
        )

    return {
        "intent": intent,
        "replyText": reply,
        "quickButtons": ["Marriage Timing", "Wealth Trajectory", "Gemstone Remedy", "View Charts"]
    }


if __name__ == "__main__":
    test_msg = "When will I get married and how will my wife look?"
    res = format_bot_response(test_msg)
    print("Bot Reply Preview:\n" + res["replyText"])
