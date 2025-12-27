# Observability

Auto-instrumented via [Grafana OTEL Java agent](https://grafana.com/docs/opentelemetry/instrument/grafana-java/).

## Configuration

Get credentials: [Grafana Cloud Portal](https://grafana.com/docs/grafana-cloud/send-data/otlp/send-data-otlp/) → Your Stack → OpenTelemetry → Configure

| Variable                      | Value                               |
| ----------------------------- | ----------------------------------- |
| `OTEL_SERVICE_NAME`           | `nextskip`                          |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `<from portal>`                     |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `http/protobuf`                     |
| `OTEL_EXPORTER_OTLP_HEADERS`  | `Authorization=Basic <from portal>` |

Disable agent: `OTEL_JAVAAGENT_ENABLED=false`

Agent version: `gradle/libs.versions.toml` → `grafana-otel-agent`

## Continuous Profiling (Pyroscope)

Continuous profiling via [Grafana Pyroscope Java agent](https://grafana.com/docs/pyroscope/latest/configure-client/language-sdks/java/).

### Configuration

Get credentials: [Grafana Cloud Portal](https://grafana.com/products/cloud/) → Your Stack → Profiles → Configure

| Variable                        | Value                                                 |
| ------------------------------- | ----------------------------------------------------- |
| `PYROSCOPE_APPLICATION_NAME`    | `nextskip`                                            |
| `PYROSCOPE_SERVER_ADDRESS`      | `https://profiles-prod-XXX.grafana.net` (from portal) |
| `PYROSCOPE_BASIC_AUTH_USER`     | `<instance-id>` (from portal)                         |
| `PYROSCOPE_BASIC_AUTH_PASSWORD` | `<api-key>` (from portal)                             |

### Full Profiling (CPU + Allocation + Lock)

| Variable                   | Value    | Description                        |
| -------------------------- | -------- | ---------------------------------- |
| `PYROSCOPE_FORMAT`         | `jfr`    | JFR format for multi-event support |
| `PYROSCOPE_PROFILER_EVENT` | `itimer` | CPU profiling event type           |
| `PYROSCOPE_PROFILER_ALLOC` | `512k`   | Allocation profiling threshold     |
| `PYROSCOPE_PROFILER_LOCK`  | `10ms`   | Lock contention threshold          |

Disable: Omit `PYROSCOPE_SERVER_ADDRESS` (agent gracefully handles missing config).

Agent version: `gradle/libs.versions.toml` → `pyroscope-agent`

## Frontend

Uses [Grafana Faro React SDK](https://grafana.com/docs/grafana-cloud/monitor-applications/frontend-observability/) for browser telemetry with React Router integration.

### Setup

1. Create app: Grafana Cloud → **Frontend Observability** → Create Application
2. Set CORS origin: production domain (e.g., `nextskip.io`)
3. Copy collector URL

### Configuration

| Variable                  | Value                                                           |
| ------------------------- | --------------------------------------------------------------- |
| `VITE_FARO_COLLECTOR_URL` | `https://faro-collector-{region}.grafana.net/collect/{app-key}` |
| `VITE_TRACE_CORS_URLS`    | Comma-separated regex patterns for trace propagation            |
| `VITE_APP_VERSION`        | Optional, e.g., `1.0.0`                                         |

Omit `VITE_FARO_COLLECTOR_URL` to disable (local dev).

### Trace Propagation

`VITE_TRACE_CORS_URLS` configures which API requests receive `traceparent` headers for frontend-backend trace linking.

Examples:

- Production: `nextskip\.io`
- Multiple environments: `nextskip\.io,staging\.nextskip\.com`

Note: Use `\\.` for literal dots (parsed as regex patterns).

### Production Build

Frontend env vars must be available at **Vite build time** (not runtime). Configure in `.env.production`:

```
VITE_FARO_COLLECTOR_URL=https://faro-collector-{region}.grafana.net/collect/{app-key}
VITE_TRACE_CORS_URLS=nextskip\.io
```

The Dockerfile copies this file during the build stage so Vite can inline the values.

> **TODO:** This is a workaround for Render not passing environment variables to Docker builds. Ideally, these values would come from Render's build-time env vars rather than being committed to the repo. Investigate Render's Docker build arg support or alternative deployment platforms.

### Local Development

Create `.env.local`:

```
VITE_FARO_COLLECTOR_URL=https://faro-collector-us-central-0.grafana.net/collect/your-app-key
VITE_TRACE_CORS_URLS=localhost:8080
```
