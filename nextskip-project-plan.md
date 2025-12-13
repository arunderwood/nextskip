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

## Implementation Phases

### Phase 1: Project Scaffolding & Propagation Module
**Goal:** Working Spring Boot app with solar/propagation data display

**Tasks:**
1. Initialize Spring Boot 4.x project with dependencies
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

### Phase 2: POTA/SOTA Activations Module
**Goal:** Display current portable activators with park/summit info

**Tasks:**
1. Create activations module structure
2. Implement POTA API client (`https://api.pota.app/spot/activator`)
3. Implement SOTA API client (`https://api2.sota.org.uk/api/spots`)
4. Create unified Activation model that normalizes both
5. Add REST endpoint: `GET /api/v1/activations`
6. Dashboard tile showing activator count + list
7. Basic filtering (by band, by program)

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
- Dashboard shows POTA + SOTA activators currently on air
- Count displayed prominently ("23 POTA / 7 SOTA on air")
- Click through to see full list with frequencies
- Auto-refresh every 60 seconds

---

### Phase 3: Satellite Tracking Module
**Goal:** Display upcoming amateur satellite passes

**Tasks:**
1. Create satellites module structure
2. Implement N2YO API client for pass predictions
3. Implement Celestrak client for TLE data
4. Integrate predict4java for local pass calculation (reduce API calls)
5. Define list of active amateur satellites (SO-50, ISS, RS-44, etc.)
6. Add REST endpoint: `GET /api/v1/satellites/passes?lat=X&lon=Y`
7. Dashboard tile showing next 3 passes
8. Geolocation support (browser API or manual entry)

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
- Shows passes for user's location (default or specified)
- Only shows passes with >20° max elevation (actually workable)
- Displays time until next pass, uplink/downlink frequencies
- TLEs refresh every 6 hours automatically

---

### Phase 4: Real-time HF Activity (PSKReporter MQTT)
**Goal:** Live band activity metrics from PSKReporter

**Tasks:**
1. Create spots module structure
2. Implement PSKReporter MQTT client using Eclipse Paho
3. Aggregate spots by band in sliding time windows
4. Detect "band openings" (spot rate >2σ above average)
5. Add WebSocket endpoint for real-time updates
6. Dashboard shows per-band activity level (Hot/Moderate/Quiet)
7. Live feed of interesting events

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

### Phase 5: Contest Calendar Module
**Goal:** Display current and upcoming contests

**Tasks:**
1. Create contests module structure
2. Parse WA7BNM iCal feed (or maintain curated JSON)
3. Implement contest state machine (upcoming/active/ended)
4. Add REST endpoint: `GET /api/v1/contests`
5. Dashboard tile for active/upcoming contests
6. "Last hour" alerts for ending contests

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
- Shows if a major contest is currently active
- Lists upcoming contests in next 7 days
- Visual alert for "ending soon" contests
- Basic contest info (bands, modes, duration)

---

### Phase 6: Dashboard Aggregation & UI
**Goal:** Polished single-page dashboard with real-time updates

**Tasks:**
1. Create DashboardService that aggregates all modules
2. Implement WebSocket for live updates
3. Build responsive frontend (React, Vue, or Thymeleaf+HTMX)
4. Add location awareness (browser geolocation or manual grid)
5. Unified live feed with filtering
6. Mobile-responsive design
7. Dark mode support

**API Design:**
```
GET  /api/v1/dashboard              # Full dashboard state
GET  /api/v1/dashboard/propagation  # Just propagation tile
GET  /api/v1/dashboard/activations  # Just activations tile
GET  /api/v1/dashboard/satellites   # Just satellite tile
GET  /api/v1/dashboard/activity     # Just band activity tile
GET  /api/v1/dashboard/contests     # Just contests tile
WS   /ws/live-feed                  # Real-time event stream
```

**Dashboard State DTO:**
```java
public record DashboardState(
    PropagationSummary propagation,
    List<Activation> activations,
    List<SatellitePass> upcomingPasses,
    Map<FrequencyBand, BandActivity> bandActivity,
    List<Contest> activeContests,
    Instant generatedAt
) {}
```

**Frontend Options:**
- **HTMX + Thymeleaf:** Simple, server-rendered, minimal JS
- **React:** Rich interactivity, familiar ecosystem
- **Vue 3:** Balance of simplicity and power

**Recommended:** Start with HTMX for rapid iteration, migrate to React if interactivity demands grow.

**Acceptance Criteria:**
- Single page shows all tiles at a glance
- Updates without full page refresh
- Works on mobile devices
- Loads in <2 seconds
- Graceful degradation when modules fail

---

### Phase 7: Spring AI Integration
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
