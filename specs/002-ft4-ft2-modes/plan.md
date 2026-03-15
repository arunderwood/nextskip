# Implementation Plan: FT4 and FT2 Digital Mode Support

**Branch**: `002-ft4-ft2-modes` | **Date**: 2026-03-13 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-ft4-ft2-modes/spec.md`

## Summary

Add FT4 and FT2 digital mode support to the spots module so operators can see real-time band activity for these modes alongside FT8. The existing spots infrastructure (MQTT ingestion, persistence, aggregation, frontend cards) already supports multiple modes in several layers but requires changes in the aggregation layer (currently returns one mode per band) and API layer (no mode parameter). A fixed, configurable rarity multiplier per mode boosts scores for less popular modes so they rank competitively on the dashboard.

## Technical Context

**Language/Version**: Java 25 (see `.tool-versions`)
**Primary Dependencies**: Spring Boot, Vaadin Hilla 24.9, Resilience4j, Caffeine, React 19 + TypeScript
**Storage**: PostgreSQL (spots table with indices on band+spotted_at, mode+spotted_at)
**Testing**: JUnit 5, WireMock, Vitest, React Testing Library, Playwright
**Target Platform**: Web application (server-side JVM + browser frontend)
**Project Type**: Modular monolith web application
**Performance Goals**: Band activity refresh within 1 aggregation cycle (currently 1 minute); MQTT spot ingestion at existing throughput
**Constraints**: Must use existing PSKReporter MQTT data source; must stay within spots module boundaries
**Scale/Scope**: Adds 2 MQTT topic subscriptions; up to ~3x more band activity cards on dashboard

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Real-Time Opportunism | PASS | FT4/FT2 band activity surfaces time-sensitive opportunities |
| II. Multi-Activity Aggregation | PASS | New mode cards integrate into existing scored dashboard |
| III. Activity-Centric Modularity | PASS | All changes stay within spots module; no other modules modified |
| IV. Responsible Data Consumption | PASS | PSKReporter MQTT supports multi-topic; adding topics is expected usage |
| V. SOLID Design | PASS | Extends existing patterns (ModeWindow enum, mode registry); Open-Closed for card system |
| VI. Quality Gates | PASS | Will include unit, integration, and frontend tests meeting coverage thresholds |
| VII. Resilience by Default | PASS | Uses existing circuit breaker, retry, and caching infrastructure |

No violations. No complexity tracking needed.

## Project Structure

### Documentation (this feature)

```text
specs/002-ft4-ft2-modes/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   └── spots-api.md     # Updated BrowserCallable contract
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
# Backend (spots module)
src/main/java/io/nextskip/spots/
├── api/
│   ├── SpotsEndpoint.java          # Update: return per-mode activities
│   ├── SpotsService.java           # Update: add mode-aware methods
│   └── BandActivityResponse.java   # Update: composite "{band}_{mode}" keys in bandActivities map
├── internal/
│   ├── SpotsServiceImpl.java       # Update: implement mode-aware aggregation
│   ├── aggregation/
│   │   └── BandActivityAggregator.java  # Update: aggregate per band+mode pair
│   └── client/
│       └── PskReporterMqttSource.java   # No change (already multi-topic)
├── model/
│   ├── BandActivity.java           # Update: add rarity multiplier to scoring
│   └── ModeWindow.java             # Update: add FT2 enum value
└── persistence/
    └── repository/SpotRepository.java  # May need new query methods

# Backend configuration
src/main/resources/
└── application.yml                 # Update: add FT4/FT2 MQTT topics, rarity config

# Frontend
src/main/frontend/
├── config/
│   └── modeRegistry.ts             # Update: enable FT4, add FT2
└── components/cards/band-activity/
    └── index.tsx                    # No change (already iterates all modes)

# Tests
src/test/java/io/nextskip/spots/
├── model/
│   ├── BandActivityTest.java       # Update: test rarity multiplier scoring
│   └── ModeWindowTest.java         # Update: test FT2 enum value
├── internal/aggregation/
│   └── BandActivityAggregatorTest.java  # Update: test multi-mode aggregation
└── api/
    └── SpotsEndpointTest.java      # Update: test per-mode responses

src/test/frontend/
└── components/cards/band-activity/  # Update: test FT4/FT2 card rendering
```

**Structure Decision**: All changes are within the existing `spots` module. No new modules, packages, or major structural changes needed. The feature extends existing patterns.
