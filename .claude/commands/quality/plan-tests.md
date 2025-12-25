---
description: Plan test coverage using 5-layer testing pyramid
argument-hint: [target feature or module]
allowed-tools: Read, Grep, Glob, Bash
---

## Purpose

This command helps systematically expand test coverage beyond the current 60 tests by:

- Identifying gaps in existing test coverage
- Recommending specific tests to add at each layer
- Prioritizing test additions for maximum value
- Applying the testing pyramid strategy (unit → integration → E2E → property → mutation)

## When to Use

- ✅ **Planning new feature development** - Design tests before implementation
- ✅ **Improving existing code coverage** - Fill gaps in current test suite
- ✅ **After refactoring** - Ensure refactored code is well-tested
- ✅ **Before adding complex business logic** - Plan comprehensive validation

## Usage

Specify the target feature or module:

```
/plan-tests for propagation package
/plan-tests for NoaaSwpcClient
/plan-tests for dashboard frontend
```

Or in conversation:

```
User: Plan tests for the band condition feature
User: How should I test the solar indices caching?
```

## 5-Layer Testing Strategy

### Layer 1: Unit Tests (Foundation)

**Purpose**: Test individual classes/methods in isolation

**For NextSkip**:

- Test DTOs (NoaaSolarCycleEntry validation)
- Test utility methods (date parsing, band rating conversion)
- Test service logic in isolation (mock dependencies)
- Test exception handling paths
- Test edge cases and boundary values

**Framework**: JUnit 5, Mockito
**Coverage Target**: 80%+ for business logic

**Example Unit Tests**:

```java
@Test
void testNoaaSolarCycleEntry_Validation_RejectsOutOfRange() {
    var entry = new NoaaSolarCycleEntry("2025-01", 1500.0, 100);
    assertThrows(InvalidApiResponseException.class, () -> entry.validate());
}

@Test
void testParseBandRating_CaseInsensitive() {
    assertEquals(BandConditionRating.GOOD, parseBandRating("GOOD"));
    assertEquals(BandConditionRating.GOOD, parseBandRating("good"));
    assertEquals(BandConditionRating.GOOD, parseBandRating("Good"));
}
```

### Layer 2: Integration Tests

**Purpose**: Test component interactions

**For NextSkip**:

- Test Spring Boot context loading (@SpringBootTest)
- Test database interactions (if H2 in-memory for tests)
- Test caching behavior (ConcurrentMapCacheManager in tests)
- Test API client → service → controller flow
- Test resilience patterns (circuit breaker, retry)

**Framework**: Spring Boot Test, WireMock for external APIs, MockMvc for controllers
**Coverage Target**: Critical paths covered

**Example Integration Tests**:

```java
@SpringBootTest
class PropagationServiceIntegrationTest {
    @Autowired
    private PropagationService service;

    @MockBean
    private NoaaSwpcClient noaaClient;

    @Test
    void testGetPropagationData_IntegratesAllSources() {
        // Stub NOAA client
        when(noaaClient.fetchSolarIndices()).thenReturn(mockIndices);

        var result = service.getPropagationData();

        assertNotNull(result.solarIndices());
        assertNotNull(result.bandConditions());
    }
}
```

### Layer 3: End-to-End Tests

**Purpose**: Test complete user workflows in the browser

**For NextSkip**:

- Test dashboard loads and displays propagation data
- Test page title and header rendering
- Test dashboard cards render after loading
- Test last update timestamp displays
- Test error states when APIs are unavailable
- Test UI responds to data updates

**Framework**: Playwright
**Location**: `src/test/e2e/`
**Coverage Target**: Happy paths + critical error scenarios

**Example E2E Tests**:

```typescript
// src/test/e2e/dashboard.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('#outlet > *', { timeout: 10000 });
  });

  test('page loads successfully', async ({ page }) => {
    await page.waitForFunction(() => document.title !== '', { timeout: 5000 });
    await expect(page).toHaveTitle(/NextSkip/i);
  });

  test('dashboard cards render after loading', async ({ page }) => {
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });
    const cards = page.locator('.activity-card');
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBeGreaterThan(0);
  });
});
```

**Running E2E Tests**:

```bash
# Run all E2E tests
npm run e2e

# Run with Playwright UI for debugging
npm run e2e:ui

# Run in headed mode (see browser)
npm run e2e:headed
```

### Layer 4: Property-Based Tests (Optional, Advanced)

**Purpose**: Test properties that should hold for random inputs

**For NextSkip**:

- SolarIndices validation always rejects out-of-range values
- Date parsing handles any valid ISO-8601 format
- Band condition rating parsing is case-insensitive
- Caching always returns same result for same key within TTL

**Framework**: jqwik (Java property-based testing library)
**Coverage Target**: Domain invariants

**Example Property Test**:

```java
@Property
void testNoaaClientValidation_AlwaysRejectsInvalidFlux(@ForAll @DoubleRange(min = 1001, max = 10000) double invalidFlux) {
    var entry = new NoaaSolarCycleEntry("2025-01", invalidFlux, 100);
    assertThrows(InvalidApiResponseException.class, () -> entry.validate());
}

@Property
void testBandRatingParsing_CaseInsensitive(@ForAll("validRatings") String rating) {
    var upper = parseBandRating(rating.toUpperCase());
    var lower = parseBandRating(rating.toLowerCase());
    assertEquals(upper, lower);
}

@Provide
Arbitrary<String> validRatings() {
    return Arbitraries.of("good", "fair", "poor");
}
```

### Layer 5: Mutation Testing (Quality Metric)

**Purpose**: Verify tests actually catch bugs

**For NextSkip**:

- Run PITest to mutate code and verify tests fail
- Identify weak test assertions
- Improve test quality based on mutation score
- Ensure test suite is robust

**Framework**: PITest
**Coverage Target**: 70%+ mutation score

**How to Run**:

```bash
./gradlew pitest
open build/reports/pitest/index.html
```

**Mutations Caught by Good Tests**:

- Changing `<` to `<=` in boundary checks
- Removing null checks
- Inverting conditionals (`if (x)` → `if (!x)`)
- Changing return values

---

## Output Format

Provide a structured test plan:

```
## Test Plan for [TARGET]

### Current State

**Existing Tests**:
- 6 unit tests in NoaaSwpcClientTest
- 8 unit tests in HamQslClientTest
- 0 integration tests
- 4 E2E tests (Playwright in src/test/e2e/)
- 0 property-based tests
- Mutation testing not configured

**Coverage**:
- Line coverage: 85%
- Branch coverage: 72%
- Mutation score: Not measured

### Gaps Identified

**Layer 1 (Unit)**:
- ❌ Missing edge case tests for date parsing (invalid formats)
- ❌ No tests for fallback behavior when cache is empty
- ❌ Missing exception message validation

**Layer 2 (Integration)**:
- ❌ No Spring Boot context loading test
- ❌ No test for circuit breaker behavior
- ❌ Cache integration not tested

**Layer 3 (E2E)**:
- ✅ Dashboard loads successfully (Playwright)
- ✅ Page title and header rendering validated
- ✅ Dashboard cards render after loading
- ✅ Last update timestamp displays
- ❌ Error states when APIs are unavailable not tested
- ❌ UI response to data updates not tested

**Layer 4 (Property)**:
- ❌ Not implemented

**Layer 5 (Mutation)**:
- ❌ Not configured

### Recommended Tests

#### High Priority (Add First)

**Layer 1 - Unit Tests**:
1. Test date parsing with various malformed inputs (est: 15 min)
2. Test cache fallback when cache is null (est: 10 min)
3. Test all validation paths in SolarData.validate() (est: 20 min)

**Layer 2 - Integration Tests**:
1. Test Spring Boot context loads (@SpringBootTest) (est: 15 min)
2. Test PropagationService integrates NOAA + HamQSL (est: 30 min)
3. Test circuit breaker opens after failures (est: 25 min)

**Layer 3 - E2E Tests**:
1. ✅ IMPLEMENTED: Dashboard loads and displays content (4 tests in dashboard.spec.ts)
2. Test error message shown when API unavailable (est: 30 min)
3. Test UI responds to data updates (est: 40 min)
4. Test navigation between different views (est: 25 min)

#### Medium Priority (Schedule for Later)

**Layer 4 - Property Tests**:
1. Property: All valid ISO-8601 dates parse successfully (est: 30 min)
2. Property: Solar flux validation always rejects >1000 (est: 20 min)

**Layer 5 - Mutation Testing**:
1. Configure PITest in build.gradle (est: 15 min)
2. Run baseline mutation testing (est: 10 min)
3. Improve weak assertions identified by PITest (est: variable)

#### Low Priority (Future Enhancement)

**Performance Tests**:
- Test API client handles 100 concurrent requests
- Test cache reduces API calls under load

**Security Tests**:
- Test XXE attack prevention in XML parsing
- Test SQL injection prevention (if database added)

### Estimated Effort

**Total New Tests**: 15 tests across 5 layers
**Time to Implement**: ~5 hours
**Expected Coverage Increase**: 85% → 90%+
**Expected Mutation Score**: 70%+

### Dependencies

**For E2E Tests**:
- ✅ CONFIGURED: Playwright (@playwright/test v1.51.1) already installed
- ✅ Configuration file: playwright.config.ts
- Run: `npm run e2e` (local dev) or `npx playwright test` (CI)

**For Property Tests**:
- Add jqwik dependency to build.gradle

**For Mutation Tests**:
- Add PITest plugin to build.gradle
- Configure target packages

### Next Steps

1. Implement high-priority unit tests (Layer 1)
2. Add integration tests for Spring Boot context (Layer 2)
3. ✅ COMPLETE: E2E testing framework (Playwright) set up with 4 dashboard tests
4. Add additional E2E tests for error states and data updates (Layer 3)
5. Configure mutation testing (Layer 5)
6. Review mutation score and improve weak tests
7. Consider property-based tests for critical invariants (Layer 4)

## Recommendation

Start with **High Priority tests** for maximum ROI. This will:
- Improve coverage from 85% to ~90%
- Catch edge cases in date parsing
- Validate circuit breaker behavior
- Ensure dashboard renders correctly

Estimated time: 3-4 hours
```

---

## Test Pyramid Principles

**80% Unit, 15% Integration, 5% E2E**:

- Unit tests are fast, isolated, and catch most bugs
- Integration tests verify components work together
- E2E tests validate complete workflows but are slow

**Benefits**:

- Faster feedback (unit tests run in seconds)
- Easier debugging (isolated failures)
- Better code design (testable code is well-designed)
- Confidence in changes (regression prevention)

## Related Commands

- `/validate-build` - Run existing tests to establish baseline
- `/find-refactor-candidates` - Identify untested complex code
- `/commit` - Commit new tests with conventional commit format

## Example Usage

```
User: Plan tests for NoaaSwpcClient
```
