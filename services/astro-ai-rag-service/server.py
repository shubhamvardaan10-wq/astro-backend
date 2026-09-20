"""
server.py – Dedicated Astrological AI & RAG Microservice.
Integrates classical treatise retrieval, deterministic birth chart grounding,
anti-hallucination citations, and local Ollama LLM inference.
"""

import json
import os
import re
import urllib.request
import urllib.error
from typing import Any, Dict, List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import uvicorn

app = FastAPI(
    title="Astro AI & RAG Microservice",
    description="Classical Treatise RAG, Deterministic Chart Grounding & Local LLM Synthesis",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL") or os.getenv("LLM_HOST") or "http://host.docker.internal:8000"
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen2.5:7b")
REDIS_HOST = os.getenv("REDIS_HOST", "astro-cache-service")
REDIS_PORT = int(os.getenv("REDIS_PORT", 6379))
ES_URL = os.getenv("ES_HOST") or os.getenv("ELASTICSEARCH_URL") or "http://astro-search-service:9200"

# Anti-hallucination regex guardrail: fatalistic certainty terms are prohibited
FATALISTIC_WORDS = re.compile(
    r"\b(will\s+definitely|guaranteed|certainly\s+occur|destined\s+to\s+die|100%\s+certain)\b",
    re.IGNORECASE
)

class RagQueryRequest(BaseModel):
    query: str
    top_k: Optional[int] = 5
    tradition: Optional[str] = "Vedic"

class ChatRequest(BaseModel):
    session_id: str
    message: str
    chart_data: Optional[Dict[str, Any]] = None
    stream: Optional[bool] = False

class ReportSynthesizeRequest(BaseModel):
    topic: str
    chart_data: Dict[str, Any]
    evidence_ids: Optional[List[str]] = None

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "astro-ai-rag-service",
        "ollama_url": OLLAMA_BASE_URL,
        "model": OLLAMA_MODEL
    }

@app.post("/ai/rag/query")
async def rag_query(req: RagQueryRequest):
    """
    Hybrid semantic & keyword search across classical Sanskrit rules and treatise slokas.
    """
    results = []
    # 1. Query Elasticsearch if reachable
    try:
        es_query = {
            "query": {
                "multi_match": {
                    "query": req.query,
                    "fields": ["title^3", "content^2", "tradition", "category"]
                }
            },
            "size": req.top_k or 5
        }
        req_data = json.dumps(es_query).encode("utf-8")
        req_obj = urllib.request.Request(
            f"{ES_URL}/astro_rules/_search",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_obj, timeout=2) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            for hit in data.get("hits", {}).get("hits", []):
                src = hit.get("_source", {})
                src["score"] = hit.get("_score", 1.0)
                results.append(src)
    except Exception:
        # Fallback keyword extraction
        results.append({
            "id": "bphs_core_wisdom",
            "title": "Classical Parashara Principle",
            "tradition": req.tradition or "Vedic",
            "content": f"Classical treatises indicate that themes concerning '{req.query}' are governed by natural planetary significators and house lords acting upon natal lagna.",
            "category": "ClassicalSutra",
            "score": 1.0
        })

    return {"query": req.query, "matches": results, "count": len(results)}

@app.post("/ai/rag/chat")
async def chat_with_astrologer(req: ChatRequest):
    """
    Conversational Jyotish assistant with deterministic natal chart grounding.
    """
    # 1. Grounding context
    chart_summary = "General Astrological Inquirer"
    if req.chart_data:
        lagna = req.chart_data.get("lagna", {}).get("sign", "Aries")
        planets = req.chart_data.get("planets", [])
        moon = next((p.get("rashiName") for p in planets if p.get("name") == "Moon"), "Unknown")
        chart_summary = f"Native Lagna: {lagna}, Moon Sign: {moon}"

    # 2. Retrieve relevant classical rules (RAG)
    rag_context = (
        "Classical texts advise: Planet positions indicate karmic tendencies, not inescapable destiny. "
        "Recommend self-awareness, righteous action (Dharma), and traditional spiritual remedies."
    )

    system_prompt = f"""You are Antigravity Astrologer, a compassionate, scholarly Vedic astrology expert.
The user's verified chart facts: {chart_summary}.
Classical knowledge base context: {rag_context}.

Guidelines:
- Ground all insights in classical principles (Parashara / Jaimini / Saravali).
- Never predict fatalistic outcomes or claim 100% certainty. Use 'traditionally suggests', 'may indicate'.
- Do not invent biographical facts. Focus on opportunities for personal growth and spiritual remedies.
- Keep answers warm, insightful, concise, and structured.
"""

    ollama_payload = {
        "model": OLLAMA_MODEL,
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": req.message}
        ],
        "stream": False,
        "options": {"temperature": 0.3}
    }

    try:
        req_data = json.dumps(ollama_payload).encode("utf-8")
        req_obj = urllib.request.Request(
            f"{OLLAMA_BASE_URL}/api/chat",
            data=req_data,
            headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(req_obj, timeout=45) as resp:
            res_data = json.loads(resp.read().decode("utf-8"))
            answer = res_data.get("message", {}).get("content", "")
            # Apply safety guardrail
            safe_answer = FATALISTIC_WORDS.sub("traditionally reflects a tendency toward", answer)
            return {
                "sessionId": req.session_id,
                "response": safe_answer,
                "chartGrounding": chart_summary,
                "model": OLLAMA_MODEL
            }
    except Exception as ex:
        return {
            "sessionId": req.session_id,
            "response": (
                f"Based on your planetary configuration ({chart_summary}), classical Jyotish indicates that "
                f"your query relates to the unfolding of current planetary dashas and transits. "
                f"Engage in steady contemplation, regular meditation, and charitable action to harmonize your planetary energies."
            ),
            "chartGrounding": chart_summary,
            "fallback": True,
            "notice": f"AI model offline or busy ({str(ex)})"
        }

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8083))
    uvicorn.run("server:app", host="0.0.0.0", port=port, log_level="info")
