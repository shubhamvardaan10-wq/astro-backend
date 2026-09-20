#!/usr/bin/env python3
"""
test_flagship_engines.py — Unit tests for Varshaphala, Synastry/Composite, and BTR Engines
"""

import unittest
from worker.varshaphala_engine import calculate_varshaphala
from worker.synastry_composite_engine import calculate_synastry_and_composite
from worker.btr_engine import rectify_birth_time

class FlagshipEnginesTest(unittest.TestCase):

    def setUp(self):
        self.sample_natal = {
            "vedic": {
                "ascendant": {
                    "sign": "Sagittarius",
                    "signIndex": 8,
                    "degree": 12.5,
                    "longitude": 252.5
                },
                "planets": {
                    "Sun": {"name": "Sun", "longitude": 240.0, "signIndex": 8, "degreeInSign": 0.0},
                    "Moon": {"name": "Moon", "longitude": 120.0, "signIndex": 4, "degreeInSign": 0.0},
                    "Mars": {"name": "Mars", "longitude": 180.0, "signIndex": 6, "degreeInSign": 0.0},
                    "Mercury": {"name": "Mercury", "longitude": 250.0, "signIndex": 8, "degreeInSign": 10.0},
                    "Jupiter": {"name": "Jupiter", "longitude": 90.0, "signIndex": 3, "degreeInSign": 0.0},
                    "Venus": {"name": "Venus", "longitude": 270.0, "signIndex": 9, "degreeInSign": 0.0},
                    "Saturn": {"name": "Saturn", "longitude": 300.0, "signIndex": 10, "degreeInSign": 0.0}
                }
            },
            "time": "14:30:00"
        }

        self.sample_partner = {
            "vedic": {
                "ascendant": {
                    "sign": "Aries",
                    "signIndex": 0,
                    "degree": 15.0,
                    "longitude": 15.0
                },
                "planets": {
                    "Sun": {"name": "Sun", "longitude": 210.0, "signIndex": 7, "degreeInSign": 0.0},
                    "Moon": {"name": "Moon", "longitude": 240.0, "signIndex": 8, "degreeInSign": 0.0},
                    "Mars": {"name": "Mars", "longitude": 140.0, "signIndex": 4, "degreeInSign": 20.0},
                    "Mercury": {"name": "Mercury", "longitude": 220.0, "signIndex": 7, "degreeInSign": 10.0},
                    "Jupiter": {"name": "Jupiter", "longitude": 120.0, "signIndex": 4, "degreeInSign": 0.0},
                    "Venus": {"name": "Venus", "longitude": 140.0, "signIndex": 4, "degreeInSign": 20.0},
                    "Saturn": {"name": "Saturn", "longitude": 310.0, "signIndex": 10, "degreeInSign": 10.0}
                }
            }
        }

    def test_varshaphala_calculation(self):
        res = calculate_varshaphala(self.sample_natal, target_year=2026, birth_date_str="1990-12-15")
        self.assertEqual(res["engine"], "Tajika Nilakanthi Varshaphala (Solar Return) Engine")
        self.assertEqual(res["targetYear"], 2026)
        self.assertEqual(res["ageInYear"], 36)
        self.assertIn("varshaLagna", res)
        self.assertIn("muntha", res)
        self.assertIn("panchaadhikaris", res)
        self.assertIn("muddaDashaSchedule", res)
        self.assertEqual(len(res["muddaDashaSchedule"]), 9)
        self.assertIn("tajikaYogas", res)
        self.assertIn("harshaBala", res)
        self.assertIn("annualSynthesis", res)

    def test_synastry_composite_calculation(self):
        res = calculate_synastry_and_composite(self.sample_natal, self.sample_partner, relationship_type="ROMANTIC")
        self.assertEqual(res["engine"], "Western Synastry & Midpoint Composite Relationship Engine")
        self.assertIn("overallSynergyScore", res)
        self.assertIn("multiDimensionalScores", res)
        self.assertIn("prominentSynastryAspects", res)
        self.assertIn("houseOverlays", res)
        self.assertIn("midpointCompositeChart", res)
        self.assertIn("davisonTimeSpaceChart", res)
        self.assertTrue(len(res["prominentSynastryAspects"]) > 0)
        self.assertEqual(res["midpointCompositeChart"]["compositeAscendant"]["sign"], "Aquarius")

    def test_birth_time_rectification_calculation(self):
        events = [
            {"eventType": "MARRIAGE", "eventDate": "2018-11-10", "description": "Wedding"},
            {"eventType": "CAREER_BREAKTHROUGH", "eventDate": "2021-04-01", "description": "VP Promotion"}
        ]
        res = rectify_birth_time(self.sample_natal, uncertainty_minutes=15, step_minutes=2, gender="MALE", life_events=events)
        self.assertEqual(res["engine"], "Vedic Birth Time Rectification (BTR) Assistant Engine")
        self.assertIn("optimalRectifiedTime", res)
        self.assertIn("timeAdjustmentMinutes", res)
        self.assertIn("confidenceScore", res)
        self.assertIn("rectifiedVargaLagnas", res)
        self.assertIn("tattvaShodhanaVerification", res)
        self.assertIn("eventCorrelations", res)
        self.assertTrue(len(res["topCandidateRankings"]) <= 5)

if __name__ == "__main__":
    unittest.main()
