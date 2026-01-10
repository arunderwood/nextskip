# Tasks: Admin Feed Manager

**Input**: Design documents from `/specs/001-admin-feed-manager/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅, data-model.md ✅, contracts/admin-api.md ✅

**Tests**: Tests are included per project constitution (VI. Quality Gates) requiring 75% coverage.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/io/nextskip/`
- **Frontend**: `src/main/frontend/`
- **Backend Tests**: `src/test/java/io/nextskip/`
- **Frontend Tests**: `src/test/frontend/`
- **E2E Tests**: `src/test/e2e/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependencies, and configuration

- [x] T001 Add Spring Security OAuth2 Client dependency to `build.gradle` (`implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'`)
- [x] T002 [P] Add OAuth2 configuration to `src/main/resources/application.yml` (GitHub client registration with env var placeholders)
- [x] T003 [P] Add admin allowlist configuration to `src/main/resources/application.yml` (`nextskip.admin.allowed-emails` property)
- [x] T004 [P] Create `src/main/resources/application-local.yml.example` with GitHub OAuth2 setup instructions

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Common Admin Interface (in `common/` module)

- [x] T005 Create `FeedType` enum in `src/main/java/io/nextskip/common/admin/FeedType.java` (SCHEDULED, SUBSCRIPTION)
- [x] T006 [P] Create `HealthStatus` enum in `src/main/java/io/nextskip/common/admin/HealthStatus.java` (HEALTHY, DEGRADED, UNHEALTHY)
- [x] T007 [P] Create `ConnectionState` enum in `src/main/java/io/nextskip/common/admin/ConnectionState.java` (CONNECTED, DISCONNECTED, RECONNECTING, STALE)
- [x] T008 Create `FeedStatus` abstract base record in `src/main/java/io/nextskip/common/admin/FeedStatus.java`
- [x] T009 Create `ScheduledFeedStatus` record in `src/main/java/io/nextskip/common/admin/ScheduledFeedStatus.java` (extends FeedStatus pattern)
- [x] T010 [P] Create `SubscriptionFeedStatus` record in `src/main/java/io/nextskip/common/admin/SubscriptionFeedStatus.java`
- [x] T011 Create `TriggerRefreshResult` record in `src/main/java/io/nextskip/common/admin/TriggerRefreshResult.java`
- [x] T012 Create `AdminStatusProvider` interface in `src/main/java/io/nextskip/common/admin/AdminStatusProvider.java` (getFeedStatuses, triggerRefresh, getModuleName)

### Security Configuration

- [x] T013 Create `AdminEmailAllowlist` component in `src/main/java/io/nextskip/admin/internal/security/AdminEmailAllowlist.java` (reads from `nextskip.admin.allowed-emails`)
- [x] T014 Create `GitHubAdminUserService` in `src/main/java/io/nextskip/admin/internal/security/GitHubAdminUserService.java` (extends DefaultOAuth2UserService, adds ADMIN role if email in allowlist)
- [x] T015 Create `SecurityConfig` in `src/main/java/io/nextskip/admin/internal/security/SecurityConfig.java` (OAuth2 login, /admin/** protected, ADMIN role required)

### Tests for Foundational Components

- [x] T016 [P] Unit test for `AdminEmailAllowlist` in `src/test/java/io/nextskip/admin/internal/security/AdminEmailAllowlistTest.java`
- [x] T017 [P] Unit test for `GitHubAdminUserService` in `src/test/java/io/nextskip/admin/internal/security/GitHubAdminUserServiceTest.java`
- [x] T018 Integration test for `SecurityConfig` in `src/test/java/io/nextskip/admin/internal/security/SecurityConfigIntegrationTest.java` (verifies /admin/** requires auth)

**Checkpoint**: Foundation ready - OAuth2 security configured, AdminStatusProvider interface defined

---

## Phase 3: User Story 1 - Admin Authentication and Access (Priority: P1) 🎯 MVP

**Goal**: Admins can securely log in via GitHub OAuth2 and access the admin area; non-admins are denied

**Independent Test**: Navigate to /admin, authenticate with GitHub, verify access granted/denied based on email allowlist

### Backend Implementation for US1

- [x] T019 [US1] Create `AdminUserInfo` record in `src/main/java/io/nextskip/admin/api/AdminUserInfo.java` (email, name, picture)
- [x] T020 [US1] Create `AdminAuthEndpoint` in `src/main/java/io/nextskip/admin/api/AdminAuthEndpoint.java` (@BrowserCallable, @RolesAllowed("ADMIN"), getCurrentUser, logout)

### Backend Tests for US1

- [x] T021 [P] [US1] Unit test for `AdminAuthEndpoint` in `src/test/java/io/nextskip/admin/api/AdminAuthEndpointTest.java`
- [x] T022 [US1] Integration test for OAuth2 login flow in `src/test/java/io/nextskip/admin/OAuth2LoginIntegrationTest.java`

### Frontend Implementation for US1

- [x] T023 [US1] Create `useAdminAuth` hook in `src/main/frontend/hooks/useAdminAuth.ts` (auth state, login redirect, logout)
- [x] T024 [US1] Create `AdminLayout.tsx` in `src/main/frontend/views/admin/@layout.tsx` (auth wrapper, redirects unauthenticated to OAuth2 login)
- [x] T025 [US1] Add admin routes to `src/main/frontend/views/admin/` (Hilla file-based routing)

### Frontend Tests for US1

- [x] T026 [P] [US1] Unit test for `useAdminAuth` hook in `src/test/frontend/hooks/useAdminAuth.test.ts`
- [x] T027 [US1] Component test for `AdminLayout.tsx` in `src/test/frontend/views/admin/layout.test.tsx`

**Checkpoint**: Admin authentication fully functional - users can log in via GitHub and are authorized based on email allowlist

---

## Phase 4: User Story 2 - Admin Landing Page with Navigation (Priority: P2)

**Goal**: Logged-in admins see a modern landing page with navigation to Feed Manager and future admin functions

**Independent Test**: After login, verify landing page displays with navigation menu containing "Feed Manager" option

### Frontend Implementation for US2

- [x] T028 [US2] Create `AdminNav.tsx` in `src/main/frontend/components/admin/AdminNav.tsx` (navigation menu with Feed Manager link, logout button)
- [x] T029 [US2] Create admin styles in `src/main/frontend/styles/admin.css` (modern, fresh design tokens for admin area)
- [x] T030 [US2] Create `AdminLandingView.tsx` in `src/main/frontend/views/admin/AdminLandingView.tsx` (welcome message, navigation cards)
- [x] T031 [US2] Update `AdminLayout.tsx` to include `AdminNav` component

### Frontend Tests for US2

- [x] T032 [P] [US2] Component test for `AdminNav.tsx` in `src/test/frontend/components/admin/AdminNav.test.tsx`
- [x] T033 [US2] Component test for `AdminLandingView.tsx` in `src/test/frontend/views/admin/AdminLandingView.test.tsx`

**Checkpoint**: Admin landing page with navigation complete - admins can navigate to different admin functions

---

## Phase 5: User Story 3 - View Feed Status (Priority: P3)

**Goal**: Admins can view status of all scheduled and subscription feeds in the Feed Manager

**Independent Test**: Navigate to Feed Manager, verify all feeds display with correct status information

### Backend Implementation for US3 - Module AdminProviders

- [ ] T034 [P] [US3] Create `PropagationAdminProvider` in `src/main/java/io/nextskip/propagation/internal/admin/PropagationAdminProvider.java` (NOAA, HamQSL feeds)
- [ ] T035 [P] [US3] Create `ActivationsAdminProvider` in `src/main/java/io/nextskip/activations/internal/admin/ActivationsAdminProvider.java` (POTA, SOTA feeds)
- [ ] T036 [P] [US3] Create `ContestsAdminProvider` in `src/main/java/io/nextskip/contests/internal/admin/ContestsAdminProvider.java` (WA7BNM feed)
- [ ] T037 [P] [US3] Create `MeteorsAdminProvider` in `src/main/java/io/nextskip/meteors/internal/admin/MeteorsAdminProvider.java` (Meteor shower feed)
- [ ] T038 [P] [US3] Create `SpotsAdminProvider` in `src/main/java/io/nextskip/spots/internal/admin/SpotsAdminProvider.java` (band activity + MQTT subscription)

### Backend Implementation for US3 - Admin Endpoint

- [ ] T039 [US3] Create `ModuleFeedStatus` record in `src/main/java/io/nextskip/admin/api/ModuleFeedStatus.java` (moduleName, scheduledFeeds, subscriptionFeeds)
- [ ] T040 [US3] Create `FeedStatusResponse` record in `src/main/java/io/nextskip/admin/api/FeedStatusResponse.java` (modules list, timestamp)
- [ ] T041 [US3] Create `AdminFeedEndpoint` in `src/main/java/io/nextskip/admin/api/AdminFeedEndpoint.java` (@BrowserCallable, @RolesAllowed("ADMIN"), getFeedStatuses - aggregates from all AdminStatusProviders)

### Backend Tests for US3

- [ ] T042 [P] [US3] Unit test for `PropagationAdminProvider` in `src/test/java/io/nextskip/propagation/internal/admin/PropagationAdminProviderTest.java`
- [ ] T043 [P] [US3] Unit test for `ActivationsAdminProvider` in `src/test/java/io/nextskip/activations/internal/admin/ActivationsAdminProviderTest.java`
- [ ] T044 [P] [US3] Unit test for `ContestsAdminProvider` in `src/test/java/io/nextskip/contests/internal/admin/ContestsAdminProviderTest.java`
- [ ] T045 [P] [US3] Unit test for `MeteorsAdminProvider` in `src/test/java/io/nextskip/meteors/internal/admin/MeteorsAdminProviderTest.java`
- [ ] T046 [P] [US3] Unit test for `SpotsAdminProvider` in `src/test/java/io/nextskip/spots/internal/admin/SpotsAdminProviderTest.java`
- [ ] T047 [US3] Unit test for `AdminFeedEndpoint.getFeedStatuses()` in `src/test/java/io/nextskip/admin/api/AdminFeedEndpointTest.java`

### Frontend Implementation for US3

- [ ] T048 [P] [US3] Create `FeedCard.tsx` base component in `src/main/frontend/components/admin/FeedCard.tsx` (common feed card structure)
- [ ] T049 [P] [US3] Create `ScheduledFeedCard.tsx` in `src/main/frontend/components/admin/ScheduledFeedCard.tsx` (last refresh, next refresh, health indicator)
- [ ] T050 [P] [US3] Create `SubscriptionFeedCard.tsx` in `src/main/frontend/components/admin/SubscriptionFeedCard.tsx` (connection state, last message)
- [ ] T051 [US3] Create `FeedManagerView.tsx` in `src/main/frontend/views/admin/FeedManagerView.tsx` (calls getFeedStatuses, renders feed cards grouped by module)
- [ ] T052 [US3] Add Feed Manager route to admin routes (/admin/feeds)

### Frontend Tests for US3

- [ ] T053 [P] [US3] Component test for `FeedCard.tsx` in `src/test/frontend/components/admin/FeedCard.test.tsx`
- [ ] T054 [P] [US3] Component test for `ScheduledFeedCard.tsx` in `src/test/frontend/components/admin/ScheduledFeedCard.test.tsx`
- [ ] T055 [P] [US3] Component test for `SubscriptionFeedCard.tsx` in `src/test/frontend/components/admin/SubscriptionFeedCard.test.tsx`
- [ ] T056 [US3] Component test for `FeedManagerView.tsx` in `src/test/frontend/views/admin/FeedManagerView.test.tsx`

**Checkpoint**: Feed Manager displays all feed statuses - admins can monitor system health at a glance

---

## Phase 6: User Story 4 - Force Feed Refresh (Priority: P4)

**Goal**: Admins can trigger immediate refresh of scheduled feeds to recover from stale data

**Independent Test**: Click refresh button on scheduled feed, verify refresh triggers and timestamp updates

### Backend Implementation for US4

- [ ] T057 [US4] Add `triggerFeedRefresh(feedName)` method to `AdminFeedEndpoint` in `src/main/java/io/nextskip/admin/api/AdminFeedEndpoint.java`
- [ ] T058 [US4] Implement `triggerRefresh()` in `PropagationAdminProvider` (reschedule via db-scheduler)
- [ ] T059 [P] [US4] Implement `triggerRefresh()` in `ActivationsAdminProvider`
- [ ] T060 [P] [US4] Implement `triggerRefresh()` in `ContestsAdminProvider`
- [ ] T061 [P] [US4] Implement `triggerRefresh()` in `MeteorsAdminProvider`
- [ ] T062 [P] [US4] Implement `triggerRefresh()` in `SpotsAdminProvider` (for band-activity scheduled feed only)

### Backend Tests for US4

- [ ] T063 [US4] Unit test for `AdminFeedEndpoint.triggerFeedRefresh()` in `src/test/java/io/nextskip/admin/api/AdminFeedEndpointTest.java`
- [ ] T064 [P] [US4] Test triggerRefresh in `PropagationAdminProviderTest`
- [ ] T065 [P] [US4] Test triggerRefresh in `ActivationsAdminProviderTest`

### Frontend Implementation for US4

- [ ] T066 [US4] Add refresh button and loading state to `ScheduledFeedCard.tsx`
- [ ] T067 [US4] Add `useFeedRefresh` hook in `src/main/frontend/hooks/useFeedRefresh.ts` (trigger refresh, handle success/error)
- [ ] T068 [US4] Integrate refresh functionality into `FeedManagerView.tsx` (disable button while refreshing, show success/error toast)

### Frontend Tests for US4

- [ ] T069 [P] [US4] Unit test for `useFeedRefresh` hook in `src/test/frontend/hooks/useFeedRefresh.test.ts`
- [ ] T070 [US4] Update `ScheduledFeedCard.test.tsx` with refresh button tests
- [ ] T071 [US4] Update `FeedManagerView.test.tsx` with refresh flow tests

**Checkpoint**: Force refresh functional - admins can manually trigger feed refreshes

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: E2E tests, accessibility, performance, documentation

### E2E Tests

- [ ] T072 [P] E2E test for admin login flow in `src/test/e2e/admin/admin-login.spec.ts`
- [ ] T073 [P] E2E test for Feed Manager view in `src/test/e2e/admin/feed-manager.spec.ts`
- [ ] T074 E2E test for force refresh in `src/test/e2e/admin/feed-refresh.spec.ts`

### Accessibility & Polish

- [ ] T075 [P] WCAG 2.1 AA compliance audit for admin components (keyboard navigation, ARIA labels)
- [ ] T076 [P] Add loading states and error handling to all admin views
- [ ] T077 Visual polish for health status indicators (color coding, icons)

### Documentation & Validation

- [ ] T078 Update `quickstart.md` with actual tested steps
- [ ] T079 Run `npm run validate && ./gradlew build` - fix any failures
- [ ] T080 Run `./gradlew bootRun` and manually test admin flow per quickstart.md
- [ ] T081 Run E2E tests: `npm run e2e`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - US1 (Auth) should complete before US2-4 (UI needs auth)
  - US2 (Navigation) should complete before US3-4 (need layout)
  - US3 (View Status) should complete before US4 (need endpoint)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **US2 (P2)**: Depends on US1 AdminLayout being complete
- **US3 (P3)**: Depends on US2 navigation structure
- **US4 (P4)**: Depends on US3 feed display infrastructure

### Within Each User Story

- Backend models before services
- Services before endpoints
- Endpoints before frontend integration
- Tests alongside implementation (TDD where practical)

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational enum/model tasks marked [P] can run in parallel
- All module AdminProvider implementations (T034-T038) can run in parallel
- All frontend card components (T048-T050) can run in parallel
- All unit tests for AdminProviders (T042-T046) can run in parallel
- E2E tests (T072-T074) can run in parallel

---

## Implementation Strategy

### MVP First (US1 + US2 + US3)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 - Admin Authentication
4. Complete Phase 4: US2 - Landing Page
5. Complete Phase 5: US3 - View Feed Status
6. **STOP and VALIDATE**: Admin can log in and view feed statuses
7. Deploy/demo MVP

### Full Feature (Add US4)

8. Complete Phase 6: US4 - Force Feed Refresh
9. Complete Phase 7: Polish
10. Final validation and PR creation

---

## Notes

- [P] tasks = different files, no dependencies between them
- [Story] label maps task to specific user story for traceability
- All `AdminStatusProvider` implementations follow existing `RefreshTaskCoordinator` pattern
- No new database entities - leverage existing `scheduled_tasks` table
- GitHub OAuth2 requires setup per quickstart.md before testing
- Tests must meet 75% instruction / 65% branch coverage per constitution
