# NextSkip

[![CI](https://github.com/arunderwood/nextskip/actions/workflows/ci.yml/badge.svg)](https://github.com/arunderwood/nextskip/actions/workflows/ci.yml)
![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen)
![Vaadin Hilla](https://img.shields.io/badge/Vaadin%20Hilla-25.0-blue)

<p align="center">
  <img src="src/main/resources/META-INF/resources/og-image.svg" alt="NextSkip" width="400">
</p>

<h3 align="center">Find your next skip</h3>

<p align="center">
  <strong><a href="https://nextskip.io">Try NextSkip</a></strong>
</p>

---

Amateur radio has many activities—DX chasing, POTA/SOTA activations, contesting, satellite work, meteor scatter, and more. Each has conditions that make it an optimal time to engage. NextSkip aggregates real-time data, scores current conditions, and surfaces the best opportunities so you know where to spend your time on the air.

## Table of Contents

- [How It Works](#how-it-works)
- [Activity Coverage](#activity-coverage)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Testing](#testing)
- [Architecture](#architecture)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Troubleshooting](#troubleshooting)
- [Future Enhancements](#future-enhancements)
- [Data Attribution](#data-attribution)

---

## How It Works

1. **Data Aggregation**: Backend modules poll feeds for each activity (NOAA for propagation, POTA API for park activations, contest calendars, etc.)
2. **Condition Scoring**: Each module calculates a score (0-100) based on current conditions
3. **Score Ranking**: Cards are arranged in an activity grid with highest-scored activities in the top-left position
4. **Hotness Indicators**: Cards display "hot," "warm," "neutral," or "cool" styling based on their scores

<details>
<summary><strong>Glossary</strong></summary>

| Term           | Definition                                                                                |
| -------------- | ----------------------------------------------------------------------------------------- |
| **Activity**   | A category of amateur radio pursuit that NextSkip tracks (DX, POTA, contesting, etc.)         |
| **Conditions** | The current state of an activity - whether it's a good time to engage                     |
| **Score**      | A numerical rating (0-100) representing how favorable conditions are                      |
| **Hotness**    | The visual tier derived from score: hot (70+), warm (45-69), neutral (20-44), cool (0-19) |
| **Card**       | A UI component displaying one activity's current conditions                               |
| **Module**     | A backend package that fetches and scores one activity's data                             |
| **Feed**       | An external data source that provides activity information                                |
| **FeedClient** | A component that fetches data from a Feed with circuit breaker and retry resilience       |
| **LoadingCache** | A Caffeine cache backed by database queries for fast read access                        |
| **RefreshTask** | A db-scheduler recurring task that coordinates: Feed fetch → DB persist → cache refresh  |

</details>

## Activity Coverage

NextSkip focuses on activities with **machine-readable, computable, or predictable conditions**:

| Activity       | Data Sources            | Status      |
| -------------- | ----------------------- | ----------- |
| HF Propagation | NOAA SWPC, HamQSL       | Live        |
| POTA/SOTA      | POTA API, SOTA API      | Live        |
| Contests       | Contest calendars       | Live        |
| Meteor Showers | Astronomical data       | Live        |
| Satellites     | Orbital prediction APIs | Coming soon |
| Band Activity  | PSKReporter, RBN        | Coming soon |

The platform is actively expanding to cover more activities and data sources.

## Features

- **Activity Scoring**: Each activity gets a 0-100 score based on current conditions
- **Hotness Ranking**: Cards automatically arrange by score - best opportunities float to top
- **Multi-source Feeds**: Data from NOAA SWPC, HamQSL, POTA API, and more
- **Real-time Conditions**: Solar indices (SFI, K-Index, A-Index) and HF band forecasts (160m-6m)
- **Resilient Architecture**: Circuit breakers, retry logic, and graceful fallbacks
- **Activity Grid UI**: Score-sorted card layout with hot/warm/neutral/cool visual indicators
- **Responsive Design**: Mobile-first design with 4-column desktop → 2-column tablet → 1-column mobile layout

## Tech Stack

### Backend

- Java 25
- Spring Boot 4.0
- Vaadin Hilla 25.0 (React integration)
- Resilience4j (circuit breakers, retry)
- Caffeine (caching)
- PostgreSQL (persistence)

### Frontend

- React 19
- TypeScript
- Vaadin Hilla (type-safe RPC)
- Vite
- Vitest + React Testing Library

### Observability

- OpenTelemetry (distributed tracing)
- Pyroscope (continuous profiling)
- PostHog (product analytics)
- Spring Boot Actuator (health & metrics)

### Build & Quality

- Gradle
- Checkstyle, PMD, SpotBugs
- JaCoCo (75% instruction, 65% branch coverage)

## Quick Start

### Prerequisites

- Java (see `.tool-versions` for exact version)
- Node.js (see `.tool-versions` for exact version)
- Docker (for local PostgreSQL, see [docs/DATABASE.md](docs/DATABASE.md))

### Build and Run

```bash
# Start PostgreSQL (first time)
docker-compose up -d

# Build the project
./gradlew build

# Start the application
./gradlew bootRun
```

The application will start on http://localhost:8080

### Development Mode

Vaadin's development mode is enabled by default and provides:

- Hot reload for frontend changes
- Development tools overlay
- Source maps for debugging

## Testing

<details>
<summary><strong>View testing details</strong></summary>

### Backend Tests

Comprehensive JUnit test suite covering utilities, external API clients, services, and integration tests.

```bash
./gradlew test
```

**Test Reports**: `build/reports/tests/test/index.html`

### Frontend Tests

Comprehensive Vitest test suite covering components, accessibility (WCAG 2.1 AA), and user interactions.

```bash
# Watch mode
npm test

# Run once
npm run test:run

# With coverage
npm run test:coverage
```

**Coverage Reports**: `coverage/index.html`

### E2E Tests

Playwright tests validating complete user workflows in the browser.

```bash
# Run E2E tests
npm run e2e

# Run with Playwright UI (for debugging)
npm run e2e:ui

# Run in headed mode (see browser)
npm run e2e:headed
```

**Test Suite**: Tests in `src/test/e2e/` covering dashboard loading, rendering, and user interactions.

**Configuration**: See `playwright.config.ts`

</details>

## Architecture

NextSkip uses a modular monolith structure with clean module boundaries:

- **common**: Shared domain models and utilities
- **propagation**: Solar indices and HF band conditions
  - `api`: Public endpoints (@BrowserCallable)
  - `model`: Domain models
  - `internal`: Service implementations and external clients

### Resilience Patterns

- Circuit Breakers: Fail fast when external services are down
- Retry Logic: Automatic retries with exponential backoff
- Caching: Reduce API calls and improve responsiveness

## Configuration

Edit `src/main/resources/application.yml` for configuration.

Default settings:

- Server port: 8080
- Cache: 30 minute expiry
- Circuit breakers: 50% failure threshold

For development-specific settings, create `src/main/resources/application-dev.yml`:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Production Build

```bash
# Set production mode in build.gradle
vaadin {
    productionMode = true
}

# Build
./gradlew build

# Run the JAR
java -jar build/libs/nextskip-0.0.1-SNAPSHOT.jar
```

## Troubleshooting

<details>
<summary><strong>View troubleshooting tips</strong></summary>

**Port 8080 Already in Use**

```bash
lsof -ti :8080 | xargs kill -9
```

**Frontend Build Fails**

```bash
./gradlew clean vaadinBuildFrontend
```

**Test Failures**

Ensure you have the correct Java version:

```bash
java -version  # Should match version in .tool-versions
```

</details>

## API Endpoints

### Hilla TypeScript Endpoints

Hilla auto-generates type-safe TypeScript clients from Java `@BrowserCallable` endpoints.

**Available Endpoints**:

| Endpoint               | Purpose                            |
| ---------------------- | ---------------------------------- |
| `PropagationEndpoint`  | Solar indices and band conditions  |
| `ActivationsEndpoint`  | POTA/SOTA activations              |
| `ContestEndpoint`      | Contest calendar                   |
| `MeteorEndpoint`       | Meteor shower predictions          |

Generated clients: `src/main/frontend/generated/`

### Actuator Endpoints

| Endpoint                              | Purpose          |
| ------------------------------------- | ---------------- |
| `http://localhost:8080/actuator/health` | Health check     |
| `http://localhost:8080/actuator/info`   | Application info |

## Future Enhancements

- Grid square-based propagation predictions
- Custom alerts and notifications
- Historical data analysis
- Additional data sources integration

See GitHub issues for planned features.

## Data Attribution

| Source | Data Provided | Link |
| ------ | ------------- | ---- |
| NOAA SWPC | Solar indices (SFI, K-Index, A-Index, Sunspot Number) | [swpc.noaa.gov](https://www.swpc.noaa.gov/) |
| HamQSL | Band conditions and solar data | [hamqsl.com](http://www.hamqsl.com/solar.html) |
| POTA API | Parks on the Air activations | [pota.app](https://pota.app/) |
| SOTA API | Summits on the Air activations | [sota.org.uk](https://www.sota.org.uk/) |
| WA7BNM | Contest calendar | [contestcalendar.com](https://www.contestcalendar.com/) |
| IMO | Meteor shower data | [imo.net](https://www.imo.net/) |
