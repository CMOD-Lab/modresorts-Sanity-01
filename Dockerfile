# =============================================================================
# Stage 1: Builder
# =============================================================================
FROM maven:3.9.4-eclipse-temurin-8 AS builder

WORKDIR /workspace

# Copy Maven build descriptor first for dependency caching
COPY pom.xml .

# Download all dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy the full project source
COPY src/ src/
COPY WebContent/ WebContent/

# Build the WAR artifact (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# =============================================================================
# Stage 2: Runtime - Tomcat on eclipse-temurin:8-jdk-alpine
# =============================================================================
FROM eclipse-temurin:8-jdk-alpine

# Metadata labels
LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Set timezone
ENV TZ=UTC

# Install Tomcat and configure as root before switching to non-root user
ENV CATALINA_HOME=/opt/tomcat
ENV TOMCAT_VERSION=9.0.82
ENV PATH=${CATALINA_HOME}/bin:${PATH}

RUN apk add --no-cache bash \
    && mkdir -p ${CATALINA_HOME} \
    && wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
         -O /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C /opt \
    && mv /opt/apache-tomcat-${TOMCAT_VERSION}/* ${CATALINA_HOME}/ \
    && rm /tmp/tomcat.tar.gz \
    && rm -rf ${CATALINA_HOME}/webapps/ROOT \
              ${CATALINA_HOME}/webapps/examples \
              ${CATALINA_HOME}/webapps/docs \
              ${CATALINA_HOME}/webapps/host-manager \
              ${CATALINA_HOME}/webapps/manager \
    && sed -i 's/port="8080"/port="9080"/' ${CATALINA_HOME}/conf/server.xml \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup \
    && chown -R appuser:appgroup ${CATALINA_HOME}

# Copy the WAR from the builder stage into Tomcat's webapps directory
# Deployed as 'resorts' to match the original context-root /resorts
COPY --from=builder --chown=appuser:appgroup \
     /workspace/target/modresorts-2.0.0.war \
     ${CATALINA_HOME}/webapps/resorts.war

# Switch to non-root user
USER appuser

# Application port (configured Tomcat HTTP port)
EXPOSE 9080

# JVM options for container awareness
ENV JAVA_OPTS="-Xmx512m -Xms256m \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.awt.headless=true \
  -Dfile.encoding=UTF-8 \
  -Duser.timezone=UTC"

# Application environment variables (overridable at runtime)
ENV WEATHER_API_KEY=""
ENV WEATHER_SERVICE_BASE_URL="http://api.wunderground.com/api/"
ENV REDIS_HOST="localhost"
ENV REDIS_PORT="6379"
ENV NAMING_SERVICE_HOST="naming-service"
ENV NAMING_SERVICE_PORT="8080"
ENV SERVER_DISPLAY_NAME="modresorts"
ENV SERVER_FULL_NAME="modresorts-server"

# Start Tomcat in foreground
CMD ["catalina.sh", "run"]
