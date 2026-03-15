# Quickstart: FT4 and FT2 Digital Mode Support

**Branch**: `002-ft4-ft2-modes` | **Date**: 2026-03-13

## Prerequisites

- Java 25 (see `.tool-versions`)
- Docker (for PostgreSQL)
- Node.js (for frontend build)

## Setup

```bash
# Start database
docker-compose up -d

# Build and run
./gradlew bootRun
```

## Verification Steps

### 1. MQTT Topic Subscriptions

After starting the application, check the logs for MQTT subscription confirmations:
```
Subscribed to topic: pskr/filter/v2/+/FT8/#
Subscribed to topic: pskr/filter/v2/+/FT4/#
Subscribed to topic: pskr/filter/v2/+/FT2/#
```

### 2. Spot Ingestion

Within a few minutes, FT4 spots should appear in the database. FT2 spots may take longer due to low activity. Check the spots status endpoint or application logs.

### 3. Dashboard Cards

Open `http://localhost:8080` and verify:
- FT8 cards continue to appear as before
- FT4 cards appear for bands with FT4 activity (labeled e.g., "20m FT4")
- Cards are sorted by score with FT4 cards ranking competitively (not always at the bottom)
- FT2 cards appear if any FT2 activity is present (may be rare)

### 4. Rarity Multiplier Effect

Compare cards for the same band across modes:
- A 20m FT4 card with moderate activity should score similarly to a 20m FT8 card with moderate activity
- A 20m FT2 card with low activity but good propagation indicators should rank above bands with poor conditions

## Key Configuration

```yaml
# application.yml
nextskip:
  spots:
    mqtt:
      topics:
        - pskr/filter/v2/+/FT8/#
        - pskr/filter/v2/+/FT4/#
        - pskr/filter/v2/+/FT2/#
    scoring:
      rarity-multipliers:
        FT8: 1.0
        FT4: 1.5
        FT2: 3.0
```

## Running Tests

```bash
# Backend tests
./gradlew test

# Frontend tests
npm run test:run

# Full validation
npm run validate
```
