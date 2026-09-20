#!/usr/bin/env python3
"""
test_final_five_engines.py — Unit Tests for the Final 5 Flagship Engines:
1. Jaimini Chara Dasha & Karakamsha Soul Blueprint
2. KP Sub-Lord 4-Step Theory & Cuspal Significators
3. South Indian Dasa Koota (10-Porutham) Matching
4. Sacred Yantra Sacred Geometry & Planetary Mantras
5. Astro-Genealogy & Pitru Dosha Karmic Lineage
"""

import unittest
from jaimini_engine import calculate_jaimini_details
from kp_engine import calculate_kp_significators
from dasa_koota_engine import calculate_dasa_koota
from yantra_mantra_engine import generate_yantra_mantra
from ancestral_lineage_engine import calculate_ancestral_lineage

class FinalFiveEnginesTest(unittest.TestCase):

    def test_jaimini_engine(self):
        res = calculate_jaimini_details()
        self.assertIn("atmakaraka", res)
        self.assertIn("sevenKarakas", res)
        self.assertIn("karakamsha", res)
        self.assertIn("ishtaDevata", res)
        self.assertIn("arudhaPadas", res)
        self.assertIn("charaDashaTimeline", res)
        self.assertEqual(len(res["charaDashaTimeline"]), 12)
        self.assertIn("recommendedDeity", res["ishtaDevata"])
        self.assertIn("arudhaLagna_AL", res["arudhaPadas"])
        self.assertIn("upapadaLagna_UL", res["arudhaPadas"])

    def test_kp_engine(self):
        res = calculate_kp_significators()
        self.assertIn("planetKpTable", res)
        self.assertIn("cuspalSubLords", res)
        self.assertIn("fourStepTheory", res)
        self.assertIn("rulingPlanets", res)
        self.assertEqual(res["totalKpSubDivisions"], 249)
        self.assertEqual(len(res["cuspalSubLords"]), 12)
        self.assertIn("marriageSeventhCsl", res["fourStepTheory"])
        self.assertIn("careerTenthCsl", res["fourStepTheory"])

    def test_dasa_koota_engine(self):
        res = calculate_dasa_koota()
        self.assertIn("poruthamScore", res)
        self.assertIn("verdict", res)
        self.assertIn("poruthams", res)
        self.assertEqual(len(res["poruthams"]), 10)
        self.assertIn("rajjuDoshaPresent", res)
        self.assertIn("groomProfile", res)
        self.assertIn("brideProfile", res)

    def test_yantra_mantra_engine(self):
        for planet in ["SURYA", "CHANDRA", "MANGAL", "BUDHA", "GURU", "SHUKRA", "SHANI", "RAHU", "KETU"]:
            res = generate_yantra_mantra(planet)
            self.assertTrue(res["mathematicallyVerified"])
            self.assertIn("beejaMantra", res)
            self.assertIn("gayatriMantra", res)
            self.assertGreater(res["recommendedJapaCount"], 0)
            self.assertIn("<svg", res["yantraSvg"])
            self.assertEqual(len(res["gridMatrix"]), 3)
            # Verify every row equals magic sum
            for row in res["gridMatrix"]:
                self.assertEqual(sum(row), res["sacredMagicSum"])

    def test_ancestral_lineage_engine(self):
        res = calculate_ancestral_lineage()
        self.assertIn("pitruDoshaPresent", res)
        self.assertIn("severityScore", res)
        self.assertIn("severityTier", res)
        self.assertIn("detectedAncestralDoshas", res)
        self.assertIn("ancestralKarmicDebts", res)
        self.assertIn("remedialProtocol", res)
        self.assertGreater(len(res["remedialProtocol"]), 0)

if __name__ == "__main__":
    unittest.main()
