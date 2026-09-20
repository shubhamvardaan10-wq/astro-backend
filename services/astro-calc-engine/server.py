"""
server.py – Production FastAPI microservice wrapper for astro-calc-engine.
Serves high-precision Swiss Ephemeris and 58 specialized expansion calculation engines.
"""

import sys
import os
from pathlib import Path
from typing import Any, Dict
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
import uvicorn

# Ensure worker/current directory is in python path
BASE_DIR = Path(__file__).resolve().parent
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

# Also support finding worker scripts in parent/worker if running locally
PARENT_WORKER = BASE_DIR.parent.parent / "worker"
if PARENT_WORKER.exists() and str(PARENT_WORKER) not in sys.path:
    sys.path.insert(0, str(PARENT_WORKER))

import daemon
import engine

app = FastAPI(
    title="Astro Calculation Engine Microservice",
    description="High-precision Swiss Ephemeris & PyJHora calculation engine with 58 domain expansions",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "astro-calc-engine",
        "engineLoaded": daemon.ENGINE_LOADED,
        "pid": os.getpid()
    }

@app.get("/calc/capabilities")
def get_capabilities():
    return {
        "status": "ok",
        "methods": getattr(engine, "METHODS", {}),
        "license": "AGPL-3.0-or-later"
    }

@app.post("/calc/execute")
async def execute_request(request: Request):
    try:
        req = await request.json()
        res = daemon.handle_request(req)
        return res
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/calc/analyze")
async def analyze_request(request: Request):
    try:
        payload = await request.json()
        return daemon.handle_request({"action": "analyze", "payload": payload})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/calc/eval")
async def eval_request(request: Request):
    try:
        req = await request.json()
        req["action"] = "eval"
        return daemon.handle_request(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/calc/timezones")
async def timezones_request(request: Request):
    try:
        req = await request.json()
        req["action"] = "timezones"
        return daemon.handle_request(req)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8081))
    uvicorn.run("server:app", host="0.0.0.0", port=port, log_level="info")
