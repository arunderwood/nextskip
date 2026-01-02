# Testing Guidelines

NextSkip uses a layered testing strategy with property-based testing for algorithmic code, integration tests for external dependencies, and E2E tests for critical user flows.

> **Reviewing tests?** Use the `test-quality-expert` agent to validate test quality.

## Test Pyramid

| Layer | Purpose | Backend | Frontend |
|-------|---------|---------|----------|
| **Unit** | Test functions/classes in isolation | JUnit 5, Mockito | Vitest |
| **Property** | Verify invariants across random inputs | jqwik | fast-check |
| **Integration** | Test with real dependencies | TestContainers, WireMock | React Testing Library |
| **Mutation** | Verify test quality by injecting bugs | PIT (pitest) | - |
| **E2E** | Test full user flows | - | Playwright |

**Target Distribution:** ~70% unit, ~20% integration, ~10% E2E

---

## Testing Philosophy

### Test Invariants, Not Implementations

Tests should verify contracts that remain stable when algorithms are tuned:

```java
// GOOD - Tests the contract
assertThat(score).isBetween(MIN_SCORE, MAX_SCORE);
assertThat(fresh.getScore()).isGreaterThanOrEqualTo(stale.getScore());

// BAD - Breaks when algorithm is tuned
assertEquals(100, activation.getScore());
assertEquals(85, olderActivation.getScore());
```

### Deterministic Tests

Tests must produce the same result every time:
- Inject `Clock` for time-dependent code (never use `Instant.now()` in assertions)
- Use fixed test data from fixtures
- Avoid random data without property testing

---

## Required Patterns

### 1. Use Shared Fixtures

Use builder-pattern fixtures instead of local factory methods:

```java
// GOOD - Shared fixture with fluent API
import static io.nextskip.test.fixtures.ActivationFixtures.pota;

Activation fresh = pota().spottedNow().build();
Activation stale = pota().spottedHoursAgo(2).build();

// BAD - Local factory (duplicates across tests)
private Activation createActivation(Instant time) { ... }
```

**Find fixtures:**
```bash
# Backend
ls src/test/java/io/nextskip/test/fixtures/

# Frontend
ls src/test/frontend/fixtures/
```

### 2. Use Test Constants

Replace magic numbers with named constants:

```java
// GOOD - Self-documenting
import static io.nextskip.test.TestConstants.*;

assertThat(score).isBetween(MIN_SCORE, MAX_SCORE);
assertThat(kIndex).isLessThan(K_INDEX_FAVORABLE_THRESHOLD);

// BAD - Magic numbers
assertThat(score).isBetween(0, 100);
assertThat(kIndex).isLessThan(4);
```

**Available constants:** `src/test/java/io/nextskip/test/TestConstants.java`

### 3. Inject Clock for Time-Dependent Tests

Never rely on real system time in tests:

```java
private static final Instant FIXED_TIME = Instant.parse("2025-01-15T12:00:00Z");
private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_TIME, ZoneOffset.UTC);

@BeforeEach
void setUp() {
    service = new MyService(dependency, FIXED_CLOCK);
}

@Test
void testTimestamp_MatchesFixedClock() {
    Result result = service.doSomething();
    assertEquals(FIXED_TIME, result.timestamp());
}
```

### 4. Follow Naming Convention

Java tests (enforced by PMD):
```java
@Test
void testMethodName_Scenario_ExpectedResult() { }

// Examples:
void testGetScore_FreshActivation_ReturnsHighScore() { }
void testFetch_ServerError_ReturnsEmpty() { }
```

Frontend tests:
```typescript
describe('ComponentName', () => {
  it('should do X when Y', () => { });
});
```

---

## Property-Based Testing

Use property tests for code with invariants that must hold across all inputs.

### When to Use

- **Bounds:** Score always in [0, 100]
- **Ordering:** GOOD >= FAIR >= POOR >= UNKNOWN
- **Monotonicity:** Newer data → higher or equal score
- **Idempotency:** Same input → same output

### Backend (jqwik)

```java
@Property(tries = 100)
void scoreAlwaysInBounds(
        @ForAll @LongRange(min = -60, max = 120) long minutesAgo) {
    Activation activation = pota()
        .spottedAt(Instant.now().minus(minutesAgo, ChronoUnit.MINUTES))
        .build();

    assertThat(activation.getScore()).isBetween(MIN_SCORE, MAX_SCORE);
}

@Provide
Arbitrary<BandConditionRating> bandConditionRatings() {
    return Arbitraries.of(BandConditionRating.values());
}
```

### Frontend (fast-check)

```typescript
import fc from 'fast-check';

it('should always return priority in [0, 100]', () => {
  fc.assert(
    fc.property(
      fc.integer({ min: 0, max: 100 }),
      fc.boolean(),
      (score, favorable) => {
        const priority = calculatePriority({ score, favorable });
        expect(priority).toBeGreaterThanOrEqual(0);
        expect(priority).toBeLessThanOrEqual(100);
      }
    ),
    { numRuns: 100 }
  );
});
```

---

## Mutation Testing (PIT)

Mutation testing verifies test quality by introducing bugs (mutations) into the code and checking if tests catch them. A mutation that survives indicates a gap in test coverage or assertion strength.

### When to Use

- **After writing tests** - Verify tests actually catch bugs
- **Low confidence code** - Critical algorithms, scoring logic
- **Code reviews** - Validate test effectiveness

### Running Mutation Tests

```bash
# Run mutation testing (included in ./gradlew check)
./gradlew pitest

# View HTML report
open build/reports/pitest/index.html

# Skip pitest for faster local iteration
./gradlew check -x pitest
```

### Understanding Results

| Metric | Target | Meaning |
|--------|--------|---------|
| **Mutation Score** | >75% | Percentage of mutations killed by tests |
| **Test Strength** | >80% | Kill rate when tests cover the code |
| **No Coverage** | <5% | Mutations in untested code |

### Interpreting Surviving Mutations

```java
// Original code
public boolean isFavorable() {
    return score >= 70;  // Mutation: changed >= to >
}

// If this mutation SURVIVES, your tests don't verify the boundary!
// Add a test for score == 70
```

### Common Mutation Types

| Mutator | What It Does | How to Kill |
|---------|--------------|-------------|
| `ConditionalsBoundary` | `>=` → `>`, `<` → `<=` | Test boundary values |
| `NullReturns` | Return `null` instead of object | Assert non-null returns |
| `VoidMethodCall` | Remove void method calls | Verify side effects |
| `MathMutator` | `+` → `-`, `*` → `/` | Test arithmetic results |

### Performance Optimization

Mutation testing uses **incremental analysis** - only changed code is re-analyzed:

| Scenario | Time |
|----------|------|
| First run (no history) | ~6 min |
| Incremental (code change) | ~1-2 min |
| No changes (UP-TO-DATE) | <1 sec |

History is stored in `.pitest-history` and cached in CI.

### Configuration

Located in `build.gradle`:

```gradle
pitest {
    targetClasses = ['io.nextskip.*']
    threads = <dynamic based on CPU cores>
    junit5PluginVersion = '1.2.1'
    historyInputLocation = file("${rootDir}/.pitest-history")
    historyOutputLocation = file("${rootDir}/.pitest-history")
}
```

---

## Backend Testing

### Base Classes

| Class | Use When |
|-------|----------|
| `AbstractIntegrationTest` | Need database connection |
| `AbstractPersistenceTest` | Testing repositories (includes cleanup) |
| `AbstractSchedulerTest` | Testing scheduler state |

```java
class MyRepositoryTest extends AbstractPersistenceTest {
    @Autowired
    private MyRepository repository;

    @Override
    protected Collection<JpaRepository<?, ?>> getRepositoriesToClean() {
        return List.of(repository);
    }
}
```

### External API Testing (WireMock)

```java
private WireMockServer wireMockServer;

@BeforeEach
void setUp() {
    wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
    wireMockServer.start();
    client = new MyClient(wireMockServer.baseUrl());
}

@Test
void testFetch_Success() {
    wireMockServer.stubFor(get(urlEqualTo("/api"))
        .willReturn(aResponse()
            .withStatus(200)
            .withBody("{\"value\": 42}")));

    var result = client.fetch();
    assertEquals(42, result.getValue());
}
```

---

## Frontend Testing

### Component Tests

```typescript
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

it('renders with accessible content', async () => {
  const { container } = render(<MyComponent data={mockData} />);

  expect(screen.getByRole('heading')).toHaveTextContent('Title');
  expect(await axe(container)).toHaveNoViolations();
});
```

### Mock Hilla Types

Use mock stubs in `src/test/frontend/mocks/generated/` to avoid Gradle dependency on generated types.

---

## Coverage

### Commands

```bash
# Backend - Line/Branch Coverage
./gradlew test jacocoTestReport
./gradlew deltaCoverage
open build/reports/jacoco/test/html/index.html

# Backend - Mutation Coverage
./gradlew pitest
open build/reports/pitest/index.html

# Frontend
npm run test:coverage
npm run test:delta
```

### Thresholds

| Metric | Overall | Delta (changed code) |
|--------|---------|---------------------|
| Instruction | 75% | 80% |
| Branch | 65% | 70% |
| Line | - | 80% |
| Mutation Score | 75% | - |
| Test Strength | 80% | - |

---

## File Organization

```
src/test/java/io/nextskip/
├── test/                      # Shared infrastructure
│   ├── Abstract*Test.java     # Base classes
│   ├── TestConstants.java     # Shared constants
│   ├── TestPostgresContainer.java
│   └── fixtures/              # Builder-pattern fixtures
└── [module]/
    ├── model/                 # Unit tests
    ├── internal/              # Integration tests
    └── api/                   # Endpoint tests

src/test/frontend/
├── setup.ts                   # Global test setup
├── testConstants.ts           # Shared constants
├── fixtures/                  # Mock factories
├── mocks/generated/           # Hilla type stubs
└── components/                # Component tests
```

---

## Quick Reference

| Pattern | Use | Avoid |
|---------|-----|-------|
| Test data | Shared fixtures | Local factory methods |
| Constants | TestConstants | Magic numbers |
| Time | Clock injection | `Instant.now()` in assertions |
| Naming | `testX_Y_Z()` | `testX()` |
| Assertions | Invariant checks | Exact value comparisons |
| Mocking | Interfaces | Concrete classes |
| Boundaries | Test edge cases (==, boundary values) | Only happy path |
| Mutation | Run `./gradlew pitest` after writing tests | Ignoring surviving mutations |
