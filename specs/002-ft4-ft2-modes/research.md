# Research: FT4 and FT2 Digital Mode Support

**Branch**: `002-ft4-ft2-modes` | **Date**: 2026-03-13

## Research Tasks & Findings

### R1: PSKReporter MQTT Topic Support for FT4/FT2

**Decision**: Subscribe to additional MQTT topics `pskr/filter/v2/+/FT4/#` and `pskr/filter/v2/+/FT2/#`

**Rationale**: PSKReporter MQTT broker uses topic-based filtering where the mode is part of the topic path (`pskr/filter/v2/{band}/{mode}/{grid}`). The existing `PskReporterMqttSource` already accepts a `List<String>` of topics and subscribes to all of them. Adding FT4 and FT2 topics requires only a configuration change in `application.yml`.

**Alternatives Considered**:
- Wildcard topic `pskr/filter/v2/+/+/#` (all modes) -- rejected: would ingest CW, SSB, RTTY and all other modes, significantly increasing data volume without current need
- Single topic with client-side mode filtering -- rejected: PSKReporter's topic hierarchy is the intended filtering mechanism

---

### R2: ModeWindow Configuration for FT2

**Decision**: Add `FT2` to the `ModeWindow` enum with the same 15-minute window and 1-hour baseline as FT8/FT4

**Rationale**: FT2 is a WSJT-X digital mode with a 6-second cycle (vs FT8's 15-second and FT4's 7.5-second cycles). Despite the faster cycle time, the aggregation window should match FT4/FT8 because:
- The purpose of the window is to accumulate enough spots for meaningful trend analysis, not to match the mode's cycle time
- FT2 has far fewer active operators, so a shorter window would yield too few spots for reliable statistics
- Consistency across FT-family modes simplifies the UX (operators expect similar behavior)

**Alternatives Considered**:
- Shorter 5-minute window for FT2 -- rejected: too few spots to calculate meaningful trends given FT2's low popularity
- Different baseline multiplier -- rejected: no evidence that FT2 trend patterns differ from FT4/FT8

---

### R3: Aggregation Layer Refactoring (Single-Mode → Multi-Mode per Band)

**Decision**: Refactor `BandActivityAggregator` to return `Map<String, Map<String, BandActivity>>` (band → mode → activity) instead of `Map<String, BandActivity>` (band → activity with primary mode)

**Rationale**: The current aggregator uses `determinePrimaryMode()` to pick the most common mode per band and returns a single `BandActivity` per band. This design cannot surface FT4 or FT2 activity on bands dominated by FT8. The aggregator must produce independent activity records for each band+mode pair.

**Alternatives Considered**:
- Keep single-mode-per-band and add separate FT4/FT2 aggregation service -- rejected: duplicates logic, violates DRY
- Run aggregation separately per mode in parallel -- acceptable alternative but sequential iteration is simpler and the current 1-minute refresh cycle has ample headroom

**Impact**:
- `BandActivityAggregator.aggregateAllBands()` return type changes
- `SpotsServiceImpl` must adapt to new structure
- `BandActivityResponse` DTO must carry the nested structure
- Frontend `findActivity()` already searches by band+mode -- compatible
- `SpotRepository` may need a query that groups by band AND mode

---

### R4: Rarity Multiplier Design

**Decision**: Apply a fixed, configurable multiplier to the activity score component of `BandActivity.getScore()` before the weighted combination

**Rationale**: The rarity multiplier should boost the activity component (40% of total score) because that's the component most affected by mode popularity differences. Trend, DX, and path scores are already normalized and mode-independent. By multiplying only the activity score, the boost compensates for volume differences without distorting propagation quality signals.

**Application point**: In `BandActivity.getScore()`, after `normalizeActivity()` returns 0-100, multiply by the mode's rarity weight before the weighted sum. Cap the result at 100 to prevent the total score from exceeding the 0-100 range.

**Configuration**: Add to `application.yml`:
```yaml
nextskip:
  spots:
    scoring:
      rarity-multipliers:
        FT8: 1.0
        FT4: 1.5
        FT2: 3.0
```

**Starting values rationale**:
- FT8 = 1.0x (baseline, most popular mode)
- FT4 = 1.5x (roughly 5-10x fewer spots than FT8 in typical conditions)
- FT2 = 3.0x (very rare, often 50-100x fewer spots than FT8)

**Safety**: FR-007 requires that low-quality rare mode activity doesn't outrank high-quality FT8. Since the multiplier only affects the activity component (40% weight), and the total is capped at 100, a card with low trend/DX/path scores cannot achieve a high total score even with a 3x activity boost.

**Alternatives Considered**:
- Multiply the final composite score -- rejected: would amplify all components equally, potentially pushing mediocre rare-mode propagation above excellent FT8 propagation
- Dynamic multiplier from rolling volume ratios -- rejected per clarification session (added complexity, harder to test and predict)

---

### R5: Frontend Mode Registry and FT2 Type

**Decision**: Enable FT4 in mode registry (`isSupported: true`), add FT2 entry (`isSupported: true`), and add 'FT2' to the Mode union type

**Rationale**: The frontend card system already iterates all modes from `getAllModes()` and creates cards when activity data exists. The only changes needed are registry configuration and type updates. No card component changes are required (Open-Closed Principle).

**Alternatives Considered**:
- Add FT2 as unsupported initially -- rejected: spec requires FT2 support in this feature

---

### R6: BandActivityResponse DTO Structure

**Decision**: Change `BandActivityResponse.bandActivities` from `Map<String, BandActivity>` (keyed by band) to `Map<String, BandActivity>` keyed by `"{band}_{mode}"` composite key (e.g., `"20m_FT4"`)

**Rationale**: A flat map with composite keys is the simplest change that preserves the existing serialization pattern. The frontend already iterates all entries and matches by band+mode, so the key format doesn't affect lookup behavior.

**Alternatives Considered**:
- Nested `Map<String, Map<String, BandActivity>>` -- rejected: changes the JSON shape more drastically, requiring frontend DTO updates
- List of BandActivity objects (not keyed) -- rejected: loses O(1) lookup by key
