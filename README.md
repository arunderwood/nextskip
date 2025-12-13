# NextSkip

Ham Radio Propagation Dashboard - Real-time HF band conditions and solar indices for amateur radio operators.

## Overview

NextSkip aggregates propagation data from multiple authoritative sources to provide ham radio operators with current HF band conditions, solar indices, and propagation forecasts. Built with Spring Boot 3.4, Vaadin Hilla, and React 19.

## Features

- **Real-time Solar Indices**: Solar Flux Index (SFI), K-Index, A-Index, Sunspot Number
- **HF Band Conditions**: Propagation forecasts for 160m through 6m bands
- **Multi-source Aggregation**: Data from NOAA SWPC and HamQSL.com
- **Resilient Architecture**: Circuit breakers, retry logic, and graceful fallbacks
- **Modern UI**: Responsive React dashboard with live updates

## Tech Stack

### Backend
- Java 25 (targeting Java 21 bytecode)
- Spring Boot 3.4.0
- Spring MVC + WebFlux (for WebClient)
- Vaadin Hilla 24.9.7 (React integration)
- Resilience4j (circuit breakers, retry)
- Caffeine (caching)
- JUnit 5, Mockito, WireMock

### Frontend
- React 19
- TypeScript
- Vaadin Hilla (type-safe RPC)
- Vite 6.4.1

### Build
- Gradle 9.2.1
- Java Toolchain 25

## Prerequisites

- Java 25 (Amazon Corretto 25.0.1.8.1 recommended)
- Internet connection (for fetching propagation data)

## Building

### Run Tests
```bash
./gradlew test
```

### Build Application
```bash
./gradlew build
```

### Build Frontend Only
```bash
./gradlew vaadinBuildFrontend
```

## Running

### Start Application
```bash
./gradlew bootRun
```

The application will start on http://localhost:8080

### Development Mode
Vaadin's development mode enables:
- Hot reload for frontend changes
- Development tools overlay
- Source maps for debugging

Development mode is enabled by default (`vaadin.productionMode = false` in build.gradle).

### Production Build
To build for production:

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

## Architecture

### Modules

- **common**: Shared domain models (Coordinates, GridSquare, FrequencyBand) and utilities
- **propagation**: Core propagation data module
  - `api`: Hilla endpoints (@BrowserCallable)
  - `model`: Domain models (SolarIndices, BandCondition)
  - `internal`: Service implementations and external clients

### Data Sources

1. **NOAA Space Weather Prediction Center**
   - Solar Flux Index
   - K-Index, A-Index
   - Sunspot Number
   - Cache TTL: 5 minutes

2. **HamQSL.com Solar XML Feed**
   - Comprehensive solar data
   - Band-by-band conditions
   - Cache TTL: 30 minutes

### Resilience Patterns

- **Circuit Breakers**: Fail fast when external services are down
- **Retry Logic**: Automatic retries with exponential backoff
- **Fallback Strategy**: Degrade gracefully to cached or alternative data
- **Caching**: Reduce external API calls and improve responsiveness

## API Endpoints

### Hilla TypeScript Endpoints
Hilla auto-generates type-safe TypeScript clients from Java @BrowserCallable endpoints.

Generated at: `src/main/frontend/generated/PropagationEndpoint.ts`

Methods:
- `getPropagationData()`: Returns complete propagation snapshot
- `getSolarIndices()`: Returns solar indices only
- `getBandConditions()`: Returns band conditions only

### Actuator Endpoints
- `http://localhost:8080/actuator/health`: Health check
- `http://localhost:8080/actuator/info`: Application info

## Configuration

### Application Properties
Edit `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: nextskip
  cache:
    caffeine:
      spec: maximumSize=100,expireAfterWrite=30m

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
```

### Development Profile
Use `src/main/resources/application-dev.yml` for dev-specific settings:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Testing

### Unit Tests
60 comprehensive tests covering:
- Utility functions (callsign validation, grid square conversion)
- External API clients (NOAA, HamQSL) with WireMock
- Service layer with Mockito
- Integration tests for Hilla endpoints

```bash
./gradlew test
```

### Test Reports
After running tests: `build/reports/tests/test/index.html`

## Known Limitations

- **Java 25 Compatibility**: Using ByteBuddy experimental mode for Mockito (see build.gradle)
- **Gradle 9 Compatibility**: Vaadin 24.9.7 supports Gradle 9.2.1
- **Java 21 Target**: Compiling to Java 21 bytecode for Spring Boot 3.4 compatibility

## Troubleshooting

### Port 8080 Already in Use
```bash
# Find and kill process
lsof -ti :8080 | xargs kill -9
```

### Frontend Build Fails
```bash
# Clean and rebuild
./gradlew clean vaadinBuildFrontend
```

### Test Failures
Ensure you have the correct Java version:
```bash
java -version  # Should show Java 25
```

## Future Enhancements

- Grid square-based propagation predictions
- Custom alerts and notifications
- Historical data analysis
- Mobile-responsive design improvements
- Additional data sources integration


## Data Attribution

- NOAA Space Weather Prediction Center: https://www.swpc.noaa.gov/
- HamQSL.com Solar Data: http://www.hamqsl.com/solar.html
