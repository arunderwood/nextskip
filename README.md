# NextSkip

## Overview

**NextSkip helps you find your next skip.**

Amateur radio has many activities—DX chasing, POTA/SOTA activations, contesting, satellite work, meteor scatter, and more. Each has conditions that make it an optimal time to engage. NextSkip aggregates real-time data, scores current conditions, and surfaces the best opportunities so you know where to spend your time on the air.

### How It Works

1. **Data Aggregation**: Backend modules poll feeds for each activity (NOAA for propagation, POTA API for park activations, contest calendars, etc.)
2. **Condition Scoring**: Each module calculates a score (0-100) based on current conditions
3. **Score Ranking**: Cards are arranged in an activity grid with highest-scored activities in the top-left position
4. **Hotness Indicators**: Cards display "hot," "warm," "neutral," or "cool" styling based on their scores

### Glossary

| Term           | Definition                                                                                |
| -------------- | ----------------------------------------------------------------------------------------- |
| **Activity**   | A category of amateur radio pursuit that NextSkip tracks (DX, POTA, contesting, etc.)         |
| **Conditions** | The current state of an activity - whether it's a good time to engage                     |
| **Score**      | A numerical rating (0-100) representing how favorable conditions are                      |
| **Hotness**    | The visual tier derived from score: hot (70+), warm (45-69), neutral (20-44), cool (0-19) |
| **Card**       | A UI component displaying one activity's current conditions                               |
| **Module**     | A backend package that fetches and scores one activity's data                             |
| **Feed**       | An external data source that provides activity information                                |

### Activity Coverage

NextSkip focuses on activities with **machine-readable, computable, or predictable conditions**:

| Activity       | Data Sources            |
| -------------- | ----------------------- |
| HF Propagation | NOAA SWPC, HamQSL       |
| POTA/SOTA      | POTA API, SOTA API      |
| Contests       | Contest calendars       |
| Meteor Showers | Astronomical data       |
| Satellites     | Orbital prediction APIs |
| Band Activity  | PSKReporter, RBN        |

The platform is actively expanding to cover more activities and data sources.

Built with Spring Boot, Vaadin Hilla, and React.

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

- Java
- Spring Boot
- Vaadin Hilla (React integration)
- Resilience4j (circuit breakers, retry)
- Caffeine (caching)

### Frontend

- React
- TypeScript
- Vaadin Hilla (type-safe RPC)
- Vite
- Vitest + React Testing Library

### Build

- Gradle
- Java Toolchain

## Prerequisites

- Java (see `.tool-versions` for specific version)
- Node.js (see `.tool-versions` for specific version)
- Internet connection (for fetching propagation data)

## Quick Start

### Build and Run

```bash
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

### Backend Tests

60 JUnit tests covering utilities, external API clients, and services.

```bash
./gradlew test
```

**Test Reports**: `build/reports/tests/test/index.html`

### Frontend Tests

90 Vitest tests covering components, accessibility, and user interactions.

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

## Architecture

NextSkip uses a modular monolith structure with clean module boundaries:

- **common**: Shared domain models and utilities
- **propagation**: Solar indices and HF band conditions
  - `api`: Public endpoints (@BrowserCallable)
  - `model`: Domain models
  - `internal`: Service implementations and external clients

### Data Sources

**NOAA Space Weather Prediction Center**

- Solar Flux Index, K-Index, A-Index, Sunspot Number
- Cache TTL: 5 minutes

**HamQSL.com Solar XML Feed**

- Comprehensive solar data and band-by-band conditions
- Cache TTL: 30 minutes

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

## API Endpoints

### Hilla TypeScript Endpoints

Hilla auto-generates type-safe TypeScript clients from Java @BrowserCallable endpoints.

Generated at: `src/main/frontend/generated/PropagationEndpoint.ts`

Methods:

- `getPropagationData()`: Complete propagation snapshot
- `getSolarIndices()`: Solar indices only
- `getBandConditions()`: Band conditions only

### Actuator Endpoints

- `http://localhost:8080/actuator/health`: Health check
- `http://localhost:8080/actuator/info`: Application info

## Future Enhancements

- Grid square-based propagation predictions
- Custom alerts and notifications
- Historical data analysis
- Additional data sources integration

See `nextskip-project-plan.md` for detailed roadmap.

## Data Attribution

- NOAA Space Weather Prediction Center: https://www.swpc.noaa.gov/
- HamQSL.com Solar Data: http://www.hamqsl.com/solar.html
