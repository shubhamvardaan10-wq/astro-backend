FROM python:3.10-slim
WORKDIR /app

ENV PYTHONUNBUFFERED=1
ENV PORT=8084

COPY services/astro-media-export-service/requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy required worker rendering and vision scripts
COPY worker/pdf_exporter.py ./worker/pdf_exporter.py
COPY worker/chart_svg.py ./worker/chart_svg.py
COPY worker/palmistry_vision_engine.py ./worker/palmistry_vision_engine.py
COPY worker/face_reading_engine.py ./worker/face_reading_engine.py

COPY services/astro-media-export-service/server.py ./server.py

ENV PYTHONPATH="/app:/app/worker"

EXPOSE 8084

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD python3 -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8084/health')" || exit 1

CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8084", "--workers", "2"]
