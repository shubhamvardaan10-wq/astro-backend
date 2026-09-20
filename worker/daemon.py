"""
daemon.py – High-performance persistent calculation daemon for Astro Backend.

Keeps Python and astrological calculation libraries warm in memory.
Communicates via standard I/O using single-line NDJSON protocol.
"""

import importlib
import json
import os
from pathlib import Path
import sys
import traceback

# Ensure worker directory is in sys.path
WORKER_DIR = Path(__file__).resolve().parent
if str(WORKER_DIR) not in sys.path:
    sys.path.insert(0, str(WORKER_DIR))

# Pre-load core engine if available
try:
    import engine
    ENGINE_LOADED = True
except Exception as e:
    sys.stderr.write(f"[WARN] Engine pre-load deferred: {e}\n")
    sys.stderr.flush()
    ENGINE_LOADED = False

MODULE_CACHE = {}

def get_module(module_name: str):
    if module_name in MODULE_CACHE:
        return MODULE_CACHE[module_name]
    mod = importlib.import_module(module_name)
    MODULE_CACHE[module_name] = mod
    return mod

def handle_request(req: dict) -> dict:
    action = req.get("action", "analyze")

    if action == "ping":
        return {"status": "pong", "pid": os.getpid()}

    if action == "capabilities":
        if ENGINE_LOADED:
            return {"methods": engine.METHODS, "license": "AGPL-3.0-or-later", "status": "ok"}
        return {"status": "ok", "methods": {}}

    if action == "analyze":
        payload = req.get("payload", req)
        if not ENGINE_LOADED:
            mod = importlib.import_module("engine")
            return mod.analyze(payload)
        return engine.analyze(payload)

    if action == "module_call":
        module_name = req.get("module")
        func_name = req.get("function")
        args = req.get("args", [])
        kwargs = req.get("kwargs", {})

        if not module_name or not func_name:
            return {"status": "error", "message": "module and function are required"}

        mod = get_module(module_name)
        func = getattr(mod, func_name)
        result = func(*args, **kwargs)
        return result

    if action == "eval":
        import io
        code = req.get("code")
        input_str = req.get("input", "")
        old_stdin = sys.stdin
        old_stdout = sys.stdout
        captured_stdout = io.StringIO()
        sys.stdin = io.StringIO(input_str)
        sys.stdout = captured_stdout
        try:
            exec(code, {"__name__": "__main__", "sys": sys, "json": json})
            return {"status": "ok", "output": captured_stdout.getvalue().strip()}
        except Exception as e:
            return {"status": "error", "message": str(e), "traceback": traceback.format_exc()}
        finally:
            sys.stdin = old_stdin
            sys.stdout = old_stdout

    return {"status": "error", "message": f"Unknown action: {action}"}

def main():
    # Signal readiness on stderr
    sys.stderr.write(f"[INFO] Python calculation daemon ready (PID: {os.getpid()})\n")
    sys.stderr.flush()

    while True:
        try:
            line = sys.stdin.readline()
            if not line:
                # EOF reached, parent process closed stdin
                break
            
            line = line.strip()
            if not line:
                continue

            req = json.loads(line)
            try:
                res = handle_request(req)
                output = json.dumps(res, default=str)
            except Exception as ex:
                sys.stderr.write(f"[ERROR] Calculation error: {ex}\n{traceback.format_exc()}\n")
                sys.stderr.flush()
                output = json.dumps({"status": "error", "message": str(ex)})

            sys.stdout.write(output + "\n")
            sys.stdout.flush()

        except KeyboardInterrupt:
            break
        except Exception as e:
            sys.stderr.write(f"[FATAL] Daemon protocol error: {e}\n")
            sys.stderr.flush()
            try:
                sys.stdout.write(json.dumps({"status": "error", "message": str(e)}) + "\n")
                sys.stdout.flush()
            except Exception:
                pass

if __name__ == "__main__":
    main()
