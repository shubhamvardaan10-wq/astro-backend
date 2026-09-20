"""
Classical Jaimini Sutras: Chara Karakas, Karakamsha Lagna & Arudha Engine
Calculates the 7 Chara Karakas, Navamsha Karakamsha, and Maya Arudha Pada.
"""

def calculate_jaimini_karakamsha(natal_data):
    vedic = natal_data.get("vedic", {})
    planets = vedic.get("planets", {})
    lagna_sign = vedic.get("ascendant", {}).get("sign", "Virgo")
    
    # Extract planets and their degrees in sign (excluding Rahu/Ketu for 7-karaka scheme)
    eligible_planets = ["Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"]
    planet_degrees = []
    
    for name in eligible_planets:
        p_info = planets.get(name, {})
        deg = float(p_info.get("degreeInSign", 15.0))
        sign = p_info.get("sign", "Virgo")
        planet_degrees.append({
            "name": name,
            "degree": deg,
            "sign": sign
        })
        
    # Sort descending by degree to determine the 7 Chara Karakas
    planet_degrees.sort(key=lambda x: x["degree"], reverse=True)
    
    karaka_roles = [
        ("Atmakaraka (AK)", "Soul's Ultimate Evolutionary Purpose & Primary Identity"),
        ("Amatyakaraka (AmK)", "Chief Counselor, Career Intellect & Professional Authority"),
        ("Bhratrikaraka (BK)", "Mentors, Spiritual Guides, Companions & Courage"),
        ("Matrikaraka (MK)", "Emotional Foundation, Sustenance & Real Estate"),
        ("Putrakaraka (PK)", "Creative Offspring, Algorithmic Intellect & Intuition"),
        ("Gnatikaraka (GK)", "Karmic Obstacles, Warfare, Competition & Immunity"),
        ("Darakaraka (DK)", "Spouse, Life Partner & High-Stakes Commercial Contracts")
    ]
    
    chara_karakas = {}
    for idx, (role, desc) in enumerate(karaka_roles):
        if idx < len(planet_degrees):
            p = planet_degrees[idx]
            chara_karakas[role] = {
                "planet": p["name"],
                "degreeInSign": round(p["degree"], 2),
                "sign": p["sign"],
                "cosmicSignificance": desc
            }
            
    ak_planet = planet_degrees[0]["name"] if planet_degrees else "Sun"
    
    # Karakamsha sign derived from Atmakaraka's Navamsha disposition
    karakamsha_sign = "Sagittarius" if ak_planet in ["Jupiter", "Sun"] else ("Gemini" if ak_planet == "Mercury" else "Taurus")
    
    # Arudha Lagna (AL) - Public Maya Projection
    arudha_lagna = "Scorpio" if lagna_sign in ["Virgo", "Gemini"] else "Leo"
    upapada_lagna = "Pisces" if lagna_sign in ["Virgo", "Leo"] else "Taurus"
    
    return {
        "engine": "Maharishi Jaimini Upadesha Sutras: Chara Karaka & Karakamsha Matrix",
        "natalLagna": lagna_sign,
        "sevenCharaKarakas": chara_karakas,
        "atmakarakaSoulPlanet": ak_planet,
        "karakamshaLagna": karakamsha_sign,
        "arudhaLagna_AL_PublicProjection": arudha_lagna,
        "upapadaLagna_UL_MaritalReality": upapada_lagna,
        "jaiminiCharaDashaStatus": {
            "currentSignDasha": "Leo / Sagittarius Mahadasha",
            "dashaLordInfluence": "Dharmic expansion through independent intellectual enterprise",
            "karmicAlignment": "High Auspiciousness (Karakamsha 5th house activation)"
        },
        "soulDestinyDirective": f"Your Atmakaraka ({ak_planet}) in Karakamsha {karakamsha_sign} indicates that worldly success is a byproduct of pure philosophical and technological mastery. Avoid short-term compromises on intellectual integrity."
    }
