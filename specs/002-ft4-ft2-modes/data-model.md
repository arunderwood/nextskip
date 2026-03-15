# Data Model: FT4 and FT2 Digital Mode Support

**Branch**: `002-ft4-ft2-modes` | **Date**: 2026-03-13

## Entity Changes

### Spot (no schema changes)

The `Spot` record and `SpotEntity` already store the `mode` field (e.g., "FT8", "FT4", "FT2"). The existing database indices on `(mode, spotted_at)` support efficient mode-filtered queries. No changes to the spot data model are required.

### ModeWindow (add FT2)

| Value   | Current Window | Baseline Window | Baseline Count | Status      |
|---------|---------------|-----------------|----------------|-------------|
| FT8     | 15 minutes    | 1 hour          | 4              | Existing    |
| FT4     | 15 minutes    | 1 hour          | 4              | Existing    |
| FT2     | 15 minutes    | 1 hour          | 4              | **New**     |
| CW      | 30 minutes    | 2 hours         | 4              | Existing    |
| SSB     | 60 minutes    | 3 hours         | 3              | Existing    |
| DEFAULT | 30 minutes    | 1 hour          | 2              | Existing    |

FT2 uses the same window parameters as FT4/FT8 because all three are short-cycle WSJT-X digital modes and the aggregation window serves trend analysis, not mode cycle matching.

### BandActivity (add rarity multiplier)

Existing fields (unchanged):
- `band`: String (e.g., "20m")
- `mode`: String (e.g., "FT4")
- `spotCount`: int
- `baselineSpotCount`: int
- `trendPercentage`: double
- `maxDxKm`: double
- `maxDxPath`: String
- `activePaths`: Set of ContinentPath
- `score`: int (calculated, 0-100)
- `favorable`: boolean (calculated)

New behavior in scoring:
- `getScore()` applies a rarity multiplier to the activity score component before the weighted combination
- The multiplier is passed into the BandActivity at construction time (from configuration)
- Capped at 100 after application to prevent score overflow

### Mode Rarity Configuration

New configuration entity (application.yml, not a database entity):

| Mode | Rarity Multiplier | Rationale |
|------|-------------------|-----------|
| FT8  | 1.0               | Baseline (most popular) |
| FT4  | 1.5               | ~5-10x fewer spots than FT8 |
| FT2  | 3.0               | ~50-100x fewer spots than FT8 |

Values are configurable via `nextskip.spots.scoring.rarity-multipliers` in application.yml.

## Relationship Changes

### Before (current)

```
Band (1) ──── (1) BandActivity
  │                    │
  │                    ├── mode (primary, detected)
  │                    └── score (no rarity adjustment)
  │
  └── Spots (many, all modes mixed)
```

### After (this feature)

```
Band (1) ──── (many) BandActivity
  │                      │
  │                      ├── mode (explicit: FT8, FT4, or FT2)
  │                      └── score (with rarity multiplier applied)
  │
  └── Spots (many, filtered by mode per BandActivity)
```

Key change: Each band now has **one BandActivity per active mode** instead of one BandActivity with a detected primary mode.

## API Response Structure Change

### Before

```
BandActivityResponse.bandActivities: Map<band, BandActivity>
  "20m" → { mode: "FT8", spotCount: 150, score: 72, ... }
  "40m" → { mode: "FT8", spotCount: 89, score: 65, ... }
```

### After

```
BandActivityResponse.bandActivities: Map<band_mode, BandActivity>
  "20m_FT8" → { mode: "FT8", spotCount: 150, score: 72, ... }
  "20m_FT4" → { mode: "FT4", spotCount: 23, score: 58, ... }
  "40m_FT8" → { mode: "FT8", spotCount: 89, score: 65, ... }
  "40m_FT2" → { mode: "FT2", spotCount: 3, score: 45, ... }
```

## Database Changes

No schema migrations required. The existing `spots` table already stores the `mode` column and has appropriate indices:
- `idx_spots_mode_spotted_at` on `(mode, spotted_at)` -- supports mode-filtered time-window queries
- `idx_spots_band_spotted_at` on `(band, spotted_at)` -- supports band-filtered queries

New repository query needed: aggregate spot counts grouped by both band AND mode within a time window (for the refactored aggregator).
