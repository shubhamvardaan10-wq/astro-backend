"""
Astro-Genealogy & Reincarnation Soul-Tie Network Engine
Constructs a multi-person directed karmic graph revealing past-life soul contracts,
Rinanu-Bandhan debts, and collaborative destiny alignment.
"""

def build_soul_graph(profiles_data):
    profiles = profiles_data.get("profiles", [])
    if not profiles or len(profiles) < 2:
        # Default 2-person executive/soulmate pair if empty
        profiles = [
            {"id": "p1", "name": "Seeker Alpha", "dob": "1990-05-15", "role": "Venture Founder / CEO"},
            {"id": "p2", "name": "Collaborator Beta", "dob": "1992-08-20", "role": "Chief Architect / CTO"}
        ]
        
    nodes = []
    links = []
    
    for idx, p in enumerate(profiles):
        p_id = p.get("id", f"p_{idx}")
        name = p.get("name", f"Native {idx+1}")
        role = p.get("role", "Executive Partner")
        
        # Determine archetype
        if idx % 3 == 0:
            ak = "Sun (Atmakaraka - Sovereign Visionary)"
            element = "Fire (Dharma)"
            lagna = "Leo"
        elif idx % 3 == 1:
            ak = "Mercury (Atmakaraka - Strategic Architect)"
            element = "Air (Kama/Intellect)"
            lagna = "Virgo"
        else:
            ak = "Jupiter (Atmakaraka - Dharmic Counselor)"
            element = "Earth (Artha)"
            lagna = "Taurus"
            
        nodes.append({
            "id": p_id,
            "name": name,
            "role": role,
            "lagna": lagna,
            "atmakaraka": ak,
            "elementalDominance": element,
            "soulMaturityLevel": "ADVANCED_DHARMIC"
        })
        
    # Build pairwise karmic links
    for i in range(len(nodes)):
        for j in range(i + 1, len(nodes)):
            n1 = nodes[i]
            n2 = nodes[j]
            
            # Link attributes
            if (i + j) % 2 == 0:
                rel_type = "PAST_LIFE_DHARMIC_ALLIANCE"
                strength = 0.94
                directive = f"{n1['name']} and {n2['name']} share deep Purva Punya (ancestral merit). In this birth, their union accelerates technological innovation and collective wealth."
            else:
                rel_type = "RINANU_BANDHAN_SOUL_DEBT_SETTLEMENT"
                strength = 0.88
                directive = f"A past-life financial and intellectual contract requires {n1['name']} to mentor {n2['name']}, dissolving ancient karmic obligations into shared equity."
                
            links.append({
                "source": n1["id"],
                "target": n2["id"],
                "relationshipType": rel_type,
                "karmicBondStrength": strength,
                "sharedPurvaPunya": "High (Complimentary Kendra-Trikona axis)",
                "karmicDirective": directive
            })
            
    return {
        "engine": "Astro-Genealogy & Reincarnation Soul-Tie Network",
        "totalEntitiesAnalyzed": len(nodes),
        "collectiveHarmonyIndex": 91.4,
        "nodes": nodes,
        "links": links,
        "collectiveKarmicMission": "The group is assembled to construct sovereign decentralized knowledge institutions (*Yantras of Knowledge*). Prioritize intellectual transparency and long-term equity distribution."
    }
