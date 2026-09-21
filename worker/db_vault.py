"""
Embedded SQLite Data Vault for Astro-Backend.
Consolidates relational database persistence directly inside the astronomical engine.
Operates in high-performance Write-Ahead Logging (WAL) mode with connection pooling.
"""
import os
import json
import sqlite3
import threading
from pathlib import Path
from datetime import datetime
from typing import Dict, Any, List, Optional, Tuple

DB_PATH = os.getenv("VAULT_DB_PATH")
if not DB_PATH:
    # Default to data/vault.db in project root or current worker dir
    base = Path(__file__).resolve().parent.parent
    data_dir = base / "data"
    data_dir.mkdir(parents=True, exist_ok=True)
    DB_PATH = str(data_dir / "astro_vault.db")
else:
    Path(DB_PATH).parent.mkdir(parents=True, exist_ok=True)

_tls = threading.local()

def get_connection() -> sqlite3.Connection:
    if not hasattr(_tls, "conn") or _tls.conn is None:
        conn = sqlite3.connect(DB_PATH, timeout=10.0)
        conn.row_factory = sqlite3.Row
        # Set performance pragmas
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA synchronous=NORMAL;")
        conn.execute("PRAGMA foreign_keys=ON;")
        conn.execute("PRAGMA busy_timeout=5000;")
        _tls.conn = conn
    return _tls.conn

def init_vault_schema():
    conn = get_connection()
    with conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS api_keys (
                key TEXT PRIMARY KEY,
                tier TEXT NOT NULL DEFAULT 'FREE',
                is_active INTEGER NOT NULL DEFAULT 1,
                rate_limit_per_min INTEGER NOT NULL DEFAULT 60,
                remaining_credits INTEGER NOT NULL DEFAULT 1000,
                customer_email TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS user_profiles (
                user_id TEXT PRIMARY KEY,
                name TEXT,
                dob TEXT,
                time TEXT,
                city TEXT,
                latitude REAL,
                longitude REAL,
                gender TEXT DEFAULT 'MALE',
                ayanamsa TEXT DEFAULT 'LAHIRI',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS chat_sessions (
                session_id TEXT PRIMARY KEY,
                user_id TEXT,
                grounding_chart TEXT,
                language TEXT DEFAULT 'en',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS chat_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                meta_json TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE
            );

            CREATE TABLE IF NOT EXISTS billing_ledger (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                provider TEXT NOT NULL,
                event_type TEXT NOT NULL,
                customer_email TEXT,
                tier TEXT NOT NULL,
                api_key TEXT NOT NULL,
                payload_json TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );

            CREATE TABLE IF NOT EXISTS usage_metering (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                api_key TEXT NOT NULL,
                endpoint TEXT NOT NULL,
                response_time_ms INTEGER DEFAULT 0,
                status_code INTEGER DEFAULT 200,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """)

        # Ensure master key exists
        master_key = os.getenv("ASTRO_ADMIN_API_KEY", "ak_live_master_astro_2026")
        conn.execute("""
            INSERT INTO api_keys (key, tier, is_active, rate_limit_per_min, remaining_credits, customer_email)
            VALUES (?, 'ENTERPRISE', 1, 10000, 999999999, 'master_admin@astro.org')
            ON CONFLICT(key) DO UPDATE SET
                tier='ENTERPRISE',
                is_active=1,
                remaining_credits=999999999;
        """, (master_key,))

# Auto-initialize schema upon module import
init_vault_schema()

# ── API Key & Billing Operations ──────────────────────────────────────────────

def provision_api_key(tier: str, email: str, provider: str, event_type: str, raw_payload: Dict[str, Any]) -> str:
    import secrets
    prefix = "ak_live_"
    random_part = secrets.token_hex(12)
    new_key = f"{prefix}{random_part}"

    tier_limits = {
        "FREE": (30, 500),
        "STARTER": (120, 10000),
        "PRO": (300, 50000),
        "ENTERPRISE": (1200, 500000)
    }
    rate_limit, credits = tier_limits.get(tier.upper(), (60, 5000))

    conn = get_connection()
    with conn:
        conn.execute("""
            INSERT INTO api_keys (key, tier, is_active, rate_limit_per_min, remaining_credits, customer_email)
            VALUES (?, ?, 1, ?, ?, ?)
        """, (new_key, tier.upper(), rate_limit, credits, email))

        conn.execute("""
            INSERT INTO billing_ledger (provider, event_type, customer_email, tier, api_key, payload_json)
            VALUES (?, ?, ?, ?, ?, ?)
        """, (provider, event_type, email, tier.upper(), new_key, json.dumps(raw_payload)))

    return new_key

def verify_api_key(api_key: str) -> Optional[Dict[str, Any]]:
    conn = get_connection()
    cur = conn.execute("SELECT * FROM api_keys WHERE key = ? AND is_active = 1", (api_key,))
    row = cur.fetchone()
    if not row:
        return None
    return dict(row)

def record_usage(api_key: str, endpoint: str, duration_ms: int = 0, status_code: int = 200) -> bool:
    conn = get_connection()
    with conn:
        conn.execute("""
            INSERT INTO usage_metering (api_key, endpoint, response_time_ms, status_code)
            VALUES (?, ?, ?, ?)
        """, (api_key, endpoint, duration_ms, status_code))

        # Decrement credits
        conn.execute("""
            UPDATE api_keys
            SET remaining_credits = MAX(0, remaining_credits - 1)
            WHERE key = ?
        """, (api_key,))
    return True

def get_usage_metrics(api_key: str) -> Dict[str, Any]:
    conn = get_connection()
    cur_key = conn.execute("SELECT * FROM api_keys WHERE key = ?", (api_key,))
    key_info = cur_key.fetchone()
    if not key_info:
        return {"error": "API Key Not Found", "valid": False}

    cur_count = conn.execute("SELECT COUNT(*), AVG(response_time_ms) FROM usage_metering WHERE api_key = ?", (api_key,))
    row_count = cur_count.fetchone()
    total_calls = row_count[0] if row_count else 0
    avg_latency = int(row_count[1]) if row_count and row_count[1] else 0

    return {
        "apiKey": api_key,
        "tier": key_info["tier"],
        "isActive": bool(key_info["is_active"]),
        "remainingCredits": key_info["remaining_credits"],
        "rateLimitPerMin": key_info["rate_limit_per_min"],
        "totalRequests": total_calls,
        "avgResponseTimeMs": avg_latency,
        "customerEmail": key_info["customer_email"],
        "vaultStatus": "HEALTHY",
        "storageEngine": "SQLite-WAL-Embedded"
    }

# ── Conversational Chat Session Persistence ───────────────────────────────────

def save_chat_turn(
    session_id: str,
    user_msg: str,
    assistant_msg: str,
    grounding_chart: Optional[Dict[str, Any]] = None,
    language: str = "en",
    meta: Optional[Dict[str, Any]] = None
):
    conn = get_connection()
    with conn:
        chart_json = json.dumps(grounding_chart) if grounding_chart else None
        conn.execute("""
            INSERT INTO chat_sessions (session_id, grounding_chart, language, updated_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT(session_id) DO UPDATE SET
                grounding_chart = COALESCE(?, chat_sessions.grounding_chart),
                language = ?,
                updated_at = CURRENT_TIMESTAMP
        """, (session_id, chart_json, language, chart_json, language))

        conn.execute("""
            INSERT INTO chat_messages (session_id, role, content, meta_json)
            VALUES (?, 'user', ?, NULL)
        """, (session_id, user_msg))

        conn.execute("""
            INSERT INTO chat_messages (session_id, role, content, meta_json)
            VALUES (?, 'assistant', ?, ?)
        """, (session_id, assistant_msg, json.dumps(meta or {})))

def get_chat_session_history(session_id: str) -> Dict[str, Any]:
    conn = get_connection()
    cur_sess = conn.execute("SELECT * FROM chat_sessions WHERE session_id = ?", (session_id,))
    sess = cur_sess.fetchone()
    if not sess:
        return {"sessionId": session_id, "turns": [], "messageCount": 0}

    cur_msgs = conn.execute("SELECT role, content, meta_json, created_at FROM chat_messages WHERE session_id = ? ORDER BY id ASC", (session_id,))
    turns = []
    for r in cur_msgs.fetchall():
        turns.append({
            "role": r["role"],
            "content": r["content"],
            "meta": json.loads(r["meta_json"]) if r["meta_json"] else {},
            "timestamp": r["created_at"]
        })

    return {
        "sessionId": session_id,
        "language": sess["language"],
        "groundingChart": json.loads(sess["grounding_chart"]) if sess["grounding_chart"] else None,
        "messageCount": len(turns),
        "turns": turns
    }
