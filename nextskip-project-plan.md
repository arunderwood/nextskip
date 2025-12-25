# NextSkip — Project Plan

## Project Overview

**Name:** NextSkip — "What's your next skip?"

**Concept:** A web dashboard answering "What's good on the air right now?" — unified situational awareness for amateur radio operators across HF propagation, satellite passes, POTA/SOTA activations, contests, and real-time band activity.

**Target User:** Any amateur radio operator wanting a quick overview of current opportunities without checking multiple sites.

**Technical Goals:**

- Learn Spring AI framework through practical application
- Build a modular monolith that can decompose into microservices
- Create genuinely useful tool for the ham radio community

---

## Domain

**Primary name:** NextSkip

The name references "skip" propagation (how HF signals bounce off the ionosphere for long-distance communication) while implying "what's your next opportunity." Immediately meaningful to ham operators.

---

## Architecture Philosophy

### Modular Monolith Design

Start as a single deployable unit with strong internal boundaries. Each data source becomes an independent module with:

- Its own domain model
- Clean interface (Java interface defining the contract)
- Own configuration properties
- Isolated external dependencies

This allows later extraction to microservices by:

1. Replacing in-process calls with HTTP/gRPC
2. Deploying modules as separate containers
3. Adding message queues between modules if needed

### Package Structure

```
io.nextskip/
├── NextSkipApplication.java
├── common/
│   ├── config/
│   │   ├── CacheConfig.java
│   │   ├── AsyncConfig.java
│   │   └── ResilienceConfig.java
│   ├── model/
│   │   ├── Coordinates.java
│   │   ├── GridSquare.java
│   │   └── FrequencyBand.java
│   └── util/
│       └── HamRadioUtils.java
│
├── propagation/                    # Module 1: Solar/Propagation
│   ├── PropagationModule.java      # Module marker/config
│   ├── api/
│   │   └── PropagationService.java # Public interface
│   ├── internal/
│   │   ├── NoaaSwpcClient.java
│   │   ├── HamQslClient.java
│   │   └── PropagationServiceImpl.java
│   └── model/
│       ├── SolarIndices.java
│       └── BandCondition.java
│
├── spots/                          # Module 2: DX Spots & Activity
│   ├── SpotsModule.java
│   ├── api/
│   │   └── SpotService.java
│   ├── internal/
│   │   ├── PskReporterMqttClient.java
│   │   ├── RbnTelnetClient.java
│   │   └── SpotServiceImpl.java
│   └── model/
│       ├── Spot.java
│       └── BandActivity.java
│
├── activations/                    # Module 3: POTA/SOTA
│   ├── ActivationsModule.java
│   ├── api/
│   │   └── ActivationService.java
│   ├── internal/
│   │   ├── PotaClient.java
│   │   ├── SotaClient.java
│   │   └── ActivationServiceImpl.java
│   └── model/
│       ├── PotaSpot.java
│       ├── SotaSpot.java
│       └── Park.java
│
├── satellites/                     # Module 4: Satellite Tracking
│   ├── SatellitesModule.java
│   ├── api/
│   │   └── SatelliteService.java
│   ├── internal/
│   │   ├── N2yoClient.java
│   │   ├── CelestrakClient.java
│   │   ├── PassPredictor.java
│   │   └── SatelliteServiceImpl.java
│   └── model/
│       ├── Satellite.java
│       ├── SatellitePass.java
│       └── Tle.java
│
├── contests/                       # Module 5: Contest Calendar
│   ├── ContestsModule.java
│   ├── api/
│   │   └── ContestService.java
│   ├── internal/
│   │   ├── ICalParser.java
│   │   └── ContestServiceImpl.java
│   └── model/
│       └── Contest.java
│
├── dashboard/                      # Aggregation & API Layer
│   ├── DashboardController.java
│   ├── DashboardService.java
│   ├── WebSocketConfig.java
│   └── dto/
│       ├── DashboardState.java
│       ├── LiveFeedEvent.java
│       └── BandSummary.java
│
└── ai/                             # Spring AI Integration (Phase 7)
    ├── AiModule.java
    ├── HamRadioAssistant.java
    └── tools/
        ├── PropagationTool.java
        ├── SatelliteTool.java
        └── ActivationTool.java
```

### Key Design Patterns

**1. Module Interfaces**
Each module exposes a clean Java interface:

```java
// propagation/api/PropagationService.java
public interface PropagationService {
    SolarIndices getCurrentSolarIndices();
    List<BandCondition> getBandConditions();
    Mono<SolarIndices> getSolarIndicesReactive();  // For streaming
}
```

**2. Configuration Isolation**
Each module has its own configuration prefix:

```yaml
nextskip:
  propagation:
    noaa:
      base-url: https://services.swpc.noaa.gov
      cache-ttl: 5m
    hamqsl:
      base-url: http://www.hamqsl.com
      cache-ttl: 30m

  spots:
    pskreporter:
      mqtt-host: mqtt.pskreporter.info
      mqtt-port: 1883
    rbn:
      telnet-host: telnet.reversebeacon.net
      telnet-port: 7000

  activations:
    pota:
      base-url: https://api.pota.app
      cache-ttl: 60s
    sota:
      base-url: https://api2.sota.org.uk/api
      cache-ttl: 60s

  satellites:
    n2yo:
      base-url: https://api.n2yo.com/rest/v1/satellite
      api-key: ${N2YO_API_KEY}
    celestrak:
      base-url: https://celestrak.org/NORAD/elements
      tle-refresh: 6h
```

**3. Resilience Pattern**
Every external call wrapped with circuit breaker + retry + cache fallback:

```java
@Service
public class NoaaSwpcClient {

    @CircuitBreaker(name = "noaa", fallbackMethod = "getCachedIndices")
    @Retry(name = "noaa")
    @Cacheable(value = "solarIndices", unless = "#result == null")
    public SolarIndices fetchSolarIndices() {
        // HTTP call to NOAA
    }

    private SolarIndices getCachedIndices(Exception e) {
        return cacheManager.getCache("solarIndices").get("latest", SolarIndices.class);
    }
}
```

**4. Event-Driven Internal Communication**
Modules communicate via Spring Events (easily replaced with Kafka/RabbitMQ later):

```java
// When PSKReporter receives interesting spot
public record SpotReceivedEvent(Spot spot, boolean isRareOpening) {}

// Dashboard listens and pushes to WebSocket
@EventListener
public void onSpotReceived(SpotReceivedEvent event) {
    if (event.isRareOpening()) {
        webSocketService.broadcast(new LiveFeedEvent("BAND_OPENING", event.spot()));
    }
}
```

---

## Activity Prioritization Philosophy

Not all ham radio activities are equally suited to NextSkip's aggregation model. Activities are prioritized for implementation based on:

### Criteria

1. **Machine Readability**: Does the feed provide structured, parseable data (JSON, XML, API)?
2. **Geographic Independence**: Can the score be computed without user location?
3. **Broad Applicability**: Does this activity appeal to a wide segment of the ham radio community?
4. **Data Freshness**: How often do conditions change? Real-time feeds score higher than static calendars.
5. **Scoring Clarity**: Can we define clear "favorable" vs "unfavorable" conditions programmatically?

### Implementation Tiers

**Tier 1 - Infrastructure & Core Activities** (location-independent):

- HF Propagation (Phase 1 ✅) - Universal conditions, machine-readable feeds
- Dashboard Infrastructure (Phase 2 ✅) - Multi-card grid, WebSocket, card registration system
- POTA/SOTA (Phase 3 ✅) - Clear "active now" signal, broad appeal
- Contests (Phase 4) - Predictable schedule, universal applicability

**Tier 2 - Advanced Activities** (requires aggregation or real-time processing):

- Band Activity / PSKReporter (Phase 5) - Real-time MQTT, statistical aggregation
- Meteor Scatter (Phase 6) - Meteor shower tracking for MS propagation opportunities

**Tier 3 - Personalized Activities** (requires user input):

- Satellites (Phase 7) - Location-dependent, needs user grid square (was Phase 6)
- Spring AI Assistant (Phase 8) - Enhancement layer (was Phase 7)

**Note:** Each phase (3-6) includes both backend module implementation AND frontend card component, enabling iterative delivery of complete features.

---

## Scoring Architecture

Each activity module must provide scoring data that the frontend uses for card ranking.

### Backend Contract

Domain models should implement:

- `isFavorable()` - Boolean indicating if conditions are good for this activity
- `getScore()` - Numeric score (0-100) representing condition quality

Example from `BandCondition.java`:

```java
public boolean isFavorable() {
    return rating == BandConditionRating.GOOD && confidence > 0.5;
}

public int getScore() {
    int baseScore = switch (rating) {
        case GOOD -> 100;
        case FAIR -> 60;
        case POOR -> 20;
        case UNKNOWN -> 0;
    };
    return (int) (baseScore * confidence);
}
```

### Frontend Score Calculation

The `usePriorityCalculation` hook combines scoring signals into a final score:

| Component | Weight | Description                            |
| --------- | ------ | -------------------------------------- |
| Favorable | 40%    | Boolean flag from backend              |
| Score     | 35%    | Numeric 0-100 from backend             |
| Rating    | 20%    | Enum mapping (GOOD=100, FAIR=60, etc.) |
| Recency   | 5%     | Time decay (fresh data scores higher)  |

### Hotness Levels

Final scores map to visual hotness tiers:

| Score  | Hotness | Visual Treatment            |
| ------ | ------- | --------------------------- |
| 70-100 | hot     | Green glow, pulse animation |
| 45-69  | warm    | Orange tint                 |
| 20-44  | neutral | Blue tint                   |
| 0-19   | cool    | Gray, reduced opacity       |

---

## Implementation Phases

### Phase 1: Project Scaffolding & Propagation Module

**Goal:** Working Spring Boot app with solar/propagation data display

**Tasks:**

1. Initialize Spring Boot project with dependencies
2. Set up module structure with propagation as first module
3. Implement NOAA SWPC client (solar flux, K-index, A-index)
4. Implement HamQSL XML parser (band conditions)
5. Add Caffeine caching with appropriate TTLs
6. Create REST endpoint: `GET /api/v1/propagation`
7. Basic HTML dashboard showing solar indices + band conditions
8. Add health checks and basic observability

**Dependencies:**

```xml
<dependencies>
    <!-- Core -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>

    <!-- Caching -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Resilience -->
    <dependency>
        <groupId>io.github.resilience4j</groupId>
        <artifactId>resilience4j-spring-boot3</artifactId>
    </dependency>

    <!-- XML Parsing -->
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
    </dependency>

    <!-- Observability -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

**Acceptance Criteria:**

- Dashboard displays current SFI, K-index, A-index
- Band condition ratings (Good/Fair/Poor) for each HF band
- Data refreshes automatically, caching prevents API hammering
- Graceful degradation when NOAA/HamQSL unavailable

---

### Phase 2: Dashboard Infrastructure

**Goal:** Set up multi-card dashboard infrastructure for adding new activity modules

**Note:** Phase 1 implemented the activity grid with scoring system. This phase extends it to support multiple module types and real-time updates.

**Tasks:**

1. Create DashboardService to aggregate multiple modules
2. Implement WebSocket infrastructure for live updates
3. Extend `useDashboardCards` hook to support multiple activity types dynamically
4. Add card registration system so new modules can easily add cards
5. Implement unified live feed component (for future real-time events)
6. Add loading states and error boundaries for individual cards
7. Dark mode support

**Key Patterns:**

```typescript
// Card registration pattern
interface ActivityCardFactory {
  type: string;
  createCard: (data: ActivityData) => ActivityCardConfig;
}

// Each module registers its card factory
const cardFactories = new Map<string, ActivityCardFactory>();
```

**Acceptance Criteria:**

- Multiple card types can coexist in activity grid
- Cards can be added/removed dynamically as modules come online
- WebSocket infrastructure ready for real-time updates
- Individual card failures don't crash entire dashboard
- Dark mode toggle works across all cards

---

### Phase 3: POTA/SOTA Activations Module

**Goal:** Add POTA/SOTA activity tracking (backend + frontend card)

**Backend Tasks:**

1. Create activations module structure
2. Implement POTA API client (`https://api.pota.app/spot/activator`)
3. Implement SOTA API client (`https://api2.sota.org.uk/api/spots`)
4. Create unified Activation model with `isFavorable()` and `getScore()` methods
5. Add Hilla endpoint: `@BrowserCallable` in `ActivationEndpoint.java`

**Frontend Tasks:** 6. Create ActivationsCard component 7. Implement scoring logic (score based on number of active stations, recency) 8. Add card factory to dashboard 9. Display activator count, list with frequencies, and basic filtering (by band, by program)

**Key Models:**

```java
public record Activation(
    String callsign,
    String reference,      // K-2548 or VK2/SM-001
    String name,           // Park or summit name
    ActivationType type,   // POTA, SOTA, WWFF
    String frequency,
    String mode,
    String grid,
    Coordinates location,
    Instant spottedAt,
    Integer qsoCount       // POTA provides this
) {}
```

**Acceptance Criteria:**

- Activations card appears in activity grid sorted by score
- Shows POTA + SOTA activators currently on air
- Count displayed prominently ("23 POTA / 7 SOTA on air")
- Auto-refresh updates card without page reload

---

### Phase 4: Contest Calendar Module

**Goal:** Add contest tracking (backend + frontend card)

**Backend Tasks:**

1. Create contests module structure
2. Parse WA7BNM iCal feed (or maintain curated JSON)
3. Implement contest state machine (upcoming/active/ended)
4. Create Contest model with `isFavorable()` and `getScore()` methods
5. Add Hilla endpoint: `@BrowserCallable` in `ContestEndpoint.java`

**Frontend Tasks:** 6. Create ContestsCard component 7. Implement scoring logic (score high when contest is active, medium when starting soon) 8. Add card factory to dashboard 9. Display active contests with "ending soon" visual alerts

**Data Source Options:**

- WA7BNM iCal: `https://www.contestcalendar.com/calendar.ics` (personal use)
- Curated JSON file with major contests (fallback)
- Community-maintained contest list

**Key Models:**

```java
public record Contest(
    String name,
    String sponsor,          // ARRL, CQ, etc.
    Instant startTime,
    Instant endTime,
    ContestStatus status,    // UPCOMING, ACTIVE, ENDED
    Set<FrequencyBand> bands,
    Set<String> modes,
    String rulesUrl,
    Duration timeRemaining
) {}
```

**Acceptance Criteria:**

- Contests card appears in activity grid sorted by score
- Shows active contests with high score, upcoming with medium score
- Visual alert for "ending soon" contests
- Lists upcoming contests in next 7 days

---

### Phase 5: Real-time HF Activity (PSKReporter MQTT)

**Goal:** Add real-time band activity tracking (backend + frontend card)

**Backend Tasks:**

1. Create spots module structure
2. Implement PSKReporter MQTT client using Eclipse Paho
3. Aggregate spots by band in sliding time windows
4. Detect "band openings" (spot rate >2σ above average)
5. Create BandActivity model with `isFavorable()` and `getScore()` methods
6. Add Hilla endpoint: `@BrowserCallable` in `BandActivityEndpoint.java`

**Frontend Tasks:** 7. Create BandActivityCard component 8. Implement scoring logic (high score for band openings, medium for active bands) 9. Add card factory to dashboard 10. Display per-band activity levels with visual hotness indicators 11. Use WebSocket for live spot updates (pushes to existing WebSocket infrastructure)

**Dependencies:**

```xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.mqttv5.client</artifactId>
    <version>1.2.5</version>
</dependency>
```

**MQTT Subscription:**

```java
// Subscribe to all FT8 spots
client.subscribe("pskr/filter/v2/+/FT8/#", 0);

// Parse incoming JSON
{
    "f": 14074653,      // Frequency Hz
    "md": "FT8",        // Mode
    "rp": -5,           // Report dB
    "t": 1662407712,    // Unix timestamp
    "sc": "SP2EWQ",     // Sender call
    "sl": "JO93fn42",   // Sender locator
    "rc": "K5ABC",      // Receiver call
    "b": "20m"          // Band
}
```

**Key Models:**

```java
public record BandActivity(
    FrequencyBand band,
    long spotsLastHour,
    long spotsLast5Min,
    double ratePerMinute,
    ActivityLevel level,  // HOT, MODERATE, QUIET
    boolean isOpening,    // Statistical anomaly detected
    Map<String, Long> byMode  // FT8: 1200, CW: 450, etc.
) {}
```

**Acceptance Criteria:**

- Real-time spot rate per band displayed
- Visual indicator when band is unusually active
- WebSocket pushes updates to dashboard
- Handles MQTT disconnection gracefully with reconnection

---

### Phase 6: Meteor Scatter Module

**Goal:** Add meteor shower tracking for MS propagation opportunities (backend + frontend card)

**Backend Tasks:**

1. Create `meteors/` module structure following established patterns
2. Implement meteor shower data client (IMO data or curated JSON)
3. Create `MeteorShower` record implementing `Event` interface
4. Calculate current activity level based on peak date and ZHR (Zenithal Hourly Rate)
5. Add `MeteorEndpoint` with `@BrowserCallable`

**Frontend Tasks:** 6. Create `MeteorShowerCard` component 7. Implement scoring logic (high score during peak, medium before/after) 8. Add card factory to dashboard 9. Display current shower activity level and peak timing

**Key Meteor Showers:**

- Quadrantids (Jan 3-4): ZHR 110
- Perseids (Aug 12-13): ZHR 100
- Geminids (Dec 13-14): ZHR 150
- Leonids (Nov 17-18): ZHR 15

**Key Models:**

```java
public record MeteorShower(
    String name,
    Instant peakStart,
    Instant peakEnd,
    Instant visibilityStart,    // Active period start
    Instant visibilityEnd,      // Active period end
    int peakZhr,                // Zenithal Hourly Rate at peak
    int currentZhr,             // Calculated current ZHR
    EventStatus status,
    Duration timeToPeak
) implements Event {}
```

**Acceptance Criteria:**

- Meteor shower card appears in activity grid sorted by score
- Shows active showers with high score during peak
- Shows upcoming showers (next 7 days) with medium score
- Displays ZHR and time to/from peak
- Reuses `Event` abstraction from Phase 4

---

### Phase 7: Satellite Tracking Module

**Goal:** Add satellite pass tracking (backend + frontend card)

**Backend Tasks:**

1. Create satellites module structure
2. Implement N2YO API client for pass predictions
3. Implement Celestrak client for TLE data
4. Integrate predict4java for local pass calculation (reduce API calls)
5. Define list of active amateur satellites (SO-50, ISS, RS-44, etc.)
6. Create SatellitePass model with `isFavorable()` and `getScore()` methods
7. Add Hilla endpoint: `@BrowserCallable` in `SatelliteEndpoint.java`

**Frontend Tasks:** 8. Create SatellitesCard component 9. Implement scoring logic (high score for passes happening soon with good elevation) 10. Add card factory to dashboard 11. Add geolocation support (browser API or manual grid square entry) 12. Display next 3 passes with countdown timers

**Dependencies:**

```xml
<dependency>
    <groupId>com.github.davidmoten</groupId>
    <artifactId>predict4java</artifactId>
    <version>1.3.1</version>
</dependency>
```

**Key Models:**

```java
public record SatellitePass(
    String satelliteName,
    int noradId,
    Instant aosTime,        // Acquisition of signal
    Instant losTime,        // Loss of signal
    double maxElevation,
    double aosAzimuth,
    double losAzimuth,
    SatelliteType type,     // FM_REPEATER, LINEAR_TRANSPONDER, DIGIPEATER
    String uplinkFreq,
    String downlinkFreq,
    String tone             // CTCSS if applicable
) {}
```

**Acceptance Criteria:**

- Satellites card appears in activity grid sorted by score
- Shows passes for user's location (default or specified via geolocation or manual entry)
- Only shows passes with >20° max elevation (actually workable)
- Displays time until next pass, uplink/downlink frequencies with countdown
- TLEs refresh every 6 hours automatically
- Card score is high when pass is imminent, medium for passes in next few hours

**Note:** This phase requires user location input, which is why it's prioritized after location-independent activities.

---

### Phase 8: Spring AI Integration

**Goal:** Add conversational AI assistant for natural language queries

**Tasks:**

1. Add Spring AI dependencies (OpenAI or Ollama)
2. Create tool definitions for each module
3. Implement HamRadioAssistant with tool-calling
4. Add chat endpoint: `POST /api/v1/assistant/chat`
5. Optional: Voice input/output

**Dependencies:**

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
<!-- OR for local models -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-ollama-spring-boot-starter</artifactId>
</dependency>
```

**Tool Definitions:**

```java
@Component
public class PropagationTools {

    private final PropagationService propagationService;

    @Tool(description = "Get current solar indices and band conditions for HF propagation")
    public PropagationSummary getCurrentPropagation() {
        return new PropagationSummary(
            propagationService.getCurrentSolarIndices(),
            propagationService.getBandConditions()
        );
    }

    @Tool(description = "Check if a specific HF band is currently open for long-distance propagation")
    public BandCondition checkBand(@ToolParam(description = "Band to check, e.g., '20m', '40m', '15m'") String band) {
        return propagationService.getBandCondition(FrequencyBand.fromString(band));
    }
}

@Component
public class SatelliteTools {

    @Tool(description = "Get upcoming amateur satellite passes for a location")
    public List<SatellitePass> getUpcomingPasses(
        @ToolParam(description = "Latitude in decimal degrees") double lat,
        @ToolParam(description = "Longitude in decimal degrees") double lon,
        @ToolParam(description = "Number of hours to look ahead") int hours
    ) {
        return satelliteService.getPasses(lat, lon, Duration.ofHours(hours));
    }
}
```

**Example Interactions:**

- "What should I do on the radio right now?"
- "Is 15 meters open to Europe?"
- "When's the next satellite pass for my location?"
- "Are there any POTA activators on 20 meters?"
- "What contests are happening this weekend?"

**Acceptance Criteria:**

- Chat interface answers ham radio questions
- AI calls appropriate tools based on query
- Responses include real-time data from modules
- Works with either cloud (OpenAI) or local (Ollama) models

---

## Testing Strategy

### Unit Tests

- Test each client in isolation with WireMock
- Test data parsing/transformation logic
- Test caching behavior

### Integration Tests

- Test module interfaces with real (mocked) dependencies
- Test WebSocket connections
- Test circuit breaker behavior

### Contract Tests

- Document external API contracts
- Alert on upstream API changes

### Load Tests

- Verify dashboard handles 100+ concurrent users
- Verify MQTT client handles high spot volume

---

## Deployment Considerations

### Local Development

```bash
./gradlew bootRun
# or with Docker
docker-compose up
```

### Production (Single Container)

- Deploy as single Spring Boot JAR
- Use external Redis for shared caching (optional)
- Configure appropriate JVM memory

### Future Microservices Split

When scale demands:

1. Extract high-traffic modules (spots) first
2. Add message queue (Kafka/RabbitMQ) between services
3. Deploy modules as separate containers
4. Add API gateway for unified frontend access

---

## Environment Variables

```bash
# Required
N2YO_API_KEY=your-n2yo-api-key

# Optional - defaults provided
NEXTSKIP_DEFAULT_LATITUDE=47.6062
NEXTSKIP_DEFAULT_LONGITUDE=-122.3321
NEXTSKIP_DEFAULT_GRID=CN87

# Spring AI (Phase 7)
SPRING_AI_OPENAI_API_KEY=your-openai-key
# OR
SPRING_AI_OLLAMA_BASE_URL=http://localhost:11434
```

---

## Success Metrics

1. **Usefulness:** Do ham operators actually use it?
2. **Reliability:** >99% uptime despite upstream API issues
3. **Performance:** Dashboard loads in <2 seconds
4. **Freshness:** Data no more than 5 minutes stale
5. **Learning:** Successfully demonstrates Spring AI patterns

---

## Claude Code Execution Instructions

When implementing this project, proceed phase by phase:

1. **Start each phase** by creating the module directory structure
2. **Implement clients first** with comprehensive error handling
3. **Add caching early** — external APIs are unreliable
4. **Write tests** for parsing logic (upstream formats change)
5. **Commit working increments** — each phase should be deployable
6. **Document API contracts** — note rate limits and auth requirements

For each external API integration:

- Verify the endpoint is still active (APIs change)
- Implement circuit breaker from the start
- Cache aggressively with appropriate TTLs
- Log failures for debugging

The project should be runnable after each phase completes.
