# ── Stage 1: Build Java Spring Boot Application ─────────────────────────────
FROM eclipse-temurin:21-jdk-jammy AS java-builder
WORKDIR /workspace

COPY pom.xml .
RUN apt-get update && apt-get install -y --no-install-recommends maven && \
    mvn dependency:go-offline -B -q && \
    rm -rf /var/lib/apt/lists/*

COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Build Python Calculation Virtualenv (with Compilers) ───────────
FROM ubuntu:22.04 AS python-builder
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    python3-dev \
    build-essential \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build
COPY worker/requirements.in worker/requirements.lock* ./

RUN python3 -m venv /opt/venv && \
    /opt/venv/bin/pip install --no-cache-dir --upgrade pip setuptools wheel && \
    /opt/venv/bin/pip install --no-cache-dir -r requirements.in

# ── Stage 3: Production Dual-Runtime (Slim JRE + Python Runtime Only) ────────
FROM eclipse-temurin:21-jre-jammy AS runtime
LABEL maintainer="astro-backend"
LABEL description="Optimized Dual-Runtime: Java 21 Spring Boot + Swiss Ephemeris Python 3 Daemon"

ENV DEBIAN_FRONTEND=noninteractive

# Install only minimal Python runtime and healthcheck utility (NO build-essential/compilers)
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    libpython3.10 \
    wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy compiled Spring Boot JAR
COPY --from=java-builder /workspace/target/astro-backend-*.jar app.jar

# Copy pre-compiled Python virtual environment from python-builder
COPY --from=python-builder /opt/venv /opt/venv

# Copy Python worker scripts
COPY worker ./worker

# Container & JVM environment settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080
ENV ASTRO_PYTHON_EXECUTABLE=/opt/venv/bin/python
ENV ASTRO_WORKER_SCRIPT=worker/daemon.py
ENV PATH="/opt/venv/bin:$PATH"

EXPOSE 8080 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${SERVER_PORT:-${PORT:-8080}}"]
