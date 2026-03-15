# Tasks: FT4 and FT2 Digital Mode Support

**Input**: Design documents from `/specs/002-ft4-ft2-modes/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configuration changes that enable FT4/FT2 data ingestion

- [x] T001 Add FT4 and FT2 MQTT topic subscriptions to `src/main/resources/application.yml` under `nextskip.spots.mqtt.topics` (add `pskr/filter/v2/+/FT4/#` and `pskr/filter/v2/+/FT2/#`)
- [x] T002 Add rarity multiplier configuration to `src/main/resources/application.yml` under new `nextskip.spots.scoring.rarity-multipliers` section (FT8: 1.0, FT4: 1.5, FT2: 3.0)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Refactor aggregation layer from single-mode-per-band to multi-mode-per-band. MUST be complete before any user story can be implemented.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T003 Add FT2 enum value to `src/main/java/io/nextskip/spots/model/ModeWindow.java` with 15-minute current window and 1-hour baseline (same as FT8/FT4). Update `forMode()` to handle "FT2" string.
- [x] T004 Add repository query method to `src/main/java/io/nextskip/spots/persistence/repository/SpotRepository.java` that returns spot counts grouped by both band AND mode within a time window (needed by the refactored aggregator)
- [x] T005 Refactor `src/main/java/io/nextskip/spots/internal/aggregation/BandActivityAggregator.java` to aggregate per band+mode pair instead of picking a single primary mode per band. Change `aggregateAllBands()` to produce one BandActivity per active band+mode combination. Remove `determinePrimaryMode()` method. Return results as a flat map with `"{band}_{mode}"` composite keys.
- [x] T006 Update `src/main/java/io/nextskip/spots/api/BandActivityResponse.java` to use composite `"{band}_{mode}"` keys in the `bandActivities` map (e.g., `"20m_FT8"`, `"20m_FT4"`)
- [x] T007 Update `src/main/java/io/nextskip/spots/internal/SpotsServiceImpl.java` to adapt to the new aggregator return type and build BandActivityResponse with composite keys
- [x] T008 Update `src/main/java/io/nextskip/spots/api/SpotsService.java` interface to change `getBandActivityForBand` return type from single `BandActivity` to `List<BandActivity>` (one per active mode on that band)
- [x] T009 Update `src/main/java/io/nextskip/spots/api/SpotsEndpoint.java` to match the updated `SpotsService` interface return type
- [x] T010 Update `src/test/java/io/nextskip/spots/model/ModeWindowTest.java` to test FT2 enum value, window parameters, `forMode("FT2")` lookup, and `forMode("UNKNOWN")` returns empty/null (edge case: unrecognized mode strings)
- [x] T011 Update `src/test/java/io/nextskip/spots/internal/aggregation/BandActivityAggregatorTest.java` to test multi-mode aggregation: verify separate BandActivity records for FT8 and FT4 on the same band, verify composite keys, verify zero-spot modes are excluded
- [x] T012 Update `src/test/java/io/nextskip/spots/api/SpotsEndpointTest.java` to test getBandActivity returns composite-keyed responses and getBandActivityForBand returns a list of modes

**Checkpoint**: Aggregation layer now produces per-mode band activity. Frontend will not yet show new modes (mode registry unchanged). FT8 behavior must be verified as unbroken.

---

## Phase 3: User Story 1 - View FT4 Band Activity (Priority: P1) MVP

**Goal**: Radio operators can see real-time FT4 band activity cards on the dashboard with the same data richness as FT8 cards.

**Independent Test**: Load the dashboard and verify FT4 band activity cards appear with live spot data, trend indicators, DX reach, and path information for each active band.

### Implementation for User Story 1

- [x] T013 [US1] Enable FT4 in `src/main/frontend/config/modeRegistry.ts` by changing `isSupported: false` to `isSupported: true` for the FT4 entry
- [x] T014 [US1] Update frontend test mocks in `src/test/frontend/mocks/generated/` if any mock stubs reference the old single-mode-per-band BandActivityResponse structure (add FT4 mock data with composite keys)
- [x] T015 [US1] Update frontend tests in `src/test/frontend/components/cards/band-activity/` to verify FT4 cards render with correct title format (e.g., "20m FT4"), spot count, trend, DX reach, path data, hotness indicator CSS classes (FR-010), and WCAG 2.1 AA compliance via jest-axe (SC-006)

**Checkpoint**: FT4 band activity cards appear on the dashboard alongside FT8 cards. Cards are labeled with band+mode. Scoring does not yet include rarity boost (FT4 cards may rank lower than FT8 due to volume differences).

---

## Phase 4: User Story 3 - Rarity-Boosted Scoring (Priority: P1)

**Goal**: FT4 and FT2 activity scores are boosted by a fixed, configurable multiplier so less popular modes rank competitively with FT8 activity of comparable propagation quality.

**Independent Test**: Compare dashboard card ordering when FT4 activity is present alongside FT8. FT4 cards with meaningful activity should appear in competitive positions.

### Implementation for User Story 3

- [x] T016 [US3] Create a Spring configuration properties class (e.g., `src/main/java/io/nextskip/spots/internal/ScoringProperties.java`) to bind `nextskip.spots.scoring.rarity-multipliers` map from application.yml. Provide default multipliers (FT8=1.0, FT4=1.5, FT2=3.0) for modes not explicitly configured.
- [x] T017 [US3] Add a `rarityMultiplier` field to `src/main/java/io/nextskip/spots/model/BandActivity.java` (set at construction time). Update `getScore()` to multiply the `normalizeActivity()` result by `rarityMultiplier` before the weighted combination. Cap the boosted activity score at 100 to prevent total score overflow.
- [x] T018 [US3] Update `src/main/java/io/nextskip/spots/internal/aggregation/BandActivityAggregator.java` to accept rarity multipliers (injected from ScoringProperties) and pass the appropriate multiplier when constructing each BandActivity based on its mode.
- [x] T019 [US3] Update `src/test/java/io/nextskip/spots/model/BandActivityTest.java` to test rarity multiplier scoring: verify FT4 (1.5x) activity score is boosted, verify FT2 (3.0x) activity score is boosted, verify boosted activity score is capped at 100, verify low-quality rare-mode cards cannot outrank high-quality FT8 cards (FR-007 safety check)
- [x] T020 [US3] Update `src/test/java/io/nextskip/spots/internal/aggregation/BandActivityAggregatorTest.java` to verify rarity multipliers are correctly passed through to BandActivity construction

**Checkpoint**: FT4 cards now score competitively with FT8 cards of comparable propagation quality. Low-quality FT4 cards do not outrank high-quality FT8 cards.

---

## Phase 5: User Story 2 - View FT2 Band Activity (Priority: P2)

**Goal**: Radio operators can see real-time FT2 band activity cards on the dashboard, with the same data and rarity-boosted scoring as FT4.

**Independent Test**: Load the dashboard and verify FT2 band activity cards appear when FT2 spots are present, displaying the same data fields as FT8 and FT4 cards.

### Implementation for User Story 2

- [x] T021 [US2] Add FT2 entry to `src/main/frontend/config/modeRegistry.ts` with `isSupported: true`. Add `'FT2'` to the Mode union type in `src/main/frontend/types/` if not already present.
- [x] T022 [US2] Update frontend test mocks in `src/test/frontend/mocks/generated/` to include FT2 mock data with composite keys
- [x] T023 [US2] Update frontend tests in `src/test/frontend/components/cards/band-activity/` to verify FT2 cards render correctly with band+mode title format, all data fields, hotness indicator CSS classes (FR-010), and WCAG 2.1 AA compliance via jest-axe (SC-006)

**Checkpoint**: FT2 band activity cards appear on the dashboard alongside FT8 and FT4 cards with rarity-boosted scoring. All three modes are fully functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and quality assurance across all stories

- [x] T024 Add a frontend test that renders cards for FT8, FT4, and FT2 simultaneously and asserts they are interleaved by score (not grouped by mode), verifying FR-011
- [x] T025 Run `./gradlew check` to verify all backend quality gates pass (Checkstyle, PMD, SpotBugs, JaCoCo coverage, PIT mutation testing)
- [x] T026 Run `npm run validate` to verify frontend format, lint, tests, and delta coverage pass
- [ ] T027 Run `./gradlew bootRun` and verify application starts without errors, MQTT subscribes to all 3 topics (check logs), and FT4/FT8 cards appear on dashboard
- [ ] T028 Verify quickstart.md scenarios: MQTT topic subscription logs, FT4 card rendering, rarity multiplier effect on card ordering

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (Phase 2) - No dependencies on other stories
- **US3 (Phase 4)**: Depends on Foundational (Phase 2) - Can run in parallel with US1 but benefits from US1 being complete for visual verification
- **US2 (Phase 5)**: Depends on Foundational (Phase 2) - Benefits from US3 (rarity scoring) being complete so FT2 cards score competitively from the start
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational - independently testable (FT4 cards appear)
- **US3 (P1)**: Can start after Foundational - independently testable (scoring boost verifiable via unit tests). Best verified visually after US1.
- **US2 (P2)**: Can start after Foundational - independently testable (FT2 cards appear). Recommended after US3 so FT2 gets rarity boost immediately.

### Within Each User Story

- Frontend registry changes before test updates
- Test updates verify acceptance scenarios

### Parallel Opportunities

- T001 and T002 can run in parallel (different config sections)
- T003 and T004 can run in parallel (different files)
- T006 and T008 can run in parallel (different files, both are type changes)
- T010, T011, T012 can run in parallel (different test files)
- T013 and T016 can run in parallel (different files, US1 and US3 are independent)
- T021, T022, T023 can run in parallel with preceding story tasks (US2 is independent)

---

## Parallel Example: Foundational Phase

```bash
# Launch independent model/repository changes:
Task: "Add FT2 to ModeWindow enum in src/main/java/io/nextskip/spots/model/ModeWindow.java"
Task: "Add repository query in src/main/java/io/nextskip/spots/persistence/repository/SpotRepository.java"

# After aggregator refactor, launch test updates together:
Task: "Update ModeWindowTest.java"
Task: "Update BandActivityAggregatorTest.java"
Task: "Update SpotsEndpointTest.java"
```

## Parallel Example: User Stories

```bash
# After Foundational phase, launch US1 and US3 in parallel:
# US1 (frontend):
Task: "Enable FT4 in modeRegistry.ts"

# US3 (backend, different files):
Task: "Create ScoringProperties config class"
Task: "Add rarityMultiplier to BandActivity"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (config changes)
2. Complete Phase 2: Foundational (aggregator refactor)
3. Complete Phase 3: User Story 1 (FT4 visible on dashboard)
4. **STOP and VALIDATE**: FT4 cards appear with full data, interleaved with FT8
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational -> Multi-mode aggregation working
2. Add US1 (FT4 visible) -> Test independently -> Deploy/Demo (MVP!)
3. Add US3 (Rarity scoring) -> FT4 cards rank competitively -> Deploy/Demo
4. Add US2 (FT2 visible) -> All modes working with rarity scoring -> Deploy/Demo
5. Each story adds value without breaking previous stories

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- No database migrations needed - existing schema supports multi-mode
- Frontend card system already iterates all modes (Open-Closed Principle) - only registry changes needed
- PskReporterMqttSource already handles multi-topic subscription - no code changes needed there
