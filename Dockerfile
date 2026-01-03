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
COPY build.gradle.kts settings.gradle.kts ./

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
# JVM settings for 2GB container (distroless doesn't support JAVA_OPTS)
# - MaxRAMPercentage=60.0: ~1.2GB heap with room for metaspace + agents
# - MaxMetaspaceSize=256m: Headroom for Hibernate/Spring class loading
# - ReservedCodeCacheSize=128m: Room for JIT compilation
# - G1GC: Better throughput than SerialGC for larger heaps
# - Xss512k: Comfortable thread stack size
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=60.0 \
  -XX:MaxMetaspaceSize=256m \
  -XX:ReservedCodeCacheSize=128m \
  -XX:+UseG1GC \
  -Xss512k"

EXPOSE 8080

# Distroless has no shell, so use exec form
# OTEL: disable with OTEL_JAVAAGENT_ENABLED=false
# Pyroscope: disable by not setting PYROSCOPE_SERVER_ADDRESS
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-javaagent:/app/pyroscope-agent.jar", "-jar", "app.jar"]
