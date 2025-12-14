FROM eclipse-temurin:25-jre

# Create non-root user for security
RUN groupadd -r nextskip && useradd -r -g nextskip nextskip

WORKDIR /app

# Copy the pre-built JAR (built by Gradle in CI)
COPY build/libs/*.jar app.jar

# Set ownership
RUN chown -R nextskip:nextskip /app

USER nextskip

# Spring Boot optimizations
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
