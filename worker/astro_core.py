"""
astro_core.py – Complete astrological calculation engine.

All methods are traditional/divinatory. Results are NOT scientifically
validated forecasts. Never substitute for medical, financial, or legal advice.
"""
from __future__ import annotations

import contextlib
import hashlib
import math
import os
import re
import sys
from datetime import date, datetime, timedelta, timezone
from pathlib import Path
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

import swisseph as swe

# ── Constants ─────────────────────────────────────────────────────────────────
SIGNS = ("Aries","Taurus","Gemini","Cancer","Leo","Virgo",
         "Libra","Scorpio","Sagittarius","Capricorn","Aquarius","Pisces")
SIGN_ELEMENTS = ("Fire","Earth","Air","Water","Fire","Earth",
                 "Air","Water","Fire","Earth","Air","Water")
SIGN_MODALITIES = ("Cardinal","Fixed","Mutable","Cardinal","Fixed","Mutable",
                   "Cardinal","Fixed","Mutable","Cardinal","Fixed","Mutable")
NAKSHATRAS = ("Ashwini","Bharani","Krittika","Rohini","Mrigashira","Ardra",
              "Punarvasu","Pushya","Ashlesha","Magha","Purva Phalguni",
              "Uttara Phalguni","Hasta","Chitra","Swati","Vishakha",
              "Anuradha","Jyeshtha","Mula","Purva Ashadha","Uttara Ashadha",
              "Shravana","Dhanishtha","Shatabhisha","Purva Bhadrapada",
              "Uttara Bhadrapada","Revati")
NAK_LORDS = ("Ketu","Venus","Sun","Moon","Mars","Rahu","Jupiter",
             "Saturn","Mercury")  # 9-lord cycle
DASHA_YEARS = {"Ketu":7,"Venus":20,"Sun":6,"Moon":10,"Mars":7,
               "Rahu":18,"Jupiter":16,"Saturn":19,"Mercury":17}
DASHA_ORDER = ("Ketu","Venus","Sun","Moon","Mars","Rahu","Jupiter","Saturn","Mercury")
VEDIC_PLANETS = ("Sun","Moon","Mars","Mercury","Jupiter","Venus","Saturn","Rahu","Ketu")
SIGN_LORDS = ("Mars","Venus","Mercury","Moon","Sun","Mercury",
              "Venus","Mars","Jupiter","Saturn","Saturn","Jupiter")
EXALTATION = {"Sun":0,"Moon":1,"Mars":9,"Mercury":5,"Jupiter":3,
              "Venus":11,"Saturn":6,"Rahu":1,"Ketu":7}   # sign index
DEBILITATION = {k:(v+6)%12 for k,v in EXALTATION.items()}
BODIES = {"Sun":swe.SUN,"Moon":swe.MOON,"Mercury":swe.MERCURY,
          "Venus":swe.VENUS,"Mars":swe.MARS,"Jupiter":swe.JUPITER,
          "Saturn":swe.SATURN,"Uranus":swe.URANUS,"Neptune":swe.NEPTUNE,
          "Pluto":swe.PLUTO}
AYANAMSAS = {"lahiri":swe.SIDM_LAHIRI,"krishnamurti":swe.SIDM_KRISHNAMURTI,
             "raman":swe.SIDM_RAMAN,"fagan_bradley":swe.SIDM_FAGAN_BRADLEY,
             "true_chitra":swe.SIDM_TRUE_CITRA}
HOUSE_SYSTEMS = {"placidus":b"P","whole_sign":b"W","equal":b"E",
                 "porphyry":b"O","koch":b"K"}
DISCLAIMER = ("Traditional/divinatory interpretations are not scientifically "
              "validated forecasts. Indicators are not event probabilities. "
              "Do not substitute for medical, financial, or legal advice.")

# KP sub-lord table: 249 divisions of 360° by Vimshottari proportions
# Each nakshatra = 13°20'; each divided into 9 subs proportional to dasha years
_KP_SUBS: list[tuple[float,str,str,str]] = []  # built lazily
def _build_kp_table():
    global _KP_SUBS
    if _KP_SUBS:
        return
    total_years = sum(DASHA_YEARS.values())  # 120
    nak_span = 360.0 / 27           # 13.333...°
    lon = 0.0
    for nak_i in range(27):
        nak_lord = NAK_LORDS[nak_i % 9]
        # sub cycle starts from nak_lord
        start_idx = DASHA_ORDER.index(nak_lord)
        for sub_i in range(9):
            sub_lord = DASHA_ORDER[(start_idx + sub_i) % 9]
            sub_span = nak_span * DASHA_YEARS[sub_lord] / total_years
            # sub-sub starts from sub_lord
            sub_start = DASHA_ORDER.index(sub_lord)
            sub_total = sum(DASHA_YEARS[DASHA_ORDER[(sub_start+k)%9]] for k in range(9))
            ss_start = lon
            for ss_i in range(9):
                ss_lord = DASHA_ORDER[(sub_start + ss_i) % 9]
                ss_span = sub_span * DASHA_YEARS[ss_lord] / sub_total
                _KP_SUBS.append((ss_start, NAKSHATRAS[nak_i], nak_lord, sub_lord, ss_lord))
                ss_start += ss_span
            lon += sub_span

# BaZi tables
_STEMS = ("Jia","Yi","Bing","Ding","Wu","Ji","Geng","Xin","Ren","Gui")
_BRANCHES = ("Zi","Chou","Yin","Mao","Chen","Si","Wu","Wei","Shen","You","Xu","Hai")
_BRANCH_ANIMALS = ("Rat","Ox","Tiger","Rabbit","Dragon","Snake",
                   "Horse","Goat","Monkey","Rooster","Dog","Pig")
_BRANCH_ELEMENTS = ("Water","Earth","Wood","Wood","Earth","Fire",
                    "Fire","Earth","Metal","Metal","Earth","Water")
_STEM_ELEMENTS = ("Wood","Wood","Fire","Fire","Earth","Earth",
                  "Metal","Metal","Water","Water")
_STEM_POLARITY = ("Yang","Yin","Yang","Yin","Yang","Yin","Yang","Yin","Yang","Yin")
# BaZi month branch: solar term solar longitude → branch index
# Major solar term (Jié) longitudes (tropical Sun °) → month branch
_SOLAR_TERM_LONS = [315,345,15,45,75,105,135,165,195,225,255,285]
_MONTH_BRANCHES  = [2,3,4,5,6,7,8,9,10,11,0,1]  # Yin,Mao,...,Hai,Zi,Chou
# Day pillar base: JD 2299160 (1582-10-15) is Gregorian day 0; day 0 pillar = Jia-Zi
_DAY_PILLAR_EPOCH_JD = 2299160  # Jia-Zi

# Tarot
_MAJOR_ARCANA = ["The Fool","The Magician","The High Priestess","The Empress",
                 "The Emperor","The Hierophant","The Lovers","The Chariot",
                 "Strength","The Hermit","Wheel of Fortune","Justice",
                 "The Hanged Man","Death","Temperance","The Devil",
                 "The Tower","The Star","The Moon","The Sun","Judgement",
                 "The World"]
_SUITS = ["Wands","Cups","Swords","Pentacles"]
_MINOR_ARCANA = [f"{r} of {s}"
                 for s in _SUITS
                 for r in ["Ace","Two","Three","Four","Five","Six","Seven",
                           "Eight","Nine","Ten","Page","Knight","Queen","King"]]
_ALL_CARDS = _MAJOR_ARCANA + _MINOR_ARCANA  # 78

# Numerology
_PYTH = {"a":1,"b":2,"c":3,"d":4,"e":5,"f":6,"g":7,"h":8,"i":9,
         "j":1,"k":2,"l":3,"m":4,"n":5,"o":6,"p":7,"q":8,"r":9,
         "s":1,"t":2,"u":3,"v":4,"w":5,"x":6,"y":7,"z":8}
_CHALD = {"a":1,"b":2,"c":3,"d":4,"e":5,"f":8,"g":3,"h":5,"i":1,
          "j":1,"k":2,"l":3,"m":4,"n":5,"o":7,"p":8,"q":1,"r":2,
          "s":3,"t":4,"u":6,"v":6,"w":6,"x":5,"y":1,"z":7}
_VOWELS = set("aeiou")

# ── Exceptions ────────────────────────────────────────────────────────────────
class InputError(ValueError):
    pass

class EngineUnavailable(RuntimeError):
    pass

# ── Primitives ─────────────────────────────────────────────────────────────────
def number(value, name, low, high):
    if isinstance(value, bool) or not isinstance(value,(int,float)) \
            or not math.isfinite(value) or not low <= value <= high:
        raise InputError(f"{name} must be a finite number between {low} and {high}")
    return float(value)

def instant(value):
    if not isinstance(value, str) or not re.fullmatch(
            r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d{1,6})?)?(Z|[+-]\d{2}:\d{2})", value):
        raise InputError("asOf must be an ISO date-time with an explicit UTC offset")
    try:
        result = datetime.fromisoformat(value.replace("Z","+00:00")).astimezone(timezone.utc)
    except ValueError as e:
        raise InputError("Invalid date-time") from e
    if not 1800 <= result.year < 2400:
        raise InputError("Astronomical dates must be within 1800–2399")
    return result

def julian(value: datetime) -> float:
    return value.timestamp() / 86400.0 + 2440587.5

def iso(jd: float) -> str:
    return (datetime.fromtimestamp((jd-2440587.5)*86400, timezone.utc)
            .isoformat(timespec="seconds").replace("+00:00","Z"))

# ── Birth input ────────────────────────────────────────────────────────────────
def birth_input(data):
    if not isinstance(data, dict):
        raise InputError("birth must be an object")
    latitude  = number(data.get("latitude"), "latitude", -89.999, 89.999)
    longitude = number(data.get("longitude"),"longitude",-180, 180)
    try:
        zone = ZoneInfo(data["timezone"])
        if not re.fullmatch(r"\d{4}-\d{2}-\d{2}", data["dob"]) \
                or not re.fullmatch(r"\d{2}:\d{2}(:\d{2})?", data["time"]):
            raise InputError("Birth dob/time must use YYYY-MM-DD and HH:mm[:ss]")
        local = datetime.fromisoformat(data["dob"]+"T"+data["time"])
    except (KeyError, TypeError, ValueError, ZoneInfoNotFoundError) as e:
        raise InputError("Valid birth dob, time, and IANA timezone are required") from e
    candidates = {}
    for fold in (0, 1):
        aware = local.replace(tzinfo=zone, fold=fold)
        utc   = aware.astimezone(timezone.utc)
        if utc.astimezone(zone).replace(tzinfo=None) == local:
            off = aware.strftime("%z")
            candidates[off[:3]+":"+off[3:]] = aware
    if not candidates:
        raise InputError("Birth time falls in a daylight-saving gap")
    chosen = data.get("utcOffset")
    if chosen is not None and chosen not in candidates:
        raise InputError("utcOffset does not match the birth timezone at that time")
    if len(candidates) > 1 and chosen is None:
        raise InputError("Birth time is ambiguous; provide utcOffset to select the DST occurrence")
    aware = candidates[chosen] if chosen else next(iter(candidates.values()))
    utc   = aware.astimezone(timezone.utc)
    instant(utc.isoformat())
    return {"dob": data["dob"], "time": data["time"],
            "city": str(data.get("city","Custom location"))[:120],
            "latitude": latitude, "longitude": longitude,
            "timezone": data["timezone"],
            "utcOffset": aware.strftime("%z"),
            "utcDateTime": utc.isoformat().replace("+00:00","Z"),
            "julianDay": julian(utc),
            "offsetHours": aware.utcoffset().total_seconds()/3600,
            "localDateTime": local.isoformat()}

# ── Configure ephemeris ────────────────────────────────────────────────────────
def configure(options):
    ayanamsa = options.get("ayanamsa","lahiri")
    nodes    = options.get("nodes","mean")
    houses   = options.get("houses","placidus")
    if ayanamsa not in AYANAMSAS or nodes not in ("mean","true") \
            or houses not in HOUSE_SYSTEMS:
        raise InputError("Unsupported ayanamsa, node convention, or house system")
    directory = Path(os.environ.get("ASTRO_EPHE_PATH", Path(__file__).parent/"ephe"))
    if not all((directory/n).is_file() for n in ("sepl_18.se1","semo_18.se1")):
        raise EngineUnavailable("Swiss ephemeris data missing; run worker/bootstrap.py")
    swe.set_ephe_path(str(directory))
    swe.set_sid_mode(AYANAMSAS[ayanamsa])
    return {"ayanamsa":ayanamsa,"nodes":nodes,"houses":houses,
            "ephemerisPath":str(directory)}

# ── Planet position ────────────────────────────────────────────────────────────
def planet(jd, name, options, sidereal=False):
    body = swe.TRUE_NODE if options["nodes"]=="true" else swe.MEAN_NODE
    if name not in ("Rahu","Ketu"):
        body = BODIES[name]
    flags = swe.FLG_SWIEPH | swe.FLG_SPEED | (swe.FLG_SIDEREAL if sidereal else 0)
    try:
        values, actual = swe.calc_ut(jd, body, flags)
    except swe.Error as e:
        raise EngineUnavailable("Swiss ephemeris calculation failed") from e
    if name not in ("Rahu","Ketu") and not actual & swe.FLG_SWIEPH:
        raise EngineUnavailable("Swiss ephemeris data unavailable; low-precision fallback disabled")
    lon = (values[0]+(180 if name=="Ketu" else 0))%360
    return {"name":name,"longitude":lon,
            "latitude": -values[1] if name=="Ketu" else values[1],
            "distanceAu":values[2],"speedDegreesPerDay":values[3],
            "retrograde":values[3]<0}

def longitude_details(lon):
    sign = int(lon//30)
    nak  = min(26, int(lon/(40/3)))
    return {"longitude":lon,"signIndex":sign,"sign":SIGNS[sign],
            "degreeInSign":lon%30,"nakshatraIndex":nak+1,
            "nakshatra":NAKSHATRAS[nak],"pada":int(lon/(10/3))%4+1,
            "nakshatraLord":NAK_LORDS[nak%9]}

def circular_difference(a, b):
    return (a-b+180)%360-180

def house_of(lon, cusps):
    for i, start in enumerate(cusps):
        if (lon-start)%360 < (cusps[(i+1)%12]-start)%360:
            return i+1
    raise InputError("Cannot assign house to supplied longitude")

def aspects(first, second=None, orb=6):
    pairs = []
    names   = list(first)
    targets = names if second is None else list(second)
    second_pos = first if second is None else second
    for i, a in enumerate(names):
        for b in (targets[i+1:] if second is None else targets):
            sep = abs(circular_difference(first[a]["longitude"],
                                          second_pos[b]["longitude"]))
            for angle, label in ((0,"conjunction"),(60,"sextile"),
                                  (90,"square"),(120,"trine"),
                                  (150,"quincunx"),(180,"opposition")):
                dist = abs(sep-angle)
                limit = min(orb, 3 if angle==150 else orb)
                if dist <= limit:
                    next_sep = abs(circular_difference(
                        first[a]["longitude"]+first[a].get("speedDegreesPerDay",0)/1440,
                        second_pos[b]["longitude"]+second_pos[b].get("speedDegreesPerDay",0)/1440))
                    pairs.append({"first":a,"second":b,"aspect":label,
                                  "angle":angle,"orb":dist,
                                  "applying":abs(next_sep-angle)<dist})
    return pairs

# ── Natal chart ────────────────────────────────────────────────────────────────
def natal(birth, options, jd=None):
    jd = birth["julianDay"] if jd is None else jd
    try:
        cusps, angles = swe.houses_ex(jd, birth["latitude"], birth["longitude"],
                                      HOUSE_SYSTEMS[options["houses"]])
        _, sid_angles = swe.houses_ex(jd, birth["latitude"], birth["longitude"],
                                      b"W", swe.FLG_SIDEREAL)
    except swe.Error as e:
        raise InputError("House system undefined here; use whole_sign or equal") from e
    tropical, sidereal = {}, {}
    for name in (*BODIES,"Rahu","Ketu"):
        tropical[name] = planet(jd, name, options)
        tropical[name]["house"] = house_of(tropical[name]["longitude"], cusps)
        if name in VEDIC_PLANETS:
            pos = planet(jd, name, options, True)
            pos.update(longitude_details(pos["longitude"]))
            pos["house"] = (pos["signIndex"] - int(sid_angles[0]//30))%12+1
            pos["dignity"] = _dignity(name, pos["signIndex"])
            sidereal[name] = pos
    asc_sid = sid_angles[0] % 360
    asc_det = longitude_details(asc_sid)
    return {"julianDay":jd,"utcDateTime":iso(jd),
            "ayanamsaDegrees":swe.get_ayanamsa_ut(jd),
            "vedic":{"ascendant":asc_det,"planets":sidereal,"houseSystem":"whole_sign"},
            "western":{"ascendant":angles[0],"midheaven":angles[1],"armc":angles[2],
                       "houseSystem":options["houses"],"cusps":list(cusps),
                       "planets":tropical,"aspects":aspects(tropical)}}

def _dignity(name, sign_idx):
    if SIGN_LORDS[sign_idx] == name:   return "domicile"
    if EXALTATION.get(name) == sign_idx: return "exaltation"
    if DEBILITATION.get(name) == sign_idx: return "debilitation"
    opp = (sign_idx+6)%12
    if SIGN_LORDS[opp] == name:        return "detriment"
    return "peregrine"

# ── jhora helper ──────────────────────────────────────────────────────────────
def _jhora_init(options):
    """One-time jhora configuration per call."""
    with open(os.devnull,"w") as sink, contextlib.redirect_stdout(sink):
        from jhora import const as jc
        jc.set_place_database_engine(jc.PLACE_DATABASE_ENGINE.NONE)
        jc.get_place_elevation_from_internet = False
        ephe = options.get("ephemerisPath","worker/ephe")
        jc._EPHIMERIDE_DATA_PATH = str(ephe)
        ayanamsa_map = {"lahiri":"LAHIRI","krishnamurti":"KP",
                        "raman":"RAMAN","fagan_bradley":"FAGAN",
                        "true_chitra":"TRUE_CITRA"}
        from jhora.panchanga import drik
        from jhora.horoscope.chart import charts
        drik.set_ayanamsa_mode(ayanamsa_map[options.get("ayanamsa", "lahiri")])
        jc.set_node_mode(options.get("nodes", "mean") == "true")
        drik.set_planet_list(set_rahu_ketu_as_true_nodes=options.get("nodes", "mean") == "true", include_western_planets=False)
        jc.dhasa_year_duration_default = jc.DHASA_YEAR_DURATION.MEAN_SIDEREAL_YEAR
        drik.PLANET_FLAGS = swe.FLG_SWIEPH | swe.FLG_SIDEREAL | swe.FLG_SPEED
        configure(options)
    return jc, drik

def _jhora_place(birth, drik):
    from jhora.panchanga.drik import Place
    return Place(birth["city"], birth["latitude"], birth["longitude"],
                 birth["offsetHours"], elevation=0)

def _dob_tob(birth, drik):
    """Return (drik.Date, (h,mi,ss)) from birth dict for jhora functions."""
    y, m, d = [int(x) for x in birth["dob"].split("-")]
    parts   = birth["time"].split(":")
    h, mi   = int(parts[0]), int(parts[1])
    ss      = int(parts[2]) if len(parts) > 2 else 0
    return drik.Date(y, m, d), (h, mi, ss)

def _jhora_jd(birth, jc=None):
    """Return jhora-style local-time JD from birth dict."""
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        from jhora import utils as ju
        from jhora.panchanga import drik
    parts = birth["time"].split(":")
    h, mi = int(parts[0]), int(parts[1])
    ss = int(parts[2]) if len(parts) > 2 else 0
    y, m, d = [int(x) for x in birth["dob"].split("-")]
    dob = drik.Date(y, m, d)
    return ju.julian_day_number(dob, (h, mi, ss))

def _rasi_chart(jd, place):
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        from jhora.horoscope.chart import charts
        return charts.rasi_chart(jd, place)

def _chart_1d(rasi):
    """Convert rasi list to 12-element house-to-planet string list."""
    h2p = [""]*12
    for planet_id, (sign, _lon) in rasi:
        if isinstance(planet_id, str):  # 'L' = Lagna
            h2p[sign] += "L/"
        else:
            h2p[sign] += str(planet_id)+"/"
    return [x.rstrip("/") for x in h2p]

# ── Divisional charts ─────────────────────────────────────────────────────────
def divisional_charts(birth, options):
    """D1 through D60 – all standard Parasara divisional charts."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import charts

    _DVFS = {"D1":1,"D2":2,"D3":3,"D4":4,"D7":7,"D9":9,"D10":10,
             "D12":12,"D16":16,"D20":20,"D24":24,"D27":27,
             "D30":30,"D40":40,"D45":45,"D60":60}

    chart = natal(birth, options)["vedic"]
    asc = chart["ascendant"]
    base = [["L", [asc["signIndex"], asc["degreeInSign"]]]]
    base.extend([i, [chart["planets"][p]["signIndex"], chart["planets"][p]["degreeInSign"]]] for i, p in enumerate(VEDIC_PLANETS))
    results = {}
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        for label, dvf in _DVFS.items():
            try:
                method = 2 if dvf == 2 else 1
                pp = charts.divisional_positions_from_rasi_positions(base, divisional_chart_factor=dvf, chart_method=method)
                rows = []
                for pid, (sign, lon) in pp:
                    rows.append({"planet": "Lagna" if pid=="L" else _pid_name(pid, jc),
                                 "sign": SIGNS[sign], "signIndex": sign,
                                 "longitude": round(lon,4)})
                results[label] = {"convention": "PyJHora chart_method=" + str(method), "planets":rows}
            except Exception as e:
                results[label] = {"error": str(e)}
    return results

def _pid_name(pid, jc):
    names = {jc.SUN_ID:"Sun",jc.MOON_ID:"Moon",jc.MARS_ID:"Mars",
             jc.MERCURY_ID:"Mercury",jc.JUPITER_ID:"Jupiter",
             jc.VENUS_ID:"Venus",jc.SATURN_ID:"Saturn",
             jc.RAHU_ID:"Rahu",jc.KETU_ID:"Ketu"}
    return names.get(pid, str(pid))

# ── Yogas ──────────────────────────────────────────────────────────────────────
def yogas(birth, options, divisional=1):
    """Identify Vedic yogas in D1 (or specified divisional chart)."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import yoga as ymod

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            results, found, total = ymod.get_yoga_details(jd, place,
                                                           divisional_chart_factor=divisional)
        except Exception as e:
            return {"error": str(e)}

    out = {}
    for fn, details in results.items():
        if isinstance(details, list) and details:
            out[fn] = {"chart": details[0] if details else "D1",
                       "name": details[1] if len(details)>1 else fn,
                       "description": details[2] if len(details)>2 else "",
                       "benefits": details[3] if len(details)>3 else ""}
        else:
            out[fn] = {"name": fn}

    return {"yogasFound": found, "yogasChecked": total,
            "convention": "Parasara/PVR", "yogas": out}

# ── Shadbala ──────────────────────────────────────────────────────────────────
def shadbala(birth, options):
    """Full Shadbala (six-fold planetary strength) computation."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import strength

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            shad = strength.shad_bala(jd, place)
        except Exception as e:
            return {"error": str(e)}

    # shad is a 9×7 list: planets × [Sthana,Dig,Kala,Cheshta,Naisargika,Drik,Total]
    _COMPONENTS = ["SthanaBala","KalaBala","DigBala","ChestaBala",
                   "NaisargikaBala","DrikBala","Total"]
    planets = list(VEDIC_PLANETS[:7])  # Sun–Saturn
    result = {}
    for i, p in enumerate(planets):
        result[p] = {comp: round(float(shad[j][i]), 3) for j, comp in enumerate(_COMPONENTS)}
        result[p].update(totalRupas=float(shad[7][i]), requiredStrengthRatio=float(shad[8][i]))
    return {"unit":"Virupas","convention":"PyJHora Shadbala; 60 Virupas per Rupa","planets":result}

# ── Bhava Bala ────────────────────────────────────────────────────────────────
def bhava_bala(birth, options):
    """House (Bhava) strength analysis."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import strength

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            bb = strength.bhava_bala(jd, place)
        except Exception as e:
            return {"error": str(e)}

    # bhava_bala returns list of 12-element rows
    _LABELS = ["totalVirupas", "totalRupas", "requiredStrengthRatio"]
    result = {}
    for h in range(12):
        result[f"House{h+1}"] = {_LABELS[j]: round(bb[j][h],3)
                                  for j in range(min(len(bb),3))}
    return {"convention":"Parasara","houses":result}

# ── Ashtakavarga ──────────────────────────────────────────────────────────────
def ashtakavarga(birth, options):
    """Bhinnashtakavarga, Sarvashtakavarga, and Sodhana Pindas."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import ashtakavarga as avmod, charts

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            rasi = charts.rasi_chart(jd, place)
            chart_1d = _chart_1d(rasi)
            bav, sav, pav = avmod.get_ashtaka_varga(chart_1d)
            pindas = avmod.sodhaya_pindas([row[:] for row in bav], chart_1d)
        except Exception as e:
            return {"error": str(e)}

    _PNAMES = ["Sun","Moon","Mars","Mercury","Jupiter","Venus","Saturn","Lagna"]
    bhinnashtaka = {}
    for i, pname in enumerate(_PNAMES):
        if i < len(bav):
            bhinnashtaka[pname] = {SIGNS[s]: bav[i][s] for s in range(12)}
    sarva = {SIGNS[s]: sav[s] for s in range(12)}

    return {"bhinnashtaka": bhinnashtaka,
            "sarvashtaka": sarva,
            "totalSarva": sum(sav),
            "sodhyaPindas": pindas if isinstance(pindas,(list,dict)) else str(pindas)}

# ── Bhava Chalit ──────────────────────────────────────────────────────────────
def bhava_chalit(birth, options):
    """KP/Bhava Chalit – planet-to-cusp house placement."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            midpoints = drik.bhaava_madhya_kp(jd, place)
        except Exception as e:
            return {"error": str(e)}

    cusps = [midpoints[i] for i in range(12)]
    # Re-configure swe after jhora may have altered it
    configure(options)
    sidereal = {}
    for name in VEDIC_PLANETS:
        pos = planet(birth["julianDay"], name, options, sidereal=True)
        h = house_of(pos["longitude"], cusps)
        sidereal[name] = {"longitude": pos["longitude"], "houseKP": h}

    return {"method":"KP/Placidus cusp midpoints",
            "kpCusps": {f"House{i+1}": round(cusps[i],4) for i in range(12)},
            "planets": sidereal}

# ── Jaimini ───────────────────────────────────────────────────────────────────
def jaimini(birth, options):
    """Jaimini karakas, arudhas, aspects, and Karakamsha."""
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.chart import arudhas, house as hmod, charts

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            rasi = charts.rasi_chart(jd, place)
            pp   = rasi[:10]  # up to Ketu
            karakas    = hmod.chara_karakas(pp)
            atma_nak   = _pid_name(karakas[0], jc) if karakas else None
            sthira     = hmod.sthira_karakas(pp)
            arudha_pp  = arudhas.bhava_arudhas_from_planet_positions(pp)
            graha_ar   = arudhas.graha_arudhas_from_planet_positions(pp)
            rasi_drishti = hmod.raasi_drishti_from_chart(_chart_1d(rasi))
        except Exception as e:
            return {"error": str(e)}

    # Karakamsha: sign of AtmaKaraka in D9
    from jhora.horoscope.chart import charts as chmod
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        d9 = chmod.divisional_chart(_jhora_jd(birth), place, divisional_chart_factor=9)
    configure(options)  # restore swe ayanamsa after jhora calls

    ak_sign = None
    if karakas:
        ak_id = karakas[0]
        for pid,(sign,_) in d9:
            if pid == ak_id:
                ak_sign = SIGNS[sign]; break

    # format karakas
    karaka_names = ["Atmakaraka","Amatyakaraka","Bhratrukaraka","Matrukaraka",
                    "Pitrukaraka","Putrakaraka","Gnatikaraka","Darakaraka"]
    karaka_out = {karaka_names[i]: _pid_name(karakas[i], jc)
                  for i in range(len(karakas))}

    arudha_out = {}
    for h, sign in enumerate(arudha_pp):
        if isinstance(sign, (list, tuple)):
            sign = sign[0]
        arudha_out[f"A{h+1}"] = {"sign": SIGNS[int(sign)], "signIndex": int(sign)}

    return {"convention":"PyJHora eight-karaka scheme including reverse-degree Rahu","charaKarakas":karaka_out,
            "atmaKaraka": atma_nak,"karakamsha":ak_sign,
            "bhavaArudhas":arudha_out, "arudhaLagna": arudha_out["A1"], "upapadaLagna": arudha_out["A12"],
            "upapadaSecondSign": SIGNS[(arudha_out["A12"]["signIndex"] + 1) % 12],
            "rasidrishti": rasi_drishti}

# ── KP Astrology ──────────────────────────────────────────────────────────────
def kp_astrology(birth, options):
    """KP sub-lord, significators, and ruling planets."""
    _build_kp_table()
    jd = birth["julianDay"]

    # KP uses Placidus cusps with Krishnamurti ayanamsa
    kp_opts = dict(options, ayanamsa="krishnamurti", houses="placidus")
    configure(kp_opts)
    try:
        cusps, angles = swe.houses_ex(jd, birth["latitude"], birth["longitude"],
                                      b"P")
        cusps, sid_angles = swe.houses_ex(jd, birth["latitude"], birth["longitude"],
                                      b"P", swe.FLG_SIDEREAL)
    except swe.Error as e:
        raise InputError("KP house calculation failed") from e

    # Reset to original ayanamsa
    swe.set_sid_mode(AYANAMSAS[options.get("ayanamsa","lahiri")])

    def _kp_division(lon):
        lon = lon % 360
        for entry in reversed(_KP_SUBS):
            start = entry[0]
            if start <= lon:
                return {"nakshatra":entry[1],"nakshatraLord":entry[2],
                        "sub":entry[3],"subSub":entry[4]}
        # fallback – scan
        prev = None
        for entry in _KP_SUBS:
            if entry[0] > lon:
                break
            prev = entry
        if prev:
            return {"nakshatra":prev[1],"nakshatraLord":prev[2],
                    "sub":prev[3],"subSub":prev[4]}
        return {}

    configure(kp_opts)
    planets_kp = {}
    for name in VEDIC_PLANETS:
        pos = planet(jd, name, {"nodes":options.get("nodes","mean"),
                                "houses":"placidus","ayanamsa":"krishnamurti"}, True)
        kp_div = _kp_division(pos["longitude"])
        planets_kp[name] = {**longitude_details(pos["longitude"]), **kp_div,
                            "house": house_of(pos["longitude"], cusps)}

    # Cusp sub-lords
    cusp_lords = {}
    for i, c in enumerate(cusps):
        kp_div = _kp_division(c % 360)
        cusp_lords[f"Cusp{i+1}"] = {**kp_div, "longitude": c}

    # Ruling planets (lagna lord, moon sign lord, moon nakshatra lord)
    asc_sid = sid_angles[0] % 360
    asc_sign = int(asc_sid // 30)
    moon_pos  = planet(jd, "Moon", {"nodes":"mean","houses":"placidus",
                                     "ayanamsa":"krishnamurti"}, True)
    moon_sign = int(moon_pos["longitude"]//30)
    moon_nak  = int(moon_pos["longitude"]/(40/3))%9
    ruling = [SIGN_LORDS[asc_sign], SIGN_LORDS[moon_sign], NAK_LORDS[moon_nak]]
    ruling = list(dict.fromkeys(ruling))  # deduplicate, order preserved

    swe.set_sid_mode(AYANAMSAS[options.get("ayanamsa","lahiri")])
    owned = {name: [i + 1 for i, cusp in enumerate(cusps) if SIGN_LORDS[int(cusp // 30)] == name] for name in VEDIC_PLANETS}
    significators = {}
    for name, row in planets_kp.items():
        star_lord = row["nakshatraLord"]
        significators[name] = {"starLordOccupiedHouse": planets_kp[star_lord]["house"], "occupiedHouse": row["house"],
                              "starLordOwnedHouses": owned[star_lord], "ownedHouses": owned[name]}
    return {"system":"KP (Krishnamurti Paddhati)","ayanamsa":"Krishnamurti",
            "houseSystem":"Placidus","planets":planets_kp,
            "cuspSubLords":cusp_lords,"rulingPlanets":ruling, "rulingPlanetsEpoch": birth["utcDateTime"],
            "significators": significators, "scope": "Basic occupation/ownership hierarchy; node representation and event judgement not inferred"}

# ── Vimshottari Dasha ─────────────────────────────────────────────────────────
def vimshottari(birth, options, as_of, levels=4):
    """4-level Vimshottari Dasha: Maha, Antara, Pratyantar, Sukshma."""
    from predictive import vimshottari_timeline
    return vimshottari_timeline(birth, options, as_of, levels)

def _format_dasha_list(dasha_list, jc, levels=4, birth=None, sign_lords=False):
    """Convert jhora dasha list to JSON-serialisable structure."""
    out = []
    offset = birth["offsetHours"] if birth else 0
    for entry in dasha_list:
        lords_tuple, (y, m, d, fh), duration_years = entry
        if duration_years <= 0:
            continue
        ids = lords_tuple if isinstance(lords_tuple, (list, tuple)) else [lords_tuple]
        lords = [SIGNS[int(lid)] if sign_lords else _pid_name(int(lid), jc) for lid in ids]
        start_jd = swe.julday(int(y), int(m), int(d), float(fh)) - offset / 24
        out.append({"lords": lords, "start": iso(start_jd),
                    "end": iso(start_jd + float(duration_years) * float(jc.sidereal_year)),
                    "durationYears": float(duration_years), "yearDays": float(jc.sidereal_year)})
    for previous, following in zip(out, out[1:]):
        previous["end"] = following["start"]
    return out

# ── Yogini Dasha ──────────────────────────────────────────────────────────────
def yogini_dasha(birth, options):
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    dob, tob = _dob_tob(birth, drik)

    from jhora.horoscope.dhasa.graha import yogini as ymod
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            result = ymod.get_dhasa_bhukthi(dob, tob, place, dhasa_level_index=3)
        except Exception as e:
            return {"error":str(e)}

    return {"system":"Yogini","periods":_format_dasha_list(result, jc, levels=3, birth=birth)}

# ── Ashtottari Dasha ──────────────────────────────────────────────────────────
def ashtottari_dasha(birth, options):
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    jd       = _jhora_jd(birth)

    from jhora.horoscope.dhasa.graha import ashtottari as amod
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            result = amod.get_ashtottari_dhasa_bhukthi(jd, place,
                                                        dhasa_level_index=3)
        except Exception as e:
            return {"error":str(e)}

    return {"system":"Ashtottari","periods":_format_dasha_list(result, jc, levels=3, birth=birth)}

# ── Chara Dasha ───────────────────────────────────────────────────────────────
def chara_dasha(birth, options):
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)

    from jhora.horoscope.dhasa.raasi import chara
    dob, tob = _dob_tob(birth, drik)

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            result = chara.get_dhasa_antardhasa(dob, tob, place, dhasa_level_index=3)
        except Exception as e:
            return {"error":str(e)}

    return {"system":"Chara (KN Rao default)","periods":_format_dasha_list(result, jc, levels=3, birth=birth, sign_lords=True)}

# ── Narayana Dasha ────────────────────────────────────────────────────────────
def narayana_dasha(birth, options):
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    dob, tob = _dob_tob(birth, drik)

    from jhora.horoscope.dhasa.raasi import narayana as nmod
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            result = nmod.narayana_dhasa_for_rasi_chart(dob, tob, place)
        except Exception as e:
            return {"error":str(e)}

    return {"system":"Narayana","periods":_format_dasha_list(result, jc, levels=2, birth=birth, sign_lords=True)}

# ── Kalachakra Dasha ──────────────────────────────────────────────────────────
def kalachakra_dasha(birth, options):
    jc, drik = _jhora_init(options)
    place    = _jhora_place(birth, drik)
    dob, tob = _dob_tob(birth, drik)

    from jhora.horoscope.dhasa.raasi import kalachakra as kmod
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            result = kmod.get_dhasa_bhukthi(dob, tob, place, dhasa_level_index=3)
        except Exception as e:
            return {"error":str(e)}

    return {"system":"Kalachakra","periods":_format_dasha_list(result, jc, levels=3, birth=birth, sign_lords=True)}

# ── Transits / Gochar ─────────────────────────────────────────────────────────
def transits(birth, options, as_of):
    """Current transit positions vs natal sidereal positions."""
    transit_jd = julian(as_of)
    natal_chart = natal(birth, options)
    natal_sid   = natal_chart["vedic"]["planets"]

    transit_planets = {}
    for name in VEDIC_PLANETS:
        pos = planet(transit_jd, name, options, sidereal=True)
        pos.update(longitude_details(pos["longitude"]))
        natal_lon = natal_sid[name]["longitude"]
        diff = circular_difference(pos["longitude"], natal_lon)
        # Determine transit house from natal ascendant
        asc_sign  = natal_sid["Sun"]["house"] - natal_sid["Sun"]["house"] + \
                    natal_chart["vedic"]["ascendant"]["signIndex"]
        pos["transitHouseFromLagna"] = (pos["signIndex"] - asc_sign) % 12 + 1
        pos["separationFromNatal"]   = round(diff, 4)
        pos["aspectingNatal"]        = abs(diff) < 8 or abs(abs(diff)-180) < 8 or abs(abs(diff)-120) < 8
        transit_planets[name]        = pos

    # Sade Sati: Saturn within 3 signs of natal Moon
    moon_sign = natal_sid["Moon"]["signIndex"]
    sat_sign  = transit_planets["Saturn"]["signIndex"]
    sat_offset= (sat_sign - moon_sign + 12) % 12
    sade_sati = sat_offset in (11,0,1)  # ±1 sign
    dhaiya    = sat_offset in (3,7)            # 4th/8th from Moon

    return {"asOf": as_of.isoformat().replace("+00:00","Z"),
            "planets": transit_planets,
            "sadeSati": {"active": sade_sati,
                         "saturnSign": SIGNS[sat_sign],
                         "natalMoonSign": SIGNS[moon_sign]},
            "dhaiya": {"active": dhaiya,
                       "saturnHouseFromMoon": sat_offset+1}}

# ── Sade Sati timeline ────────────────────────────────────────────────────────
def sade_sati_timeline(birth, options, years_ahead=30):
    """Periods when Saturn is in the sign before, same, or after natal Moon."""
    natal_chart = natal(birth, options)
    moon_sign   = natal_chart["vedic"]["planets"]["Moon"]["signIndex"]
    jd_start    = birth["julianDay"]
    jd_end      = jd_start + years_ahead * 365.25

    periods = []
    jd = jd_start
    step = 27.0  # days
    in_period = False
    period_start = None

    while jd <= jd_end:
        sat_lon = planet(jd, "Saturn", options, sidereal=True)["longitude"]
        sat_sign = int(sat_lon // 30)
        offset   = (sat_sign - moon_sign + 12) % 12
        active   = offset in (11, 0, 1)
        if active and not in_period:
            in_period = True
            period_start = jd
        elif not active and in_period:
            in_period = False
            periods.append({"start": iso(period_start),
                            "end": iso(jd),
                            "durationYears": round((jd-period_start)/365.25, 2)})
        jd += step

    if in_period:
        periods.append({"start": iso(period_start), "end": iso(jd_end), "ongoing": True})

    return {"natalMoonSign": SIGNS[moon_sign], "yearsAhead": years_ahead,
            "periods": periods}

# ── Solar Return ──────────────────────────────────────────────────────────────
def solar_return(birth, options, return_year: int):
    """Chart for the moment the Sun returns to its natal longitude."""
    natal_chart = natal(birth, options)
    natal_sun   = natal_chart["western"]["planets"]["Sun"]["longitude"]
    # Search within the return year
    jd0 = julian(datetime(return_year, 1, 1, tzinfo=timezone.utc))
    jd1 = julian(datetime(return_year, 12, 31, tzinfo=timezone.utc))
    # Binary search
    lo, hi = jd0, jd0 + 366
    for _ in range(50):
        mid = (lo+hi)/2
        sun_lon = planet(mid, "Sun", options)["longitude"]
        diff = circular_difference(sun_lon, natal_sun)
        if abs(diff) < 1e-6:
            break
        if diff < 0:
            lo = mid
        else:
            hi = mid
    sr_jd = (lo+hi)/2
    chart  = natal(birth, options, jd=sr_jd)
    return {"type":"SolarReturn","year":return_year,
            "momentUTC":iso(sr_jd),"natalSunLongitude":natal_sun,
            "chart":chart}

# ── Lunar Return ──────────────────────────────────────────────────────────────
def lunar_return(birth, options, as_of):
    """Chart for the next moment Moon returns to natal longitude."""
    natal_chart = natal(birth, options)
    natal_moon  = natal_chart["western"]["planets"]["Moon"]["longitude"]
    jd0 = julian(as_of)
    lo, hi = jd0, jd0 + 30
    for _ in range(60):
        mid = (lo+hi)/2
        moon_lon = planet(mid, "Moon", options)["longitude"]
        diff = circular_difference(moon_lon, natal_moon)
        if abs(diff) < 1e-6:
            break
        if diff < 0:
            lo = mid
        else:
            hi = mid
    lr_jd = (lo+hi)/2
    chart  = natal(birth, options, jd=lr_jd)
    return {"type":"LunarReturn","momentUTC":iso(lr_jd),
            "natalMoonLongitude":natal_moon,"chart":chart}

# ── Secondary Progressions ────────────────────────────────────────────────────
def secondary_progressions(birth, options, as_of):
    """One day after birth = one year of life (Day-for-a-Year method)."""
    days_elapsed = (julian(as_of) - birth["julianDay"])
    prog_jd = birth["julianDay"] + days_elapsed / 365.25
    prog_birth = dict(birth, julianDay=prog_jd)
    prog_chart = natal(prog_birth, options)
    natal_c    = natal(birth, options)

    asp = aspects(prog_chart["western"]["planets"],
                  natal_c["western"]["planets"], orb=3)
    return {"method":"SecondaryProgressions","dayForAYear":True,
            "progressedJD": prog_jd,"progressedDate": iso(prog_jd),
            "progressedChart": prog_chart,
            "aspectsToNatal": asp}

# ── Solar Arc Directions ──────────────────────────────────────────────────────
def solar_arc(birth, options, as_of):
    """Solar arc: each planet advanced by Sun's progressed motion."""
    jd_birth = birth["julianDay"]
    days_elapsed = julian(as_of) - jd_birth
    prog_jd  = jd_birth + days_elapsed / 365.25
    sun_natal   = planet(jd_birth, "Sun", options)["longitude"]
    sun_prog    = planet(prog_jd, "Sun", options)["longitude"]
    arc = (sun_prog - sun_natal) % 360

    natal_c = natal(birth, options)
    directed = {}
    for name, data in natal_c["western"]["planets"].items():
        dl = (data["longitude"] + arc) % 360
        directed[name] = {**longitude_details(dl), "longitude": dl}

    return {"method":"SolarArc","arcDegrees":round(arc,4),
            "directedPlanets":directed}

# ── Annual Profections ────────────────────────────────────────────────────────
def annual_profections(birth, as_of, options=None):
    """Western timing: one house per year, activated house and its lord."""
    dob = date.fromisoformat(birth["dob"])
    local = as_of.astimezone(ZoneInfo(birth["timezone"])).date()
    age = local.year - dob.year - ((local.month, local.day) < (dob.month, dob.day))
    if age < 0:
        raise InputError("Profections require an analysis date on or after birth")
    settings = configure(options or {})
    ascendant = natal(birth, settings)["western"]["ascendant"]
    profected_house = age % 12 + 1
    sign = (int(ascendant // 30) + age) % 12
    return {"method":"AnnualProfections", "age":age, "profectedSign": SIGNS[sign],
            "profectedHouse":profected_house, "convention":"Tropical whole-sign; civil birthday; March 1 for Feb 29 in non-leap years",
            "timeLord":SIGN_LORDS[sign],"yearTheme":_house_theme(profected_house)}

def _house_theme(h):
    themes = {1:"Self/health/body",2:"Finances/possessions",
              3:"Communication/siblings",4:"Home/family/mother",
              5:"Creativity/children/romance",6:"Work/service/health",
              7:"Partnerships/marriage",8:"Transformation/shared resources",
              9:"Travel/philosophy/higher learning",
              10:"Career/reputation/authority",
              11:"Friends/goals/networks",12:"Hidden matters/spirituality"}
    return themes.get(h,"")

# ── Zodiacal Releasing ────────────────────────────────────────────────────────
def zodiacal_releasing(birth, options, as_of, lot="spirit"):
    """Hellenistic timing technique from Lot of Spirit or Fortune."""
    natal_c  = natal(birth, options)
    # Fortune = Asc + Moon – Sun; Spirit = Asc + Sun – Moon (diurnal)
    asc_lon  = natal_c["western"]["ascendant"]
    sun_lon  = natal_c["western"]["planets"]["Sun"]["longitude"]
    moon_lon = natal_c["western"]["planets"]["Moon"]["longitude"]
    if lot == "fortune":
        lot_lon = (asc_lon + moon_lon - sun_lon) % 360
    else:  # spirit
        lot_lon = (asc_lon + sun_lon - moon_lon) % 360
    lot_sign = int(lot_lon // 30)
    # Release periods (sign years by traditional scheme)
    _SIGN_YEARS = [15,8,20,20,19,20,8,15,12,27,30,12]  # Aries..Pisces
    periods = []
    jd = birth["julianDay"]
    for cycle in range(4):
        for i in range(12):
            sign = (lot_sign + i + cycle * 12) % 12
            y = _SIGN_YEARS[sign]
            end_jd = jd + y * 365.25
            if jd > julian(as_of) + 20 * 365.25:
                break
            periods.append({"sign":SIGNS[sign],"lord":SIGN_LORDS[sign],
                            "start":iso(jd),"end":iso(end_jd),
                            "durationYears":y})
            jd = end_jd

    # Find current period
    as_of_jd = julian(as_of)
    current = next((p for p in periods
                    if iso_to_jd(p["start"]) <= as_of_jd < iso_to_jd(p["end"])), None)
    return {"lot":lot,"lotLongitude":round(lot_lon,4),
            "lotSign":SIGNS[lot_sign],"releasingPeriods":periods[:20],
            "currentPeriod":current}

def iso_to_jd(iso_str):
    dt = datetime.fromisoformat(iso_str.replace("Z","+00:00"))
    return julian(dt.astimezone(timezone.utc))

# ── Synastry ──────────────────────────────────────────────────────────────────
def synastry(birth1, birth2, options):
    """Cross-chart aspects between two natal charts."""
    c1 = natal(birth1, options)
    c2 = natal(birth2, options)
    asp12 = aspects(c1["western"]["planets"], c2["western"]["planets"], orb=8)
    asp21 = aspects(c2["western"]["planets"], c1["western"]["planets"], orb=8)
    # House overlays: person2 planets in person1 houses
    overlays = {}
    for name, data in c2["western"]["planets"].items():
        h = house_of(data["longitude"], c1["western"]["cusps"])
        overlays[name] = {"house":h,"theme":_house_theme(h)}
    return {"type":"Synastry",
            "chart1_to_chart2_aspects":asp12,
            "chart2_to_chart1_aspects":asp21,
            "chart2PlanetsInChart1Houses":overlays}

# ── Composite Chart ───────────────────────────────────────────────────────────
def composite_chart(birth1, birth2, options):
    """Midpoint composite chart."""
    c1 = natal(birth1, options)
    c2 = natal(birth2, options)
    composite = {}
    for name in c1["western"]["planets"]:
        if name in c2["western"]["planets"]:
            lon1 = c1["western"]["planets"][name]["longitude"]
            lon2 = c2["western"]["planets"][name]["longitude"]
            mid  = _midpoint(lon1, lon2)
            composite[name] = {**longitude_details(mid), "longitude": mid}
    asc_mid = _midpoint(c1["western"]["ascendant"], c2["western"]["ascendant"])
    mc_mid  = _midpoint(c1["western"]["midheaven"], c2["western"]["midheaven"])
    return {"type":"Composite","ascendant":round(asc_mid,4),
            "midheaven":round(mc_mid,4),"planets":composite,
            "aspects":aspects(composite)}

def _midpoint(a, b):
    diff = (b - a + 180) % 360 - 180
    return (a + diff/2) % 360

# ── Compatibility / Guna Milan ────────────────────────────────────────────────
def guna_milan(birth1, birth2, options):
    """Vedic Ashtakoota compatibility scoring."""
    jc, drik = _jhora_init(options)

    def _moon_nak(birth):
        configure(options)
        row = longitude_details(planet(birth["julianDay"], "Moon", options, True)["longitude"])
        return row["nakshatraIndex"], row["pada"]

    from jhora.horoscope.match import compatibility as cm

    nak1, pada1 = _moon_nak(birth1)
    nak2, pada2 = _moon_nak(birth2)

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        ak = cm.Ashtakoota(nak1, pada1, nak2, pada2)
        total = ak.compatibility_score()

    _KOOTAS = [
        ("Varna",   "varna_porutham",         1),
        ("Vasya",   "vasiya_porutham",         2),
        ("Dina",    "dina_porutham",           3),
        ("Gana",    "gana_porutham",           6),
        ("Yoni",    "yoni_porutham",           4),
        ("Maitri",  "raasi_adhipathi_porutham",5),
        ("Bhakoot", "raasi_porutham",          7),
        ("Naadi",   "naadi_porutham",          8),
    ]
    scores = {}
    for koota_name, method_name, max_score in _KOOTAS:
        try:
            method = getattr(ak, method_name, None)
            sc = method() if method else None
            if isinstance(sc, (list, tuple)): sc = sc[0] if sc else None
        except Exception:
            sc = None
        scores[koota_name] = {"score": sc, "maxScore": max_score}

    # compatibility_score() returns list; index 8 is total
    total_score = total[8] if isinstance(total, (list,tuple)) and len(total)>8 else total

    return {"system":"Ashtakoota","person1Nakshatra":NAKSHATRAS[nak1-1],
            "person2Nakshatra":NAKSHATRAS[nak2-1],
            "totalScore":total_score,"maxScore":36,
            "kootas":scores,
            "interpretation":_guna_interpret(total_score)}

def _guna_interpret(score):
    if score >= 32: return "Excellent match"
    if score >= 28: return "Very good match"
    if score >= 18: return "Good match"
    if score >= 10: return "Average match"
    return "Below average – traditional caution advised"

# ── Eclipse analysis ──────────────────────────────────────────────────────────
def eclipses(as_of, options, count=6):
    """Find next solar and lunar eclipses using Swiss Ephemeris."""
    jd_start = julian(as_of)
    solar_list, lunar_list = [], []

    # Solar eclipses: advance jd past each found eclipse
    jd = jd_start
    for _ in range(count):
        try:
            ret = swe.sol_eclipse_when_glob(jd, swe.FLG_SWIEPH, 0, False)
            if ret and ret[1] and ret[1][0] > jd:
                eclipse_jd = ret[1][0]
                solar_list.append({"utc": iso(eclipse_jd),
                                   "type": _eclipse_type(ret[0], solar=True),
                                   "julianDay": eclipse_jd})
                jd = eclipse_jd + 25
            else:
                jd += 180
        except Exception:
            jd += 180

    # Lunar eclipses
    jd = jd_start
    for _ in range(count):
        try:
            ret = swe.lun_eclipse_when(jd, swe.FLG_SWIEPH, 0, False)
            if ret and ret[1] and ret[1][0] > jd:
                eclipse_jd = ret[1][0]
                lunar_list.append({"utc": iso(eclipse_jd),
                                   "type": _eclipse_type(ret[0], solar=False),
                                   "julianDay": eclipse_jd})
                jd = eclipse_jd + 25
            else:
                jd += 180
        except Exception:
            jd += 180

    return {"eclipses": {"solar": solar_list, "lunar": lunar_list},
            "disclaimer": DISCLAIMER}

def _eclipse_type(iflg, solar):
    if solar:
        if iflg & swe.ECL_TOTAL:   return "Total"
        if iflg & swe.ECL_ANNULAR: return "Annular"
        if iflg & swe.ECL_ANNULAR_TOTAL: return "Annular-Total"
        return "Partial"
    else:
        if iflg & swe.ECL_TOTAL: return "Total"
        if iflg & swe.ECL_PENUMBRAL: return "Penumbral"
        return "Partial"

# ── Retrograde periods ────────────────────────────────────────────────────────
def retrograde_periods(as_of, options, years_ahead=2):
    """Retrograde ingress/egress dates for all visible planets."""
    jd_start = julian(as_of)
    jd_end   = jd_start + years_ahead * 365.25
    planets_to_check = ["Mercury","Venus","Mars","Jupiter","Saturn",
                        "Uranus","Neptune","Pluto"]
    result = {}
    step = 1.0
    for name in planets_to_check:
        periods = []
        in_retro = False
        retro_start = None
        jd = jd_start
        prev_speed = None
        while jd <= jd_end:
            try:
                pos = planet(jd, name, options)
                speed = pos["speedDegreesPerDay"]
                if prev_speed is not None:
                    if speed < 0 and prev_speed >= 0 and not in_retro:
                        in_retro = True; retro_start = jd
                    elif speed >= 0 and prev_speed < 0 and in_retro:
                        in_retro = False
                        periods.append({"start":iso(retro_start),"end":iso(jd),
                                        "durationDays":round(jd-retro_start,1)})
                prev_speed = speed
            except Exception:
                pass
            jd += step
        if in_retro:
            periods.append({"start":iso(retro_start),"end":iso(jd_end),"ongoing":True})
        result[name] = periods

    return {"retrogradesByPlanet":result,"yearsAhead":years_ahead}

# ── Planetary cycles ──────────────────────────────────────────────────────────
def planetary_cycles(birth, options, as_of):
    """Saturn return, Jupiter return, nodal return, and outer-planet cycles."""
    configure(options)
    natal_c = natal(birth, options)
    results = {}
    cycles = {
        "SaturnReturn":   ("Saturn",   29.5),
        "JupiterReturn":  ("Jupiter",  11.86),
        "RahuReturn":     ("Rahu",     18.6),
        "UranusOpposition":("Uranus",  42.0),
    }
    as_of_jd = julian(as_of)
    birth_jd  = birth["julianDay"]
    for label, (planet_name, period_years) in cycles.items():
        natal_lon = natal_c["western"]["planets"].get(planet_name,{}).get("longitude")
        if natal_lon is None:
            continue
        age_years = (as_of_jd - birth_jd) / 365.25
        returns = []
        for n in range(1, 5):
            approx_jd = birth_jd + n * period_years * 365.25
            if approx_jd < birth_jd: continue
            # Refine
            lo, hi = approx_jd - period_years*0.05*365.25, approx_jd + period_years*0.05*365.25
            for _ in range(40):
                mid = (lo+hi)/2
                try:
                    lon = planet(mid, planet_name, options)["longitude"]
                except Exception:
                    break
                diff = circular_difference(lon, natal_lon)
                if abs(diff) < 1e-5: break
                if diff < 0: lo = mid
                else: hi = mid
            returns.append({"returnNumber":n,"approximateDate":iso((lo+hi)/2),
                            "ageAtReturn":round(((lo+hi)/2-birth_jd)/365.25,1)})
        results[label] = {"planet":planet_name,"periodYears":period_years,
                          "returns":returns}
    return results

# ── Numerology ────────────────────────────────────────────────────────────────
def numerology(birth, full_name="", as_of=None):
    if as_of is None:
        raise InputError("Numerology requires an explicit analysis date")
    if any(letter.isalpha() and letter.lower() not in _PYTH for letter in full_name):
        raise InputError("Provide a Latin A-Z transliteration for name numerology")
    analysis_date = as_of.astimezone(ZoneInfo(birth["timezone"]))
    dob = birth["dob"]
    digits_dob = [int(c) for c in dob if c.isdigit()]
    life_path  = _reduce(sum(digits_dob))
    birth_day  = _reduce(int(dob.split("-")[2]))

    name_clean = full_name.lower().strip()
    letters    = [c for c in name_clean if c.isalpha()]
    vowel_letters   = [c for c in letters if c in _VOWELS]
    consonant_letters = [c for c in letters if c not in _VOWELS]

    pyth_expr  = _reduce(sum(_PYTH.get(c,0) for c in letters))
    pyth_soul  = _reduce(sum(_PYTH.get(c,0) for c in vowel_letters))
    pyth_pers  = _reduce(sum(_PYTH.get(c,0) for c in consonant_letters))
    chald_name = _reduce(sum(_CHALD.get(c,0) for c in letters))

    # Personal year: life path + current year digits
    today_year = int(dob.split("-")[0])  # use birth year as base; caller supplies as_of separately
    py_digits = [int(c) for c in dob.split("-")[1]] + [int(c) for c in dob.split("-")[2]]
    # Standard personal year = sum of month + day + current_year reduced
    current_year = analysis_date.year
    personal_year = _reduce(sum([int(d) for d in dob.split("-")[1]]
                               +[int(d) for d in dob.split("-")[2]]
                               +[int(d) for d in str(current_year)]))

    return {"systems":{"pythagorean":{"lifePath":life_path,
                                       "expression":pyth_expr,
                                       "soulUrge":pyth_soul,
                                       "personality":pyth_pers,
                                       "birthDay":birth_day,
                                       "personalYear":personal_year,
                                       "personalMonth":_reduce(personal_year + analysis_date.month),
                                       "personalDay":_reduce(_reduce(personal_year + analysis_date.month) + analysis_date.day)},
                        "convention":"Calendar-year cycle in birth timezone; master numbers 11/22/33 retained; Y is a consonant",
                        "chaldean":{"nameNumber":chald_name,
                                    "lifePath":_reduce(sum([int(d) for d in str(_reduce(sum(digits_dob)))]))}},
            "disclaimer":DISCLAIMER}

def _reduce(n):
    """Reduce to single digit, keeping master numbers 11, 22, 33."""
    while n > 9 and n not in (11, 22, 33):
        n = sum(int(d) for d in str(n))
    return n

# ── Tarot ─────────────────────────────────────────────────────────────────────
def tarot(seed_string: str, spread: str = "celtic_cross"):
    """Reproducible tarot spread seeded from a string (date + question hash)."""
    h = hashlib.sha256(seed_string.encode()).digest()
    deck = list(range(78))
    # Fisher-Yates with SHA-256 bytes as entropy
    rng_bytes = bytearray(hashlib.sha256(h+b"\x00").digest()
                         +hashlib.sha256(h+b"\x01").digest()
                         +hashlib.sha256(h+b"\x02").digest()
                         +hashlib.sha256(h+b"\x03").digest())
    idx = 0
    for i in range(77, 0, -1):
        j = int.from_bytes(rng_bytes[idx:idx+2], "big") % (i+1)
        idx = (idx+2) % len(rng_bytes)
        deck[i], deck[j] = deck[j], deck[i]
    reversed_prob = [bool((rng_bytes[k%len(rng_bytes)] & 1)) for k in range(78)]

    spreads = {
        "celtic_cross": ["Significator","Crossing","Foundation","Recent Past",
                         "Crown","Near Future","Self","Environment",
                         "Hopes/Fears","Outcome"],
        "three_card":   ["Past","Present","Future"],
        "yes_no":       ["Answer"],
        "relationship": ["You","Partner","Relationship","Strength","Challenge","Advice","Outcome"],
        "career":       ["Current Situation","Opportunities","Challenges",
                         "Skills","Outcome"],
        "timing":       ["Current phase", "What may accelerate", "What may delay", "Next phase", "Readiness cue"],
    }
    if spread not in spreads:
        raise InputError("Unsupported tarot spread")
    positions = spreads[spread]
    drawn = []
    for i, pos in enumerate(positions):
        card_idx  = deck[i % 78]
        is_major  = card_idx < 22
        card_name = _ALL_CARDS[card_idx]
        rev       = reversed_prob[i]
        drawn.append({"position":pos,"card":card_name,
                      "reversed":rev,"arcana":"major" if is_major else "minor",
                      "display": card_name+(" (Reversed)" if rev else "")})
    return {"spread":spread,"seed":seed_string,"cards":drawn,
            "disclaimer":DISCLAIMER}

# ── Chinese Astrology – BaZi / Four Pillars ───────────────────────────────────
def chinese_astrology(birth, options):
    """BaZi Four Pillars from birth date/time using Swiss Ephemeris solar terms."""
    dob     = birth["dob"]
    time_s  = birth["time"]
    parts   = time_s.split(":")
    hour    = int(parts[0]); minute = int(parts[1])
    year, month, day = [int(x) for x in dob.split("-")]
    local_dt = datetime(year, month, day, hour, minute, tzinfo=timezone.utc)
    jd_birth = birth["julianDay"]

    # Year pillar: BaZi year starts at Li Chun (Sun at 315° tropical)
    bazi_year = _bazi_year(jd_birth, year)
    year_stem   = _STEMS[(bazi_year-4) % 10]
    year_branch = _BRANCHES[(bazi_year-4) % 12]
    year_animal = _BRANCH_ANIMALS[(bazi_year-4) % 12]

    # Month pillar: based on solar term the birth falls in
    month_branch_idx, month_stem_idx = _bazi_month(jd_birth, bazi_year)
    month_stem   = _STEMS[month_stem_idx % 10]
    month_branch = _BRANCHES[month_branch_idx]

    # Day pillar: 60-day cycle from epoch
    day_cycle = int(jd_birth - _DAY_PILLAR_EPOCH_JD) % 60
    day_stem   = _STEMS[day_cycle % 10]
    day_branch = _BRANCHES[day_cycle % 12]

    # Hour pillar: 2-hour intervals, 0=Zi(23-01), 1=Chou(01-03)...
    hour_branch_idx = ((hour + 1) // 2) % 12
    hour_stem_idx   = (day_cycle % 5) * 2 + hour_branch_idx % 2
    hour_stem   = _STEMS[hour_stem_idx % 10]
    hour_branch = _BRANCHES[hour_branch_idx]

    def pillar(stem, branch):
        b_idx = _BRANCHES.index(branch)
        return {"heavenlyStem":stem,"earthlyBranch":branch,
                "animal":_BRANCH_ANIMALS[b_idx],
                "stemElement":_STEM_ELEMENTS[_STEMS.index(stem)],
                "stemPolarity":_STEM_POLARITY[_STEMS.index(stem)],
                "branchElement":_BRANCH_ELEMENTS[b_idx]}

    # Luck pillars (10-year cycles starting 10 years from birth)
    luck_pillars = []
    for i in range(8):
        offset = 10 + i * 10
        lp_stem   = _STEMS[(day_cycle % 10 + offset) % 10]
        lp_branch = _BRANCHES[(day_cycle % 12 + offset) % 12]
        luck_pillars.append({"startAge":offset, **pillar(lp_stem, lp_branch)})

    return {"system":"BaZi / Four Pillars of Destiny",
            "pillars":{"year":pillar(year_stem, year_branch),
                       "month":pillar(month_stem, month_branch),
                       "day":pillar(day_stem, day_branch),
                       "hour":pillar(hour_stem, hour_branch)},
            "baziYear":bazi_year,
            "luckPillars":luck_pillars,
            "disclaimer":DISCLAIMER}

def _bazi_year(jd_birth, gregorian_year):
    """Return BaZi year integer (same as Gregorian if birth after Li Chun)."""
    # Li Chun = Sun at 315° tropical, around Feb 4
    li_chun_jd = _solar_term_jd(315.0, gregorian_year)
    return gregorian_year if jd_birth >= li_chun_jd else gregorian_year - 1

def _bazi_month(jd_birth, bazi_year):
    """Return (branch_index, stem_index) for BaZi month."""
    gregorian_year = bazi_year
    # Find which solar term interval the birth falls in
    for i in range(12):
        term_lon = _SOLAR_TERM_LONS[i]
        start_jd = _solar_term_jd(term_lon, gregorian_year)
        next_i   = (i+1) % 12
        next_y   = gregorian_year + (1 if next_i == 0 else 0)
        end_jd   = _solar_term_jd(_SOLAR_TERM_LONS[next_i], next_y)
        if start_jd <= jd_birth < end_jd:
            branch = _MONTH_BRANCHES[i]
            # month stem: based on year stem index
            year_stem_idx = (bazi_year - 4) % 10
            stem = (year_stem_idx % 5) * 2 + _MONTH_BRANCHES.index(branch) % 2
            return branch, stem
    return 2, 0  # fallback: Tiger month, Jia stem

def _solar_term_jd(target_lon: float, year: int) -> float:
    """Find JD when tropical Sun longitude equals target_lon in given year."""
    jd0 = julian(datetime(year, 1, 1, tzinfo=timezone.utc))
    lo, hi = jd0, jd0 + 370
    for _ in range(60):
        mid = (lo+hi)/2
        try:
            lon,*_ = swe.calc_ut(mid, swe.SUN, swe.FLG_SWIEPH)[0]
        except Exception:
            return mid
        diff = circular_difference(lon % 360, target_lon)
        if abs(diff) < 1e-6: break
        if diff < 0: lo = mid
        else: hi = mid
    return (lo+hi)/2

# ── Prashna / Horary ──────────────────────────────────────────────────────────
def prashna(location, as_of, options, question=""):
    """Chart cast for the moment and place a question is asked."""
    configure(options)
    prashna_birth = {
        "dob":    as_of.strftime("%Y-%m-%d"),
        "time":   as_of.strftime("%H:%M:%S"),
        "city":   location.get("city","Custom"),
        "latitude":  number(location.get("latitude", 28.6), "latitude", -89.999, 89.999),
        "longitude": number(location.get("longitude",77.2),"longitude",-180,180),
        "timezone":  location.get("timezone","UTC"),
        "utcOffset": "+00:00",
        "utcDateTime": as_of.isoformat().replace("+00:00","Z"),
        "julianDay": julian(as_of),
        "offsetHours": 0.0,
        "localDateTime": as_of.isoformat(),
    }
    chart = natal(prashna_birth, options)
    # Vedic Prashna analysis
    asc_sign = chart["vedic"]["ascendant"]["signIndex"]
    asc_lord = SIGN_LORDS[asc_sign]
    moon_sign = chart["vedic"]["planets"]["Moon"]["signIndex"]
    moon_lord = SIGN_LORDS[moon_sign]
    moon_nak  = chart["vedic"]["planets"]["Moon"]["nakshatra"]
    return {"type":"Prashna","question":question[:500],
            "momentUTC": as_of.isoformat().replace("+00:00","Z"),
            "ascendant": chart["vedic"]["ascendant"],
            "ascendantLord": asc_lord,
            "moonSign": SIGNS[moon_sign],"moonLord": moon_lord,
            "moonNakshatra": moon_nak,
            "chart": chart,
            "disclaimer":DISCLAIMER}

# ── Muhurta ───────────────────────────────────────────────────────────────────
def muhurta(location, as_of, duration_hours, options, purpose="general"):
    """Evaluate auspiciousness of a time window using panchanga elements."""
    jc, drik = _jhora_init(options)

    lat = number(location.get("latitude",28.6),"latitude",-89.999,89.999)
    lon_l = number(location.get("longitude",77.2),"longitude",-180,180)
    tz  = location.get("timezone","UTC")
    offset = location.get("offsetHours", 0.0)
    place = drik.Place(location.get("city","Custom"), lat, lon_l, offset, elevation=0)

    jd = julian(as_of)

    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            tithi_r   = drik.tithi(jd, place)
            naksh_r   = drik.nakshatra(jd, place)
            yoga_r    = drik.yogam(jd, place)
            karana_r  = drik.karana(jd, place)
            weekday   = drik.civil_weekday(jd)
        except Exception as e:
            return {"error":str(e)}

    # Inauspicious periods (Rahukalam etc.)
    with open(os.devnull,"w") as s, contextlib.redirect_stdout(s):
        try:
            rahu = drik.trikalam(jd, place, option="raahu kaalam")
            yama = drik.trikalam(jd, place, option="yamagandam")
            gulika = drik.trikalam(jd, place, option="gulikai")
        except Exception:
            rahu = yama = gulika = None

    _WEEKDAYS = ["Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"]
    return {"purpose":purpose,
            "momentUTC":as_of.isoformat().replace("+00:00","Z"),
            "durationHours":duration_hours,
            "panchanga":{"tithi":tithi_r,"nakshatra":naksh_r,
                         "yoga":yoga_r,"karana":karana_r,
                         "weekday":_WEEKDAYS[weekday % 7]},
            "inauspiciousPeriods":{"rahuKalam":_fmt_period(rahu),
                                   "yamaganda":_fmt_period(yama),
                                   "gulikaKalam":_fmt_period(gulika)},
            "disclaimer":DISCLAIMER}

def _fmt_period(r):
    if r is None: return None
    if isinstance(r,(list,tuple)) and len(r)>=2:
        # trikalam returns time strings like ['14:03:11','15:33:48']
        return {"start": str(r[0]), "end": str(r[1])}
    return str(r)

# ── Topic prediction engine ───────────────────────────────────────────────────
def topic_prediction(birth, options, as_of, topic="general"):
    """Multi-method synthesis: natal + dasha + transits for a given topic."""
    configure(options)
    natal_c     = natal(birth, options)
    transit_c   = transits(birth, options, as_of)
    vim         = vimshottari(birth, options, as_of, levels=3)
    profection  = annual_profections(birth, as_of)

    # Identify the active Maha, Antara dasha lords
    as_jd = julian(as_of)
    active_maha = active_antara = None
    for p in vim.get("periods",[]):
        start_jd = iso_to_jd(p["start"])
        end_jd   = start_jd + p["durationYears"]*365.25
        if start_jd <= as_jd <= end_jd:
            lords = p.get("lords",[])
            if len(lords) >= 1: active_maha   = lords[0]
            if len(lords) >= 2: active_antara = lords[1]
            break

    _TOPICS = {
        "career":   [10,6,2,11],
        "marriage": [7,2,5],
        "health":   [1,6,8,12],
        "finance":  [2,5,9,11],
        "education":[4,5,9],
        "travel":   [3,9,12],
        "general":  list(range(1,13)),
    }
    relevant_houses = _TOPICS.get(topic, _TOPICS["general"])

    # Find natal planets in relevant houses
    relevant_planets = [name for name,data in natal_c["vedic"]["planets"].items()
                        if data.get("house") in relevant_houses]

    # Find transiting planets in relevant houses
    asc_sign = natal_c["vedic"]["ascendant"]["signIndex"]
    transiting = [name for name, data in transit_c["planets"].items()
                  if data.get("transitHouseFromLagna") in relevant_houses]

    indicators = {
        "topic": topic,
        "relevantHouses": relevant_houses,
        "natalPlanetsInTopicHouses": relevant_planets,
        "currentTransitsInTopicHouses": transiting,
        "activeMahaDasha": active_maha,
        "activeAntarDasha": active_antara,
        "profectedHouse": profection["profectedHouse"],
        "profectionTimeLord": profection["timeLord"],
        "sadeSatiActive": transit_c["sadeSati"]["active"],
        "supporting": [],
        "conflicting": [],
    }

    # Simple rule-based supporting/conflicting indicators
    if active_maha in relevant_planets:
        indicators["supporting"].append(f"Active Maha Dasha lord {active_maha} is a topic planet")
    if "Jupiter" in transiting and topic in ("finance","career","education"):
        indicators["supporting"].append("Jupiter transiting a topic house is considered auspicious")
    if "Saturn" in relevant_planets and transit_c["sadeSati"]["active"]:
        indicators["conflicting"].append("Sade Sati active with natal Saturn in a topic house")
    if profection["timeLord"] in relevant_planets:
        indicators["supporting"].append(f"Annual profection time lord {profection['timeLord']} is a topic planet")

    return {"method":"MultiMethodSynthesis",
            "conventions":"Vimshottari+Transits+Profections",
            "indicators": indicators,
            "disclaimer": DISCLAIMER}
