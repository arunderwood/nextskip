# Stage 1: Build the application
FROM eclipse-temurin:25-jdk AS builder

# Install Node.js 24.x (required for Vaadin Hilla frontend build)
RUN apt-get update && apt-get install -y curl \
    && curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Gradle wrapper and build files first (better layer caching)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

# Copy source code
COPY src/ src/
COPY package.json package-lock.json ./
COPY tsconfig.json vite.config.ts types.d.ts .env.production ./
COPY config/ config/

# Build the application (includes frontend via vaadinBuildFrontend)
RUN chmod +x gradlew && ./gradlew bootJar -Pvaadin.productionMode=true --no-daemon

# Stage 2: Runtime image (distroless)
FROM gcr.io/distroless/java25-debian13

WORKDIR /app

# Copy the built JAR and agents from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/build/otel-agent/grafana-opentelemetry-java.jar otel-agent.jar
COPY --from=builder /app/build/pyroscope-agent/pyroscope.jar pyroscope-agent.jar

# Distroless runs as non-root by default (uid 65532)
# JVM memory optimizations for 512MB container (distroless doesn't support JAVA_OPTS)
# - MaxRAMPercentage=50.0: ~256MB heap leaves room for metaspace + agents
# - MaxMetaspaceSize=160m: Cap metaspace growth (baseline ~152MB)
# - ReservedCodeCacheSize=64m: Limit CodeHeap (baseline ~36MB)
# - UseSerialGC: Lower overhead than G1 for small heaps
# - Xss256k: Reduce thread stack from 1MB to 256KB
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=50.0 \
  -XX:MaxMetaspaceSize=160m \
  -XX:ReservedCodeCacheSize=64m \
  -XX:+UseSerialGC \
  -Xss256k"

EXPOSE 8080

# Distroless has no shell, so use exec form
# OTEL: disable with OTEL_JAVAAGENT_ENABLED=false
# Pyroscope: disable by not setting PYROSCOPE_SERVER_ADDRESS
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-javaagent:/app/pyroscope-agent.jar", "-jar", "app.jar"]
