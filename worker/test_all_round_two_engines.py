#!/usr/bin/env python3
"""
test_all_round_two_engines.py — Unit tests for Lal Kitab, Panchangam, Dosha Cancellation, Market Gann, Famous Charts, and Report Customizer
"""

import unittest
from worker.lal_kitab_engine import calculate_lal_kitab
from worker.panchangam_engine import calculate_panchangam
from worker.dosha_cancellation_engine import evaluate_dosha_cancellation
from worker.market_gann_engine import calculate_market_gann
from worker.famous_charts_engine import search_famous_charts
from worker.report_customizer_engine import customize_report

class RoundTwoEnginesTest(unittest.TestCase):

    def setUp(self):
        self.sample_natal = {
            "vedic": {
                "ascendant": {"sign": "Leo", "signIndex": 4, "longitude": 125.0},
                "planets": {
                    "Sun": {"name": "Sun", "longitude": 240.0, "house": 5, "sign": "Sagittarius"},
                    "Moon": {"name": "Moon", "longitude": 120.0, "house": 1, "sign": "Leo"},
                    "Mars": {"name": "Mars", "longitude": 15.0, "house": 9, "sign": "Aries"},
                    "Mercury": {"name": "Mercury", "longitude": 250.0, "house": 5, "sign": "Sagittarius"},
                    "Jupiter": {"name": "Jupiter", "longitude": 90.0, "house": 12, "sign": "Cancer"},
                    "Venus": {"name": "Venus", "longitude": 270.0, "house": 6, "sign": "Capricorn"},
                    "Saturn": {"name": "Saturn", "longitude": 300.0, "house": 7, "sign": "Aquarius"}
                }
            }
        }

    def test_lal_kitab(self):
        res = calculate_lal_kitab(self.sample_natal)
        self.assertEqual(res["engine"], "Classical Lal Kitab Kundli, Karmic Debts & Upayas Engine")
        self.assertIn("fixedHouseChart", res)
        self.assertIn("detectedKarmicDebts", res)
        self.assertIn("customizedPracticalUpayas", res)

    def test_panchangam(self):
        res = calculate_panchangam(28.6139, 77.2090, "2026-09-20")
        self.assertEqual(res["engine"], "Full-Fledged Panchangam & Hindu Festival Calendar Engine")
        self.assertIn("fiveLimbsOfPanchang", res)
        self.assertIn("celestialSunAndMoon", res)
        self.assertIn("identifiedHinduFestival", res)

    def test_dosha_cancellation(self):
        res = evaluate_dosha_cancellation(self.sample_natal)
        self.assertEqual(res["engine"], "Deep Manglik & Nadi Dosha Cancellation Matrix")
        self.assertIn("activeCancellationsTriggered", res)
        self.assertIn("finalManglikVerdict", res)

    def test_market_gann(self):
        res = calculate_market_gann("BTC", "2026-09-20")
        self.assertEqual(res["engine"], "Astro-Financial & W.D. Gann Square of 9 Timing Engine")
        self.assertIn("gannGeometricHarmonics", res)
        self.assertIn("trendBias", res)

    def test_famous_charts(self):
        res = search_famous_charts(query="Einstein")
        self.assertEqual(res["engine"], "Famous & Historic Horoscopes Database & Search Engine")
        self.assertTrue(res["matchesFound"] >= 1)
        self.assertEqual(res["results"][0]["name"], "Albert Einstein")

        res_yoga = search_famous_charts(yoga="Gajakesari")
        self.assertTrue(res_yoga["matchesFound"] >= 1)

    def test_report_customizer(self):
        res = customize_report("Antigravity Astro", "ROYAL_GOLD", "Acharya Shastry", "PRIVATE")
        self.assertEqual(res["engine"], "White-Label Enterprise Report Designer & Customizer")
        self.assertEqual(res["selectedTheme"], "ROYAL_GOLD")
        self.assertIn("stylingPalette", res)

if __name__ == "__main__":
    unittest.main()
