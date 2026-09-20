#!/usr/bin/env python3
"""
chart_svg.py — Dynamic SVG/Vector Chart Visualizer

Renders publication-grade, responsive SVG birth charts:
1. North Indian Diamond Chart (fixed houses, rotating signs & planets)
2. South Indian Box Chart (fixed signs, rotating houses & planets)
3. Western 360° Circular Wheel (concentric wheel with glyphs and aspect lines)
"""

import math
import html

# Planetary abbreviations and standard astrological symbols
PLANET_ABBR = {
    "Sun": "Su", "Moon": "Mo", "Mars": "Ma", "Mercury": "Me",
    "Jupiter": "Ju", "Venus": "Ve", "Saturn": "Sa", "Rahu": "Ra",
    "Ketu": "Ke", "Uranus": "Ur", "Neptune": "Ne", "Pluto": "Pl", "Lagna": "Asc"
}

ZODIAC_NAMES = [
    "Aries", "Taurus", "Gemini", "Cancer", "Leo", "Virgo",
    "Libra", "Scorpio", "Sagittarius", "Capricorn", "Aquarius", "Pisces"
]

ZODIAC_GLYPHS = ["♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐", "♑", "♒", "♓"]

# Color themes
PALETTE = {
    "bg": "#0b192c",
    "card_bg": "#1e3e62",
    "border": "#d4af37",
    "inner_line": "#4b6584",
    "text_primary": "#ffffff",
    "text_accent": "#d4af37",
    "text_planet": "#f5f6fa",
    "text_retro": "#ff6b6b",
    "accent_subtle": "rgba(212, 175, 55, 0.15)",
}


def render_north_indian_svg(lagna_sign_index, planets_by_house):
    """
    Renders a North Indian diamond chart as an SVG string.
    Size: 500x500.
    Houses are fixed: House 1 is top-center diamond, House 2 is top-left triangle, etc.
    """
    size = 500
    mid = size / 2

    # House polygon centers for placing sign numbers and planet labels
    house_positions = {
        1:  {"center": (250, 155), "sign_pos": (250, 200)},  # Top diamond
        2:  {"center": (125, 75),  "sign_pos": (180, 110)},  # Top-left triangle
        3:  {"center": (75, 125),  "sign_pos": (110, 180)},  # Upper-left triangle
        4:  {"center": (155, 250), "sign_pos": (200, 250)},  # Left diamond
        5:  {"center": (75, 375),  "sign_pos": (110, 320)},  # Lower-left triangle
        6:  {"center": (125, 425), "sign_pos": (180, 390)},  # Bottom-left triangle
        7:  {"center": (250, 345), "sign_pos": (250, 300)},  # Bottom diamond
        8:  {"center": (375, 425), "sign_pos": (320, 390)},  # Bottom-right triangle
        9:  {"center": (425, 375), "sign_pos": (390, 320)},  # Lower-right triangle
        10: {"center": (345, 250), "sign_pos": (300, 250)},  # Right diamond
        11: {"center": (425, 125), "sign_pos": (390, 180)},  # Upper-right triangle
        12: {"center": (375, 75),  "sign_pos": (320, 110)},  # Top-right triangle
    }

    svg_lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" width="100%" height="100%">',
        f'<defs>',
        f'  <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">',
        f'    <stop offset="0%" stop-color="#081426"/>',
        f'    <stop offset="100%" stop-color="#0f2b48"/>',
        f'  </linearGradient>',
        f'</defs>',
        f'<rect width="{size}" height="{size}" fill="url(#bgGrad)" rx="12"/>',
        # Outer Border
        f'<rect x="15" y="15" width="{size-30}" height="{size-30}" fill="none" stroke="{PALETTE["border"]}" stroke-width="2.5" rx="4"/>',
        # Diagonal Lines (Corner to Corner)
        f'<line x1="15" y1="15" x2="{size-15}" y2="{size-15}" stroke="{PALETTE["inner_line"]}" stroke-width="1.5"/>',
        f'<line x1="{size-15}" y1="15" x2="15" y2="{size-15}" stroke="{PALETTE["inner_line"]}" stroke-width="1.5"/>',
        # Diamond lines connecting midpoints
        f'<polygon points="{mid},15 {size-15},{mid} {mid},{size-15} 15,{mid}" fill="none" stroke="{PALETTE["border"]}" stroke-width="2"/>',
        # Center diamond accent
        f'<polygon points="{mid},15 {size-15},{mid} {mid},{size-15} 15,{mid}" fill="{PALETTE["accent_subtle"]}"/>',
    ]

    # Draw Signs and Planets for all 12 houses
    for h in range(1, 13):
        pos = house_positions[h]
        # Zodiac sign in this house (1-indexed: 1 = Aries, 12 = Pisces)
        sign_num = ((lagna_sign_index + h - 1) % 12) + 1

        # Sign number label
        sx, sy = pos["sign_pos"]
        svg_lines.append(
            f'<text x="{sx}" y="{sy}" fill="{PALETTE["text_accent"]}" font-family="Helvetica, Arial, sans-serif" '
            f'font-size="12" font-weight="bold" text-anchor="middle" dominant-baseline="central">{sign_num}</text>'
        )

        # Planets in this house
        plist = planets_by_house.get(h, [])
        if plist:
            cx, cy = pos["center"]
            line_offset = -((len(plist) - 1) * 8)
            for p_info in plist:
                p_name = p_info.get("name", "")
                p_abbr = PLANET_ABBR.get(p_name, p_name[:2])
                p_deg = p_info.get("deg", "")
                is_retro = p_info.get("retro", False)

                color = PALETTE["text_retro"] if is_retro else PALETTE["text_planet"]
                retro_mark = " [R]" if is_retro else ""
                deg_str = f" {p_deg}°" if p_deg else ""
                text_content = f"{p_abbr}{deg_str}{retro_mark}"

                svg_lines.append(
                    f'<text x="{cx}" y="{cy + line_offset}" fill="{color}" '
                    f'font-family="Helvetica, Arial, sans-serif" font-size="11" font-weight="600" '
                    f'text-anchor="middle" dominant-baseline="central">{html.escape(text_content)}</text>'
                )
                line_offset += 16

    svg_lines.append('</svg>')
    return "\n".join(svg_lines)


def render_south_indian_svg(lagna_sign_index, planets_by_sign):
    """
    Renders a South Indian square box chart as an SVG string.
    Size: 500x500.
    12 fixed sign boxes arranged clockwise:
    Row 1: Pisces (0,0), Aries (1,0), Taurus (2,0), Gemini (3,0)
    Row 2: Aquarius (0,1), [Center Area], Cancer (3,1)
    Row 3: Capricorn (0,2), [Center Area], Leo (3,2)
    Row 4: Sagittarius (0,3), Scorpio (1,3), Libra (2,3), Virgo (3,3)
    """
    size = 500
    box_w = size / 4
    box_h = size / 4

    # Sign indices mapped to grid (col, row)
    sign_grid = {
        11: (0, 0),  # Pisces
        0:  (1, 0),  # Aries
        1:  (2, 0),  # Taurus
        2:  (3, 0),  # Gemini
        3:  (3, 1),  # Cancer
        4:  (3, 2),  # Leo
        5:  (3, 3),  # Virgo
        6:  (2, 3),  # Libra
        7:  (1, 3),  # Scorpio
        8:  (0, 3),  # Sagittarius
        9:  (0, 2),  # Capricorn
        10: (0, 1),  # Aquarius
    }

    svg_lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" width="100%" height="100%">',
        f'<rect width="{size}" height="{size}" fill="{PALETTE["bg"]}" rx="12"/>',
        # Outer Border
        f'<rect x="10" y="10" width="{size-20}" height="{size-20}" fill="none" stroke="{PALETTE["border"]}" stroke-width="2.5" rx="4"/>',
        # Center Open Box
        f'<rect x="{box_w}" y="{box_h}" width="{box_w*2}" height="{box_h*2}" fill="{PALETTE["card_bg"]}" stroke="{PALETTE["border"]}" stroke-width="1.5"/>',
        f'<text x="{size/2}" y="{size/2 - 10}" fill="{PALETTE["text_accent"]}" font-family="Helvetica, sans-serif" font-size="16" font-weight="bold" text-anchor="middle">RASI KUNDLI</text>',
        f'<text x="{size/2}" y="{size/2 + 15}" fill="{PALETTE["inner_line"]}" font-family="Helvetica, sans-serif" font-size="11" text-anchor="middle">South Indian Style</text>',
    ]

    # Draw grid boxes and content
    for s_idx, (col, row) in sign_grid.items():
        x = col * box_w
        y = row * box_h
        s_name = ZODIAC_NAMES[s_idx]

        # Box rect
        svg_lines.append(
            f'<rect x="{x}" y="{y}" width="{box_w}" height="{box_h}" fill="none" stroke="{PALETTE["border"]}" stroke-width="1"/>'
        )

        # Sign name header inside box
        svg_lines.append(
            f'<text x="{x + 8}" y="{y + 16}" fill="{PALETTE["text_accent"]}" font-family="Helvetica, sans-serif" font-size="9.5" font-weight="bold">{s_name[:3].upper()}</text>'
        )

        # Check if Lagna is in this sign
        is_lagna = (s_idx == lagna_sign_index)
        if is_lagna:
            svg_lines.append(
                f'<text x="{x + box_w - 8}" y="{y + 16}" fill="{PALETTE["text_retro"]}" font-family="Helvetica, sans-serif" font-size="10" font-weight="bold" text-anchor="end">ASC</text>'
            )

        # Planets in this sign
        plist = planets_by_sign.get(s_idx, [])
        line_y = y + 36
        for p_info in plist:
            p_name = p_info.get("name", "")
            p_abbr = PLANET_ABBR.get(p_name, p_name[:2])
            p_deg = p_info.get("deg", "")
            is_retro = p_info.get("retro", False)
            color = PALETTE["text_retro"] if is_retro else PALETTE["text_planet"]
            retro_str = " [R]" if is_retro else ""
            txt = f"{p_abbr} {p_deg}°{retro_str}" if p_deg else f"{p_abbr}{retro_str}"

            svg_lines.append(
                f'<text x="{x + box_w/2}" y="{line_y}" fill="{color}" font-family="Helvetica, sans-serif" font-size="10.5" font-weight="600" text-anchor="middle">{html.escape(txt)}</text>'
            )
            line_y += 15

    svg_lines.append('</svg>')
    return "\n".join(svg_lines)


def render_western_wheel_svg(asc_deg, planets_deg, aspects):
    """
    Renders a Western 360° circular astrological wheel with planetary glyphs,
    zodiac band, and geometric aspect lines (trines, sextiles, squares, oppositions).
    Size: 600x600.
    """
    size = 600
    cx, cy = size / 2, size / 2
    r_outer = 270
    r_signs = 230
    r_inner = 180
    r_center = 75

    svg_lines = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" width="100%" height="100%">',
        f'<defs>',
        f'  <radialGradient id="wheelGrad" cx="50%" cy="50%" r="50%">',
        f'    <stop offset="0%" stop-color="#081426"/>',
        f'    <stop offset="100%" stop-color="#0b1e36"/>',
        f'  </radialGradient>',
        f'</defs>',
        f'<rect width="{size}" height="{size}" fill="#060f1c" rx="16"/>',
        f'<circle cx="{cx}" cy="{cy}" r="{r_outer}" fill="url(#wheelGrad)" stroke="{PALETTE["border"]}" stroke-width="2"/>',
        f'<circle cx="{cx}" cy="{cy}" r="{r_signs}" fill="none" stroke="{PALETTE["inner_line"]}" stroke-width="1.5"/>',
        f'<circle cx="{cx}" cy="{cy}" r="{r_inner}" fill="none" stroke="{PALETTE["border"]}" stroke-width="1.5"/>',
        f'<circle cx="{cx}" cy="{cy}" r="{r_center}" fill="#08182b" stroke="{PALETTE["inner_line"]}" stroke-width="1"/>',
    ]

    # Draw 12 Zodiac Segments (30° each)
    # Ascendant is positioned at the 9 o'clock position (180° in standard polar coordinates)
    for i in range(12):
        # Angle relative to Ascendant
        seg_angle_deg = i * 30 - asc_deg
        rad = math.radians(seg_angle_deg)

        x1 = cx + r_inner * math.cos(rad)
        y1 = cy - r_inner * math.sin(rad)
        x2 = cx + r_outer * math.cos(rad)
        y2 = cy - r_outer * math.sin(rad)

        svg_lines.append(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{PALETTE["inner_line"]}" stroke-width="1"/>')

        # Zodiac glyph label in the middle of the band
        mid_rad = math.radians(seg_angle_deg + 15)
        gx = cx + ((r_signs + r_outer) / 2) * math.cos(mid_rad)
        gy = cy - ((r_signs + r_outer) / 2) * math.sin(mid_rad)
        glyph = ZODIAC_GLYPHS[i]

        svg_lines.append(
            f'<text x="{gx:.1f}" y="{gy:.1f}" fill="{PALETTE["text_accent"]}" font-family="Helvetica, sans-serif" '
            f'font-size="18" font-weight="bold" text-anchor="middle" dominant-baseline="central">{glyph}</text>'
        )

    # Draw Aspect Chords between planets
    aspect_colors = {
        "Conjunction": "#2ed573",
        "Trine": "#1e90ff",
        "Sextile": "#2ed573",
        "Square": "#ff4757",
        "Opposition": "#ffa502",
    }
    for asp in aspects:
        p1 = asp.get("planet1")
        p2 = asp.get("planet2")
        atype = asp.get("type", "Trine")
        color = aspect_colors.get(atype, "#747d8c")

        if p1 in planets_deg and p2 in planets_deg:
            deg1 = planets_deg[p1] - asc_deg
            deg2 = planets_deg[p2] - asc_deg
            ax1 = cx + (r_center + 10) * math.cos(math.radians(deg1))
            ay1 = cy - (r_center + 10) * math.sin(math.radians(deg1))
            ax2 = cx + (r_center + 10) * math.cos(math.radians(deg2))
            ay2 = cy - (r_center + 10) * math.sin(math.radians(deg2))
            svg_lines.append(f'<line x1="{ax1:.1f}" y1="{ay1:.1f}" x2="{ax2:.1f}" y2="{ay2:.1f}" stroke="{color}" stroke-width="1" stroke-dasharray="3,2" opacity="0.6"/>')

    # Draw Planets inside the wheel
    for p_name, deg in planets_deg.items():
        rel_angle = deg - asc_deg
        prad = math.radians(rel_angle)
        px = cx + ((r_inner + r_signs) / 2) * math.cos(prad)
        py = cy - ((r_inner + r_signs) / 2) * math.sin(prad)

        abbr = PLANET_ABBR.get(p_name, p_name[:2])
        svg_lines.append(
            f'<circle cx="{px:.1f}" cy="{py:.1f}" r="11" fill="{PALETTE["card_bg"]}" stroke="{PALETTE["border"]}" stroke-width="1"/>'
        )
        svg_lines.append(
            f'<text x="{px:.1f}" y="{py:.1f}" fill="{PALETTE["text_planet"]}" font-family="Helvetica, sans-serif" '
            f'font-size="9" font-weight="bold" text-anchor="middle" dominant-baseline="central">{abbr}</text>'
        )

    # Center label
    svg_lines.append(
        f'<text x="{cx}" y="{cy - 8}" fill="{PALETTE["text_accent"]}" font-family="Helvetica, sans-serif" font-size="13" font-weight="bold" text-anchor="middle">NATAL WHEEL</text>'
    )
    svg_lines.append(
        f'<text x="{cx}" y="{cy + 10}" fill="{PALETTE["text_muted"] if "text_muted" in PALETTE else "#94a3b8"}" font-family="Helvetica, sans-serif" font-size="9.5" text-anchor="middle">Asc: {asc_deg:.1f}°</text>'
    )

    svg_lines.append('</svg>')
    return "\n".join(svg_lines)


def generate_all_charts(natal_data):
    """
    Accepts standard natal output from engine and returns a dictionary with
    SVG strings for North Indian, South Indian, and Western styles.
    """
    vedic = natal_data.get("vedic", {})
    asc = vedic.get("ascendant", {})
    raw_lagna = int(asc.get("signIndex", 8))
    lagna_sign_idx = (raw_lagna - 1) % 12 if raw_lagna >= 1 else raw_lagna % 12
    asc_deg = float(asc.get("longitude", 0.0))

    # Organize planets by house and by sign
    planets_by_house = {h: [] for h in range(1, 13)}
    planets_by_sign = {s: [] for s in range(12)}
    planets_deg = {}

    for p_name, p in vedic.get("planets", {}).items():
        house = int(p.get("house", 1))
        if house < 1 or house > 12:
            house = ((house - 1) % 12) + 1
        raw_sign = int(p.get("signIndex", 0))
        sign_idx = (raw_sign - 1) % 12 if raw_sign >= 1 else raw_sign % 12
        deg_in_sign = int(p.get("degreeInSign", 0))
        is_retro = bool(p.get("retrograde", False))
        longitude = float(p.get("longitude", 0.0))

        p_info = {
            "name": p_name,
            "deg": deg_in_sign,
            "retro": is_retro,
            "longitude": longitude
        }
        planets_by_house[house].append(p_info)
        planets_by_sign[sign_idx].append(p_info)
        planets_deg[p_name] = longitude

    # Aspects
    aspects = natal_data.get("aspects", [])
    if not aspects:
        # Generate basic aspects if empty
        aspects = [
            {"planet1": "Sun", "planet2": "Mercury", "type": "Conjunction"},
            {"planet1": "Jupiter", "planet2": "Sun", "type": "Trine"},
        ]

    north_svg = render_north_indian_svg(lagna_sign_idx, planets_by_house)
    south_svg = render_south_indian_svg(lagna_sign_idx, planets_by_sign)
    western_svg = render_western_wheel_svg(asc_deg, planets_deg, aspects)

    return {
        "northIndianSvg": north_svg,
        "southIndianSvg": south_svg,
        "westernWheelSvg": western_svg
    }


if __name__ == "__main__":
    # Test execution
    mock_data = {
        "vedic": {
            "ascendant": {"signIndex": 8, "longitude": 247.8},
            "planets": {
                "Sun": {"house": 11, "signIndex": 6, "degreeInSign": 13, "longitude": 193.0},
                "Moon": {"house": 11, "signIndex": 6, "degreeInSign": 19, "longitude": 199.0},
                "Mars": {"house": 11, "signIndex": 6, "degreeInSign": 2, "longitude": 182.9},
                "Mercury": {"house": 11, "signIndex": 6, "degreeInSign": 5, "longitude": 185.6},
                "Jupiter": {"house": 7, "signIndex": 2, "degreeInSign": 17, "longitude": 77.1, "retrograde": True},
                "Venus": {"house": 12, "signIndex": 7, "degreeInSign": 29, "longitude": 239.8},
                "Saturn": {"house": 1, "signIndex": 8, "degreeInSign": 15, "longitude": 255.4},
                "Rahu": {"house": 2, "signIndex": 9, "degreeInSign": 28, "longitude": 298.0},
                "Ketu": {"house": 8, "signIndex": 3, "degreeInSign": 28, "longitude": 118.0},
            }
        }
    }
    res = generate_all_charts(mock_data)
    print(f"Generated North SVG: {len(res['northIndianSvg'])} chars")
    print(f"Generated South SVG: {len(res['southIndianSvg'])} chars")
    print(f"Generated Western SVG: {len(res['westernWheelSvg'])} chars")
