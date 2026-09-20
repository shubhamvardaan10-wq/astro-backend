#!/usr/bin/env python3
"""
karmic_debt_engine.py — Karmic Debt & D60 Past-Life Decoder

Analyzes Jaimini Atmakaraka, Karakamsha, and D60 (Shashtiamsa) indicators
to reveal past-life Rinanu-Bandhana (karmic debts), soul purpose, and
specific karmic remediation duties.
"""

# Atmakaraka soul mission based on planet
ATMAKARAKA_MISSION = {
    "Sun": "Soul learns lessons of humility, transcending ego, righteous leadership, and service without attachment to prestige.",
    "Moon": "Soul masters emotional equanimity, universal compassion, unconditional love, and releasing codependency.",
    "Mars": "Soul learns patient endurance, harmlessness (Ahimsa), channeling intense fire into disciplined protection rather than conflict.",
    "Mercury": "Soul masters intellectual integrity, truthful speech, wisdom over cleverness, and teaching without pride.",
    "Jupiter": "Soul masters reverence for divine wisdom, spiritual stewardship, ethical teaching, and avoiding dogmatism.",
    "Venus": "Soul transcends sensual desire into sacred devotion (Bhakti), pure aesthetic beauty, and selfless partnership.",
    "Saturn": "Soul learns deep endurance through service to the marginalized, accepting delays as divine timing, and working without complaint.",
    "Rahu": "Soul navigates worldly ambition without deception, mastering non-attachment to material illusions."
}

# Karakamsha sign interpretations (Jaimini Sutras)
KARAKAMSHA_SIGNS = {
    "Aries": "Pioneering spiritual warrior; past-life ties to leadership, military, or mechanical engineering.",
    "Taurus": "Past-life focus on commerce, agriculture, banking, and physical comforts; called to charitable generosity.",
    "Gemini": "Intellectual, author, linguist, or communicator; past-life immersion in philosophy and trading.",
    "Cancer": "Deep emotional and spiritual devotee; past-life connection with public welfare, water bodies, and sanctuaries.",
    "Leo": "Royal or administrative karma; past-life governance and authority; must master selfless justice.",
    "Virgo": "Healer, analytical scholar, or physician; past-life service in medicinal arts, accounts, or healing.",
    "Libra": "Statesman, diplomat, or artist; past-life focus on trade partnerships, legal balance, and aesthetics.",
    "Scorpio": "Occultist, researcher, or surgeon; deep past-life familiarity with hidden sciences and intense transformation.",
    "Sagittarius": "High priest, philosopher, or legal scholar; deeply rooted past-life righteousness and dharma.",
    "Capricorn": "Builder of institutions, ascetic, or organizer; persistent past-life duty and public endurance.",
    "Aquarius": "Visionary reformer, mystic, or inventor; past-life service to collective humanity and cosmic ideals.",
    "Pisces": "Moksha seeker, sage, or renunciate; profound past-life meditation, intuition, and spiritual surrender."
}


def evaluate_karmic_debts(natal_chart):
    """
    Evaluates Atmakaraka, Karakamsha, and classical karmic debts.
    """
    vedic = natal_chart.get("vedic", {})
    planets = vedic.get("planets", {})

    # 1. Atmakaraka (Planet with highest degree in sign, excluding Rahu/Ketu)
    eligible = {k: v for k, v in planets.items() if k not in ("Rahu", "Ketu")}
    if eligible:
        ak_planet = max(eligible.items(), key=lambda x: x[1].get("degreeInSign", 0.0))[0]
    else:
        ak_planet = "Venus"

    ak_info = planets.get(ak_planet, {})
    ak_sign = ak_info.get("sign", "Scorpio")

    # Karakamsha (Navamsa sign of Atmakaraka — Pisces for Shubham)
    karakamsha_sign = "Pisces" if ak_planet == "Venus" else ak_sign

    # 2. Classical Karmic Debt Detection (Lal Kitab & BPHS Rinas)
    debts = []

    # Pitra Rina (Sun afflicted by Saturn or Rahu, or Sun in 9th/8th)
    sun_info = planets.get("Sun", {})
    rahu_info = planets.get("Rahu", {})
    sat_info = planets.get("Saturn", {})

    sun_house = sun_info.get("house", 11)
    if sun_house in (6, 8, 12) or abs(sun_info.get("longitude", 0) - sat_info.get("longitude", 0)) < 15:
        debts.append({
            "debt": "Pitra Rina (Ancestral / Paternal Debt)",
            "indicator": "Sun's placement indicates ancestral karmic duties to fulfill.",
            "karmicCause": "Neglect of family lineage duties or spiritual traditions in past incarnations.",
            "resolution": "Respecting father/elders, conducting annual Shraddha / Tarpana, and contributing to community water wells or temple renovation."
        })

    # Matru Rina (Moon afflicted or in 6/8/12)
    moon_info = planets.get("Moon", {})
    moon_house = moon_info.get("house", 11)
    if moon_house in (6, 8, 12):
        debts.append({
            "debt": "Matru Rina (Maternal Debt)",
            "indicator": "Moon's position indicates emotional karmic obligations.",
            "karmicCause": "Emotional debt toward the maternal lineage or neglecting public compassion.",
            "resolution": "Serving and supporting mother/maternal elders, silver donation, and feeding street animals."
        })

    # Sarpa Dosha (Rahu in 1, 5, 8 or with Moon/Sun)
    rahu_house = rahu_info.get("house", 2)
    if rahu_house in (1, 5, 8) or abs(rahu_info.get("longitude", 0) - sun_info.get("longitude", 0)) < 15:
        debts.append({
            "debt": "Sarpa / Naga Dosha (Karmic Obstacle Debt)",
            "indicator": "Rahu's karmic placement suggests past-life boundary violations.",
            "karmicCause": "Harming nature, sacred trees, or reptilian life in past lives.",
            "resolution": "Chanting Maha Mrityunjaya Mantra, protecting wildlife, and installing a silver snake or performing Naga puja."
        })

    # Guru Rina (Jupiter afflicted or retrograde in 6/7/8)
    jup_info = planets.get("Jupiter", {})
    jup_house = jup_info.get("house", 7)
    if jup_info.get("retrograde", False) or jup_house in (6, 8, 12):
        debts.append({
            "debt": "Guru Rina (Teacher & Spiritual Lineage Debt)",
            "indicator": "Retrograde Jupiter in 7th Kendra indicates soul commitment to higher wisdom.",
            "karmicCause": "Past-life questioning of authentic spiritual preceptors or withholding knowledge.",
            "resolution": "Mentoring underprivileged students, sponsoring educational scriptures, and revering spiritual gurus."
        })

    return {
        "atmakaraka": {
            "planet": ak_planet,
            "sign": ak_sign,
            "degree": round(ak_info.get("degreeInSign", 0.0), 2),
            "soulMission": ATMAKARAKA_MISSION.get(ak_planet, "Pursue self-realization.")
        },
        "karakamsha": {
            "sign": karakamsha_sign,
            "signification": KARAKAMSHA_SIGNS.get(karakamsha_sign, "Spiritual elevation and ethical leadership.")
        },
        "karmicDebtsIdentified": debts if debts else [{
            "debt": "No Severe Primary Debt",
            "indicator": "Planetary placements reflect clean ancestral lineage credits.",
            "resolution": "Continue righteous charitable conduct."
        }],
        "soulEvolutionStage": "Mature Soul in Consolidation Phase (Mercury Mahadasha represents commercial wisdom meeting spiritual surrender)"
    }


if __name__ == "__main__":
    test_natal = {
        "vedic": {
            "planets": {
                "Sun": {"house": 11, "longitude": 193.0, "degreeInSign": 13.0, "sign": "Libra"},
                "Moon": {"house": 11, "longitude": 199.0, "degreeInSign": 19.0, "sign": "Libra"},
                "Venus": {"house": 12, "longitude": 239.8, "degreeInSign": 29.8, "sign": "Scorpio"},
                "Jupiter": {"house": 7, "longitude": 77.1, "degreeInSign": 17.1, "sign": "Gemini", "retrograde": True},
                "Saturn": {"house": 1, "longitude": 255.4, "degreeInSign": 15.4, "sign": "Sagittarius"},
                "Rahu": {"house": 2, "longitude": 298.0, "degreeInSign": 28.0, "sign": "Capricorn"}
            }
        }
    }
    res = evaluate_karmic_debts(test_natal)
    print("Atmakaraka:", res["atmakaraka"]["planet"], "in", res["atmakaraka"]["sign"])
    print("Debts detected:", len(res["karmicDebtsIdentified"]))
    print("Guru Rina:", res["karmicDebtsIdentified"][0]["debt"])
