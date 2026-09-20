#!/usr/bin/env python3
"""
yantra_mantra_engine.py — Sacred Yantra Sacred Geometry & Planetary Mantra Frequency Engine

Implements the classical mathematical magic square Yantras and sacred sonic remedies:
1. Authentic 3x3 Magic Square Yantras for all 9 Grahas (Surya=15 up to Ketu=39)
2. Rigorous mathematical sum verification (Rows, Columns, Diagonals)
3. Beeja Mantras, Gayatri Mantras, Japa Counts & Mala Beads
4. Vector SVG Yantra Generation (Golden-etched geometric design with Bhupura)
"""

YANTRA_DATA = {
    "SURYA": {
        "graha": "Surya (Sun)",
        "magicSum": 15,
        "grid": [
            [6, 1, 8],
            [7, 5, 3],
            [2, 9, 4]
        ],
        "beejaMantra": "Om Hram Hreem Hroum Sah Suryaya Namah",
        "gayatriMantra": "Om Bhaskaraya Vidmahe Divakaraya Dheemahi Tanno Suryah Prachodayat",
        "japaCount": 7000,
        "metal": "Copper / Gold",
        "mala": "Ruby / Red Sandalwood",
        "direction": "East",
        "auspiciousDay": "Sunday Morning (Sunrise)",
        "color": "Ruby Red / Golden Orange",
        "deity": "Lord Shiva / Lord Rama"
    },
    "CHANDRA": {
        "graha": "Chandra (Moon)",
        "magicSum": 18,
        "grid": [
            [7, 2, 9],
            [8, 6, 4],
            [3, 10, 5]
        ],
        "beejaMantra": "Om Shram Shreem Shroum Sah Chandraya Namah",
        "gayatriMantra": "Om Ksheeraputraya Vidmahe Amritatattwaya Dheemahi Tanno Chandrah Prachodayat",
        "japaCount": 11000,
        "metal": "Silver",
        "mala": "Pearl / Sphatik",
        "direction": "North-West",
        "auspiciousDay": "Monday Evening",
        "color": "Milky White / Silver",
        "deity": "Goddess Parvati / Lord Krishna"
    },
    "MANGAL": {
        "graha": "Mangal (Mars)",
        "magicSum": 21,
        "grid": [
            [8, 3, 10],
            [9, 7, 5],
            [4, 11, 6]
        ],
        "beejaMantra": "Om Kram Kreem Kroum Sah Bhaumaya Namah",
        "gayatriMantra": "Om Angarakaya Vidmahe Shaktihastaya Dheemahi Tanno Bhaumah Prachodayat",
        "japaCount": 10000,
        "metal": "Copper / Brass",
        "mala": "Red Coral / Carnelian",
        "direction": "South",
        "auspiciousDay": "Tuesday Noon",
        "color": "Coral Red / Crimson",
        "deity": "Lord Kartikeya / Lord Hanuman"
    },
    "BUDHA": {
        "graha": "Budha (Mercury)",
        "magicSum": 24,
        "grid": [
            [9, 4, 11],
            [10, 8, 6],
            [5, 12, 7]
        ],
        "beejaMantra": "Om Bram Breem Broum Sah Budhaya Namah",
        "gayatriMantra": "Om Saumyaroopaya Vidmahe Vaneshaya Dheemahi Tanno Budhah Prachodayat",
        "japaCount": 9000,
        "metal": "Bronze / Gold",
        "mala": "Emerald / Green Jade",
        "direction": "North",
        "auspiciousDay": "Wednesday Morning",
        "color": "Emerald Green",
        "deity": "Lord Maha Vishnu / Narayana"
    },
    "GURU": {
        "graha": "Guru (Jupiter)",
        "magicSum": 27,
        "grid": [
            [10, 5, 12],
            [11, 9, 7],
            [6, 13, 8]
        ],
        "beejaMantra": "Om Gram Greem Groum Sah Gurave Namah",
        "gayatriMantra": "Om Vrishabhadhvajaya Vidmahe Grani Hastaya Dheemahi Tanno Guruh Prachodayat",
        "japaCount": 19000,
        "metal": "Gold / Brass",
        "mala": "Yellow Sapphire / Tulsi",
        "direction": "North-East (Ishanya)",
        "auspiciousDay": "Thursday Dawn",
        "color": "Vedic Yellow / Saffron",
        "deity": "Lord Samba Sadashiva / Dakshinamurthy"
    },
    "SHUKRA": {
        "graha": "Shukra (Venus)",
        "magicSum": 30,
        "grid": [
            [11, 6, 13],
            [12, 10, 8],
            [7, 14, 9]
        ],
        "beejaMantra": "Om Dram Dreem Droum Sah Shukraya Namah",
        "gayatriMantra": "Om Bhrigujaya Vidmahe Divyadehaya Dheemahi Tanno Shukrah Prachodayat",
        "japaCount": 16000,
        "metal": "Silver / White Gold",
        "mala": "Diamond / Clear Quartz (Sphatik)",
        "direction": "South-East (Agneya)",
        "auspiciousDay": "Friday Morning",
        "color": "Diamond White / Pastel Pink",
        "deity": "Goddess Maha Lakshmi"
    },
    "SHANI": {
        "graha": "Shani (Saturn)",
        "magicSum": 33,
        "grid": [
            [12, 7, 14],
            [13, 11, 9],
            [8, 15, 10]
        ],
        "beejaMantra": "Om Pram Preem Proum Sah Shanaischaraya Namah",
        "gayatriMantra": "Om Kakadhvajaya Vidmahe Khadgahastaya Dheemahi Tanno Mandah Prachodayat",
        "japaCount": 23000,
        "metal": "Iron / Lead",
        "mala": "Blue Sapphire / Amethyst / Rudraksha",
        "direction": "West",
        "auspiciousDay": "Saturday Sunset",
        "color": "Midnight Blue / Charcoal Black",
        "deity": "Lord Bhairava / Lord Kurma"
    },
    "RAHU": {
        "graha": "Rahu (North Node)",
        "magicSum": 36,
        "grid": [
            [13, 8, 15],
            [14, 12, 10],
            [9, 16, 11]
        ],
        "beejaMantra": "Om Bhram Bhreem Bhroum Sah Rahave Namah",
        "gayatriMantra": "Om Shirooopaya Vidmahe Dwandwadehaya Dheemahi Tanno Rahuh Prachodayat",
        "japaCount": 18000,
        "metal": "Mixed Alloy (Ashtadhatu)",
        "mala": "Hessonite Garnet (Gomed) / Sandalwood",
        "direction": "South-West (Nairritya)",
        "auspiciousDay": "Saturday Night",
        "color": "Smoky Grey / Electric Blue",
        "deity": "Goddess Durga / Lord Varaha"
    },
    "KETU": {
        "graha": "Ketu (South Node)",
        "magicSum": 39,
        "grid": [
            [14, 9, 16],
            [15, 13, 11],
            [10, 17, 12]
        ],
        "beejaMantra": "Om Sram Sreem Sroum Sah Ketave Namah",
        "gayatriMantra": "Om Chitravarnaya Vidmahe Sarparoopaya Dheemahi Tanno Ketuh Prachodayat",
        "japaCount": 17000,
        "metal": "Ashtadhatu / Panchadhatu",
        "mala": "Cat's Eye (Vaidurya) / Rudraksha",
        "direction": "North-East Center (Transcendent)",
        "auspiciousDay": "Tuesday Twilight",
        "color": "Multi-colored / Earth Brown",
        "deity": "Lord Ganesha / Lord Matsya"
    }
}

def generate_svg_yantra(planet_key, info):
    grid = info["grid"]
    svg_cells = ""
    start_x = 90
    start_y = 90
    cell_size = 73

    for r in range(3):
        for c in range(3):
            x = start_x + (c * cell_size)
            y = start_y + (r * cell_size)
            val = grid[r][c]
            svg_cells += f"""
            <rect x="{x}" y="{y}" width="{cell_size}" height="{cell_size}" fill="rgba(245,158,11,0.06)" stroke="#f59e0b" stroke-width="1.5" />
            <text x="{x + cell_size/2}" y="{y + cell_size/2 + 7}" fill="#f59e0b" font-family="'Cinzel', serif" font-size="22" font-weight="700" text-anchor="middle">{val}</text>
            """

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400" width="100%" height="100%" style="background:#090d16; border-radius:12px;">
      <defs>
        <radialGradient id="ygold" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="#f59e0b" stop-opacity="0.25"/>
          <stop offset="100%" stop-color="#000000" stop-opacity="0"/>
        </radialGradient>
      </defs>
      <!-- Aura -->
      <circle cx="200" cy="200" r="180" fill="url(#ygold)" />
      
      <!-- Bhupura Gates (Outer Temple Enclosure) -->
      <rect x="35" y="35" width="330" height="330" fill="none" stroke="#f59e0b" stroke-width="2.5" />
      <rect x="50" y="50" width="300" height="300" fill="none" stroke="#d97706" stroke-width="1.5" opacity="0.7"/>
      
      <!-- Outer Circle & Lotus Petals Accent -->
      <circle cx="200" cy="200" r="135" fill="none" stroke="#f59e0b" stroke-width="1.5" stroke-dasharray="6,4"/>
      
      <!-- Magic Square Grid -->
      <g>
        {svg_cells}
      </g>
      
      <!-- Header Title -->
      <text x="200" y="28" fill="#f59e0b" font-family="'Cinzel', serif" font-size="14" font-weight="700" letter-spacing="2" text-anchor="middle">श्री {info['graha'].upper()} यन्त्रम्</text>
      <text x="200" y="385" fill="#38bdf8" font-family="'Plus Jakarta Sans', sans-serif" font-size="11" text-anchor="middle">गणितीय योग (Magic Sum): {info['magicSum']}</text>
    </svg>"""
    return svg

def generate_yantra_mantra(planet_str="SURYA"):
    key = planet_str.upper().strip()
    if key not in YANTRA_DATA:
        key = "SURYA"

    info = YANTRA_DATA[key]
    svg = generate_svg_yantra(key, info)

    # Verify rows, cols, diagonals
    grid = info["grid"]
    row_sums = [sum(row) for row in grid]
    col_sums = [sum(grid[r][c] for r in range(3)) for c in range(3)]
    diag1 = grid[0][0] + grid[1][1] + grid[2][2]
    diag2 = grid[0][2] + grid[1][1] + grid[2][0]

    verified = all(s == info["magicSum"] for s in row_sums + col_sums + [diag1, diag2])

    return {
        "planet": info["graha"],
        "sacredMagicSum": info["magicSum"],
        "mathematicallyVerified": verified,
        "gridMatrix": grid,
        "rowSums": row_sums,
        "columnSums": col_sums,
        "diagonalSums": [diag1, diag2],
        "beejaMantra": info["beejaMantra"],
        "gayatriMantra": info["gayatriMantra"],
        "recommendedJapaCount": info["japaCount"],
        "sacredMetal": info["metal"],
        "rosaryMala": info["mala"],
        "facingDirection": info["direction"],
        "auspiciousMuhurta": info["auspiciousDay"],
        "cosmicColor": info["color"],
        "tutelaryDeity": info["deity"],
        "yantraSvg": svg,
        "summary": f"The authentic 3x3 {info['graha']} Yantra is perfectly balanced with every vector adding to {info['magicSum']}. Chant {info['beejaMantra']} for {info['japaCount']:,} repetitions facing {info['direction']} for maximum spiritual elevation."
    }

if __name__ == "__main__":
    import json
    res = generate_yantra_mantra("SHANI")
    print(json.dumps(res, indent=2))
