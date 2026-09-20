#!/usr/bin/env python3
"""
gemology_engine.py — Precision Astro-Gemology & Crystal Yantra Grid

Calculates individualized, body-mass-indexed astrological gemology:
 1. Exact Carat Weight Formula: (Body Weight in kg / 10) + 0.5 carats.
 2. Primary Sovereign Gemstone (Lagneesh / Bhagyesh).
 3. Secondary Up-Ratna Alternatives (Affordable & Energetically Tuned).
 4. Consecration Protocols: Metal setting, auspicious finger, day, hora, and Beej Mantra.
 5. 2D Sacred Crystal Yantra Grid for office desk or meditation altar.
"""

from datetime import datetime
import json

GEM_DATA = {
    "Sagittarius": {
        "primaryGem": "Yellow Sapphire (Pukhraj)",
        "rulingPlanet": "Jupiter (Devaguru)",
        "upRatnas": ["Yellow Topaz", "Citrine (Sunela)", "Heliodor"],
        "metal": "22k or 18k Yellow Gold / Brass",
        "finger": "Index Finger (Tarjani) of active right hand",
        "dayAndHora": "Thursday morning during Shukla Paksha (Guru Hora at sunrise)",
        "mantra": "Om Graam Greem Graum Sah Gurave Namaha (108 times)",
        "benefits": "Expands executive authority, brings divine wisdom, protects wealth, and stabilizes higher purpose."
    },
    "default": {
        "primaryGem": "Emerald (Panna)",
        "rulingPlanet": "Mercury (Budh)",
        "upRatnas": ["Green Tourmaline (Verdelite)", "Peridot", "Tsavorite Garnet"],
        "metal": "Panchadhatu (5-Metal Alloy) or 18k Yellow Gold",
        "finger": "Little Finger (Kanishtha) of active right hand",
        "dayAndHora": "Wednesday morning during Shukla Paksha (Budh Hora at sunrise)",
        "mantra": "Om Braam Breem Braum Sah Budhaya Namaha (108 times)",
        "benefits": "Maximizes commercial intellect, mathematical precision, software architecture discernment, and income flow."
    }
}


def compute_gemology_profile(natal_chart, body_weight_kg=70.0):
    """
    Computes precise carat weight and gemology matrix.
    """
    weight = max(40.0, min(140.0, float(body_weight_kg)))
    carat_weight = round((weight / 10.0) + 0.5, 2)
    ratti_weight = round(carat_weight * 1.1, 2)  # Traditional Indian Ratti conversion

    vedic = natal_chart.get("vedic", {})
    asc = vedic.get("ascendant", {})
    lagna_sign = asc.get("sign", "Sagittarius")

    gem_info = GEM_DATA.get(lagna_sign, GEM_DATA["default"])
    mercury_info = GEM_DATA["default"]

    # 2D Crystal Yantra Grid Layout for Workstation Desk
    crystal_grid = {
        "gridName": "Sovereign Kubera-Lakshmi Crystal Matrix",
        "centerStone": "Natural Pyrite Cluster (Fool's Gold) — Placed in center for magnetic commercial wealth and solar grounding.",
        "fourCorners": [
            {"corner": "North-East (Ishan)", "crystal": "Clear Quartz Crystal Point", "purpose": "Channeling high-frequency intellectual clarity and mental focus."},
            {"corner": "South-East (Agneya)", "crystal": "Carnelian / Red Jasper", "purpose": "Fueling execution drive, fire energy, and technical stamina."},
            {"corner": "South-West (Nairruti)", "crystal": "Black Tourmaline", "purpose": "Grounding corporate authority and deflecting negative psychic interference."},
            {"corner": "North (Kuber)", "crystal": "Green Aventurine / Malachite", "purpose": "Attracting continuous commercial revenue and software contracts."}
        ],
        "activationAffirmation": "My mind is an open channel for infinite divine intelligence, and my work creates lasting, compounded value for the world."
    }

    return {
        "subjectLagna": lagna_sign,
        "inputBodyWeightKg": weight,
        "precisionDosage": {
            "recommendedCaratWeight": carat_weight,
            "traditionalRattiWeight": ratti_weight,
            "mathematicalFormula": "Body Mass Dosage: (Weight / 10) + 0.5 Carats"
        },
        "sovereignGemstone": {
            "gemstoneName": gem_info["primaryGem"],
            "governingPlanet": gem_info["rulingPlanet"],
            "secondaryUpRatnas": gem_info["upRatnas"],
            "metalSetting": gem_info["metal"],
            "fingerPlacement": gem_info["finger"],
            "consecrationProtocol": {
                "timing": gem_info["dayAndHora"],
                "vedicMantra": gem_info["mantra"],
                "purification": "Immerse in raw cow milk and Ganga water overnight; energize facing East."
            },
            "karmicBenefits": gem_info["benefits"]
        },
        "wealthCatalystGemstone": {
            "gemstoneName": mercury_info["primaryGem"],
            "purpose": "10th Lord of Enterprise & Software Income",
            "recommendedCarats": round(carat_weight * 0.75, 2),
            "secondaryUpRatnas": mercury_info["upRatnas"],
            "finger": mercury_info["finger"]
        },
        "workstationCrystalYantraGrid": crystal_grid
    }


if __name__ == "__main__":
    test_natal = {"vedic": {"ascendant": {"sign": "Sagittarius"}}}
    res = compute_gemology_profile(test_natal, 65.0)
    print("Gemology Engine Test:")
    print("  Dosage:", res["precisionDosage"]["recommendedCaratWeight"], "Carats")
    print("  Primary Gem:", res["sovereignGemstone"]["gemstoneName"])
    print("  Crystal Grid:", res["workstationCrystalYantraGrid"]["gridName"])
