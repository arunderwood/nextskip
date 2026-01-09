<!--
SYNC IMPACT REPORT
==================
Version change: 0.0.0 → 1.0.0 (MAJOR - initial constitution)
Modified principles: N/A (initial creation)
Added sections:
  - Product Principles (4 principles)
  - Engineering Principles (3 principles)
  - Permitted Technologies
  - Governance
Removed sections: N/A (initial creation)
Templates requiring updates:
  - .specify/templates/plan-template.md ✅ aligned (Constitution Check references principles)
  - .specify/templates/spec-template.md ✅ aligned (user stories support activity-centric design)
  - .specify/templates/tasks-template.md ✅ aligned (phased implementation supports modularity)
Follow-up TODOs: None
-->

# NextSkip Constitution

NextSkip helps amateur radio operators find the best opportunities across propagation
conditions, portable activations, contests, and more. This constitution defines the
non-negotiable principles that guide all development.

## Product Principles

### I. Real-Time Opportunism

NextSkip answers one question: **"What should I do on the air RIGHT NOW?"**

**Requirements**:
- Every feature MUST surface time-sensitive opportunities
- Stale data is better than no data—show last-known-good when feeds fail
- Data MUST be as fresh as possible while being a responsible consumer of external feeds
- Features that don't help operators decide what to do next do not belong

**Rationale**: Operators have limited time on the air. NextSkip exists to maximize their
chances of success by surfacing the best current opportunities.

### II. Multi-Activity Aggregation

One dashboard spans all amateur radio activities: DX, POTA/SOTA, contests, satellites,
meteor scatter, and more.

**Requirements**:
- All activities appear on a single, unified dashboard
- Activities compete for attention through a universal scoring system (0-100)
- Highest-scored activities surface to the top ("hotness" ranking)
- New activities MUST integrate into the existing dashboard—no separate views

**Rationale**: Operators shouldn't need multiple tools to discover opportunities.
Cross-activity comparison reveals the best use of limited air time.

### III. Activity-Centric Modularity

Each activity type is a self-contained module within a modular monolith architecture.

**Requirements**:
- Modules MUST have clean boundaries with explicit public APIs
- Each module owns its external feed integrations (clients, parsers, resilience)
- Scoring algorithms are module-specific—no universal scoring formula
- Module APIs (`@BrowserCallable`) expose data to the frontend; internals stay hidden
- Adding a new activity MUST NOT require modifying existing modules

**Rationale**: Activity types have fundamentally different data sources and success
criteria. Clean module boundaries enable the platform to expand without destabilizing
existing functionality.

### IV. Responsible Data Consumption

External feeds power NextSkip. We MUST be good citizens of these data sources.

**Requirements**:
- Cache aggressively—minimize redundant API calls
- Respect rate limits and terms of service
- Persist feed data to database for resilience and analytics
- Circuit breakers MUST prevent cascade failures when feeds degrade
- Attribute data sources clearly in the UI and documentation

**Rationale**: Our data providers (NOAA, POTA, contest calendars) make NextSkip possible.
Irresponsible consumption damages the ecosystem and risks losing access.

## Engineering Principles

### V. SOLID Design (NON-NEGOTIABLE)

All code MUST adhere to SOLID principles. This is not optional.

**Requirements**:
- **Single Responsibility**: Each class has one reason to change
- **Open-Closed**: Classes are open for extension, closed for modification
- **Liskov Substitution**: Derived classes are substitutable for their base types
- **Interface Segregation**: Clients depend only on methods they use
- **Dependency Inversion**: Depend on abstractions, not concretions

**Rationale**: SOLID principles produce code that is easier to test, extend, and maintain.
Violations create rigid, fragile systems that resist change.

### VI. Quality Gates Are Non-Negotiable

Code quality is enforced automatically. Violations fail the build—no exceptions.

**Requirements**:
- Static analysis tools enforce coverage, style, and bug detection
- Quality check suppressions MUST include documented rationale
- Tests use shared fixtures and constants—no magic numbers
- Fix violations, don't suppress them

**Rationale**: Manual quality enforcement doesn't scale. Automated gates ensure every
merge request meets the bar without relying on reviewer vigilance.

### VII. Resilience by Default

Every external integration is assumed to fail. The system MUST degrade gracefully.

**Requirements**:
- Circuit Breaker + Retry pattern for all external API clients
- Database persistence for all feed data (cache alone is insufficient)
- LoadingCache serves last-known-good data during outages

**Rationale**: External services fail unpredictably. Users should see stale data—not
error pages—when dependencies are unavailable.

## Permitted Technologies

New dependencies MUST align with the established technology stack. Introducing technologies
outside this list requires constitutional amendment.

### Backend
- **Language**: Java (see `.tool-versions` for version)
- **Framework**: Spring Boot
- **API Layer**: Vaadin Hilla (`@BrowserCallable` endpoints)
- **Resilience**: Resilience4j (circuit breakers, retry)
- **Caching**: Caffeine
- **Database**: PostgreSQL
- **Build**: Gradle

### Frontend
- **Framework**: React with TypeScript
- **Build**: Vite
- **API Client**: Vaadin Hilla (auto-generated TypeScript clients)
- **Testing**: Vitest, React Testing Library, Playwright

### Observability
- **Tracing**: OpenTelemetry
- **Profiling**: Pyroscope
- **Analytics**: PostHog
- **Health/Metrics**: Spring Boot Actuator

### Quality
- **Static Analysis**: Checkstyle, PMD, SpotBugs
- **Coverage**: JaCoCo
- **Mutation Testing**: PIT (pitest)
- **Linting**: ESLint, Prettier

## Governance

This constitution supersedes conflicting practices. Amendments require:

1. Documented rationale for the change
2. Pull request review and approval
3. Migration plan for existing violations
4. Version increment (MAJOR: principle change, MINOR: new principle, PATCH: clarification)

All code reviews MUST verify constitutional compliance. Deviations require explicit
justification in the PR description.

For detailed implementation guidance, refer to `.claude/CLAUDE.md`.

**Version**: 1.0.0 | **Ratified**: 2026-01-08 | **Last Amended**: 2026-01-08
