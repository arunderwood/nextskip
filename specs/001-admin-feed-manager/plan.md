# Implementation Plan: Admin Feed Manager

**Branch**: `001-admin-feed-manager` | **Date**: 2026-01-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-admin-feed-manager/spec.md`

## Summary

Add an authenticated admin-only area to NextSkip with GitHub OAuth2 authentication (email allowlist authorization). The initial admin function is a Feed Manager view displaying scheduled task status (last refresh, next run) and subscription feed connection status (MQTT). Admins can force immediate refresh of scheduled feeds.

**Auth Strategy**: GitHub OAuth2 for MVP (session-based); Auth0 recommended for future JWT migration.

## Technical Context

**Language/Version**: Java 25 (see `.tool-versions`)
**Primary Dependencies**: Spring Boot 4.0.1, Vaadin Hilla 25.0.2, Spring Security OAuth2 (GitHub), Resilience4j
**Storage**: PostgreSQL (existing), db-scheduler `scheduled_tasks` table
**Testing**: JUnit 5, Vitest, React Testing Library, Playwright
**Target Platform**: Web application (Linux server deployment)
**Project Type**: Web (Java backend + React frontend via Vaadin Hilla)
**Performance Goals**: Feed status display <3s, refresh trigger <2s (per spec SC-003, SC-004)
**Constraints**: Must integrate with existing db-scheduler infrastructure; GitHub OAuth App required (future: Auth0 for JWT)
**Scale/Scope**: Single admin role, ~10 scheduled feeds, 1 MQTT subscription source

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Compliance | Notes |
|-----------|------------|-------|
| **I. Real-Time Opportunism** | N/A | Admin feature, not operator-facing dashboard |
| **II. Multi-Activity Aggregation** | N/A | Admin tool, separate from main dashboard |
| **III. Activity-Centric Modularity** | PASS | New `admin` module with clean API boundaries |
| **IV. Responsible Data Consumption** | PASS | Force-refresh uses existing resilient clients |
| **V. SOLID Design** | PASS | Will follow existing patterns (service/client separation, DI) |
| **VI. Quality Gates** | PASS | Will meet coverage requirements |
| **VII. Resilience by Default** | PASS | Leverages existing circuit breakers |
| **Permitted Technologies** | PASS | Spring Security OAuth2 is Spring Boot ecosystem |
| **Before Merging** | GATE | Must pass: `npm run validate`, `./gradlew build`, `./gradlew bootRun` |

**Gate Status**: PASS - No constitution violations identified.

## Project Structure

### Documentation (this feature)

```text
specs/001-admin-feed-manager/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
# Backend (Java - distributed admin pattern)
src/main/java/io/nextskip/

# COMMON - Shared admin interface (like RefreshTaskCoordinator pattern)
├── common/
│   └── admin/                      # NEW
│       ├── AdminStatusProvider.java      # Interface: getFeedStatuses(), triggerRefresh()
│       ├── FeedStatus.java               # Base model
│       ├── ScheduledFeedStatus.java      # Scheduled feed model
│       └── SubscriptionFeedStatus.java   # Subscription feed model

# EACH MODULE implements AdminStatusProvider
├── propagation/
│   └── internal/
│       └── admin/                  # NEW
│           └── PropagationAdminProvider.java  # NOAA, HamQSL feed status
├── activations/
│   └── internal/
│       └── admin/                  # NEW
│           └── ActivationsAdminProvider.java  # POTA, SOTA feed status
├── contests/
│   └── internal/
│       └── admin/                  # NEW
│           └── ContestsAdminProvider.java     # WA7BNM feed status
├── meteors/
│   └── internal/
│       └── admin/                  # NEW
│           └── MeteorsAdminProvider.java      # Meteor feed status
├── spots/
│   └── internal/
│       └── admin/                  # NEW
│           └── SpotsAdminProvider.java        # MQTT + band activity status

# ADMIN MODULE - Auth + aggregation only
├── admin/                          # NEW MODULE
│   ├── api/                        # @BrowserCallable endpoints (admin-protected)
│   │   ├── AdminFeedEndpoint.java        # Injects List<AdminStatusProvider>
│   │   └── AdminAuthEndpoint.java        # User info, logout
│   └── internal/
│       └── security/
│           ├── SecurityConfig.java       # OAuth2 + route protection
│           └── AdminEmailAllowlist.java  # Email-based authorization
├── config/
│   └── SecurityConfig.java         # NEW: Spring Security OAuth2 config

# Frontend (React - existing structure extended)
src/main/frontend/
├── views/
│   └── admin/                      # NEW
│       ├── AdminLayout.tsx         # Auth wrapper + navigation
│       ├── AdminLandingView.tsx    # Landing page with nav
│       └── FeedManagerView.tsx     # Feed status list with refresh actions
├── components/
│   └── admin/                      # NEW
│       ├── AdminNav.tsx            # Navigation menu
│       ├── FeedCard.tsx            # Individual feed status card
│       ├── ScheduledFeedCard.tsx   # Scheduled feed details
│       └── SubscriptionFeedCard.tsx # Subscription feed details
└── hooks/
    └── useAdminAuth.ts             # NEW: Auth state hook

# Tests (distributed per module + admin integration)
src/test/java/io/nextskip/
├── common/admin/                   # AdminStatusProvider contract tests
├── propagation/internal/admin/     # PropagationAdminProvider tests
├── activations/internal/admin/     # ActivationsAdminProvider tests
├── admin/                          # Admin endpoint + security tests
src/test/frontend/views/admin/      # Frontend tests
src/test/e2e/admin/                 # E2E tests
```

**Structure Decision**: Follows the existing `RefreshTaskCoordinator` pattern for modularity. Each activity module implements `AdminStatusProvider` to expose its own feed status. The `admin/` module only handles authentication, authorization, and aggregation—it does NOT contain feed-specific logic. Adding a new activity module automatically adds its admin functionality (Open-Closed Principle).

## Constitution Re-Check (Post-Phase 1 Design)

| Principle | Compliance | Verification |
|-----------|------------|--------------|
| **III. Activity-Centric Modularity** | PASS | Each module implements `AdminStatusProvider`; adding new module auto-registers its admin status (Open-Closed) |
| **IV. Responsible Data Consumption** | PASS | Force-refresh reuses existing `RefreshTaskCoordinator` pattern with circuit breakers |
| **V. SOLID Design** | PASS | SRP: Each module owns its admin logic; DIP: `admin/` depends on `AdminStatusProvider` abstraction |
| **VI. Quality Gates** | PENDING | Tests to be written with feature; delta coverage enforced |
| **VII. Resilience by Default** | PASS | No new external APIs; subscription status uses existing MQTT reconnection logic |
| **Permitted Technologies** | PASS | Spring Security OAuth2 Client is part of Spring Boot 4.x ecosystem |

**Post-Design Gate Status**: PASS - Design aligns with constitution principles. Distributed `AdminStatusProvider` pattern follows existing `RefreshTaskCoordinator` precedent.

## Complexity Tracking

No constitution violations requiring justification. Feature uses:
- Existing db-scheduler infrastructure (no new scheduler patterns)
- Existing resilient client patterns (no new external API clients)
- Standard Spring Security OAuth2 (permitted technology)
- No new database entities (reads from existing `scheduled_tasks` table)

## Generated Artifacts

| Artifact | Path | Status |
|----------|------|--------|
| Implementation Plan | `specs/001-admin-feed-manager/plan.md` | Complete |
| Research | `specs/001-admin-feed-manager/research.md` | Complete |
| Data Model | `specs/001-admin-feed-manager/data-model.md` | Complete |
| API Contract | `specs/001-admin-feed-manager/contracts/admin-api.md` | Complete |
| Quickstart Guide | `specs/001-admin-feed-manager/quickstart.md` | Complete |
| Tasks | `specs/001-admin-feed-manager/tasks.md` | Complete |
