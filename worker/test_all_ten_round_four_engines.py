#!/usr/bin/env python3
"""
test_all_ten_round_four_engines.py — Unit Tests for 10 Flagship Engines (Round 4)
"""

import unittest
from prashna_horary_engine import calculate_prashna
from sarvatobhadra_engine import calculate_sarvatobhadra
from medical_ayurveda_engine import analyze_medical_astrology
from tara_bala_engine import calculate_tara_bala
from muhurta_engine import find_muhurta
from solar_lunar_return_engine import calculate_solar_lunar_return
from gemstone_rudraksha_engine import recommend_gemstones_and_rudraksha
from draconic_chart_engine import calculate_draconic_chart
from kota_chakra_engine import calculate_kota_chakra
from lo_shu_engine import calculate_lo_shu

class RoundFourEnginesTest(unittest.TestCase):

    def setUp(self):
        self.mock_natal = {
            "vedic": {
                "lagna": {"sign": "Sagittarius", "signIndex": 8, "degree": 7.8},
                "planets": {
                    "Sun": {"sign": "Sagittarius", "signIndex": 8, "house": 1, "degreeInSign": 23.5, "longitude": 263.5},
                    "Moon": {"sign": "Taurus", "signIndex": 1, "house": 6, "degreeInSign": 15.2, "longitude": 45.2},
                    "Mars": {"sign": "Taurus", "signIndex": 1, "house": 6, "degreeInSign": 28.7, "longitude": 58.7},
                    "Saturn": {"sign": "Capricorn", "signIndex": 9, "house": 2, "degreeInSign": 28.6, "longitude": 298.6}
                }
            },
            "western": {
                "ascendant": {"longitude": 247.8},
                "planets": {
                    "Sun": {"longitude": 263.5},
                    "Moon": {"longitude": 45.2},
                    "NorthNode": {"longitude": 312.4}
                }
            }
        }

    def test_prashna_horary(self):
        res = calculate_prashna("CAREER", 108, "2026-09-20 14:30:00", "Will my new initiative succeed?")
        self.assertEqual(res["engine"], "Classical Vedic & KP Horary Astrological Engine")
        self.assertIn("probabilityPercentage", res["karyaSiddhiAssessment"])
        self.assertIn("activeYoga", res["tajikaYogaAnalysis"])
        self.assertIn("lagnesha", res["horaryChart"])

    def test_sarvatobhadra(self):
        res = calculate_sarvatobhadra(self.mock_natal, "2026-09-20")
        self.assertEqual(res["engine"], "Sarvatobhadra Chakra (SBC) & 4-Directional Vedha Engine")
        self.assertIn("sbcFortitudeRating", res)
        self.assertTrue(len(res["activeTransitsAcrossChakra"]) > 0)
        self.assertTrue(len(res["detectedVedhas"]) > 0)

    def test_medical_ayurveda(self):
        res = analyze_medical_astrology(self.mock_natal)
        self.assertEqual(res["engine"], "Medical Astrology & Ayur-Jyotish Tridosha Engine")
        self.assertIn("tridoshaProfile", res)
        self.assertIn("prakritiConstitution", res["tridoshaProfile"])
        self.assertTrue(len(res["dhatuTissueAnalysis"]) > 0)

    def test_tara_bala(self):
        res = calculate_tara_bala(self.mock_natal, "2026-10")
        self.assertEqual(res["engine"], "Navatara Chakra & Daily Tara Bala / Chandra Bala Engine")
        self.assertIn("natalMoonAnchor", res)
        self.assertEqual(res["totalDaysEvaluated"], 14)
        self.assertTrue(len(res["dailyForecastTimeline"]) > 0)

    def test_muhurta(self):
        res = find_muhurta("BUSINESS", "2026-10-01", 7, 28.6139, 77.2090)
        self.assertEqual(res["engine"], "Classical Vedic Muhurta & Electional Timing Assistant")
        self.assertIn("topRankedWindows", res)
        self.assertTrue(len(res["topRankedWindows"]) > 0)

    def test_solar_lunar_return(self):
        solar = calculate_solar_lunar_return(self.mock_natal, 2026, "SOLAR", "New Delhi")
        self.assertEqual(solar["engine"], "Western Solar & Lunar Return Precision Engine")
        self.assertEqual(solar["returnType"], "SOLAR")
        self.assertIn("solarAscendant", solar["returnAngles"])

        lunar = calculate_solar_lunar_return(self.mock_natal, 2026, "LUNAR", "Mumbai")
        self.assertEqual(lunar["returnType"], "LUNAR")

    def test_gemstone_rudraksha(self):
        res = recommend_gemstones_and_rudraksha(self.mock_natal)
        self.assertEqual(res["engine"], "Vedic Gemstone (Ratna) & Rudraksha Recommendation Engine")
        self.assertIn("jeevaRatnaLifeStone", res["prescribedGemstones"])
        self.assertIn("prescribedSacredRudraksha", res)

    def test_draconic_chart(self):
        res = calculate_draconic_chart(self.mock_natal)
        self.assertEqual(res["engine"], "Western Draconic Chart (Soul Purpose & Higher Self) Engine")
        self.assertIn("draconicPlanetaryPositions", res)
        self.assertIn("karmicConjunctionsToNatal", res)

    def test_kota_chakra(self):
        res = calculate_kota_chakra(self.mock_natal, "2026-09-20")
        self.assertEqual(res["engine"], "Classical Vedic Kota Chakra (Fortress Chart) Engine")
        self.assertIn("fortressPillars", res)
        self.assertIn("fortressDefenseRating", res)

    def test_lo_shu(self):
        res = calculate_lo_shu("1990-12-15", "MALE")
        self.assertEqual(res["engine"], "Classical Astro-Numerology & Lo Shu Magic Square Engine")
        self.assertEqual(res["coreVedicNumerology"]["mulankDriverNumber"], 6)
        self.assertIn("topRow", res["loShuGridFrequencies"])
        self.assertTrue(len(res["remedialCures"]) > 0)

    def test_daily_widget(self):
        from daily_widget_engine import calculate_daily_widget
        res = calculate_daily_widget(28.6139, 77.2090, "2026-09-20")
        self.assertEqual(res["engine"], "Unified Daily Cosmic Dashboard Widget Engine")
        self.assertIn("panchangCore", res)
        self.assertIn("realtimeSky", res)
        self.assertIn("dailyAtmosphereSentiment", res)

if __name__ == "__main__":
    unittest.main()
