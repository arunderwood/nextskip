# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

NextSkip is a ham radio propagation dashboard providing real-time HF band conditions and solar indices for amateur radio operators. Built as a modular monolith using Spring Boot, Vaadin Hilla, and React.

**Current Status**: Phase 1 complete - Propagation module with NOAA SWPC and HamQSL integration.

## Build & Development Commands

### Essential Commands

```bash
# Run all tests
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

### Port 8080 Conflicts

```bash
lsof -ti :8080 | xargs kill -9
```

### Quality Checks

Quality checks are **blocking** - violations will fail the build. Always run `./gradlew check` before committing.

```bash
# Run all quality checks (Checkstyle, PMD, SpotBugs, JaCoCo)
./gradlew check

# View reports
open build/reports/tests/test/index.html
open build/reports/jacoco/test/html/index.html
open build/reports/checkstyle/main.html
open build/reports/pmd/main.html
```

**Coverage Requirements**: 75% instruction, 65% branch (enforced by JaCoCo)

**Fix violations, don't suppress them.** Rule exclusions lower the quality bar. Suppressions require documented justification (e.g., `@SuppressWarnings("PMD.TooManyMethods") // Comprehensive test suite`).

**Common Violations**:

- `AvoidLiteralsInIfCondition`: Extract magic numbers to named constants
- `UseLocaleWithCaseConversions`: Use `toUpperCase(Locale.ROOT)` instead of `toUpperCase()`
- `OperatorWrap`: Place `+` at start of next line, not end of current line
- `TooManyMethods`: Add `@SuppressWarnings("PMD.TooManyMethods")` to comprehensive test classes

**Test Naming Convention** (enforced by PMD):

- Java tests MUST use BDD-style: `testMethodName_Scenario_ExpectedResult`
- Example: `testGetScore_FreshActivation_Returns100`

### Frontend Testing

#### Unit/Component Tests (Vitest)

```bash
# Watch mode (re-runs on file changes)
npm test

# Run once (CI mode)
npm run test:run

# Interactive UI
npm run test:ui

# With coverage
npm run test:coverage
```

**Test Suite**: Comprehensive tests in `src/test/frontend/` covering:

- Priority calculation algorithm
- Component rendering and behavior
- Grid sorting and layout
- WCAG 2.1 AA compliance

**Configuration**: See `vitest.config.ts` and `src/test/frontend/setup.ts`

**Test Location**: Following Maven/Gradle conventions and Vaadin's official example repos, tests are in `src/test/frontend/` (parallel to `src/main/frontend/`)

**Mock Generated Types**: Tests use mock stubs instead of Hilla-generated types to avoid Gradle dependency. Mocks are in `src/test/frontend/mocks/generated/` and aliased via `vitest.config.ts`. When adding new generated type imports to tests, create corresponding mock stubs.

#### End-to-End Tests (Playwright)

```bash
# Run E2E tests
npm run e2e

# Run with Playwright UI (for debugging)
npm run e2e:ui

# Run in headed mode (see browser)
npm run e2e:headed
```

**Test Suite**: E2E tests in `src/test/e2e/` covering:

- Dashboard loads successfully
- Page title rendering
- Dashboard cards render
- Last update timestamp display

**Configuration**: See `playwright.config.ts`

**Local Development**: Playwright automatically starts the application via `./gradlew bootRun` before running tests

**CI Mode**: Tests run against the production JAR artifact to validate the actual deployable build

#### Frontend Linting (ESLint + Prettier)

```bash
# Check for linting errors
npm run lint

# Auto-fix linting errors
npm run lint:fix

# Check formatting
npm run format:check

# Auto-format code
npm run format
```

**Configuration**:

- ESLint: `eslint.config.js` (ESLint 9 flat config with `eslint-config-vaadin`)
- Prettier: `.prettierrc` (single quotes, 120 char width, trailing commas)
- Pre-commit: `simple-git-hooks` + `lint-staged` (auto-fix on commit)

**Rules**: Uses Vaadin's official ESLint config with these customizations:

- JSX literals allowed (no i18n requirement)
- TypeScript strict mode
- React hooks linting
- JSX accessibility (jsx-a11y)
- Performance rules for React

## Architecture Guidelines

### Modular Monolith Design

- Each module has clean public API (Java interface in `api/` package)
- Internal implementations in `internal/` package (not exposed)
- Module-specific models in `model/` package
- Isolated external dependencies

**Current Modules**: common, propagation, activations, contests, meteors
**Planned Modules**: See `nextskip-project-plan.md`

### Package Structure

```
io.nextskip/
├── common/           # Shared models and utilities
├── propagation/      # Solar indices, band conditions (NOAA, HamQSL)
├── activations/      # POTA/SOTA activations
├── contests/         # Contest calendar (WA7BNM)
├── meteors/          # Meteor shower tracking
│   ├── api/          # Public contracts (@BrowserCallable endpoints)
│   ├── internal/     # Implementations (clients, services)
│   └── model/        # Domain models
├── config/           # Global configuration
└── NextSkipApplication.java
```

### SOLID Principles

All code should adhere to SOLID design principles:

- **Single Responsibility Principle**: Each class has one reason to change (e.g., `PotaClient` only handles POTA API communication)
- **Open-Closed Principle**: Classes are open for extension, closed for modification (e.g., `CardRegistry` allows new cards without modifying core dashboard code)
- **Liskov Substitution Principle**: Derived classes are substitutable for their base types (e.g., all `ExternalDataClient` implementations can be used interchangeably)
- **Interface Segregation Principle**: Clients depend only on methods they use (e.g., `Scoreable` interface exposes only scoring methods, not internal implementation)
- **Dependency Inversion Principle**: Depend on abstractions, not concretions (e.g., `ActivationsServiceImpl` depends on `ExternalDataClient<T>` interface, not specific client classes)

### Key Design Patterns

**Resilience Pattern**: All external API clients use Circuit Breaker + Retry + Cache Fallback

- Configuration in `application.yml`
- See `NoaaSwpcClient.java` and `HamQslClient.java` for reference implementations

**Type-Safe DTOs**: Java records with built-in validation

- See `NoaaSolarCycleEntry.java` for pattern

**Vaadin Hilla Integration**: Type-safe RPC between Java backend and React frontend

- Backend: `@BrowserCallable` methods in `api/` package
- Frontend: Auto-generated TypeScript clients in `src/main/frontend/generated/`

**XML Security**: Disable DTD processing to prevent XXE attacks

- See `HamQslClient.java` for pattern

## Scoring System

NextSkip uses a score-based system to rank activity cards by "hotness."

### Backend Scoring Pattern

Each module's domain models should implement:

1. `isFavorable()` - Boolean for good conditions
2. `getScore()` - Integer 0-100 for condition quality

See `SolarIndices.java` and `BandCondition.java` for reference.

### Frontend Score Calculation

Location: `frontend/components/activity/usePriorityCalculation.ts`

Weighted algorithm:

- 40% favorable flag
- 35% numeric score
- 20% rating enum
- 5% recency (time decay)

### Hotness Levels

| Score  | Hotness | Visual Treatment            |
| ------ | ------- | --------------------------- |
| 70-100 | hot     | Green glow, pulse animation |
| 45-69  | warm    | Orange tint                 |
| 20-44  | neutral | Blue tint                   |
| 0-19   | cool    | Gray, reduced opacity       |

### Adding a New Activity Module

1. Create domain models with `isFavorable()` and `getScore()` methods
2. Expose data via `@BrowserCallable` endpoint
3. Add card configuration in `useDashboardCards.ts` with scoring inputs
4. Frontend automatically sorts cards by calculated score

## Frontend Structure

```
src/main/frontend/         # Production frontend code
├── components/            # Reusable React components
│   ├── bento/             # Bento grid system (priority-based layout)
│   └── cards/             # Activity card content components
├── hooks/                 # Custom React hooks
├── types/                 # TypeScript type definitions
├── views/                 # Page-level components (routes)
├── styles/                # Global styles and design tokens
├── docs/                  # Frontend documentation
│   ├── DESIGN_SYSTEM.md
│   ├── COMPONENT_PATTERNS.md
│   └── ACCESSIBILITY.md
└── generated/             # Hilla auto-generated TypeScript clients

src/test/frontend/         # Frontend tests (parallel to src/main)
├── components/            # Component tests
│   └── activity/          # Activity grid system tests
└── setup.ts               # Vitest test setup
```

**Activity Grid System**:

- Priority-based card layout (highest priority = top-left position)
- Hotness visual indicators (hot/warm/neutral/cool based on priority)
- Responsive: 4 columns (desktop) → 2 columns (tablet) → 1 column (mobile)
- WCAG 2.1 AA compliant with keyboard navigation

**Documentation**: See `src/main/frontend/docs/` for design tokens, component patterns, and accessibility requirements

## Testing Guidelines

### Backend Tests (JUnit 5)

- Tests covering utilities, external API clients, services, DTOs
- Use **WireMock** for HTTP mocking (see `NoaaSwpcClientTest.java`)
- Use **Mockito** for dependency mocking
- Recent Java versions require ByteBuddy experimental mode (configured in build.gradle)

### Frontend Tests (Vitest + React Testing Library)

- Tests in `src/test/frontend/` directory (parallel to `src/main/frontend/`)
- Use **jest-axe** for automated WCAG 2.1 AA compliance testing
- Use **jsdom** environment (faster than browser mode)
- Import components with `Frontend/` alias (configured in `vitest.config.ts`)

**Test Patterns**: See existing test files for reference:

- `src/test/frontend/components/activity/ActivityCard.test.tsx` - Component testing
- `src/test/frontend/components/activity/usePriorityCalculation.test.ts` - Hook testing
- `src/test/frontend/components/activity/ActivitySystem.a11y.test.tsx` - Accessibility testing

**Coverage Targets**: 80%+ statements/functions/lines, 75%+ branches

## External Data Sources

### NOAA Space Weather Prediction Center

- **Endpoint**: `https://services.swpc.noaa.gov/json/solar-cycle/observed-solar-cycle-indices.json`
- **Data**: Solar Flux Index (SFI), Sunspot Number
- **Cache TTL**: 5 minutes
- **Known Issues**: Sometimes returns partial dates - client has fallback parsing

### HamQSL Solar XML Feed

- **Endpoint**: `http://www.hamqsl.com/solarxml.php`
- **Data**: Solar indices + band-by-band conditions
- **Cache TTL**: 30 minutes
- **Known Issues**: Malformed DOCTYPE - client disables DTD processing

## Configuration

- **Primary config**: `src/main/resources/application.yml`
- **Development profile**: `src/main/resources/application-dev.yml` (create for local overrides)
- **Circuit breakers**: 50% failure threshold, automatic half-open transition
- **Retry**: 3 attempts, 500ms wait (2 attempts for HamQSL)
- **Cache**: Caffeine with 500 entry max, 10m expiry

## Java Version Compatibility

**Critical**: This project uses Java 25 for both compilation and bytecode target.

**build.gradle configuration**:

- `languageVersion = JavaLanguageVersion.of(25)` - Compile with Java 25
- `sourceCompatibility = JavaVersion.VERSION_25` - Target Java 25 bytecode
- `targetCompatibility = JavaVersion.VERSION_25`

**Why**: Spring Boot and Vaadin Hilla require Java 17+. Mockito needs ByteBuddy experimental mode for Java 25.

**Testing JVM args** (configured in build.gradle):

```
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
-Dnet.bytebuddy.experimental=true
```

## Claude Code Agents & Commands

This repository includes specialized agents and commands in `.claude/`:

### Agents

- **`java-test-debugger`**: Debug JUnit failures (Test → Analyze → Hypothesize → Fix → Verify)
- **`gradle-build-validator`**: Full-stack build validation (auto-kills port 8080 conflicts)
- **`code-refactoring`**: SOLID-focused refactoring (requires clean git + passing tests)

### Commands

- **`/validate-build`**: Pre-commit validation workflow
- **`/plan-tests`**: 5-layer test pyramid planning
- **`/find-refactor-candidates`**: Data-driven refactoring priorities
- **`/commit`**: Conventional commits with NextSkip scopes

**Usage**: Invoke via `/command-name` or natural language matching their domain

## Common Development Patterns

### Adding a New External API Client

1. Create DTO with validation (see `NoaaSolarCycleEntry.java`)
2. Create client with Circuit Breaker + Retry + Cache (see `NoaaSwpcClient.java`)
3. Configure circuit breaker in `application.yml`
4. Write WireMock tests (see `NoaaSwpcClientTest.java`)

### Adding a New Hilla Endpoint

1. Create endpoint with `@BrowserCallable` in `api/` package
2. Hilla auto-generates TypeScript client in `src/main/frontend/generated/`
3. Use generated client in React components

### Adding Frontend Components

1. Follow activity grid patterns (see `src/main/frontend/components/activity/`)
2. Use design tokens from `src/main/frontend/styles/global.css`
3. Write tests in `src/test/frontend/` directory
4. Ensure WCAG 2.1 AA compliance with jest-axe

## Future Development

See `nextskip-project-plan.md` for detailed roadmap:

- **Phase 2**: POTA/SOTA activations module
- **Phase 3**: Satellite tracking (N2YO, Celestrak)
- **Phase 4**: Real-time HF activity (PSKReporter MQTT)
- **Phase 5**: Contest calendar
- **Phase 6**: Aggregated dashboard with WebSocket live updates
- **Phase 7**: Spring AI conversational assistant

Each module follows the same patterns established in Phase 1.
