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

### Local Development

Create `.env.local`:

```
VITE_FARO_COLLECTOR_URL=https://faro-collector-us-central-0.grafana.net/collect/your-app-key
VITE_TRACE_CORS_URLS=localhost:8080
```
