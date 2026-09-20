from datetime import datetime, timedelta, timezone
from zoneinfo import ZoneInfo

import swisseph as swe
from lunar_python import Solar

import astro_core as c

TOPICS = {
    "career": ((10, 6, 2, 11), ("Sun", "Saturn", "Mercury"), ("D10",)),
    "marriage": ((7, 2, 5), ("Venus", "Jupiter"), ("D9",)),
    "relationships": ((7, 5, 11), ("Venus", "Moon", "Jupiter"), ("D9",)),
    "finance": ((2, 5, 9, 11), ("Jupiter", "Venus"), ("D2",)),
    "health": ((1, 6, 8, 12), ("Sun", "Moon"), ("D1",)),
    "education": ((4, 5, 9), ("Mercury", "Jupiter"), ("D24",)),
    "travel": ((3, 9, 12), ("Moon", "Rahu"), ("D4",)),
    "life_transitions": ((1, 4, 8, 10, 12), ("Saturn", "Rahu", "Ketu"), ("D9",)),
    "general": (tuple(range(1, 13)), ("Sun", "Moon", "Jupiter"), ("D1",)),
}


def integer(value, name, low, high):
    if isinstance(value, bool) or not isinstance(value, int) or not low <= value <= high:
        raise c.InputError(f"{name} must be an integer from {low} to {high}")
    return value


def active_period(periods, jd):
    return next((p for p in periods if c.iso_to_jd(p["start"]) <= jd < c.iso_to_jd(p["end"])), None)


def vimshottari_timeline(birth, options, as_of, levels=4):
    integer(levels, "dashaLevels", 1, 4)
    c.configure(options)
    moon = c.planet(birth["julianDay"], "Moon", options, True)["longitude"]
    mansion = int(moon / (40 / 3))
    lord_index = mansion % 9
    fraction = (moon % (40 / 3)) / (40 / 3)
    year_days = 365.25
    beginning = birth["julianDay"] - fraction * c.DASHA_YEARS[c.DASHA_ORDER[lord_index]] * year_days
    stop = birth["julianDay"] + 120 * year_days
    periods = []

    def descend(lords, start, end):
        if end <= birth["julianDay"] or start >= stop:
            return
        if len(lords) == levels:
            clipped_start, clipped_end = max(start, birth["julianDay"]), min(end, stop)
            periods.append({"lords": lords, "start": c.iso(clipped_start), "end": c.iso(clipped_end),
                            "fullStart": c.iso(start), "durationYears": (clipped_end - clipped_start) / year_days})
            return
        first = c.DASHA_ORDER.index(lords[-1])
        cursor = start
        for index in range(9):
            child = c.DASHA_ORDER[(first + index) % 9]
            boundary = end if index == 8 else cursor + (end - start) * c.DASHA_YEARS[child] / 120
            descend(lords + [child], cursor, boundary)
            cursor = boundary

    cursor = beginning
    first_end = beginning + c.DASHA_YEARS[c.DASHA_ORDER[lord_index]] * year_days
    for index in range(11):
        lord = c.DASHA_ORDER[(lord_index + index) % 9]
        end = cursor + c.DASHA_YEARS[lord] * year_days
        descend([lord], cursor, end)
        cursor = end
        if cursor >= stop:
            break
    return {"system": "Vimshottari", "levels": levels, "yearDays": year_days,
            "intervalConvention": "UTC start inclusive, end exclusive; children calculated before birth clipping",
            "birthLord": c.DASHA_ORDER[lord_index], "balanceYears": (first_end - birth["julianDay"]) / year_days,
            "periods": periods, "current": active_period(periods, c.julian(as_of)),
            "asOf": as_of.isoformat().replace("+00:00", "Z")}


def topic_analysis(birth, options, as_of, topic="general"):
    if topic not in TOPICS:
        raise c.InputError("Unsupported topic")
    numbers, significators, vargas = TOPICS[topic]
    c.configure(options)
    chart = c.natal(birth, options)
    planets = chart["vedic"]["planets"]
    lagna = chart["vedic"]["ascendant"]["signIndex"]
    divisions = c.divisional_charts(birth, options)
    jaimini = c.jaimini(birth, options)
    av = c.ashtakavarga(birth, options)
    if any("error" in v for v in (divisions, jaimini, av)):
        raise c.EngineUnavailable("A required topic calculation failed")
    c.configure(options)
    periods = vimshottari_timeline(birth, options, as_of, 2)
    current = periods["current"]
    indicators, houses = [], []

    def add(rule, method, polarity, interpretation, values):
        indicators.append({"id": f"{topic}.{rule}", "rule": rule, "method": method, "polarity": polarity,
                           "interpretation": interpretation, "values": values})

    for h in numbers:
        sign = (lagna + h - 1) % 12
        lord = c.SIGN_LORDS[sign]
        occupants = [p for p, row in planets.items() if row["house"] == h]
        aspectors = []
        for p, row in planets.items():
            offsets = {"Mars": (3, 6, 7), "Jupiter": (4, 6, 8), "Saturn": (2, 6, 9)}.get(p, (6,))
            if p not in ("Rahu", "Ketu") and (sign - row["signIndex"]) % 12 in offsets:
                aspectors.append(p)
        values = {"number": h, "sign": c.SIGNS[sign], "lord": lord, "lordHouse": planets[lord]["house"],
                  "occupants": occupants, "aspectingPlanets": aspectors,
                  "sarvashtakavarga": int(av["sarvashtaka"][c.SIGNS[sign]])}
        houses.append(values)
        add(f"house-{h}", "D1-house-lord", "context", f"House {h} and its lord are traditionally examined for {topic}.", values)
        dignity = planets[lord]["dignity"]
        polarity = "supporting" if dignity in ("domicile", "exaltation") else "conflicting" if dignity == "debilitation" else "context"
        add(f"lord-{h}-dignity", "D1-dignity", polarity,
            f"The house {h} lord's {dignity} condition is a traditional strength indicator, not an outcome.",
            {"planet": lord, "dignity": dignity})
        if values["sarvashtakavarga"] >= 30 or values["sarvashtakavarga"] <= 24:
            add(f"sav-{h}", "Ashtakavarga", "supporting" if values["sarvashtakavarga"] >= 30 else "conflicting",
                "Relative sign-point threshold: 30+ supportive, 24 or below caution; thresholds are heuristic.",
                {"sign": values["sign"], "points": values["sarvashtakavarga"]})
    for p in significators:
        add(f"significator-{p}", "natural-significator", "context", f"{p} is a traditional {topic} significator.", planets[p])
    for label in vargas:
        data = divisions[label]
        if "error" in data:
            raise c.EngineUnavailable("Required divisional chart failed")
        add(f"varga-{label}", label, "context", f"{label} provides a separate traditional perspective; exact birth time is important.", data)
    jaimini_fields = {k: jaimini[k] for k in ("charaKarakas", "karakamsha", "upapadaLagna", "arudhaLagna")}
    add("jaimini", "Jaimini", "context", "Jaimini indicators complement, rather than confirm, other methods.", jaimini_fields)
    relevant = set(significators) | {h["lord"] for h in houses} | {p for h in houses for p in h["occupants"]}
    if current:
        linked = [p for p in current["lords"] if p in relevant]
        add("running-dasha", "Vimshottari", "supporting" if linked else "context",
            "Running period lords linked to the topic suggest symbolic emphasis, not a promised event.",
            {"period": current, "linkedLords": linked})
    transit = c.transits(birth, options, as_of)
    for p in ("Jupiter", "Saturn", "Rahu", "Ketu"):
        row = transit["planets"][p]
        if row["transitHouseFromLagna"] in numbers:
            add(f"transit-{p}", "Gochar", "supporting" if p == "Jupiter" else "conflicting",
                f"{p} in a topic house can traditionally emphasize {'opportunity' if p == 'Jupiter' else 'pressure or reassessment'}.", row)
    profection = c.annual_profections(birth, as_of, options)
    add("profection", "annual-profections", "context", "Western profections use tropical whole-sign houses independently of the Vedic houses.", profection)
    return {"topic": topic, "method": "traceable-topic-rules-v1", "houses": houses,
            "divisionalCharts": {v: divisions[v] for v in vargas}, "jaimini": jaimini_fields,
            "indicators": indicators, "supporting": [i["id"] for i in indicators if i["polarity"] == "supporting"],
            "conflicting": [i["id"] for i in indicators if i["polarity"] == "conflicting"],
            "currentDasha": current, "profection": profection,
            "uncertainty": {"probabilityCalibrated": False, "birthTimeSensitivity": "Unquantified; no rectification performed",
                            "limitations": ["Rule-based traditional interpretations; no clinical or financial predictions",
                                            "Methods share inputs and are not independent evidence", "Not an exhaustive school-specific interpretation"]},
            "disclaimer": c.DISCLAIMER}


def scenarios(birth, options, as_of, topic="general", horizon_days=365, analysis=None):
    integer(horizon_days, "horizonDays", 1, 730)
    analysis = analysis or topic_analysis(birth, options, as_of, topic)
    start, end = c.julian(as_of), c.julian(as_of + timedelta(days=horizon_days))
    if end >= c.julian(datetime(2400, 1, 1, tzinfo=timezone.utc)):
        raise c.InputError("Scenario horizon exceeds the ephemeris range")
    dasha = vimshottari_timeline(birth, options, as_of, 2)
    windows = []
    relevant = {h["lord"] for h in analysis["houses"]} | set(TOPICS[topic][1])
    for period in dasha["periods"]:
        lo = max(start, c.iso_to_jd(period["start"]))
        hi = min(end, c.iso_to_jd(period["end"]))
        if lo >= hi:
            continue
        contacts = []
        for name in ("Jupiter", "Saturn"):
            p = c.planet((lo + hi) / 2, name, options, True)
            contacts.append({"planet": name, "sampleUTC": c.iso((lo + hi) / 2), **c.longitude_details(p["longitude"])})
        windows.append({"start": c.iso(lo), "end": c.iso(hi), "periodLords": period["lords"],
                        "topicLinkedLords": [p for p in period["lords"] if p in relevant],
                        "transitSamples": contacts, "boundaryBasis": "Vimshottari Antardasha; not a predicted event date",
                        "transitResolution": "One midpoint sample per window, not an ingress or exact-aspect search"})
    alternatives = [
        {"id": "current", "label": "Current symbolic emphasis", "evidenceIds": analysis["supporting"],
         "interpretation": f"The supporting rules may frame opportunities to review {topic}; no event is guaranteed."},
        {"id": "delayed", "label": "Delays or reassessment", "evidenceIds": analysis["conflicting"],
         "interpretation": "Conflicting rules suggest an alternative emphasis on patience, constraints, or changing plans."},
        {"id": "unchanged", "label": "No notable external change", "evidenceIds": [],
         "interpretation": "Ordinary continuity remains possible regardless of the indicators; practical circumstances matter."},
    ]
    return {"topic": topic, "analysis": analysis, "scenarios": alternatives, "timingWindows": windows,
            "uncoveredWindow": None if windows else {"start": c.iso(start), "end": c.iso(end), "reason": "Outside the 120-year Dasha output span"},
            "uncertainty": analysis["uncertainty"], "disclaimer": c.DISCLAIMER}


def eclipse_contacts(birth, options, as_of, count=6, orb=3):
    integer(count, "count", 1, 20)
    c.number(orb, "eclipseOrb", 0.1, 5)
    c.configure(options)
    chart = c.natal(birth, options)["western"]
    natal_positions = {k: dict(v, speedDegreesPerDay=0) for k, v in chart["planets"].items()}
    events = []
    for kind, rows in c.eclipses(as_of, options, count)["eclipses"].items():
        for row in rows:
            name = "Sun" if kind == "solar" else "Moon"
            position = c.planet(row["julianDay"], name, options)
            contacts = c.aspects({name: position}, natal_positions, orb=orb)
            events.append({**row, "kind": kind, "longitude": position["longitude"],
                           "natalHouse": c.house_of(position["longitude"], chart["cusps"]), "contacts": contacts})
    return {"events": sorted(events, key=lambda e: e["julianDay"]), "orbDegrees": orb,
            "convention": "Tropical geocentric longitude at global eclipse maximum; natal house system as requested",
            "localVisibilityCalculated": False, "disclaimer": c.DISCLAIMER}


def jaimini_timing(birth, options, as_of):
    jc, drik = c._jhora_init(options)
    from jhora.horoscope.dhasa.raasi import drig
    place = c._jhora_place(birth, drik)
    rows = drig.get_dhasa_antardhasa(c._jhora_jd(birth), place, dhasa_method=jc.DRIG_TYPE.PVR_PAPER,
                                   dhasa_level_index=2, round_duration=False,
                                   dhasa_duration_type=jc.DHASA_YEAR_DURATION.MEAN_SIDEREAL_YEAR)
    periods = c._format_dasha_list(rows, jc, levels=2, birth=birth, sign_lords=True)
    return {"system": "Jaimini-family Drig Dasha", "variant": "PyJHora PVR_PAPER", "yearDays": float(jc.sidereal_year),
            "periods": periods, "current": active_period(periods, c.julian(as_of)),
            "scope": "Named Drig variant; Jaimini Dasha is a family of systems, not a single algorithm"}


def direction_arc(right_ascension, meridian, motion):
    if motion not in ("direct", "converse"):
        raise c.InputError("directionMotion must be direct or converse")
    return ((right_ascension - meridian) if motion == "direct" else (meridian - right_ascension)) % 360


def primary_directions(birth, options, as_of, key="naibod", motion="both", max_age=100):
    keys = {"naibod": 0.98564733, "ptolemy": 1.0}
    if key not in keys or motion not in ("direct", "converse", "both"):
        raise c.InputError("Unsupported primary-direction key or motion")
    c.number(max_age, "directionMaxAge", 1, 120)
    c.configure(options)
    chart = c.natal(birth, options)["western"]
    jd = birth["julianDay"]
    rows = []
    for name, body in c.BODIES.items():
        c.planet(jd, name, options)
        pos, flags = swe.calc_ut(jd, body, swe.FLG_SWIEPH | swe.FLG_EQUATORIAL)
        if not flags & swe.FLG_SWIEPH:
            raise c.EngineUnavailable("Primary directions require Swiss data")
        for angle, ra in (("MC", chart["armc"]), ("IC", (chart["armc"] + 180) % 360)):
            for way in (("direct", "converse") if motion == "both" else (motion,)):
                arc = direction_arc(pos[0], ra, way)
                age = arc / keys[key]
                if age <= max_age:
                    event_jd = jd + age * 365.25
                    rows.append({"promissor": name, "significator": angle, "motion": way, "arcDegrees": arc,
                                 "ageYears": age, "symbolicDate": c.iso(event_jd),
                                 "relativeToAsOf": "past" if event_jd < c.julian(as_of) else "upcoming"})
    return {"method": "mundane-meridian-primary-directions", "key": key, "degreesPerYear": keys[key],
            "scope": "Planetary conjunctions to MC/IC only; natal latitude retained, no secondary motion",
            "notIncluded": ["ASC/DSC semiarcs", "interplanetary directions", "zodiacal aspects", "rectification"],
            "directions": sorted(rows, key=lambda r: r["ageYears"]), "disclaimer": c.DISCLAIMER}


def ziwei_start(day, bureau):
    integer(day, "lunarDay", 1, 30)
    integer(bureau, "bureau", 2, 6)
    extra = (-day) % bureau
    steps = (day + extra) // bureau - 1 + (extra if extra % 2 == 0 else -extra)
    return (steps + 2) % 12


ZIWEI_TRANSFORMS = (
    ("Lian Zhen", "Po Jun", "Wu Qu", "Tai Yang"), ("Tian Ji", "Tian Liang", "Zi Wei", "Tai Yin"),
    ("Tian Tong", "Tian Ji", "Wen Chang", "Lian Zhen"), ("Tai Yin", "Tian Tong", "Tian Ji", "Ju Men"),
    ("Tan Lang", "Tai Yin", "You Bi", "Tian Ji"), ("Wu Qu", "Tan Lang", "Tian Liang", "Wen Qu"),
    ("Tai Yang", "Wu Qu", "Tai Yin", "Tian Tong"), ("Ju Men", "Tai Yang", "Wen Qu", "Wen Chang"),
    ("Tian Liang", "Zi Wei", "Zuo Fu", "Wu Qu"), ("Po Jun", "Ju Men", "Tai Yin", "Tan Lang"),
)


def zi_wei_dou_shu(birth, gender, as_of, leap_rule="split_at_15"):
    if gender not in ("male", "female"):
        raise c.InputError("Zi Wei decade direction requires explicit traditional gender: male or female")
    if leap_rule not in ("split_at_15", "repeat_month"):
        raise c.InputError("ziweiLeapRule must be split_at_15 or repeat_month")
    local = datetime.fromisoformat(birth["localDateTime"])
    lunar = Solar.fromYmdHms(local.year, local.month, local.day, local.hour, local.minute, local.second).getLunar()
    month, day, year = lunar.getMonth(), lunar.getDay(), lunar.getYear()
    adjusted_month = abs(month) + (1 if month < 0 and day > 15 and leap_rule == "split_at_15" else 0)
    hour_branch = (local.hour + 1) // 2 % 12
    ming = (2 + adjusted_month - 1 - hour_branch) % 12
    shen = (2 + adjusted_month - 1 + hour_branch) % 12
    stem = (year - 4) % 10
    tiger_stem = (2 * (stem % 5) + 2) % 10
    palace_stem = (tiger_stem + (ming - 2) % 12) % 10
    element_index = (palace_stem // 2 + 1 + (ming % 6) // 2 + 1 - 1) % 5
    element, bureau = (("Wood", 3), ("Metal", 4), ("Water", 2), ("Fire", 6), ("Earth", 5))[element_index]
    ziwei = ziwei_start(day, bureau)
    tianfu = (4 - ziwei) % 12
    groups = ((ziwei, -1, {0: "Zi Wei", 1: "Tian Ji", 3: "Tai Yang", 4: "Wu Qu", 5: "Tian Tong", 8: "Lian Zhen"}),
              (tianfu, 1, {0: "Tian Fu", 1: "Tai Yin", 2: "Tan Lang", 3: "Ju Men", 4: "Tian Xiang", 5: "Tian Liang", 6: "Qi Sha", 10: "Po Jun"}))
    palace_names = ("Life", "Parents", "Spirit", "Property", "Career", "Friends", "Travel", "Health", "Wealth", "Children", "Spouse", "Siblings")
    palaces = [{"branch": c._BRANCHES[i], "branchIndex": i, "name": palace_names[(i - ming) % 12],
                "stem": c._STEMS[(tiger_stem + (i - 2) % 12) % 10], "isBodyPalace": i == shen,
                "majorStars": [], "auxiliaryStars": []} for i in range(12)]
    locations = {}
    for base, direction, stars in groups:
        for offset, star in stars.items():
            index = (base + direction * offset) % 12
            palaces[index]["majorStars"].append(star)
            locations[star] = index
    for star, index in {"Zuo Fu": (4 + adjusted_month - 1) % 12, "You Bi": (10 - adjusted_month + 1) % 12,
                        "Wen Chang": (10 - hour_branch) % 12, "Wen Qu": (4 + hour_branch) % 12}.items():
        palaces[index]["auxiliaryStars"].append(star)
        locations[star] = index

    def transformations(stem_index):
        return [{"transformation": label, "star": star, "branch": c._BRANCHES[locations[star]]}
                for label, star in zip(("Lu", "Quan", "Ke", "Ji"), ZIWEI_TRANSFORMS[stem_index])]

    forward = (stem % 2 == 0) == (gender == "male")
    decades = [{"startNominalAge": bureau + i * 10, "endNominalAge": bureau + i * 10 + 9,
                "branch": c._BRANCHES[(ming + (i if forward else -i)) % 12]} for i in range(12)]
    analysis_local = as_of.astimezone(ZoneInfo(birth["timezone"]))
    analysis_lunar = Solar.fromYmd(analysis_local.year, analysis_local.month, analysis_local.day).getLunar()
    analysis_year = analysis_lunar.getYear()
    return {"system": "Zi Wei Dou Shu", "variant": "14-major-star natal chart with four transformations and nominal-age decades",
            "calendar": {"lunarYear": year, "lunarMonth": month, "lunarDay": day, "leapRule": leap_rule,
                         "yearBoundary": "Lunar New Year", "dayBoundary": "local civil midnight", "trueSolarCorrection": False},
            "lifePalace": c._BRANCHES[ming], "bodyPalace": c._BRANCHES[shen],
            "fiveElementBureau": {"element": element, "number": bureau}, "palaces": palaces,
            "transformations": transformations(stem), "decades": decades,
            "annual": {"lunarYear": analysis_year, "nominalAge": analysis_year - year + 1,
                       "transformations": transformations((analysis_year - 4) % 10)},
            "reference": "iztro 2.5.4 published placement rules; independent Python implementation",
            "notIncluded": ["Full minor-star catalogue", "brightness tables", "flying-star school interpretations", "monthly/daily/hourly flows"],
            "disclaimer": c.DISCLAIMER}


def bazi(birth, gender, as_of):
    if gender not in ("male", "female"):
        raise c.InputError("BaZi luck direction requires explicit traditional gender: male or female")
    local = datetime.fromisoformat(birth["localDateTime"])
    solar = Solar.fromYmdHms(local.year, local.month, local.day, local.hour, local.minute, local.second)
    eight = solar.getLunar().getEightChar()
    eight.setSect(2)
    beijing = c.instant(birth["utcDateTime"]).astimezone(timezone(timedelta(hours=8)))
    seasonal = Solar.fromYmdHms(beijing.year, beijing.month, beijing.day, beijing.hour, beijing.minute, beijing.second).getLunar().getEightChar()
    seasonal.setSect(2)
    pillars = {}
    for label in ("Year", "Month", "Day", "Time"):
        source = seasonal if label in ("Year", "Month") else eight
        pillars[label.lower()] = {"ganZhi": getattr(source, "get" + label)(), "elements": getattr(source, "get" + label + "WuXing")(),
                                 "hiddenStems": getattr(source, "get" + label + "HideGan")()}
    yun = seasonal.getYun(1 if gender == "male" else 0, 2)
    luck = [{"ganZhi": d.getGanZhi(), "startAge": d.getStartAge(), "endAge": d.getEndAge(),
             "startYear": d.getStartYear(), "endYear": d.getEndYear()} for d in yun.getDaYun(9)[1:]]
    analysis_local = as_of.astimezone(timezone(timedelta(hours=8)))
    current = Solar.fromYmdHms(analysis_local.year, analysis_local.month, analysis_local.day,
                              analysis_local.hour, analysis_local.minute, analysis_local.second).getLunar().getEightChar()
    luck_start = datetime.fromisoformat(yun.getStartSolar().toYmdHms()).replace(tzinfo=timezone(timedelta(hours=8)))
    return {"system": "BaZi", "provider": "lunar-python 1.4.8", "pillars": pillars, "luckPillars": luck,
            "luckStarts": luck_start.astimezone(timezone.utc).isoformat(), "forward": yun.isForward(), "annualPillar": current.getYear(),
            "convention": "Li Chun/Jie solar-term instants evaluated in UTC+08; local civil day/hour, midnight day rollover (sect 2), minute-based Yun (sect 2); no true-solar correction",
            "disclaimer": c.DISCLAIMER}
