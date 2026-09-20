"""
engine.py – Request router for the Python calculation worker.

Reads one JSON request from stdin, writes one JSON response to stdout.
All astrological results are traditional/divinatory; see DISCLAIMER in astro_core.
"""
import contextlib
import importlib.metadata
import json
import math
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))

from astro_core import (
    DISCLAIMER,
    EngineUnavailable,
    InputError,
    birth_input,
    configure,
    instant,
    julian,
    natal,
    number,
    divisional_charts,
    yogas,
    shadbala,
    bhava_bala,
    ashtakavarga,
    bhava_chalit,
    jaimini,
    kp_astrology,
    vimshottari,
    yogini_dasha,
    ashtottari_dasha,
    chara_dasha,
    narayana_dasha,
    kalachakra_dasha,
    transits,
    sade_sati_timeline,
    solar_return,
    lunar_return,
    secondary_progressions,
    solar_arc,
    annual_profections,
    zodiacal_releasing,
    synastry,
    composite_chart,
    guna_milan,
    eclipses,
    retrograde_periods,
    planetary_cycles,
    numerology,
    tarot,
    chinese_astrology,
    prashna,
    muhurta,
    topic_prediction,
)

from predictive import (TOPICS, integer, topic_analysis, scenarios, eclipse_contacts,
                        jaimini_timing, primary_directions, zi_wei_dou_shu, bazi)

METHODS = {
    # ── Core ──────────────────────────────────────────────────────────────────
    "natal":               "Swiss Ephemeris tropical and sidereal natal chart",
    # ── Vedic ─────────────────────────────────────────────────────────────────
    "divisional_charts":   "All standard Parasara divisional charts D1–D60",
    "yogas":               "Vedic yoga detection (Parasara/PVR conventions)",
    "shadbala":            "Shadbala – six-fold planetary strength (Rupas)",
    "bhava_bala":          "Bhava Bala – house strength analysis",
    "ashtakavarga":        "Bhinnashtakavarga, Sarvashtakavarga, Sodhana Pindas",
    "bhava_chalit":        "KP/Bhava Chalit – planet-to-cusp placement",
    "jaimini":             "Jaimini karakas, arudhas, Rasi Drishti, Karakamsha",
    "kp":                  "KP Astrology – sub-lords, significators, ruling planets",
    # ── Dasha systems ─────────────────────────────────────────────────────────
    "vimshottari":         "4-level Vimshottari Dasha (Maha/Antara/Pratyantar/Sukshma)",
    "yogini_dasha":        "3-level Yogini Dasha",
    "ashtottari_dasha":    "3-level Ashtottari Dasha",
    "chara_dasha":         "3-level Chara Dasha (sign-based, KN Rao default)",
    "narayana_dasha":      "Narayana Dasha (rasi-based)",
    "kalachakra_dasha":    "Kalachakra Dasha",
    # ── Transits & timing ─────────────────────────────────────────────────────
    "transits":            "Current planetary transits vs natal chart",
    "sade_sati":           "Sade Sati and Dhaiya timeline (Saturn over Moon)",
    "eclipses":            "Upcoming solar and lunar eclipses",
    "retrogrades":         "Retrograde ingress/egress for visible planets",
    "planetary_cycles":    "Saturn return, Jupiter return, nodal return, Uranus opposition",
    # ── Western predictive ────────────────────────────────────────────────────
    "solar_return":        "Solar return chart for a specified year",
    "lunar_return":        "Next lunar return chart from asOf date",
    "secondary_progressions": "Secondary progressions (day-for-a-year)",
    "solar_arc":           "Solar arc directions",
    "annual_profections":  "Annual profections (house per year)",
    "zodiacal_releasing":  "Zodiacal releasing from Lot of Spirit or Fortune",
    # ── Multi-chart ───────────────────────────────────────────────────────────
    "synastry":            "Synastry: cross-chart aspects and house overlays",
    "composite":           "Composite (midpoint) chart",
    "guna_milan":          "Vedic compatibility – Ashtakoota Guna Milan",
    # ── Divination & other systems ────────────────────────────────────────────
    "numerology":          "Pythagorean and Chaldean numerology",
    "tarot":               "Reproducible tarot spread seeded from question/date hash",
    "chinese_astrology":   "BaZi Four Pillars of Destiny with luck pillars",
    "prashna":             "Prashna/Horary chart for a question moment",
    "muhurta":             "Muhurta/electional auspiciousness analysis",
    "prediction":          "Traceable traditional topic analysis with supporting and conflicting rules",
    "career":              "Career houses, lords, significators, D10 and timing indicators",
    "marriage":            "Relationship houses, D9, Darakaraka, Upapada and timing indicators",
    "finance":             "Wealth houses, D2, lords and timing indicators",
    "health":              "Non-diagnostic traditional health indicators; not medical advice",
    "scenarios":           "Alternative interpretations and Dasha-bounded timing windows; no probabilities",
    "eclipse_contacts":    "Eclipse longitudes, natal houses and aspect contacts",
    "jaimini_dasha":       "Drig Dasha, explicit PVR paper variant, sign-based UTC intervals",
    "primary_directions":  "Mundane planetary conjunctions to MC/IC; Naibod or Ptolemy key",
    "zi_wei_dou_shu":      "14 major stars, 12 palaces, four transformations and decade periods",
}


def _require_birth(request):
    return birth_input(request.get("birth") or {})


def _require_partner(request):
    if "partner" not in request:
        raise InputError("partner birth details are required for this method")
    return birth_input(request["partner"])


def _require_as_of(request):
    return instant(request.get("asOf") or "")


def analyze(request):
    if not isinstance(request, dict):
        raise InputError("Request must be a JSON object")

    if request.get("action") == "capabilities":
        return {"methods": METHODS, "license": "AGPL-3.0-or-later",
                "dateRange": [1800, 2399]}

    _finite_input(request)
    if request.get("action") == "timezones":
        return resolve_timezones(request.get("locations"))
    as_of = _require_as_of(request)
    options = request.get("options") if request.get("options") is not None else {}
    if not isinstance(options, dict):
        raise InputError("options must be a JSON object")
    request = {**options, **{k: v for k, v in request.items() if v is not None}}
    methods = request.get("methods", ["natal"])
    if not isinstance(methods, list) or not 1 <= len(methods) <= 64 or any(not isinstance(m, str) for m in methods):
        raise InputError("methods must be a non-empty list of identifiers with at most 64 entries")
    unknown = [m for m in methods if m not in METHODS]
    if unknown:
        raise InputError(f"Unknown method(s): {', '.join(unknown)}. Call /v2/methods for the supported list.")
    bounds = {"horizonDays": (1, 730), "count": (1, 20), "yearsAhead": (1, 30), "dashaLevels": (1, 4),
              "returnYear": (1800, 2399), "directionMaxAge": (1, 120)}
    for key, (low, high) in bounds.items():
        if key in request:
            integer(request[key], key, low, high)
    if request.get("topic", "general") not in TOPICS:
        raise InputError("Unsupported topic")
    for key, limit in (("name", 200), ("fullName", 200), ("seed", 128), ("question", 2000), ("spread", 40)):
        if key in request and (not isinstance(request[key], str) or len(request[key]) > limit):
            raise InputError(f"{key} must be a string of at most {limit} characters")

    # Methods that need a birth chart
    _NEEDS_BIRTH = set(METHODS) - {"eclipses", "retrogrades", "tarot", "prashna", "muhurta"}

    needs_birth = any(m in _NEEDS_BIRTH for m in methods)
    birth = _require_birth(request) if needs_birth else None

    needs_partner = any(m in ("synastry","composite","guna_milan") for m in methods)
    partner = _require_partner(request) if needs_partner else None

    non_astronomical = {"tarot", "numerology", "zi_wei_dou_shu", "chinese_astrology"}
    settings = configure(options) if any(m not in non_astronomical for m in methods) else {}

    results = {}
    for method in dict.fromkeys(methods):  # deduplicate, preserve order
        try:
            if method not in non_astronomical:
                configure(options)
            data = _dispatch(method, birth, partner, as_of, options, settings, request)
            if _contains_error(data):
                raise EngineUnavailable("A component calculation failed; no successful result is claimed")
            json.dumps(data, allow_nan=False)
            results[method] = data
        except (InputError, EngineUnavailable) as e:
            results[method] = {"status": "error", "message": str(e)}
        except Exception as e:
            print(method + ": " + type(e).__name__, file=sys.stderr)
            results[method] = {"status": "error", "message": "Calculation failed; consult the server operator"}

    status = "complete" if all(
        not isinstance(v, dict) or v.get("status") != "error"
        for v in results.values()) else "partial"

    meta = {"engine": "Swiss Ephemeris + PyJHora",
            "sweVersion": _swe_version(),
            "license": "AGPL-3.0-or-later",
            "disclaimer": DISCLAIMER}
    if settings:
        meta.update({"ayanamsa": settings.get("ayanamsa"),
                     "nodes": settings.get("nodes"),
                     "westernHouses": settings.get("houses"),
                     "dateRange": [1800, 2399]})

    return {"status": status,
            "birth": birth,
            "asOf": as_of.isoformat().replace("+00:00", "Z"),
            "metadata": meta,
            "results": {m: {"status": "computed", "data": results[m]}
                        if not (isinstance(results[m], dict)
                                and results[m].get("status") == "error")
                        else results[m]
                        for m in results}}


def resolve_timezones(locations):
    if not isinstance(locations, list) or not 1 <= len(locations) <= 5:
        raise InputError("Provide one to five coordinate pairs")
    points = []
    for location in locations:
        if not isinstance(location, dict):
            raise InputError("Each location must be an object")
        points.append((number(location.get("latitude"), "latitude", -90, 90),
                       number(location.get("longitude"), "longitude", -180, 180)))
    try:
        from timezonefinder import TimezoneFinder
        from zoneinfo import ZoneInfo
        finder = TimezoneFinder()
        zones = []
        for latitude, longitude in points:
            zone = finder.timezone_at(lat=latitude, lng=longitude)
            if zone is not None:
                ZoneInfo(zone)
            zones.append(zone)
    except (ImportError, OSError, RuntimeError, KeyError) as error:
        raise EngineUnavailable("Offline timezone data is unavailable") from error
    return {"status": "complete", "timezones": zones,
            "source": "timezonefinder " + importlib.metadata.version("timezonefinder"),
            "boundaryModel": "Current geographic timezone polygons; birth-date offsets are resolved separately"}


def _finite_input(value):
    if isinstance(value, float) and not math.isfinite(value):
        raise InputError("Non-finite numbers are not allowed")
    if isinstance(value, dict):
        for item in value.values():
            _finite_input(item)
    elif isinstance(value, list):
        for item in value:
            _finite_input(item)


def _contains_error(value):
    if isinstance(value, dict):
        return "error" in value or value.get("status") == "error" or any(_contains_error(v) for v in value.values())
    return isinstance(value, (tuple, list)) and any(_contains_error(v) for v in value)


def _dispatch(method, birth, partner, as_of, options, settings, request):
    if method in ("career", "marriage", "finance", "health", "prediction"):
        topic = request.get("topic", "general") if method == "prediction" else method
        return topic_analysis(birth, settings, as_of, topic)
    if method == "scenarios":
        return scenarios(birth, settings, as_of, request.get("topic", "general"), request.get("horizonDays", 365))
    if method == "eclipse_contacts":
        return eclipse_contacts(birth, settings, as_of, request.get("count", 6), request.get("eclipseOrb", 3))
    if method == "jaimini_dasha":
        return jaimini_timing(birth, settings, as_of)
    if method == "primary_directions":
        return primary_directions(birth, settings, as_of, request.get("directionKey", "naibod"),
                                  request.get("directionMotion", "both"), request.get("directionMaxAge", 100))
    if method == "zi_wei_dou_shu":
        return zi_wei_dou_shu(birth, request.get("gender"), as_of, request.get("ziweiLeapRule", "split_at_15"))
    # ── Core ──────────────────────────────────────────────────────────────────
    if method == "natal":
        return natal(birth, settings)

    # ── Vedic ─────────────────────────────────────────────────────────────────
    if method == "divisional_charts":
        return divisional_charts(birth, settings)
    if method == "yogas":
        dvf = int(request.get("divisionalChartFactor", 1))
        return yogas(birth, settings, divisional=dvf)
    if method == "shadbala":
        return shadbala(birth, settings)
    if method == "bhava_bala":
        return bhava_bala(birth, settings)
    if method == "ashtakavarga":
        return ashtakavarga(birth, settings)
    if method == "bhava_chalit":
        return bhava_chalit(birth, settings)
    if method == "jaimini":
        return jaimini(birth, settings)
    if method == "kp":
        return kp_astrology(birth, settings)

    # ── Dasha systems ─────────────────────────────────────────────────────────
    if method == "vimshottari":
        levels = int(request.get("dashaLevels", 4))
        return vimshottari(birth, settings, as_of, levels=levels)
    if method == "yogini_dasha":
        return yogini_dasha(birth, settings)
    if method == "ashtottari_dasha":
        return ashtottari_dasha(birth, settings)
    if method == "chara_dasha":
        return chara_dasha(birth, settings)
    if method == "narayana_dasha":
        return narayana_dasha(birth, settings)
    if method == "kalachakra_dasha":
        return kalachakra_dasha(birth, settings)

    # ── Transits & timing ─────────────────────────────────────────────────────
    if method == "transits":
        return transits(birth, settings, as_of)
    if method == "sade_sati":
        years = int(request.get("yearsAhead", 30))
        return sade_sati_timeline(birth, settings, years_ahead=years)
    if method == "eclipses":
        count = min(int(request.get("count", 6)), 20)
        return eclipses(as_of, settings, count=count)
    if method == "retrogrades":
        years = int(request.get("yearsAhead", 2))
        return retrograde_periods(as_of, settings, years_ahead=years)
    if method == "planetary_cycles":
        return planetary_cycles(birth, settings, as_of)

    # ── Western predictive ────────────────────────────────────────────────────
    if method == "solar_return":
        year = int(request.get("returnYear", as_of.year))
        return solar_return(birth, settings, year)
    if method == "lunar_return":
        return lunar_return(birth, settings, as_of)
    if method == "secondary_progressions":
        return secondary_progressions(birth, settings, as_of)
    if method == "solar_arc":
        return solar_arc(birth, settings, as_of)
    if method == "annual_profections":
        return annual_profections(birth, as_of, settings)
    if method == "zodiacal_releasing":
        lot = request.get("lot", "spirit")
        return zodiacal_releasing(birth, settings, as_of, lot=lot)

    # ── Multi-chart ───────────────────────────────────────────────────────────
    if method == "synastry":
        return synastry(birth, partner, settings)
    if method == "composite":
        return composite_chart(birth, partner, settings)
    if method == "guna_milan":
        return guna_milan(birth, partner, settings)

    # ── Other systems ─────────────────────────────────────────────────────────
    if method == "numerology":
        name = request.get("name", request.get("fullName", ""))
        return numerology(birth, full_name=name, as_of=as_of)
    if method == "tarot":
        seed = str(request.get("seed", as_of.isoformat()))
        spread = str(request.get("spread", "celtic_cross"))
        return tarot(seed, spread=spread)
    if method == "chinese_astrology":
        return bazi(birth, request.get("gender"), as_of)
    if method == "prashna":
        loc = request.get("location") or {}
        if not isinstance(loc, dict):
            raise InputError("location must be an object")
        question = str(request.get("question", ""))
        return prashna(loc, as_of, settings, question=question)
    if method == "muhurta":
        loc = request.get("location") or {}
        if not isinstance(loc, dict):
            raise InputError("location must be an object")
        duration = float(request.get("durationHours", 2.0))
        purpose  = str(request.get("purpose", "general"))
        return muhurta(loc, as_of, duration, settings, purpose=purpose)
    if method == "prediction":
        topic = str(request.get("topic", "general"))
        return topic_prediction(birth, settings, as_of, topic=topic)

    raise InputError(f"Method '{method}' is listed but has no handler")


def _swe_version():
    try:
        return importlib.metadata.version("pyswisseph")
    except Exception:
        return "unknown"


def main():
    try:
        data = sys.stdin.buffer.read(32769)
        if len(data) > 32768:
            raise InputError("Request exceeds 32 KiB")
        request = json.loads(data)
        with open(os.devnull, "w") as sink, contextlib.redirect_stdout(sink):
            response = analyze(request)
    except (InputError, json.JSONDecodeError, UnicodeDecodeError) as e:
        response = {"error": {"status": 400, "message": str(e)}}
    except EngineUnavailable as e:
        response = {"error": {"status": 503, "message": str(e)}}
    except Exception as e:
        print(type(e).__name__, file=sys.stderr)
        response = {"error": {"status": 500, "message": "Calculation failed; consult the server operator"}}
    encoded = json.dumps(response, allow_nan=False, ensure_ascii=False, separators=(",", ":"))
    sys.stdout.write(encoded)


if __name__ == "__main__":
    main()
