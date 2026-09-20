import math
import os
from pathlib import Path
import unittest
from unittest.mock import patch

from astro_core import InputError, EngineUnavailable, birth_input, configure, natal

BIRTH = {"dob": "1990-01-15", "time": "14:30", "city": "Mumbai", "latitude": 19.076, "longitude": 72.8777, "timezone": "Asia/Kolkata"}


class AstronomyTests(unittest.TestCase):
    def test_midnight_and_leap_rollover(self):
        b = birth_input(dict(BIRTH, dob="2000-03-01", time="00:00:01"))
        self.assertEqual(b["utcDateTime"], "2000-02-29T18:30:01Z")

    def test_rejects_nonexistent_dst_time(self):
        with self.assertRaises(InputError):
            birth_input(dict(BIRTH, dob="2024-03-10", time="02:30", timezone="America/New_York"))

    def test_ambiguous_dst_time_requires_explicit_offset(self):
        data = dict(BIRTH, dob="2024-11-03", time="01:30", timezone="America/New_York")
        with self.assertRaises(InputError):
            birth_input(data)
        early = birth_input(dict(data, utcOffset="-04:00"))
        late = birth_input(dict(data, utcOffset="-05:00"))
        self.assertAlmostEqual(late["julianDay"] - early["julianDay"], 1 / 24, places=8)

    def test_rejects_invalid_calendar_coordinates_and_offsets(self):
        for updates in ({"dob": "2001-02-29"}, {"time": "24:00"}, {"latitude": float("nan")},
                        {"longitude": 181}, {"latitude": True}, {"utcOffset": "+01:00"}):
            with self.subTest(updates=updates), self.assertRaises(InputError):
                birth_input(dict(BIRTH, **updates))

    def test_missing_data_does_not_silently_fall_back(self):
        with patch.dict(os.environ, {"ASTRO_EPHE_PATH": str(Path(__file__).parent / "not-installed")}):
            with self.assertRaises(EngineUnavailable):
                configure({})

    def test_real_swiss_data_and_outer_planets(self):
        options = configure({})
        chart = natal(birth_input(BIRTH), options)
        self.assertEqual(len(chart["vedic"]["planets"]), 9)
        self.assertEqual(len(chart["western"]["planets"]), 12)
        self.assertEqual(len(chart["western"]["cusps"]), 12)
        for p in chart["western"]["planets"].values():
            self.assertTrue(math.isfinite(p["longitude"]))
            self.assertIn(p["house"], range(1, 13))
        self.assertAlmostEqual(chart["western"]["ascendant"], 61.9769, delta=0.02)

    def test_ayanamsa_and_node_modes_are_explicit(self):
        birth = birth_input(BIRTH)
        lahiri = natal(birth, configure({"ayanamsa": "lahiri", "nodes": "mean"}))
        raman = natal(birth, configure({"ayanamsa": "raman", "nodes": "true"}))
        self.assertNotEqual(lahiri["vedic"]["planets"]["Moon"]["longitude"], raman["vedic"]["planets"]["Moon"]["longitude"])
        self.assertNotEqual(lahiri["western"]["planets"]["Rahu"]["longitude"], raman["western"]["planets"]["Rahu"]["longitude"])


class TimezoneLookupTests(unittest.TestCase):
    def test_worldwide_timezone_lookup_without_birth_or_ephemeris(self):
        from engine import analyze
        points = [(25.381, 86.465), (40.7128, -74.006), (-33.8688, 151.2093), (51.5074, -0.1278), (1.3521, 103.8198)]
        with patch.dict(os.environ, {"ASTRO_EPHE_PATH": "/not-installed"}):
            result = analyze({"action": "timezones", "locations": [{"latitude": lat, "longitude": lon} for lat, lon in points]})
        self.assertEqual(result["timezones"], ["Asia/Kolkata", "America/New_York", "Australia/Sydney", "Europe/London", "Asia/Singapore"])
        self.assertEqual(result["status"], "complete")

    def test_timezone_lookup_validates_before_loading_data(self):
        from engine import analyze
        for locations in (None, [], [{}], [{"latitude": True, "longitude": 1}],
                          [{"latitude": 91, "longitude": 0}], [{"latitude": 0, "longitude": float("nan")}],
                          [{"latitude": 0, "longitude": 0}] * 6):
            with self.subTest(locations=locations), self.assertRaises(InputError):
                analyze({"action": "timezones", "locations": locations})

    def test_timezone_missing_match_is_not_guessed(self):
        from engine import analyze
        with patch("timezonefinder.TimezoneFinder.timezone_at", return_value=None):
            result = analyze({"action": "timezones", "locations": [{"latitude": 0, "longitude": 0}]})
        self.assertEqual(result["timezones"], [None])


class AdvancedRegressionTests(unittest.TestCase):
    def setUp(self):
        import astro_core as core
        self.core = core
        self.birth = birth_input(BIRTH)
        self.options = configure({})
        self.as_of = core.instant("2026-09-18T00:00:00Z")

    def test_numerology_uses_analysis_date_and_has_month_day(self):
        result = self.core.numerology(self.birth, "Ada Lovelace", self.as_of)
        p = result["systems"]["pythagorean"]
        self.assertEqual((p["personalYear"], p["personalMonth"], p["personalDay"]), (8, 8, 8))

    def test_timing_spread_is_not_silently_three_cards(self):
        result = self.core.tarot("fixed-seed", "timing")
        self.assertEqual(len(result["cards"]), 5)
        self.assertEqual(result, self.core.tarot("fixed-seed", "timing"))
        self.assertEqual(len({c["card"] for c in result["cards"]}), 5)
        with self.assertRaises(InputError):
            self.core.tarot("seed", "not-a-spread")

    def test_shadbala_columns_and_units(self):
        result = self.core.shadbala(self.birth, self.options)
        self.assertEqual(result["unit"], "Virupas")
        for p in result["planets"].values():
            self.assertAlmostEqual(sum(p[k] for k in ("SthanaBala", "KalaBala", "DigBala", "ChestaBala", "NaisargikaBala", "DrikBala")), p["Total"], delta=0.1)
        self.assertAlmostEqual(result["planets"]["Sun"]["NaisargikaBala"], 60, delta=0.01)

    def test_profection_uses_ascendant_and_local_birthday(self):
        result = self.core.annual_profections(self.birth, self.as_of, self.options)
        self.assertEqual(result["profectedHouse"], 1)
        self.assertEqual(result["timeLord"], "Mercury")
        before = self.core.instant("2026-01-14T12:00:00Z")
        self.assertEqual(self.core.annual_profections(self.birth, before, self.options)["profectedHouse"], 12)

    def test_jaimini_upapada_and_eight_karakas(self):
        result = self.core.jaimini(self.birth, self.options)
        self.assertEqual(result["upapadaLagna"], result["bhavaArudhas"]["A12"])
        self.assertEqual(len(result["charaKarakas"]), 8)
        self.assertIn("Pitrukaraka", result["charaKarakas"])

    def test_eclipse_contacts_use_eclipse_longitude_and_natal_houses(self):
        from predictive import eclipse_contacts
        result = eclipse_contacts(self.birth, self.options, self.as_of, 1)
        self.assertEqual(len(result["events"]), 2)
        for event in result["events"]:
            self.assertIn(event["natalHouse"], range(1, 13))
            self.assertTrue(all(c["orb"] <= 3 for c in event["contacts"]))

    def test_topic_evidence_and_scenarios(self):
        from predictive import topic_analysis, scenarios
        r = topic_analysis(self.birth, self.options, self.as_of, "career")
        self.assertIn("D10", r["divisionalCharts"])
        self.assertEqual(r["houses"][0]["number"], 10)
        ids = {i["id"] for i in r["indicators"]}
        s = scenarios(self.birth, self.options, self.as_of, "career", 90, analysis=r)
        self.assertEqual(len(s["scenarios"]), 3)
        self.assertTrue(s["timingWindows"])
        self.assertEqual(s["uncertainty"]["probabilityCalibrated"], False)
        self.assertTrue(all(set(x["evidenceIds"]) <= ids for x in s["scenarios"]))
        for window in s["timingWindows"]:
            self.assertLess(window["start"], window["end"])

    def test_ziwei_published_placement_examples(self):
        from predictive import ziwei_start, zi_wei_dou_shu
        self.assertEqual([ziwei_start(day, bureau) for day, bureau in [(27, 3), (13, 6), (6, 5)]], [10, 11, 7])
        result = zi_wei_dou_shu(self.birth, "male", self.as_of)
        self.assertEqual(len(result["palaces"]), 12)
        self.assertEqual(sum(len(p["majorStars"]) for p in result["palaces"]), 14)
        self.assertEqual(len(result["decades"]), 12)

    def test_primary_directions_equatorial_geometry(self):
        from predictive import primary_directions, direction_arc
        self.assertEqual(direction_arc(100, 90, "direct"), 10)
        self.assertEqual(direction_arc(80, 90, "converse"), 10)
        r = primary_directions(self.birth, self.options, self.as_of)
        self.assertTrue(r["directions"])
        for item in r["directions"]:
            self.assertAlmostEqual(item["arcDegrees"] / r["degreesPerYear"], item["ageYears"])
        self.assertIn("MC/IC", r["scope"])

    def test_jaimini_drig_has_sign_names_and_utc_intervals(self):
        from predictive import jaimini_timing
        result = jaimini_timing(self.birth, self.options, self.as_of)
        self.assertTrue(result["periods"])
        for period in result["periods"]:
            self.assertTrue(all(s in self.core.SIGNS for s in period["lords"]))
            self.assertTrue(period["start"].endswith("Z"))
            self.assertLess(period["start"], period["end"])

    def test_engine_rejects_invalid_methods_and_bounds(self):
        from engine import analyze
        for methods in ([{}], [1], [], "natal"):
            with self.subTest(methods=methods), self.assertRaises(InputError):
                analyze({"birth": BIRTH, "asOf": self.as_of.isoformat(), "methods": methods})
        with self.assertRaises(InputError):
            analyze({"birth": BIRTH, "asOf": self.as_of.isoformat(), "methods": ["scenarios"], "options": {"horizonDays": -1}})

    def test_embedded_error_is_not_marked_computed(self):
        from engine import analyze
        with patch("engine._dispatch", return_value={"error": "broken calculation"}):
            result = analyze({"birth": BIRTH, "asOf": self.as_of.isoformat(), "methods": ["natal"]})
        self.assertEqual(result["status"], "partial")
        self.assertEqual(result["results"]["natal"]["status"], "error")

    def test_divisional_d1_agrees_with_natal_for_all_node_modes(self):
        for ayanamsa in self.core.AYANAMSAS:
            for nodes in ("mean", "true"):
                with self.subTest(ayanamsa=ayanamsa, nodes=nodes):
                    options = configure({"ayanamsa": ayanamsa, "nodes": nodes})
                    chart = natal(self.birth, options)
                    rows = self.core.divisional_charts(self.birth, options)["D1"]["planets"]
                    for row in rows:
                        expected = chart["vedic"]["ascendant"] if row["planet"] == "Lagna" else chart["vedic"]["planets"][row["planet"]]
                        self.assertAlmostEqual(row["signIndex"] * 30 + row["longitude"], expected["longitude"], delta=0.0001)

    def test_bav_scores_are_not_mutated_by_reductions(self):
        r = self.core.ashtakavarga(self.birth, self.options)
        self.assertEqual([sum(r["bhinnashtaka"][p].values()) for p in self.core.VEDIC_PLANETS[:7]], [48, 49, 39, 54, 56, 52, 39])
        self.assertEqual(r["totalSarva"], 337)

    def test_ziwei_full_reference_charts_iztro_2_5_4(self):
        from predictive import zi_wei_dou_shu
        for day, ming, shen, bureau, ziwei, tianfu in [
            ("1990-01-15", "Wu", "Shen", 5, "Chen", "Zi"),
            ("2000-08-16", "Chou", "Mao", 6, "Mao", "Chou"),
            ("2023-04-07", "You", "Hai", 3, "Wu", "Xu")]:
            b = birth_input(dict(BIRTH, dob=day, time="14:00"))
            r = zi_wei_dou_shu(b, "male", self.as_of)
            self.assertEqual((r["lifePalace"], r["bodyPalace"], r["fiveElementBureau"]["number"]), (ming, shen, bureau))
            self.assertEqual(next(p["branch"] for p in r["palaces"] if "Zi Wei" in p["majorStars"]), ziwei)
            self.assertEqual(next(p["branch"] for p in r["palaces"] if "Tian Fu" in p["majorStars"]), tianfu)

    def test_vimshottari_four_levels_partition_120_years(self):
        result = self.core.vimshottari(self.birth, self.options, self.as_of)
        periods = result["periods"]
        self.assertEqual(periods[0]["start"], self.birth["utcDateTime"])
        self.assertEqual(periods[-1]["end"], self.core.iso(self.birth["julianDay"] + 120 * 365.25))
        self.assertTrue(all(len(p["lords"]) == 4 and p["start"] < p["end"] for p in periods))
        self.assertTrue(all(a["end"] == b["start"] for a, b in zip(periods, periods[1:])))
        self.assertIsNotNone(result["current"])

    def test_bazi_day_fixture_and_global_solar_term_invariance(self):
        from predictive import bazi
        shanghai = birth_input(dict(BIRTH, dob="2000-01-07", time="12:00", timezone="Asia/Shanghai"))
        london = birth_input(dict(BIRTH, dob="2000-01-07", time="04:00", timezone="Europe/London"))
        a = bazi(shanghai, "male", self.as_of)
        b = bazi(london, "male", self.as_of)
        self.assertEqual(a["pillars"]["day"]["ganZhi"], "甲子")
        self.assertEqual(a["pillars"]["year"], b["pillars"]["year"])
        self.assertEqual(a["pillars"]["month"], b["pillars"]["month"])
        self.assertEqual(a["luckStarts"], b["luckStarts"])
        self.assertEqual(len({p["ganZhi"] for p in a["luckPillars"]}), 8)

    def test_kp_houses_use_all_twelve_sidereal_cusps(self):
        r = self.core.kp_astrology(self.birth, self.options)
        cusps = [r["cuspSubLords"][f"Cusp{i}"]["longitude"] for i in range(1, 13)]
        self.assertNotEqual(cusps[0], self.core.natal(self.birth, self.options)["western"]["cusps"][0])
        for p in r["planets"].values():
            self.assertEqual(p["house"], self.core.house_of(p["longitude"], cusps))
        self.assertIn("significators", r)

    def test_api_options_and_nonfinite_inputs(self):
        from engine import analyze
        result = analyze({"asOf": self.as_of.isoformat(), "methods": ["tarot"], "options": {"spread": "timing", "seed": "fixed"}})
        self.assertEqual(len(result["results"]["tarot"]["data"]["cards"]), 5)
        with self.assertRaises(InputError):
            analyze({"asOf": self.as_of.isoformat(), "methods": ["tarot"], "options": {"x": float("nan")}})

    def test_default_request_does_not_require_partner(self):
        from engine import analyze
        r = analyze({"birth": BIRTH, "asOf": self.as_of.isoformat()})
        self.assertEqual(set(r["results"]), {"natal"})


if __name__ == "__main__":
    unittest.main()
