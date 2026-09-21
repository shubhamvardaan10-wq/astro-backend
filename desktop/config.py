"""
Global Configuration for Astro Enterprise Desktop Suite.
"""
import os

DEFAULT_INGRESS_URL = os.getenv("ASTRO_INGRESS_URL", "http://localhost:18080")
DEFAULT_GATEWAY_URL = os.getenv("ASTRO_GATEWAY_URL", "http://localhost:18080")
DEFAULT_CALC_URL = os.getenv("ASTRO_CALC_URL", "http://localhost:8081")
DEFAULT_AI_URL = os.getenv("ASTRO_AI_URL", "http://localhost:8083")
DEFAULT_MEDIA_URL = os.getenv("ASTRO_MEDIA_URL", "http://localhost:8084")

DEFAULT_API_KEY = os.getenv("ASTRO_API_KEY", "ak_live_master_astro_2026")

DEFAULT_BIRTH_PROFILE = {
    "dob": "1990-01-01",
    "time": "12:00",
    "city": "Delhi",
    "latitude": 28.6139,
    "longitude": 77.2090,
    "gender": "MALE",
    "ayanamsa": "LAHIRI"
}

# Theme and Styling Palette
COLORS = {
    "bg_dark": "#0B0F19",
    "sidebar_bg": "#0D1322",
    "card_bg": "#131C31",
    "card_border": "#1E2A47",
    "accent_gold": "#F5A623",
    "accent_cyan": "#00C8D7",
    "accent_purple": "#8B5CF6",
    "success": "#10B981",
    "danger": "#EF4444",
    "warning": "#F59E0B",
    "text_primary": "#F3F4F6",
    "text_secondary": "#9CA3AF",
    "text_muted": "#6B7280",
    "entry_bg": "#1A233A",
    "entry_border": "#2D3748"
}
