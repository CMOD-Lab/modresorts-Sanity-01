# =============================================================================
# ModResorts - Multi-Stage Dockerfile
# Build Tool : Maven
# Java       : 8
# Package    : WAR (deployed on Apache Tomcat)
# Base Image : openjdk:8-jdk (explicit)
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1 – Builder
# -----------------------------------------------------------------------------
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy dependency descriptor first for layer-cache optimisation
COPY pom.xml .

# Pre-download all dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy the rest of the project source
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR, skipping tests
RUN mvn clean package -DskipTests -B

# -----------------------------------------------------------------------------
# Stage 2 – Runtime
# Uses the explicit base image supplied via EXPLICIT_BASE_IMAGE parameter
# -----------------------------------------------------------------------------
FROM openjdk:8-jdk

# Install Tomcat 9 (supports Servlet 4.0 / Java EE 7 WARs)
ENV CATALINA_HOME=/opt/tomcat
ENV TOMCAT_VERSION=9.0.85

RUN apt-get update -qq && \
    apt-get install -y --no-install-recommends wget ca-certificates && \
    wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
         -O /tmp/tomcat.tar.gz && \
    mkdir -p ${CATALINA_HOME} && \
    tar -xzf /tmp/tomcat.tar.gz -C ${CATALINA_HOME} --strip-components=1 && \
    rm /tmp/tomcat.tar.gz && \
    apt-get remove -y wget && \
    apt-get autoremove -y && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -d ${CATALINA_HOME} -s /sbin/nologin appuser

# Remove default Tomcat webapps to keep the image lean
RUN rm -rf ${CATALINA_HOME}/webapps/*

# Copy the built WAR into Tomcat's webapps directory
# The WAR is deployed at the ROOT context so it is accessible at /
COPY --from=builder /workspace/target/modresorts-*.war ${CATALINA_HOME}/webapps/ROOT.war

# Adjust ownership
RUN chown -R appuser:appgroup ${CATALINA_HOME}

# JVM tuning – container-aware memory settings
ENV JAVA_OPTS="-Xms256m -Xmx512m \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=UTC"

# Timezone
ENV TZ=UTC

# Application port (Tomcat default HTTP connector)
EXPOSE 8080

USER appuser

# Graceful shutdown: use Tomcat's catalina.sh run (traps SIGTERM)
CMD ["sh", "-c", "${CATALINA_HOME}/bin/catalina.sh run"]
