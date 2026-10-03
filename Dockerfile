# ============================================================
# Stage 1: Builder
# ============================================================
FROM maven:3.8.6-openjdk-8-slim AS builder

WORKDIR /workspace

# Copy Maven build descriptor first for dependency layer caching
COPY pom.xml .

# Download all dependencies (cached layer unless pom.xml changes)
RUN mvn dependency:go-offline -B

# Copy full project source
COPY src ./src
COPY WebContent ./WebContent

# Build the WAR artifact (skip tests for Docker build)
RUN mvn clean package -DskipTests -B

# ============================================================
# Stage 2: Runtime
# ============================================================
FROM eclipse-temurin:8-jdk-alpine

# Metadata labels
LABEL maintainer="ModResorts Team" \
      application="modresorts" \
      version="2.0.0"

# Install Tomcat 9 (Java EE 7 / Servlet 3.1 compatible)
ENV CATALINA_HOME=/opt/tomcat
ENV TOMCAT_VERSION=9.0.85

RUN apk add --no-cache bash \
    && mkdir -p ${CATALINA_HOME} \
    && wget -q "https://archive.apache.org/dist/tomcat/tomcat-9/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" -O /tmp/tomcat.tar.gz \
    && tar -xzf /tmp/tomcat.tar.gz -C ${CATALINA_HOME} --strip-components=1 \
    && rm /tmp/tomcat.tar.gz \
    && rm -rf ${CATALINA_HOME}/webapps/ROOT \
               ${CATALINA_HOME}/webapps/examples \
               ${CATALINA_HOME}/webapps/docs \
               ${CATALINA_HOME}/webapps/host-manager \
               ${CATALINA_HOME}/webapps/manager

ENV PATH=${CATALINA_HOME}/bin:${PATH}

# Create non-root user for security
RUN addgroup -S modresorts && adduser -S modresorts -G modresorts \
    && chown -R modresorts:modresorts ${CATALINA_HOME}

# Copy the built WAR from builder stage into Tomcat webapps as ROOT
COPY --from=builder --chown=modresorts:modresorts /workspace/target/modresorts-2.0.0.war \
     ${CATALINA_HOME}/webapps/ROOT.war

# Switch to non-root user
USER modresorts

# Application port (Tomcat HTTP)
EXPOSE 9080

# JVM options for container awareness
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
ENV TZ=UTC
ENV CATALINA_OPTS="${JAVA_OPTS}"

# Configure Tomcat to listen on port 9080
RUN sed -i 's/port="8080"/port="9080"/' ${CATALINA_HOME}/conf/server.xml

# Start Tomcat in foreground
CMD ["catalina.sh", "run"]
