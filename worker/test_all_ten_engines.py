#!/usr/bin/env python3
"""
test_all_ten_engines.py — Unit tests for the 10 new flagship calculation engines
"""

import unittest
from worker.sade_sati_engine import calculate_sade_sati
from worker.calendar_feed_engine import generate_ics_calendar
from worker.planetary_clock_engine import calculate_planetary_clock
from worker.progressions_directions_engine import calculate_progressions_and_directions
from worker.ashtakavarga_kaksha_engine import calculate_kaksha_transits
from worker.bhrigu_nandi_nadi_engine import calculate_bnn
from worker.astrocartography_geojson_engine import generate_astrocartography_geojson
from worker.namakaran_tuning_engine import calculate_namakaran_tuning
from worker.multilingual_report_engine import generate_multilingual_report

class AllTenEnginesTest(unittest.TestCase):

    def setUp(self):
        self.sample_natal = {
            "vedic": {
                "ascendant": {"sign": "Sagittarius", "signIndex": 8, "longitude": 252.5, "degree": 12.5},
                "planets": {
                    "Sun": {"name": "Sun", "longitude": 240.0, "signIndex": 8, "degreeInSign": 0.0},
                    "Moon": {"name": "Moon", "longitude": 120.0, "signIndex": 4, "degreeInSign": 0.0, "sign": "Leo"},
                    "Mars": {"name": "Mars", "longitude": 180.0, "signIndex": 6, "degreeInSign": 0.0},
                    "Mercury": {"name": "Mercury", "longitude": 250.0, "signIndex": 8, "degreeInSign": 10.0},
                    "Jupiter": {"name": "Jupiter", "longitude": 90.0, "signIndex": 3, "degreeInSign": 0.0},
                    "Venus": {"name": "Venus", "longitude": 270.0, "signIndex": 9, "degreeInSign": 0.0},
                    "Saturn": {"name": "Saturn", "longitude": 300.0, "signIndex": 10, "degreeInSign": 0.0}
                }
            },
            "dob": "1990-12-15",
            "time": "14:30:00"
        }

    def test_sade_sati(self):
        res = calculate_sade_sati(self.sample_natal, 30)
        self.assertEqual(res["engine"], "Vedic Saturn Sade Sati, Dhaiya & Kantaka Shani Engine")
        self.assertIn("activeSadeSatiStatus", res)
        self.assertIn("sadeSatiLifetimeCycles", res)
        self.assertTrue(len(res["classicalRemedies"]) > 0)

    def test_calendar_feed(self):
        res = generate_ics_calendar(self.sample_natal, 2026)
        self.assertEqual(res["engine"], "Personal Astrological Calendar (.ics / CalDAV Feed) Generator")
        self.assertIn("icsCalendarFeed", res)
        self.assertTrue("BEGIN:VCALENDAR" in res["icsCalendarFeed"])
        self.assertTrue("END:VCALENDAR" in res["icsCalendarFeed"])

    def test_planetary_clock(self):
        res = calculate_planetary_clock(28.6139, 77.2090)
        self.assertEqual(res["engine"], "Real-Time Planetary Clock & Sky Live Stream Engine")
        self.assertIn("liveAscendant", res)
        self.assertIn("currentHora", res)
        self.assertIn("activeChoghadiya", res)
        self.assertIn("inauspiciousWindows", res)

    def test_progressions_directions(self):
        res = calculate_progressions_and_directions(self.sample_natal, "2026-09-20")
        self.assertEqual(res["engine"], "Western Secondary Progressions & Solar Arc Directions Engine")
        self.assertIn("progressedMoonPhase", res)
        self.assertIn("secondaryProgressedPlanets", res)
        self.assertIn("solarArcDirectedPoints", res)

    def test_ashtakavarga_kaksha(self):
        res = calculate_kaksha_transits(self.sample_natal, "2026-09-20")
        self.assertEqual(res["engine"], "Ashtakavarga Transit Heatmap & Kaksha Precision Engine")
        self.assertIn("overallDailyProductivityIndex", res)
        self.assertIn("kakshaTransitDetails", res)

    def test_bhrigu_nandi_nadi(self):
        res = calculate_bnn(self.sample_natal)
        self.assertEqual(res["engine"], "Bhrigu Nandi Nadi (BNN) Directional Alignment & Combinations Engine")
        self.assertIn("directionalZodiacGrouping", res)
        self.assertIn("prominentNadiYogas", res)

    def test_astrocartography_geojson(self):
        res = generate_astrocartography_geojson(self.sample_natal)
        self.assertEqual(res["engine"], "Astrocartography GeoJSON Vector Line Generator")
        self.assertEqual(res["geoJson"]["type"], "FeatureCollection")
        self.assertTrue(len(res["geoJson"]["features"]) > 0)

    def test_namakaran_tuning(self):
        res = calculate_namakaran_tuning(self.sample_natal, "MALE", "SANSKRIT")
        self.assertEqual(res["engine"], "Sacred Vedic Baby Namakaran & Phonetic Tuning Engine")
        self.assertIn("sacredStartingSyllables", res)
        self.assertIn("curatedCosmicNames", res)

    def test_multilingual_report(self):
        res_hi = generate_multilingual_report(self.sample_natal, "hi")
        self.assertEqual(res_hi["engine"], "Native Multilingual Astrological Synthesis (i18n)")
        self.assertEqual(res_hi["targetLanguage"], "hi")
        self.assertIn("वैदिक", res_hi["reportHeader"])

        res_ta = generate_multilingual_report(self.sample_natal, "ta")
        self.assertEqual(res_ta["targetLanguage"], "ta")

        res_es = generate_multilingual_report(self.sample_natal, "es")
        self.assertEqual(res_es["targetLanguage"], "es")

if __name__ == "__main__":
    unittest.main()
