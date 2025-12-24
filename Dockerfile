# Stage 1: Build the application
FROM eclipse-temurin:25.0.1_8-jdk AS builder

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
COPY tsconfig.json vite.config.ts types.d.ts ./
COPY config/ config/

# Build the application (includes frontend via vaadinBuildFrontend)
RUN chmod +x gradlew && ./gradlew bootJar -Pvaadin.productionMode=true --no-daemon

# Stage 2: Runtime image (distroless)
FROM gcr.io/distroless/java25-debian13

WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Distroless runs as non-root by default (uid 65532)
# Spring Boot optimizations via JAVA_TOOL_OPTIONS (distroless doesn't support JAVA_OPTS)
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

# Distroless has no shell, so use exec form
# Note: Health checks won't work in distroless (no curl), rely on orchestrator health checks
ENTRYPOINT ["java", "-jar", "app.jar"]
