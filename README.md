# NextSkip

Ham Radio Propagation Dashboard - Real-time HF band conditions and solar indices for amateur radio operators.

## Overview

NextSkip aggregates propagation data from multiple authoritative sources to provide ham radio operators with current HF band conditions, solar indices, and propagation forecasts. Built with Spring Boot 3.4, Vaadin Hilla, and React 19.

## Features

- **Real-time Solar Indices**: Solar Flux Index (SFI), K-Index, A-Index, Sunspot Number
- **HF Band Conditions**: Propagation forecasts for 160m through 6m bands
- **Multi-source Aggregation**: Data from NOAA SWPC and HamQSL.com
- **Resilient Architecture**: Circuit breakers, retry logic, and graceful fallbacks
- **Bento Grid UI**: Priority-based card layout showing "hottest" (most favorable) conditions first
- **Responsive Design**: Mobile-first design with 4-column desktop → 2-column tablet → 1-column mobile layout

## Tech Stack

### Backend
- Java 25 (targeting Java 21 bytecode)
- Spring Boot 3.4.0
- Vaadin Hilla 24.9.7 (React integration)
- Resilience4j (circuit breakers, retry)
- Caffeine (caching)

### Frontend
- React 19
- TypeScript
- Vaadin Hilla (type-safe RPC)
- Vite 6.4.1
- Vitest + React Testing Library

### Build
- Gradle 9.2.1
- Java Toolchain 25

## Prerequisites

- Java 25 (Amazon Corretto 25.0.1.8.1 recommended)
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
Ensure you have Java 25:
```bash
java -version  # Should show Java 25
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
