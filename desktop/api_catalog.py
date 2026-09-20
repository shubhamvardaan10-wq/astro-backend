"""
Comprehensive Catalog of all Astro-Backend Platform APIs (64+ Microservice Endpoints).
"""

API_CATEGORIES = [
    "All",
    "Production Ingress",
    "Microservice",
    "Security & Auth",
    "Billing & Plans",
    "Webhooks",
    "Gateway",
    "Core Ephemeris",
    "Worker Expansion",
    "Multi-Chart",
    "Conversational AI",
    "Life Event Timing",
    "Birth Rectification",
    "Daily Transit Alarms",
    "Localization i18n"
]

STANDARD_BIRTH_PAYLOAD = {
    "dob": "1990-01-01",
    "time": "12:00",
    "city": "Delhi",
    "latitude": 28.6139,
    "longitude": 77.2090
}

MATCHMAKING_PAYLOAD = {
    "partner1": {
        "dob": "1990-01-01",
        "time": "12:00",
        "city": "Delhi",
        "latitude": 28.6139,
        "longitude": 77.2090
    },
    "partner2": {
        "dob": "1992-05-15",
        "time": "14:30",
        "city": "Mumbai",
        "latitude": 19.0760,
        "longitude": 72.8777
    }
}

ENDPOINTS = [
    # ── 1. Production Ingress & TLS Termination ──
    {
        "id": "ingress_port80",
        "category": "Production Ingress",
        "name": "Ingress Port 80 Health",
        "method": "GET",
        "url": "http://localhost/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Checks HTTP Port 80 reverse proxy endpoint."
    },
    {
        "id": "ingress_port443",
        "category": "Production Ingress",
        "name": "Ingress Port 443 HTTPS Health",
        "method": "GET",
        "url": "{ingress}/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Verifies TLS/HTTPS termination on Port 443."
    },
    {
        "id": "ingress_calc_route",
        "category": "Production Ingress",
        "name": "Ingress Direct /calc/ Route",
        "method": "GET",
        "url": "{ingress}/calc/capabilities",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Ingress proxy route forwarding to Python Calc Engine."
    },
    {
        "id": "ingress_ai_route",
        "category": "Production Ingress",
        "name": "Ingress Direct /ai/ Route",
        "method": "POST",
        "url": "{ingress}/ai/rag/query",
        "body": {"query": "jupiter transit ascendant"},
        "skip_auth": True,
        "expected_code": 200,
        "description": "Ingress proxy route forwarding to AI RAG Service."
    },
    {
        "id": "ingress_media_route",
        "category": "Production Ingress",
        "name": "Ingress Direct /media/ Route",
        "method": "GET",
        "url": "{ingress}/media/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Ingress proxy route forwarding to Media Export Service."
    },

    # ── 2. Direct Microservices Cluster Infrastructure ──
    {
        "id": "calc_health",
        "category": "Microservice",
        "name": "Calc Engine Health",
        "method": "GET",
        "url": "{calc}/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Direct health probe to astro-calc-engine container (:8081)."
    },
    {
        "id": "calc_capabilities",
        "category": "Microservice",
        "name": "Calc Capabilities",
        "method": "GET",
        "url": "{calc}/calc/capabilities",
        "skip_auth": True,
        "expected_code": 200,
        "description": "List of active mathematical engines loaded in memory."
    },
    {
        "id": "calc_execute",
        "category": "Microservice",
        "name": "Calc Execute (Ping)",
        "method": "POST",
        "url": "{calc}/calc/execute",
        "body": {"action": "ping"},
        "skip_auth": True,
        "expected_code": 200,
        "description": "Direct JSON-RPC execution interface on calc engine."
    },
    {
        "id": "ai_health",
        "category": "Microservice",
        "name": "AI RAG Health",
        "method": "GET",
        "url": "{ai}/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Direct health probe to astro-ai-rag-service container (:8083)."
    },
    {
        "id": "ai_query",
        "category": "Microservice",
        "name": "AI RAG Query",
        "method": "POST",
        "url": "{ai}/ai/rag/query",
        "body": {"query": "jupiter transit ascendant"},
        "skip_auth": True,
        "expected_code": 200,
        "description": "Hybrid Vector + BM25 classical astrological retrieval."
    },
    {
        "id": "media_health",
        "category": "Microservice",
        "name": "Media Service Health",
        "method": "GET",
        "url": "{media}/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Direct health probe to astro-media-export-service (:8084)."
    },

    # ── 3. Security, Authentication & Rate Limiting ──
    {
        "id": "sec_missing_key",
        "category": "Security & Auth",
        "name": "Missing API Key (Reject 401)",
        "method": "POST",
        "url": "{ingress}/api/astro/vedic-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "skip_auth": True,
        "expected_code": 401,
        "description": "Verifies unauthenticated calls receive HTTP 401 Unauthorized."
    },
    {
        "id": "sec_invalid_key",
        "category": "Security & Auth",
        "name": "Invalid API Key (Reject 401)",
        "method": "POST",
        "url": "{ingress}/api/astro/vedic-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "api_key": "ak_live_invalid_fake_key",
        "expected_code": 401,
        "description": "Verifies bogus API keys are rejected with HTTP 401."
    },
    {
        "id": "sec_master_key",
        "category": "Security & Auth",
        "name": "Master Key Auth (Accept 200)",
        "method": "POST",
        "url": "{ingress}/api/astro/vedic-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Verifies valid master production API key is accepted."
    },

    # ── 4. Commercial Monetization, Plans & Webhooks ──
    {
        "id": "billing_plans",
        "category": "Billing & Plans",
        "name": "Public Pricing Plans",
        "method": "GET",
        "url": "{ingress}/api/billing/plans",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Fetches Free, Starter, Pro, and Enterprise subscription tiers."
    },
    {
        "id": "billing_usage",
        "category": "Billing & Plans",
        "name": "API Key Usage Metering",
        "method": "GET",
        "url": "{ingress}/api/billing/usage",
        "expected_code": 200,
        "description": "Returns credit balance, requests consumed, and rate limit bucket."
    },
    {
        "id": "webhook_stripe",
        "category": "Webhooks",
        "name": "Stripe Automated Provisioning",
        "method": "POST",
        "url": "{ingress}/api/billing/webhook/stripe",
        "body": {
            "type": "checkout.session.completed",
            "data": {
                "object": {
                    "customer_email": "enterprise_desktop@example.com",
                    "metadata": {"tier": "PRO"}
                }
            }
        },
        "skip_auth": True,
        "expected_code": 200,
        "description": "Automated key provisioning upon Stripe successful checkout."
    },
    {
        "id": "webhook_razorpay",
        "category": "Webhooks",
        "name": "Razorpay Automated Provisioning",
        "method": "POST",
        "url": "{ingress}/api/billing/webhook/razorpay",
        "body": {
            "event": "payment.captured",
            "payload": {
                "payment": {
                    "entity": {
                        "email": "indian_startup@example.in",
                        "notes": {"tier": "STARTER"}
                    }
                }
            }
        },
        "skip_auth": True,
        "expected_code": 200,
        "description": "Automated key provisioning upon Razorpay payment capture."
    },

    # ── 5. Gateway Core & Search ──
    {
        "id": "gateway_health",
        "category": "Gateway",
        "name": "Actuator Health",
        "method": "GET",
        "url": "{ingress}/actuator/health",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Spring Boot 3 Actuator health check and component diagnostics."
    },
    {
        "id": "gateway_info",
        "category": "Gateway",
        "name": "Actuator Info",
        "method": "GET",
        "url": "{ingress}/actuator/info",
        "skip_auth": True,
        "expected_code": 200,
        "description": "Build version, runtime details, and git commit status."
    },
    {
        "id": "gateway_search_rules",
        "category": "Gateway",
        "name": "Classical Rules Search",
        "method": "GET",
        "url": "{ingress}/api/astro/v2/search/rules?q=Sun+in+Aries&limit=5",
        "expected_code": 200,
        "description": "Instant lexical search across classical Sanskrit astrological aphorisms."
    },

    # ── 6. Core Ephemeris Calculation Endpoints ──
    {
        "id": "core_vedic_chart",
        "category": "Core Ephemeris",
        "name": "Vedic Chart",
        "method": "POST",
        "url": "{ingress}/api/astro/vedic-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Computes Lagna, 12 Rashi houses, planetary coordinates, and dignity."
    },
    {
        "id": "core_western_chart",
        "category": "Core Ephemeris",
        "name": "Western Chart",
        "method": "POST",
        "url": "{ingress}/api/astro/western-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Computes Tropical zodiac placements, Placidus cusps, and aspects."
    },
    {
        "id": "core_dasha",
        "category": "Core Ephemeris",
        "name": "Vimshottari Dasha",
        "method": "POST",
        "url": "{ingress}/api/astro/dasha",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Calculates Mahadasha, Antardasha, and Pratyantardasha hierarchy."
    },
    {
        "id": "core_nakshatra",
        "category": "Core Ephemeris",
        "name": "Nakshatra Computation",
        "method": "POST",
        "url": "{ingress}/api/astro/nakshatra",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Determines Janma Nakshatra, Pada, Lord, Deity, and Gana."
    },
    {
        "id": "core_planets",
        "category": "Core Ephemeris",
        "name": "Planetary Positions",
        "method": "POST",
        "url": "{ingress}/api/astro/planets",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "High-precision planetary degrees, retrogression, speed, and signs."
    },
    {
        "id": "core_yogas",
        "category": "Core Ephemeris",
        "name": "Yogas Detection",
        "method": "POST",
        "url": "{ingress}/api/astro/yogas",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Identifies Raja, Dhana, Gaja-Kesari, Budhaditya, and Viparita yogas."
    },
    {
        "id": "core_prediction",
        "category": "Core Ephemeris",
        "name": "Life Prediction Synthesis",
        "method": "POST",
        "url": "{ingress}/api/astro/prediction",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Holistic natal chart life reading synthesizing house lords and yogas."
    },

    # ── 7. Decoupled Specialized Calculation Worker Expansions ──
    {
        "id": "worker_panchangam",
        "category": "Worker Expansion",
        "name": "Full Panchangam",
        "method": "POST",
        "url": "{ingress}/api/astro/panchangam",
        "body": {"latitude": 28.6139, "longitude": 77.2090, "date": "2026-09-21"},
        "expected_code": 200,
        "description": "Five limbs of time: Tithi, Nakshatra, Yoga, Karana, and Vara."
    },
    {
        "id": "worker_chart_svg",
        "category": "Worker Expansion",
        "name": "Dynamic SVG Chart",
        "method": "POST",
        "url": "{ingress}/api/astro/chart-svg",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Generates pure SVG vector rendering of North/South Indian Kundli."
    },
    {
        "id": "worker_remedies",
        "category": "Worker Expansion",
        "name": "Ayurvedic Remedies",
        "method": "POST",
        "url": "{ingress}/api/astro/remedies",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Prescribes herbs, diet, and lifestyle balancing planetary afflictions."
    },
    {
        "id": "worker_medical",
        "category": "Worker Expansion",
        "name": "Medical Astrology",
        "method": "POST",
        "url": "{ingress}/api/astro/medical",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Tri-dosha (Vata, Pitta, Kapha) balance and anatomical vulnerabilities."
    },
    {
        "id": "worker_vastu",
        "category": "Worker Expansion",
        "name": "Vastu Matrix",
        "method": "POST",
        "url": "{ingress}/api/astro/vastu",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "16-directional Vastu energy distribution and recommendations."
    },
    {
        "id": "worker_destiny_curve",
        "category": "Worker Expansion",
        "name": "Destiny Curve",
        "method": "POST",
        "url": "{ingress}/api/astro/destiny-curve",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Age 0-100 quantitative life vitality and prosperity curve."
    },
    {
        "id": "worker_career_ikigai",
        "category": "Worker Expansion",
        "name": "Career Ikigai",
        "method": "POST",
        "url": "{ingress}/api/astro/career-ikigai",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Vocation suitability, 10th house strength, and entrepreneurial index."
    },
    {
        "id": "worker_numerology_tuning",
        "category": "Worker Expansion",
        "name": "Numerology Tuning",
        "method": "POST",
        "url": "{ingress}/api/astro/numerology-tuning",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Pythagorean & Chaldean name vibrations and lucky frequency tuning."
    },
    {
        "id": "worker_sade_sati",
        "category": "Worker Expansion",
        "name": "Sade Sati Timeline",
        "method": "POST",
        "url": "{ingress}/api/astro/sade-sati-timeline",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Saturn 7.5 year transit cycles across the lifetime with dates."
    },
    {
        "id": "worker_kalasarpa",
        "category": "Worker Expansion",
        "name": "Kalasarpa Optimizer",
        "method": "POST",
        "url": "{ingress}/api/astro/kalasarpa-optimizer",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Identifies 12 variations of Kaal Sarp yoga and mitigation mantras."
    },
    {
        "id": "worker_ashtakavarga",
        "category": "Worker Expansion",
        "name": "Ashtakavarga Kaksha",
        "method": "POST",
        "url": "{ingress}/api/astro/ashtakavarga-kaksha",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Sarvashtakavarga 337 bindu grid and kaksha division strength."
    },
    {
        "id": "worker_varshaphala",
        "category": "Worker Expansion",
        "name": "Varshaphala Annual",
        "method": "POST",
        "url": "{ingress}/api/astro/varshaphala",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Tajika annual solar return chart, Muntha, and Varsheshwara lord."
    },
    {
        "id": "worker_jaimini_karakamsha",
        "category": "Worker Expansion",
        "name": "Jaimini Karakamsha",
        "method": "POST",
        "url": "{ingress}/api/astro/jaimini-karakamsha",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Atmakaraka planet in Navamsha and spiritual/career soul purpose."
    },
    {
        "id": "worker_jaimini_chara_dasha",
        "category": "Worker Expansion",
        "name": "Jaimini Chara Dasha",
        "method": "POST",
        "url": "{ingress}/api/astro/jaimini-chara-dasha",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Sign-based timing periods per sage Jaimini's Upadesha Sutras."
    },
    {
        "id": "worker_kp_significators",
        "category": "Worker Expansion",
        "name": "KP Stellar Significators",
        "method": "POST",
        "url": "{ingress}/api/astro/kp-significators",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Krishnamurti Padhdhati 4-fold planetary and cusp sub-lord matrix."
    },
    {
        "id": "worker_kp_horary",
        "category": "Worker Expansion",
        "name": "KP Horary 1-249",
        "method": "POST",
        "url": "{ingress}/api/astro/kp-horary",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Prashna horary analysis using Krishnamurti's 249 sub-divisions."
    },
    {
        "id": "worker_bhrigu_nandi_nadi",
        "category": "Worker Expansion",
        "name": "Bhrigu Nandi Nadi",
        "method": "POST",
        "url": "{ingress}/api/astro/bhrigu-nandi-nadi",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Nadi planetary combinations, trine linkages, and karma transits."
    },
    {
        "id": "worker_lal_kitab",
        "category": "Worker Expansion",
        "name": "Lal Kitab Debts",
        "method": "POST",
        "url": "{ingress}/api/astro/lal-kitab",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Rin (ancestral karmic debts), blind planets, and practical remedies."
    },
    {
        "id": "worker_draconic_chart",
        "category": "Worker Expansion",
        "name": "Draconic Soul Chart",
        "method": "POST",
        "url": "{ingress}/api/astro/draconic-chart",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "True North Node anchored soul contract chart."
    },
    {
        "id": "worker_kota_chakra",
        "category": "Worker Expansion",
        "name": "Kota Chakra Defense",
        "method": "POST",
        "url": "{ingress}/api/astro/kota-chakra",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Fortress diagram for health defense and vulnerability periods."
    },
    {
        "id": "worker_lo_shu_grid",
        "category": "Worker Expansion",
        "name": "Lo Shu Numerology Grid",
        "method": "POST",
        "url": "{ingress}/api/astro/lo-shu-grid",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "3x3 magic square arrows of strength, weakness, and missing numbers."
    },
    {
        "id": "worker_sarvatobhadra",
        "category": "Worker Expansion",
        "name": "Sarvatobhadra Chakra",
        "method": "POST",
        "url": "{ingress}/api/astro/sarvatobhadra-chakra",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "81-square Sarvatobhadra transit vedha afflictions."
    },
    {
        "id": "worker_panch_pakshi",
        "category": "Worker Expansion",
        "name": "Panch Pakshi Rhythm",
        "method": "POST",
        "url": "{ingress}/api/astro/panch-pakshi",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Five-bird bio-rhythm activities (Rule, Eat, Walk, Sleep, Die)."
    },
    {
        "id": "worker_aura_chakra",
        "category": "Worker Expansion",
        "name": "Aura Chakra Alignment",
        "method": "POST",
        "url": "{ingress}/api/astro/aura-chakra",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "7 subtle body chakras correlated to planetary frequencies."
    },
    {
        "id": "worker_sound_therapy",
        "category": "Worker Expansion",
        "name": "Sound Therapy Frequency",
        "method": "POST",
        "url": "{ingress}/api/astro/sound-therapy",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Binaural sound hertz and Bija mantras for planetary resonance."
    },
    {
        "id": "worker_gemstone_rudraksha",
        "category": "Worker Expansion",
        "name": "Gemstone & Rudraksha",
        "method": "POST",
        "url": "{ingress}/api/astro/gemstone-rudraksha",
        "body": STANDARD_BIRTH_PAYLOAD,
        "expected_code": 200,
        "description": "Therapeutic gemstones, metals, wearing fingers, and Rudraksha mukhis."
    },

    # ── 8. Multi-Chart Matchmaking ──
    {
        "id": "match_ashtakoota",
        "category": "Multi-Chart",
        "name": "36-Guna Kundli Milan",
        "method": "POST",
        "url": "{ingress}/api/astro/matchmaking",
        "body": MATCHMAKING_PAYLOAD,
        "expected_code": 200,
        "description": "North Indian Ashtakoota 36-point compatibility with Manglik dosha."
    },
    {
        "id": "match_dasakoota",
        "category": "Multi-Chart",
        "name": "Dasa Koota South System",
        "method": "POST",
        "url": "{ingress}/api/astro/dasa-koota-matching",
        "body": MATCHMAKING_PAYLOAD,
        "expected_code": 200,
        "description": "South Indian Dasa Koota system with Vedha and Rajju checks."
    },

    # ── 9. Melooha Parity & Superiority Features ──
    {
        "id": "chat_turn1",
        "category": "Conversational AI",
        "name": "Chat Turn 1 (Marriage Grounding)",
        "method": "POST",
        "url": "{ingress}/api/astro/chat",
        "body": {
            "message": "When will I get married?",
            "birth": STANDARD_BIRTH_PAYLOAD,
            "language": "en"
        },
        "expected_code": 200,
        "description": "Initial chat consultation grounded in natal D9 and 7th house."
    },
    {
        "id": "chat_turn2",
        "category": "Conversational AI",
        "name": "Chat Turn 2 (Session Memory Recall)",
        "method": "POST",
        "url": "{ingress}/api/astro/chat",
        "body": {
            "message": "What about my wealth and promotion?",
            "language": "en"
        },
        "expected_code": 200,
        "requires_session": True,
        "description": "Follow-up question relying on in-memory chat session state."
    },
    {
        "id": "chat_turn3",
        "category": "Conversational AI",
        "name": "Chat Turn 3 (Native Hindi Consultation)",
        "method": "POST",
        "url": "{ingress}/api/astro/chat",
        "body": {
            "message": "कैरियर में सफलता कब मिलेगी?",
            "language": "hi"
        },
        "expected_code": 200,
        "requires_session": True,
        "description": "Conversational query answered in natural Hindi with astrological terms."
    },
    {
        "id": "chat_history",
        "category": "Conversational AI",
        "name": "Retrieve Full Chat Thread",
        "method": "GET",
        "url": "{ingress}/api/astro/chat/history/{sessionId}",
        "expected_code": 200,
        "requires_session": True,
        "description": "Retrieves the complete dialogue transcript with timestamps."
    },
    {
        "id": "timing_trajectory",
        "category": "Life Event Timing",
        "name": "Month-by-Month Trajectory (12M)",
        "method": "POST",
        "url": "{ingress}/api/astro/timeline-forecast",
        "body": {
            "dob": "1990-01-01",
            "time": "12:00",
            "city": "Delhi",
            "horizonMonths": 12
        },
        "expected_code": 200,
        "description": "Quantitative domain trajectory across Career, Wealth, Love, and Health."
    },
    {
        "id": "btr_scoring",
        "category": "Birth Rectification",
        "name": "Automated BTR Scoring Engine",
        "method": "POST",
        "url": "{ingress}/api/astro/birth-time-rectification",
        "body": {
            "dob": "1990-01-01",
            "time": "12:00",
            "city": "Delhi",
            "uncertaintyMinutes": 20,
            "stepMinutes": 5,
            "gender": "MALE",
            "lifeEvents": [
                {"eventType": "CAREER_BREAKTHROUGH", "eventDate": "2015-06-01"}
            ]
        },
        "expected_code": 200,
        "description": "Automated birth time rectification scoring past life milestones."
    },
    {
        "id": "transit_alarms",
        "category": "Daily Transit Alarms",
        "name": "Chandrashtama & Gochara Alarms",
        "method": "POST",
        "url": "{ingress}/api/astro/transit-alerts",
        "body": {
            "dob": "1990-01-01",
            "time": "12:00",
            "city": "Delhi",
            "targetDate": "2026-09-21"
        },
        "expected_code": 200,
        "description": "Daily alerts for Chandrashtama, Sade Sati, and adverse transit aspects."
    },
    {
        "id": "multilingual_report",
        "category": "Localization i18n",
        "name": "Hindi Vedic Synthesis Report",
        "method": "POST",
        "url": "{ingress}/api/astro/multilingual-report",
        "body": {
            "dob": "1990-01-01",
            "time": "12:00",
            "city": "Delhi",
            "targetLanguage": "hi"
        },
        "expected_code": 200,
        "description": "Full synthesis report rendered natively in Hindi Devanagari script."
    }
]
