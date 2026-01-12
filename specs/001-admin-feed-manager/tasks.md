# Tasks: Admin Feed Manager

**Input**: Design documents from `/specs/001-admin-feed-manager/`
**Prerequisites**: plan.md ✓, spec.md ✓, research.md ✓

**Tests**: Not explicitly requested in the feature specification. Test tasks are omitted per template guidance.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Backend**: `src/main/java/io/nextskip/`
- **Frontend**: `src/main/frontend/`
- **Tests**: `src/test/java/io/nextskip/` and `src/test/frontend/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and dependencies for admin functionality

- [ ] T001 Add Spring Security OAuth2 Client dependency in build.gradle.kts
- [ ] T002 [P] Add `@vaadin/hilla-react-auth` verification in package.json (confirm already included via Hilla)
- [ ] T003 [P] Add admin configuration properties schema in src/main/resources/application.yml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core abstractions that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Common Abstractions

- [ ] T004 Add `getDisplayName()` default method to RefreshTaskCoordinator interface in src/main/java/io/nextskip/common/scheduler/RefreshTaskCoordinator.java
- [ ] T005 [P] Create SubscriptionStatusProvider interface in src/main/java/io/nextskip/common/api/SubscriptionStatusProvider.java
- [ ] T006 [P] Implement `getDisplayName()` in NoaaRefreshTask in src/main/java/io/nextskip/propagation/internal/NoaaRefreshTask.java
- [ ] T007 [P] Implement `getDisplayName()` in HamQslSolarRefreshTask in src/main/java/io/nextskip/propagation/internal/HamQslSolarRefreshTask.java
- [ ] T008 [P] Implement `getDisplayName()` in HamQslBandRefreshTask in src/main/java/io/nextskip/propagation/internal/HamQslBandRefreshTask.java
- [ ] T009 [P] Implement `getDisplayName()` in PotaRefreshTask in src/main/java/io/nextskip/activations/internal/PotaRefreshTask.java
- [ ] T010 [P] Implement `getDisplayName()` in SotaRefreshTask in src/main/java/io/nextskip/activations/internal/SotaRefreshTask.java
- [ ] T011 [P] Implement `getDisplayName()` in ContestRefreshTask in src/main/java/io/nextskip/contests/internal/ContestRefreshTask.java
- [ ] T012 [P] Implement `getDisplayName()` in MeteorRefreshTask in src/main/java/io/nextskip/meteors/internal/MeteorRefreshTask.java
- [ ] T013 Implement SubscriptionStatusProvider in PskReporterMqttSource in src/main/java/io/nextskip/spots/internal/PskReporterMqttSource.java

### Admin Module Structure

- [ ] T014 [P] Create FeedType enum in src/main/java/io/nextskip/admin/model/FeedType.java
- [ ] T015 [P] Create FeedStatus record in src/main/java/io/nextskip/admin/model/FeedStatus.java
- [ ] T016 [P] Create UserInfo record in src/main/java/io/nextskip/admin/model/UserInfo.java
- [ ] T017 Create FeedStatusService in src/main/java/io/nextskip/admin/internal/FeedStatusService.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Admin Authentication and Access (Priority: P1) 🎯 MVP

**Goal**: Secure GitHub OAuth2 authentication for admin area with email allowlist authorization

**Independent Test**: Navigate to /admin without auth → redirect to GitHub. Login with allowed email → access granted. Login with non-allowed email → access denied.

### Implementation for User Story 1

- [ ] T018 [US1] Create AdminProperties configuration class in src/main/java/io/nextskip/admin/config/AdminProperties.java
- [ ] T019 [US1] Create GitHubAdminUserService in src/main/java/io/nextskip/admin/internal/GitHubAdminUserService.java
- [ ] T020 [US1] Create SecurityConfig in src/main/java/io/nextskip/config/SecurityConfig.java
- [ ] T021 [P] [US1] Create UserInfoService @BrowserCallable endpoint in src/main/java/io/nextskip/admin/api/UserInfoService.java
- [ ] T022 [US1] Configure OAuth2 client settings in src/main/resources/application.yml
- [ ] T023 [US1] Create auth.ts Hilla auth configuration in src/main/frontend/auth.ts
- [ ] T024 [US1] Update App.tsx with AuthProvider wrapper in src/main/frontend/App.tsx
- [ ] T025 [US1] Create routes.tsx with protected admin routes in src/main/frontend/routes.tsx

**Checkpoint**: At this point, User Story 1 should be fully functional - admin can authenticate via GitHub OAuth2 and access is restricted to allowed emails

---

## Phase 4: User Story 2 - Admin Landing Page with Navigation (Priority: P2)

**Goal**: Clean admin landing page with modern navigation to admin functions

**Independent Test**: After admin login, landing page displays with navigation menu showing "Feed Manager" option. Navigation works correctly.

### Implementation for User Story 2

- [ ] T026 [US2] Create admin CSS variables and styles in src/main/frontend/styles/admin.css
- [ ] T027 [US2] Create AdminLayout.tsx with sidebar navigation in src/main/frontend/views/admin/AdminLayout.tsx
- [ ] T028 [US2] Create AdminLandingView.tsx in src/main/frontend/views/admin/AdminLandingView.tsx
- [ ] T029 [US2] Update routes.tsx to include admin child routes in src/main/frontend/routes.tsx

**Checkpoint**: At this point, User Story 2 should be fully functional - admin sees landing page with navigation after login

---

## Phase 5: User Story 3 - View Feed Status (Priority: P3)

**Goal**: Display all data feeds with their current status (last refresh for scheduled, connection status for subscription)

**Independent Test**: Navigate to Feed Manager, verify all known feeds display with appropriate status information. Scheduled feeds show timestamps, subscription feeds show connection status.

### Implementation for User Story 3

- [ ] T030 [US3] Create AdminEndpoint @BrowserCallable in src/main/java/io/nextskip/admin/api/AdminEndpoint.java
- [ ] T031 [P] [US3] Create FeedStatusCard.tsx component in src/main/frontend/components/admin/FeedStatusCard.tsx
- [ ] T032 [P] [US3] Create FeedStatusGrid.tsx component in src/main/frontend/components/admin/FeedStatusGrid.tsx
- [ ] T033 [US3] Create FeedManagerView.tsx with 5-second polling interval for status updates in src/main/frontend/views/admin/FeedManagerView.tsx
- [ ] T034 [US3] Add visual health indicators and styling for feed cards in src/main/frontend/components/admin/FeedStatusCard.tsx

**Checkpoint**: At this point, User Story 3 should be fully functional - admin can view all feeds with status information

---

## Phase 6: User Story 4 - Force Feed Refresh (Priority: P4)

**Goal**: Manual refresh trigger for scheduled feeds with visual feedback

**Independent Test**: Click refresh button on a scheduled feed, verify feed refresh is triggered and timestamp updates. Verify subscription feeds do NOT show refresh button.

### Implementation for User Story 4

- [ ] T035 [US4] Add triggerRefresh method to AdminEndpoint in src/main/java/io/nextskip/admin/api/AdminEndpoint.java
- [ ] T036 [US4] Create RefreshButton.tsx component in src/main/frontend/components/admin/RefreshButton.tsx
- [ ] T037 [US4] Integrate RefreshButton into FeedStatusCard in src/main/frontend/components/admin/FeedStatusCard.tsx
- [ ] T038 [US4] Add loading and success/error feedback states to refresh flow in src/main/frontend/components/admin/RefreshButton.tsx

**Checkpoint**: At this point, User Story 4 should be fully functional - admin can trigger manual refresh of scheduled feeds

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T039 Add responsive breakpoints for mobile admin in src/main/frontend/styles/admin.css
- [ ] T040 [P] Verify all existing @AnonymousAllowed endpoints still work without auth
- [ ] T041 [P] Run `./gradlew check` to verify quality gates pass
- [ ] T042 [P] Run `npm run validate` to verify frontend quality gates pass
- [ ] T043 Manual verification: Complete checklist from plan.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - Can proceed sequentially in priority order (P1 → P2 → P3 → P4)
  - Or in parallel if resources allow
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational - No dependencies on other stories (can parallel with US1)
- **User Story 3 (P3)**: Can start after Foundational - Uses FeedStatusService from foundational phase
- **User Story 4 (P4)**: Depends on US3 (needs FeedStatusCard to add RefreshButton to)

### Within Each User Story

- Backend endpoints before frontend components that consume them
- Layout/container components before child views
- Core implementation before polish/refinement

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- T006-T012 (getDisplayName implementations) can all run in parallel
- T014-T016 (model DTOs) can all run in parallel
- T031-T032 (feed status components) can all run in parallel
- T039-T042 (polish tasks) can all run in parallel

---

## Parallel Example: Foundational Phase

```bash
# Launch all getDisplayName implementations together:
Task: T006 - NoaaRefreshTask.getDisplayName()
Task: T007 - HamQslSolarRefreshTask.getDisplayName()
Task: T008 - HamQslBandRefreshTask.getDisplayName()
Task: T009 - PotaRefreshTask.getDisplayName()
Task: T010 - SotaRefreshTask.getDisplayName()
Task: T011 - ContestRefreshTask.getDisplayName()
Task: T012 - MeteorRefreshTask.getDisplayName()

# Launch all model DTOs together:
Task: T014 - FeedType enum
Task: T015 - FeedStatus record
Task: T016 - UserInfo record
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Authentication)
4. **STOP and VALIDATE**: Test GitHub OAuth2 login independently
5. Deploy/demo if ready - admin can securely access admin area

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 (Auth) → Test independently → Deploy (MVP!)
3. Add User Story 2 (Navigation) → Test independently → Deploy
4. Add User Story 3 (View Status) → Test independently → Deploy
5. Add User Story 4 (Refresh) → Test independently → Deploy
6. Each story adds value without breaking previous stories

### Suggested Story Parallelization

- **US1 (Auth) + US2 (Navigation)**: Can develop in parallel - no dependencies
- **US3 (View Status)**: Start after foundational, can overlap with US1/US2 finalization
- **US4 (Refresh)**: Must wait for US3 FeedStatusCard component

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Test tasks omitted - not explicitly requested in spec
