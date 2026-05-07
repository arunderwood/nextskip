# Runtime image. The JAR and observability agents are produced by the
# `backend` CI job and pulled into the build context via download-artifact;
# locally, run `./gradlew bootJar` (which depends on downloadOtelAgent and
# downloadPyroscopeAgent) before `docker build .`.
FROM gcr.io/distroless/java25-debian13

WORKDIR /app

COPY build/libs/*.jar app.jar
COPY build/otel-agent/grafana-opentelemetry-java.jar otel-agent.jar
COPY build/pyroscope-agent/pyroscope.jar pyroscope-agent.jar
COPY config/otel-agent.properties otel-agent.properties

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
# OTEL: non-secret config in otel-agent.properties, secrets via Render env vars
# Pyroscope: non-secret config in otel-agent.properties, secrets via Render env vars
ENTRYPOINT ["java", "-javaagent:/app/otel-agent.jar", "-Dotel.javaagent.configuration-file=/app/otel-agent.properties", "-javaagent:/app/pyroscope-agent.jar", "-jar", "app.jar"]
