# =============================================================================
# ModResorts – Multi-stage Dockerfile
# Build tool : Maven (system mvn – NOT mvnw)
# Java       : 8 (eclipse-temurin)
# Packaging  : WAR deployed on Open Liberty
# Runtime    : eclipse-temurin:8-jdk (explicit base image)
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 – Builder
# -----------------------------------------------------------------------------
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy dependency descriptors first to leverage Docker layer caching
COPY pom.xml .

# Download all dependencies (offline-friendly layer)
RUN mvn dependency:go-offline -B

# Copy the full project source (wrapper files are excluded via .dockerignore)
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR artifact, skipping tests
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2 – Runtime
# Explicit base image: eclipse-temurin:8-jdk
# Open Liberty is installed to serve the WAR
# -----------------------------------------------------------------------------
FROM eclipse-temurin:8-jdk

# Install Open Liberty
ENV LIBERTY_VERSION=23.0.0.12
ENV LIBERTY_HOME=/opt/ol/wlp

RUN apt-get update && apt-get install -y --no-install-recommends \
        unzip \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /opt/ol \
    && curl -fsSL "https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/release/${LIBERTY_VERSION}/openliberty-${LIBERTY_VERSION}.zip" \
       -o /tmp/liberty.zip \
    && unzip -q /tmp/liberty.zip -d /opt/ol \
    && mv /opt/ol/wlp-* /opt/ol/wlp 2>/dev/null || true \
    && rm /tmp/liberty.zip \
    && apt-get purge -y --auto-remove unzip

# Create a non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d /home/appuser -s /bin/bash appuser \
    && mkdir -p /home/appuser \
    && chown -R appuser:appgroup /home/appuser /opt/ol

# Create Liberty server
USER appuser
RUN ${LIBERTY_HOME}/bin/server create modresorts

# Copy Liberty server configuration
COPY --chown=appuser:appgroup docker/server.xml ${LIBERTY_HOME}/usr/servers/modresorts/server.xml

# Copy the built WAR from the builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/modresorts-2.0.0.war \
     ${LIBERTY_HOME}/usr/servers/modresorts/apps/modresorts.war

# Application port (Liberty HTTP)
EXPOSE 9080

# JVM tuning – container-aware settings
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Environment variables – supplied at runtime via Kubernetes ConfigMap / Secret
ENV REDIS_HOST=localhost
ENV REDIS_PORT=6379
ENV WEATHER_API_KEY=""
ENV SERVICE_DISCOVERY_URL="http://service-registry:8080"
ENV CUSTOMER_SERVICE_URL="http://customer-service:8080"
ENV TZ=UTC

WORKDIR ${LIBERTY_HOME}

# Start Open Liberty in foreground
CMD ["bin/server", "run", "modresorts"]
