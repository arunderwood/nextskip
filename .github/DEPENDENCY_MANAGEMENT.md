# Dependency Management

**Renovate** handles Gradle and npm dependencies. **Dependabot** handles GitHub Actions only.

Docker images are manually managed.

## Vaadin/Hilla Updates

When `com.vaadin` packages update, Renovate runs:

```bash
install-tool java 25.0.1+8
install-tool node 24.12.0
./gradlew vaadinPrepareFrontend --no-daemon
npm install --package-lock-only
```

This syncs npm `package.json` and `package-lock.json` with Gradle-managed Vaadin versions.

**Ignored npm packages** (controlled by Vaadin Gradle plugin):

- `@vaadin/*`, `react*`, `lit`, `vite`, `typescript`, `workbox-*`

## Observability Agents

Renovate tracks these Java agents via custom regex managers:

| Agent        | Version Catalog Key  | GitHub Repo                          |
| ------------ | -------------------- | ------------------------------------ |
| Grafana OTEL | `grafana-otel-agent` | `grafana/grafana-opentelemetry-java` |
| Pyroscope    | `pyroscope-agent`    | `grafana/pyroscope-java`             |

Both are downloaded at build time by Gradle tasks (`downloadOtelAgent`, `downloadPyroscopeAgent`) and included in the Docker image.

## Gradle Dependency Locking

Enabled via `dependencyLocking { lockAllConfigurations() }` in `build.gradle`.

The `gradle.lockfile` is required for Renovate to detect changes and trigger postUpgradeTasks.

## Renovate PAT Permissions

Fine-grained token scoped to this repo with:

- Contents: Read and write
- Pull requests: Read and write
- Commit statuses: Read and write

## Security

Workflow uses `schedule` and `workflow_dispatch` triggers only - executes code from `main` branch, never from PRs.
