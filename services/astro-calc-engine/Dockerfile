# ── Stage 1: Build Wheel Dependencies ─────────────────────────────────────────
FROM python:3.10-slim AS builder
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY services/astro-calc-engine/requirements.txt ./requirements.txt

RUN python3 -m venv /opt/venv && \
    /opt/venv/bin/pip install --no-cache-dir --upgrade pip setuptools wheel && \
    /opt/venv/bin/pip install --no-cache-dir -r requirements.txt

# ── Stage 2: Lean Production Runtime ─────────────────────────────────────────
FROM python:3.10-slim AS runtime
WORKDIR /app

ENV PATH="/opt/venv/bin:$PATH"
ENV PYTHONUNBUFFERED=1
ENV PORT=8081

# Copy compiled virtualenv
COPY --from=builder /opt/venv /opt/venv

# Copy all calculation worker modules
COPY worker/ ./worker/
COPY services/astro-calc-engine/server.py ./server.py

# Ensure worker directory is on PYTHONPATH
ENV PYTHONPATH="/app:/app/worker"

EXPOSE 8081 50051

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD python3 -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8081/health')" || exit 1

CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8081", "--workers", "4"]
