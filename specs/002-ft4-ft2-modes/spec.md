# Feature Specification: FT4 and FT2 Digital Mode Support

**Feature Branch**: `002-ft4-ft2-modes`
**Created**: 2026-03-13
**Status**: Draft
**Input**: User description: "Nextskip currently supports FT8 digital mode band activity but it should also support the FT4 and FT2 modes that are increasing in popularity. It should be possible for a user to see all the same data they see today on FT8 on both of these new modes. Because these modes are less popular than FT8 that makes those spots more rare and their score should be increased accordingly to offset the sheer scale of FT8 spots."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View FT4 Band Activity (Priority: P1)

As a radio operator interested in FT4, I want to see real-time FT4 band activity on the dashboard so I can identify which bands have active FT4 operators and find opportunities to make contacts on this faster digital mode.

FT4 activity cards should appear alongside existing FT8 cards on the dashboard, showing the same data: spot count, trend direction, maximum DX distance, and active propagation paths. Each card should be clearly labeled with the mode (e.g., "20m FT4") so operators can distinguish FT4 from FT8 activity at a glance.

**Why this priority**: FT4 is the most popular of the two new modes and has the largest existing user base after FT8. Adding FT4 support delivers the most value to the most users and validates the multi-mode architecture for FT2.

**Independent Test**: Can be fully tested by loading the dashboard and verifying that FT4 band activity cards appear with live spot data, trend indicators, DX reach, and path information for each active band.

**Acceptance Scenarios**:

1. **Given** the dashboard is loaded and FT4 spots are being received, **When** a user views the dashboard, **Then** they see FT4 band activity cards for each band with active FT4 spots.
2. **Given** FT4 activity cards are displayed, **When** a user examines a card, **Then** it shows spot count, trend percentage, maximum DX distance, active propagation paths, and propagation condition rating -- the same data shown on FT8 cards.
3. **Given** FT4 cards are displayed alongside FT8 cards, **When** a user scans the dashboard, **Then** each card is clearly labeled with both band and mode (e.g., "20m FT4" vs "20m FT8").

---

### User Story 2 - View FT2 Band Activity (Priority: P2)

As a radio operator experimenting with FT2, I want to see real-time FT2 band activity on the dashboard so I can find the rare occasions when FT2 activity is present on a band and jump on those opportunities.

FT2 activity cards should appear on the dashboard with the same data fields as FT8 and FT4 cards. Because FT2 is the least popular of the three modes, even small amounts of activity are noteworthy and should be surfaced prominently.

**Why this priority**: FT2 has a smaller user base than FT4, so it delivers value to fewer operators. However, the implementation is nearly identical to FT4, making it low incremental effort once FT4 is working.

**Independent Test**: Can be fully tested by loading the dashboard and verifying that FT2 band activity cards appear when FT2 spots are present, displaying the same data fields as FT8 and FT4 cards.

**Acceptance Scenarios**:

1. **Given** the dashboard is loaded and FT2 spots are being received, **When** a user views the dashboard, **Then** they see FT2 band activity cards for bands with active FT2 spots.
2. **Given** FT2 activity cards are displayed, **When** a user examines a card, **Then** it shows the same data fields as FT8 cards: spot count, trend, DX reach, active paths, and condition rating.

---

### User Story 3 - Rarity-Boosted Scoring for Less Popular Modes (Priority: P1)

As a radio operator, I want FT4 and FT2 activity to be scored higher relative to their spot volume so that interesting activity on these less popular modes is not buried beneath the massive volume of FT8 spots.

FT8 generates significantly more spots than FT4 or FT2. Without a scoring adjustment, FT4 and FT2 cards would almost always appear below FT8 cards on the dashboard because their raw spot counts are lower. A rarity multiplier should boost the scores of less popular modes so that meaningful activity on FT4 or FT2 is ranked comparably to equivalent-quality FT8 activity.

**Why this priority**: This is essential for the feature to be useful. Without rarity-based scoring, FT4 and FT2 cards would always sink to the bottom of the dashboard, defeating the purpose of adding these modes.

**Independent Test**: Can be tested by comparing dashboard card ordering when FT4/FT2 activity is present alongside FT8 activity. Cards for less popular modes with meaningful activity should appear in competitive positions rather than always at the bottom.

**Acceptance Scenarios**:

1. **Given** a band has both FT8 and FT4 activity, **When** both have comparable propagation quality (similar DX distances, active paths, positive trends), **Then** the FT4 card appears at a similar position on the dashboard as the FT8 card, not buried at the bottom.
2. **Given** a band has FT2 activity with excellent propagation indicators (long DX, multiple paths, positive trend), **When** the dashboard is sorted by score, **Then** the FT2 card can rank higher than an FT8 card on a band with mediocre conditions.
3. **Given** a band has FT4 activity but very few spots and no notable propagation, **When** the dashboard is sorted, **Then** the rarity boost alone is not enough to push a low-quality FT4 card above a high-quality FT8 card.

---

### Edge Cases

- What happens when a mode has zero spots on all bands? No cards should appear for that mode on those bands -- the dashboard should not show empty cards.
- What happens when a new mode starts receiving spots for the first time during a session? The card should appear on the dashboard during the next data refresh without requiring a page reload.
- How does the system handle a sudden surge in FT4/FT2 popularity where spot volumes approach FT8 levels? The fixed rarity multiplier may over-boost scores in this scenario; operators can adjust the configuration values to reflect changed popularity levels.
- What happens when spot data contains an unrecognized mode string? The system should ignore spots with unrecognized modes and not display cards for them.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST receive and process real-time FT4 spots from the spot data source with the same data fields as FT8 spots (band, frequency, SNR, callsigns, grids, distance, timestamp).
- **FR-002**: System MUST receive and process real-time FT2 spots from the spot data source with the same data fields as FT8 spots.
- **FR-003**: System MUST display FT4 band activity cards on the dashboard with the same information as FT8 cards: spot count, trend indicator, maximum DX distance and path, active propagation paths, and condition rating.
- **FR-004**: System MUST display FT2 band activity cards on the dashboard with the same information as FT8 cards.
- **FR-005**: Each band activity card MUST clearly indicate both the band and the mode in its title (e.g., "20m FT4", "40m FT2").
- **FR-006**: System MUST apply a rarity-based scoring boost to FT4 and FT2 activity using a fixed, configurable multiplier per mode so that meaningful activity on these modes ranks competitively with FT8 activity of comparable propagation quality.
- **FR-007**: The rarity scoring boost MUST NOT cause low-quality activity on a rare mode to outrank high-quality activity on FT8. The boost should compensate for volume differences, not override propagation quality signals.
- **FR-008**: System MUST aggregate FT4 and FT2 spots independently from FT8 -- each mode's activity cards reflect only spots for that specific mode.
- **FR-009**: System MUST only display cards for band/mode combinations that have at least 1 spot within the aggregation window. No empty placeholder cards should be shown.
- **FR-010**: FT4 and FT2 cards MUST support the same visual hotness indicators as FT8 cards (hot, warm, neutral, cool) based on their calculated scores.
- **FR-011**: FT4 and FT2 cards MUST be interleaved with FT8 cards on the dashboard, sorted by score. No mode-based filtering or grouping is included in this feature.

### Key Entities

- **Spot**: A single reception report from the spot data network, containing source, band, mode, frequency, signal strength, callsigns, grid squares, distance, and timestamp. Mode distinguishes FT8, FT4, and FT2 spots.
- **Band Activity**: An aggregation of spots for a specific band and mode combination within a time window. Contains spot count, trend analysis, maximum DX distance, active propagation paths, and a calculated score. Each band can have multiple Band Activity records -- one per active mode.
- **Mode Rarity Weight**: A fixed, configurable scoring multiplier associated with each mode that reflects how uncommon that mode's spots are relative to FT8. Used to boost scores for less popular modes so they rank competitively on the dashboard. Values are tunable via configuration.

## Clarifications

### Session 2026-03-13

- Q: Should the dashboard show all mode cards interleaved by score, or should users be able to filter by mode? → A: All cards interleaved by score only (no filtering); rely on scoring to surface the best cards.
- Q: Should the rarity multiplier be fixed per mode or dynamically calculated from observed spot volumes? → A: Fixed multiplier per mode (e.g., FT4=1.5x, FT2=3x), tunable via configuration.
- Q: What is the minimum number of spots needed to display a band/mode card? → A: 1 spot is enough (any activity counts).

## Assumptions

- FT4 and FT2 spots are available from the same data source that currently provides FT8 spots (PSKReporter).
- FT4 uses the same time window parameters as FT8 for aggregation (short cycle digital modes have similar aggregation needs).
- FT2 uses similar time window parameters to FT4 and FT8, as it is also a short-cycle WSJT-X digital mode.
- The rarity multiplier values are fixed per mode and tunable via configuration. Reasonable starting values can be set based on known relative popularity (FT8=1.0x, FT4~1.5x, FT2~3x) and adjusted after observing real-world behavior.
- "FT2" refers to the FT2 mode as implemented in WSJT-X (a 6-second cycle variant of FT8), not a different protocol.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view FT4 band activity on the dashboard with the same data richness as FT8 within 1 data refresh cycle of FT4 spots being available.
- **SC-002**: Users can view FT2 band activity on the dashboard with the same data richness as FT8 within 1 data refresh cycle of FT2 spots being available.
- **SC-003**: When FT4 activity on a band has comparable propagation quality to FT8 activity on the same band, the FT4 card's calculated score is within 15% of the equivalent FT8 card's score after rarity multiplier is applied.
- **SC-004**: When FT2 activity on a band shows strong propagation indicators, the FT2 card ranks above FT8 cards on bands with weak or no propagation at least 80% of the time.
- **SC-005**: No dashboard cards appear for band/mode combinations with zero spots -- 100% of displayed cards represent active band/mode pairs.
- **SC-006**: All FT4 and FT2 cards are accessible via keyboard navigation and meet the same WCAG 2.1 AA compliance standards as existing FT8 cards.
