FROM python:3.10-slim
WORKDIR /app

ENV PYTHONUNBUFFERED=1
ENV PORT=8083

COPY services/astro-ai-rag-service/requirements.txt ./
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

COPY services/astro-ai-rag-service/server.py ./server.py

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD python3 -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:8083/health')" || exit 1

CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8083", "--workers", "2"]
