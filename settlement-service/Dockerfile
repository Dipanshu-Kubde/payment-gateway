# ============================================================================
# Settlement Service - Docker Image
# ============================================================================
# Multi-stage is not needed since we build with Maven externally.
# Uses Alpine-based JRE for minimal image size (~180MB).
# ============================================================================

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the fat JAR produced by spring-boot-maven-plugin
COPY target/*.jar app.jar

EXPOSE 8085

# JVM tuning: container-aware memory settings
ENTRYPOINT ["java", "-jar", "app.jar"]
