# ── Stage 1: Build Java Application ─────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

COPY pom.xml .
RUN apt-get update && apt-get install -y maven && mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Stage 2: Production Dual-Runtime (Java 21 + Python 3 Worker) ───────────
FROM eclipse-temurin:21-jre AS runtime
LABEL maintainer="astro-backend"
LABEL description="Dual Runtime: Java 21 Spring Boot + Swiss Ephemeris Python 3 Worker"

# Install Python 3, venv, and build-essential for C-extensions (pyswisseph)
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    python3-venv \
    build-essential \
    curl \
    wget \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy compiled Spring Boot JAR
COPY --from=builder /workspace/target/astro-backend-*.jar app.jar

# Copy Python worker scripts and requirements
COPY worker ./worker

# Create Python virtual environment matching project structure
RUN python3 -m venv target/engine-venv && \
    target/engine-venv/bin/pip install --no-cache-dir --upgrade pip setuptools wheel && \
    target/engine-venv/bin/pip install --no-cache-dir -r worker/requirements.in

# Container & JVM environment settings
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=70.0 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_PORT=8080

EXPOSE 8080 7860

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --server.port=${SERVER_PORT:-${PORT:-8080}}"]
