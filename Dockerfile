FROM eclipse-temurin:21.0.10_7-jdk-jammy AS builder
LABEL authors="yuwatinyi"

WORKDIR /build

# Copy Gradle wrapper and config files
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source and build
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Extract layers
RUN java -Djarmode=layertools -jar build/libs/*.jar extract --destination /extracted

# === Runtime Stage ===
FROM eclipse-temurin:21.0.10_7-jre-jammy

# Create a non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser

WORKDIR /app

# Copy the extracted layers in order of change frequency
# Dependencies change least often, application code changes most often
COPY --from=builder /extracted/dependencies/ ./
COPY --from=builder /extracted/spring-boot-loader/ ./
COPY --from=builder /extracted/snapshot-dependencies/ ./
COPY --from=builder /extracted/application/ ./

# Switch to non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Health check that tests the application's health endpoint
HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=30s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM flags optimized for containers
# -XX:+UseContainerSupport enables container-aware memory settings
# -XX:MaxRAMPercentage sets heap as a percentage of container memory limit
# -XX:+UseG1GC uses the G1 garbage collector for balanced throughput and latency
ENV JAVA_OPTS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:+UseG1GC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]