# ==========================================
# Payment Service - Multi-stage Dockerfile
# ==========================================
# Stage 1: Build with Maven
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy parent pom and common-lib first for layer caching
COPY pom.xml ./pom.xml
COPY common-lib ./common-lib
COPY payment-service ./payment-service

# Build common-lib first, then payment-service
RUN apk add --no-cache maven && \
    mvn -pl common-lib install -DskipTests -q && \
    mvn -pl payment-service package -DskipTests -q -am

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="Payment Gateway Team"
LABEL service="payment-service"

# Security: run as non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/payment-service/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

# JVM tuning for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
