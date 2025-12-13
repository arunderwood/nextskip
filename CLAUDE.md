# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NextSkip is a ham radio propagation dashboard providing real-time HF band conditions and solar indices for amateur radio operators. Built as a modular monolith using Spring Boot 3.4, Vaadin Hilla 24.9.7, and React 19.

**Current Status**: Phase 1 complete - Propagation module with NOAA SWPC and HamQSL integration.

## Build & Development Commands

### Essential Commands

```bash
# Run all tests (expect 60 passing)
./gradlew test

# Build entire project
./gradlew build

# Start application (http://localhost:8080)
./gradlew bootRun

# Build frontend only
./gradlew vaadinBuildFrontend

# Clean build artifacts
./gradlew clean
```

### Running Specific Tests

```bash
# Run single test class
./gradlew test --tests NoaaSwpcClientTest

# Run single test method
./gradlew test --tests NoaaSwpcClientTest.testFetchSolarIndices_Success

# Run tests with debug output
./gradlew test --info
```

### Port 8080 Conflicts

If port 8080 is already in use:
```bash
lsof -ti :8080 | xargs kill -9
```

### Quality Checks

```bash
# Run all quality checks (Checkstyle, PMD, SpotBugs, JaCoCo)
./gradlew check

# Individual quality tools
./gradlew checkstyleMain checkstyleTest  # Code style
./gradlew pmdMain pmdTest                # Programming errors
./gradlew spotbugsMain spotbugsTest      # Bug detection
./gradlew jacocoTestReport               # Coverage report (80% target)
./gradlew pitest                         # Mutation testing

# View reports
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
```

## Architecture

### Modular Monolith Design

NextSkip uses a modular monolith structure designed for future microservices extraction. Each module has:
- Clean public API (Java interface in `api/` package)
- Internal implementations (`internal/` package - not exposed)
- Module-specific models (`model/` package)
- Isolated external dependencies

**Current Modules:**
- **common**: Shared domain models (Coordinates, GridSquare, FrequencyBand) and utilities
- **propagation**: Solar indices and HF band conditions (Phase 1 - COMPLETE)

**Planned Modules** (see nextskip-project-plan.md):
- **spots**: Real-time HF activity (PSKReporter MQTT, RBN)
- **activations**: POTA/SOTA portable operations
- **satellites**: Amateur satellite pass predictions
- **contests**: Contest calendar
- **ai**: Spring AI conversational assistant

### Package Structure

```
io.nextskip/
├── common/
│   ├── config/          # Shared configs (Cache, Async, Resilience)
│   ├── model/           # Domain models (Coordinates, GridSquare, FrequencyBand)
│   └── util/            # Utilities (HamRadioUtils for callsign/grid validation)
│
├── propagation/         # Phase 1 module
│   ├── api/            # Public interface
│   │   ├── PropagationService.java      # Service contract
│   │   ├── PropagationEndpoint.java     # Vaadin Hilla @BrowserCallable endpoint
│   │   └── PropagationResponse.java     # DTO for frontend
│   ├── internal/       # Implementation details (not exposed)
│   │   ├── PropagationServiceImpl.java  # Service implementation
│   │   ├── NoaaSwpcClient.java          # NOAA API client
│   │   ├── HamQslClient.java            # HamQSL XML parser
│   │   ├── NoaaSolarCycleEntry.java     # Type-safe DTO for NOAA
│   │   ├── ExternalApiException.java    # Custom exception hierarchy
│   │   └── InvalidApiResponseException.java
│   ├── model/          # Domain models
│   │   ├── SolarIndices.java            # Solar data (SFI, K-index, A-index, sunspots)
│   │   ├── BandCondition.java           # Band propagation forecast
│   │   └── BandConditionRating.java     # GOOD/FAIR/POOR/UNKNOWN
│   └── PropagationModule.java           # Module marker
│
├── config/
│   └── WebClientConfig.java             # Global WebClient config
│
└── NextSkipApplication.java             # Spring Boot main
```

### Key Design Patterns

#### 1. Resilience Pattern (Circuit Breaker + Retry + Cache Fallback)

Every external API client follows this pattern:

```java
@CircuitBreaker(name = "noaa", fallbackMethod = "getCachedIndices")
@Retry(name = "noaa")
@Cacheable(value = "solarIndices", key = "'latest'", unless = "#result == null")
public SolarIndices fetchSolarIndices() {
    // HTTP call to external API
}

private SolarIndices getCachedIndices(Exception e) {
    // Fallback to cached data when circuit is open
    return cacheManager.getCache("solarIndices").get("latest", SolarIndices.class);
}
```

**Configuration** in `application.yml`:
- Circuit breaker: 50% failure rate threshold, 60s open state
- Retry: 3 attempts, 500ms wait between attempts
- Cache: Caffeine with 10m expiry

#### 2. Type-Safe DTOs with Validation

All external API responses use Java records with built-in validation:

```java
@JsonIgnoreProperties(ignoreUnknown = true)
record NoaaSolarCycleEntry(
    @JsonProperty("time-tag") String timeTag,
    @JsonProperty("f10.7") Double solarFlux,
    @JsonProperty("ssn") Integer sunspotNumber
) {
    public void validate() {
        if (solarFlux < 0 || solarFlux > 1000) {
            throw new InvalidApiResponseException("NOAA",
                "Solar flux out of range: " + solarFlux);
        }
        // More validation...
    }
}
```

#### 3. Vaadin Hilla Integration (React + Type-Safe RPC)

Hilla provides type-safe RPC between Java backend and React frontend:

**Backend** (`@BrowserCallable` methods):
```java
@BrowserCallable
@AnonymousAllowed
public class PropagationEndpoint {
    public PropagationResponse getPropagationData() {
        // Returns data to React
    }
}
```

**Frontend** (auto-generated TypeScript client):
```typescript
// Generated at: src/main/frontend/generated/PropagationEndpoint.ts
import { PropagationEndpoint } from 'Frontend/generated/endpoints';

const data = await PropagationEndpoint.getPropagationData();
// Fully typed - no manual API contracts needed
```

#### 4. XML Security (HamQSL Client)

HamQSL returns XML with sometimes malformed DOCTYPE. The client uses secure XML parsing:

```java
private static XmlMapper createXmlMapper() {
    XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();

    // Disable DTD processing to prevent XXE attacks
    xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

    XmlMapper mapper = XmlMapper.builder().defaultUseWrapper(false).build();
    mapper.getFactory().setXMLInputFactory(xmlInputFactory);
    return mapper;
}
```

## Frontend Structure

### Vaadin Hilla + React Architecture

```
frontend/                      # Root-level frontend source
├── views/                    # React components
│   └── dashboard/
│       └── DashboardView.tsx # Main dashboard component
├── themes/                   # Vaadin themes
└── index.ts                  # Entry point

src/main/frontend/            # Vaadin integration directory
└── generated/                # Auto-generated Hilla TypeScript clients
    ├── PropagationEndpoint.ts
    └── jar-resources/        # Vaadin runtime resources
```

**Generated Files** (do not edit manually):
- `src/main/frontend/generated/` - Hilla generates TypeScript clients from `@BrowserCallable` endpoints
- `vite.generated.ts` - Vaadin generates Vite configuration
- `index.html` - Vaadin generates entry point

**Development Mode**:
- Hot reload enabled for React components
- Source maps available for debugging
- Vaadin dev tools overlay shows component tree

## Testing Strategy

### Current Test Suite

**60 comprehensive tests** covering:
- Utility functions (callsign validation, grid square conversion, frequency band parsing)
- External API clients (NOAA, HamQSL) with **WireMock** for mocking HTTP
- Service layer with **Mockito** for dependency mocking
- DTO validation and parsing edge cases

### Test Patterns

**WireMock for External APIs**:
```java
@BeforeEach
void setUp() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMockServer.start();

    String baseUrl = "http://localhost:" + wireMockServer.port();
    client = new TestNoaaSwpcClient(webClientBuilder, cacheManager, baseUrl);
}

@Test
void testFetchSolarIndices_Success() {
    wireMockServer.stubFor(get(urlEqualTo("/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody(jsonResponse)));

    SolarIndices result = client.fetchSolarIndices();
    assertNotNull(result);
}
```

**Testing Custom Exceptions**:
```java
@Test
void testFetchSolarIndices_EmptyResponse() {
    // Empty response should throw InvalidApiResponseException
    assertThrows(InvalidApiResponseException.class,
                () -> client.fetchSolarIndices());
}
```

### Test Configuration

Tests use Java 25 with ByteBuddy experimental mode for Mockito:
```gradle
tasks.named('test') {
    useJUnitPlatform()
    jvmArgs = [
        '--add-opens', 'java.base/java.lang=ALL-UNNAMED',
        '--add-opens', 'java.base/java.util=ALL-UNNAMED',
        '-Dnet.bytebuddy.experimental=true'
    ]
}
```

## External Data Sources

### 1. NOAA Space Weather Prediction Center

**Endpoint**: `https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json`

**Data**: Solar Flux Index (SFI), Sunspot Number
**Update Frequency**: ~30 minutes
**Cache TTL**: 5 minutes (via `@Cacheable`)
**Circuit Breaker**: `noaa` (60s open state)

**Response Format**:
```json
[
  {
    "time-tag": "2025-01",
    "f10.7": 150.5,
    "ssn": 120
  }
]
```

**Known Issues**:
- Sometimes returns partial dates ("2025-11" instead of "2025-11-01T00:00:00Z")
- Client has fallback date parsing to handle this

### 2. HamQSL Solar XML Feed

**Endpoint**: `http://www.hamqsl.com/solarxml.php`

**Data**: Solar indices (SFI, K-index, A-index, Sunspots) + band-by-band conditions
**Update Frequency**: ~30 minutes (per HamQSL guidance)
**Cache TTL**: 30 minutes (longer than NOAA due to slower update cycle)
**Circuit Breaker**: `hamqsl` (120s open state)

**Response Format** (XML):
```xml
<solar>
  <solardata>
    <solarflux>145.5</solarflux>
    <aindex>8</aindex>
    <kindex>3</kindex>
    <sunspots>115</sunspots>
    <calculatedconditions>
      <band name="80m-40m" time="day">Poor</band>
      <band name="30m-20m" time="day">Good</band>
      <band name="17m-15m" time="day">Fair</band>
      <band name="12m-10m" time="day">Poor</band>
    </calculatedconditions>
  </solardata>
</solar>
```

**Known Issues**:
- Sometimes has malformed DOCTYPE declaration
- Client disables DTD processing entirely for security and robustness

## Configuration

### Application Properties

Primary config: `src/main/resources/application.yml`

**Key settings**:
- Cache: Caffeine with 500 entry max, 10m expiry
- Circuit breakers: 50% failure threshold, automatic half-open transition
- Retry: 3 attempts, 500ms wait (2 attempts for slower HamQSL)
- Actuator: Health, metrics, Prometheus exposed

**Development profile**: `src/main/resources/application-dev.yml` (create for local overrides)

### Environment Variables

Currently none required (all defaults provided).

Future phases may need:
- `N2YO_API_KEY` - Satellite tracking API (Phase 4)
- `SPRING_AI_OPENAI_API_KEY` - AI assistant (Phase 7)

## Java Version Compatibility

**Critical**: This project uses Java 25 for compilation but targets Java 21 bytecode.

**build.gradle configuration**:
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)  // Compile with Java 25
    }
    sourceCompatibility = JavaVersion.VERSION_21     // Target Java 21 bytecode
    targetCompatibility = JavaVersion.VERSION_21
}
```

**Why this matters**:
- Spring Boot 3.4 requires Java 17+ (tested up to Java 24)
- Vaadin Hilla 24.9 requires Java 17+
- Mockito needs ByteBuddy experimental mode for Java 25

**Testing on Java 25**: Tests require special JVM args (configured in build.gradle):
```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
-Dnet.bytebuddy.experimental=true
```

## Claude Code Agents & Commands

This repository includes specialized agents and commands in `.claude/`:

### Agents (70-80% token reduction via specialization)

**`java-test-debugger`**: Debug JUnit failures with structured approach
- Tools: Read, Grep, Glob, Bash only
- Pattern: Test → Analyze → Hypothesize → Fix → Verify

**`gradle-build-validator`**: Full-stack build validation
- Validates: Gradle → Tests → Spring Boot → Vaadin → Integration
- Auto-kills port 8080 conflicts

**`code-refactoring`**: SOLID-focused refactoring
- Pre-flight: Clean git + passing tests required
- 4 phases: Analysis → Planning → Implementation → Validation
- Spring Boot aware (constructor injection, @ConfigurationProperties)

### Commands

**Quality Commands** (`.claude/commands/quality/`):
- `/validate-build` - Pre-commit validation workflow
- `/plan-tests` - 5-layer test pyramid planning (unit → integration → E2E → property → mutation)
- `/find-refactor-candidates` - Data-driven priorities (complexity, git history, smells, static analysis, coverage)

**Git Commands** (`.claude/commands/git/`):
- `/commit` - Conventional commits with NextSkip scopes (propagation, api, dashboard, config, dto, test)

**Usage**: Commands invoked via `/command-name` or natural language matching their domain.

## Common Development Patterns

### Adding a New External API Client

1. Create DTO with validation:
```java
@JsonIgnoreProperties(ignoreUnknown = true)
record NewSourceEntry(
    @JsonProperty("field") String field
) {
    public void validate() {
        // Validate fields
    }
}
```

2. Create client with resilience pattern:
```java
@Component
public class NewSourceClient {
    @CircuitBreaker(name = "newsource", fallbackMethod = "getCached")
    @Retry(name = "newsource")
    @Cacheable(value = "cacheName", unless = "#result == null")
    public Data fetch() {
        // WebClient call
    }
}
```

3. Configure circuit breaker in `application.yml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      newsource:
        baseConfig: default
```

4. Write WireMock tests:
```java
@Test
void testFetch_Success() {
    wireMockServer.stubFor(get(urlEqualTo("/endpoint"))
        .willReturn(aResponse().withStatus(200).withBody(json)));

    Data result = client.fetch();
    assertNotNull(result);
}
```

### Adding a New Hilla Endpoint

1. Create endpoint with `@BrowserCallable`:
```java
@BrowserCallable
@AnonymousAllowed
public class NewEndpoint {
    public ResponseType getData() {
        return service.getData();
    }
}
```

2. Hilla auto-generates TypeScript client at `src/main/frontend/generated/NewEndpoint.ts`

3. Use in React:
```typescript
import { NewEndpoint } from 'Frontend/generated/endpoints';

const data = await NewEndpoint.getData();
```

## Future Development

See `nextskip-project-plan.md` for detailed roadmap. Next phases:
- **Phase 2**: POTA/SOTA activations module
- **Phase 3**: Satellite tracking (N2YO, Celestrak)
- **Phase 4**: Real-time HF activity (PSKReporter MQTT)
- **Phase 5**: Contest calendar
- **Phase 6**: Aggregated dashboard with WebSocket live updates
- **Phase 7**: Spring AI conversational assistant

Each module follows the same patterns established in Phase 1.
