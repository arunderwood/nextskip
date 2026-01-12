# Implementation Plan: Admin Feed Manager

**Branch**: `001-admin-feed-manager` | **Date**: 2026-01-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-admin-feed-manager/spec.md`

## Summary

Add an authenticated admin-only area to NextSkip using GitHub OAuth2 for authentication and Hilla's native security integration. The admin area provides a Feed Manager view showing status of all data feeds (scheduled and subscription types) with the ability to force-refresh scheduled feeds.

**Key Design Choices**:
- Admin depends on abstractions (`RefreshTaskCoordinator`, `SubscriptionStatusProvider`) in `common/`, not on concrete activity modules
- Single `FeedStatusService` combines db-scheduler runtime API with coordinator/provider metadata
- New feeds are auto-discovered via Spring DI - zero admin module changes required (Open-Closed Principle)

## Technical Context

**Language/Version**: Java 25, TypeScript 5.9
**Primary Dependencies**: Spring Boot, Vaadin Hilla, Spring Security OAuth2 Client
**Storage**: PostgreSQL (existing), session store (default in-memory, prod: Spring Session JDBC)
**Testing**: JUnit 5, Vitest, Playwright
**Target Platform**: Web (desktop/mobile responsive)
**Project Type**: Full-stack monolith (Java backend + React frontend)
**Performance Goals**: Admin page loads <3s, feed status updates <5s, manual refresh triggers <2s
**Constraints**: Stateless public endpoints (no session), session-based admin routes
**Scale/Scope**: Single admin role, ~10 feeds to monitor, 1-5 admin users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Real-Time Opportunism | N/A | Admin feature, not user-facing opportunity |
| II. Multi-Activity Aggregation | N/A | Admin dashboard separate from main dashboard |
| III. Activity-Centric Modularity | PASS | Admin is its own module with clean API boundary |
| IV. Responsible Data Consumption | PASS | No new external feeds; monitoring existing |
| V. SOLID Design | PASS | Admin depends on abstractions in `common/`, not concrete modules (DIP). New feeds auto-discovered (O/C). |
| VI. Quality Gates | PASS | Standard coverage requirements apply |
| VII. Resilience by Default | PASS | Circuit breakers already on feeds; admin reads status |

**Permitted Technologies Check**:
- Spring Security OAuth2 Client: **REQUIRED** - Standard Spring Security extension
- Hilla React Auth: **INCLUDED** - Already part of Vaadin Hilla stack

## Project Structure

### Documentation (this feature)

```text
specs/001-admin-feed-manager/
├── plan.md              # This file
├── research.md          # Hilla security research (Phase 0)
├── data-model.md        # Entity definitions
├── quickstart.md        # Dev setup guide
├── contracts/           # API contracts
│   ├── AdminEndpoint.yaml
│   └── UserInfoEndpoint.yaml
└── tasks.md             # Implementation tasks (Phase 2)
```

### Source Code (repository root)

```text
src/main/java/io/nextskip/
├── admin/                               # NEW: Admin module
│   ├── api/                             # Public contracts
│   │   ├── AdminEndpoint.java           # @BrowserCallable for feed management
│   │   └── UserInfoService.java         # @BrowserCallable for auth info
│   ├── internal/                        # Implementation details
│   │   └── FeedStatusService.java       # Unified feed status + refresh
│   └── model/                           # DTOs
│       ├── FeedStatus.java              # Feed status record
│       ├── FeedType.java                # SCHEDULED | SUBSCRIPTION enum
│       └── UserInfo.java                # User info for frontend
├── common/
│   ├── api/
│   │   ├── Scoreable.java               # Existing
│   │   └── SubscriptionStatusProvider.java  # NEW: Interface for subscription feeds
│   └── scheduler/
│       └── RefreshTaskCoordinator.java  # MODIFIED: Add getDisplayName()
├── spots/
│   └── internal/
│       └── PskReporterMqttSource.java   # MODIFIED: implements SubscriptionStatusProvider
├── config/
│   └── SecurityConfig.java              # NEW: Spring Security configuration
└── [other existing modules unchanged]

src/main/frontend/
├── auth.ts                              # NEW: Hilla auth configuration
├── routes.tsx                           # NEW: Protected route definitions
├── App.tsx                              # MODIFIED: Add AuthProvider wrapper
├── views/
│   ├── DashboardView.tsx                # UNCHANGED
│   └── admin/                           # NEW: Admin views
│       ├── AdminLayout.tsx              # Admin shell with navigation
│       └── FeedManagerView.tsx          # Feed status and controls
└── components/
    └── admin/                           # NEW: Admin components
        ├── FeedStatusCard.tsx           # Individual feed display
        ├── FeedStatusGrid.tsx           # Feed list container
        └── RefreshButton.tsx            # Manual refresh trigger
```

**Structure Decision**: Extends existing modular monolith pattern. Admin module follows same `api/internal/model` structure as other activity modules. Admin depends only on abstractions in `common/` (`RefreshTaskCoordinator`, `SubscriptionStatusProvider`), not on concrete activity modules. Follows the same auto-discovery pattern as `DataRefreshStartupHandler`.

---

## Architecture Design

### SOLID-Compliant Feed Discovery

#### For Scheduled Feeds: Abstraction-Based Discovery

Admin depends on the `RefreshTaskCoordinator` interface (in `common/`), not on concrete activity modules. This follows the existing pattern used by `DataRefreshStartupHandler`.

**Interface Enhancement** (add to existing interface):

```java
// In common/scheduler/RefreshTaskCoordinator.java - ADD getDisplayName()
public interface RefreshTaskCoordinator {
    String getTaskName();           // Already exists
    String getDisplayName();        // NEW: Human-readable name for admin UI
    RecurringTask<Void> getRecurringTask();  // Already exists
    boolean needsInitialLoad();     // Already exists
}
```

**Unified FeedStatusService** (single service handles both feed types):

```java
@Component
public class FeedStatusService {
    private final Scheduler scheduler;
    private final Map<String, RefreshTaskCoordinator> scheduledFeeds;
    private final List<SubscriptionStatusProvider> subscriptionFeeds;

    // Spring auto-wires ALL implementations of both interfaces
    public FeedStatusService(
            Scheduler scheduler,
            List<RefreshTaskCoordinator> coordinators,
            List<SubscriptionStatusProvider> subscriptions) {
        this.scheduler = scheduler;
        this.scheduledFeeds = coordinators.stream()
            .collect(Collectors.toMap(
                RefreshTaskCoordinator::getTaskName,
                coordinator -> coordinator));
        this.subscriptionFeeds = List.copyOf(subscriptions);
    }

    public List<FeedStatus> getAllFeedStatuses() {
        List<FeedStatus> all = new ArrayList<>();
        all.addAll(getScheduledFeedStatuses());
        all.addAll(getSubscriptionFeedStatuses());
        return all;
    }

    public List<FeedStatus> getScheduledFeedStatuses() {
        return scheduler.getScheduledExecutions().stream()
            .filter(exec -> scheduledFeeds.containsKey(
                exec.getTaskInstance().getTaskName()))
            .map(exec -> {
                String taskName = exec.getTaskInstance().getTaskName();
                RefreshTaskCoordinator coordinator = scheduledFeeds.get(taskName);
                return new FeedStatus(
                    taskName,
                    coordinator.getDisplayName(),
                    FeedType.SCHEDULED,
                    exec.getLastSuccess(),
                    exec.getExecutionTime(),
                    null,
                    exec.getConsecutiveFailures() == 0
                );
            })
            .toList();
    }

    public List<FeedStatus> getSubscriptionFeedStatuses() {
        return subscriptionFeeds.stream()
            .map(sub -> new FeedStatus(
                sub.getSubscriptionId(),
                sub.getDisplayName(),
                FeedType.SUBSCRIPTION,
                sub.getLastMessageTime(),
                null,
                sub.isConnected(),
                sub.isConnected()
            ))
            .toList();
    }

    public void triggerRefresh(String feedId) {
        RefreshTaskCoordinator coordinator = scheduledFeeds.get(feedId);
        if (coordinator == null) {
            throw new IllegalArgumentException("Unknown scheduled feed: " + feedId);
        }
        scheduler.reschedule(
            coordinator.getRecurringTask().instance(RecurringTask.INSTANCE),
            Instant.now()
        );
    }
}
```

**Dependency Graph** (admin depends on abstractions only):

```
┌─────────────────────────────────────────────────────────────────┐
│                         common/                                  │
│  ┌─────────────────────────┐  ┌──────────────────────────────┐  │
│  │ RefreshTaskCoordinator  │  │ SubscriptionStatusProvider   │  │
│  │ (interface)             │  │ (interface)                  │  │
│  └───────────▲─────────────┘  └──────────────▲───────────────┘  │
└──────────────┼───────────────────────────────┼──────────────────┘
               │                               │
    ┌──────────┴──────────┐         ┌──────────┴──────────┐
    │                     │         │                     │
┌───┴───┐ ┌───────┐ ┌─────┴──┐  ┌───┴───┐            ┌────┴────┐
│ noaa  │ │ pota  │ │contests│  │ spots │            │  admin  │
│       │ │       │ │        │  │ (mqtt)│            │         │
└───────┘ └───────┘ └────────┘  └───────┘            └─────────┘
  implements                      implements           depends on
  RefreshTaskCoordinator          SubscriptionStatus   both interfaces
```

**Open-Closed Compliance**:
- Adding a new scheduled feed → Implement `RefreshTaskCoordinator` (already required) with `getDisplayName()`
- Admin automatically discovers it via Spring DI → Zero admin module changes
- Admin depends on abstraction → No coupling to concrete activity modules

#### For Subscription Feeds: Provider Interface

Subscription feeds (like MQTT) implement `SubscriptionStatusProvider` to expose their connection status:

```java
// In common/api/ - alongside Scoreable interface
public interface SubscriptionStatusProvider {
    String getSubscriptionId();
    String getDisplayName();
    boolean isConnected();
    Instant getLastMessageTime();  // When last data was received
}
```

**Open-Closed Compliance**:
- Adding a new subscription feed → Implement `SubscriptionStatusProvider` (4 methods)
- `FeedStatusService` automatically discovers it via Spring DI → Zero admin module changes
- Interface is minimal and stable

**Note**: The existing `PskReporterMqttSource` already tracks connection status internally (`isConnected()`, `lastMessageTime`). Adding the interface requires only exposing existing state, not adding new functionality.

---

### Authentication Flow (GitHub OAuth2)

```
User navigates to /admin
    ↓
Not authenticated? → Spring Security redirects to GitHub OAuth2
    ↓
User authenticates on GitHub
    ↓
GitHub redirects to /login/oauth2/code/github
    ↓
CustomOAuth2UserService:
    1. Fetches email from GitHub API (if not public)
    2. Checks email against allowlist
    3. Grants ADMIN authority if match
    ↓
Redirect to original /admin URL
    ↓
Hilla protectRoutes() checks ADMIN role → renders admin UI
```

### Security Configuration Pattern

Using Hilla's `VaadinSecurityConfigurer` with OAuth2:

```java
@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Hilla defaults: CSRF protection, endpoint security
        http.with(VaadinSecurityConfigurer.vaadin(), configurer -> {
            // No login view - OAuth2 handles redirect
        });

        // OAuth2 login configuration
        http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo
                .userService(customOAuth2UserService())
            )
            .defaultSuccessUrl("/admin", true)
        );

        // Stateless for public, session for admin
        http.sessionManagement(session -> session
            .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
        );

        return http.build();
    }
}
```

### Hilla Frontend Security Integration

**auth.ts** - Configures Hilla's auth system:

```typescript
import { configureAuth } from '@vaadin/hilla-react-auth';
import { UserInfoService } from 'Frontend/generated/endpoints';

const auth = configureAuth(UserInfoService.getUserInfo, {
  getRoles: (userInfo) => userInfo?.authorities ?? [],
});

export const useAuth = auth.useAuth;
export const AuthProvider = auth.AuthProvider;
```

**routes.tsx** - Protected routes using Hilla's protectRoutes:

```typescript
import { protectRoutes } from '@vaadin/hilla-react-auth';

export const routes = protectRoutes([
  {
    path: '/',
    element: <DashboardView />,
    handle: { title: 'Dashboard' },
  },
  {
    path: '/admin',
    element: <AdminLayout />,
    handle: {
      title: 'Admin',
      rolesAllowed: ['ADMIN'],
    },
    children: [
      { path: '', element: <Navigate to="feeds" replace /> },
      { path: 'feeds', element: <FeedManagerView /> },
    ],
  },
]);
```

### Session Management Strategy

| Route Pattern | Session | Auth Required |
|--------------|---------|---------------|
| `/` (public dashboard) | No | No |
| `/admin/**` | Yes | ADMIN role |
| `/connect/**` (Hilla internal) | IF_REQUIRED | Per-endpoint |

---

## Key Implementation Decisions

### 1. Abstraction-Based Feed Discovery
Admin depends on abstractions in `common/`, not on concrete activity modules:
- **Scheduled feeds**: `RefreshTaskCoordinator` interface (add `getDisplayName()` method)
- **Subscription feeds**: `SubscriptionStatusProvider` interface (new, 4 methods)
- db-scheduler API provides runtime data (execution times, health status)
- Coordinators provide metadata (display names, task references)
- Spring auto-wires all implementations → zero admin module coupling

### 2. Minimal Interface for Subscriptions
`SubscriptionStatusProvider` has only 4 methods - the minimum needed. The existing MQTT client already tracks this state internally.

### 3. No Login Button on Main Page
Per user requirement: Direct OAuth2 redirect when accessing `/admin`. No visible login UI on the public dashboard.

### 4. Email Allowlist Authorization
Admin emails configured via Spring property:
```yaml
nextskip:
  admin:
    allowed-emails:
      - admin@example.com
      - operator@hamradio.club
```

### 5. GitHub Email Retrieval
GitHub doesn't always return email in OAuth2 response. `CustomOAuth2UserService` calls GitHub's user/emails API if needed.

### 6. SecurityContext for UserInfo
Frontend uses Hilla's `useAuth()` hook with `SecurityContext`-based `UserInfoService`. Auth-agnostic, works if we switch providers later.

### 7. Admin Layout Separate from Main
Admin area has its own layout (`AdminLayout.tsx`) with sidebar navigation, logout button, and user info display.

---

## Data Model

### FeedStatus (DTO)

```java
public record FeedStatus(
    String id,
    String displayName,
    FeedType type,
    Instant lastRefresh,
    @Nullable Instant nextRefresh,      // null for subscriptions
    @Nullable Boolean isConnected,       // null for scheduled
    boolean isHealthy
) {}
```

### FeedType (Enum)

```java
public enum FeedType {
    SCHEDULED,
    SUBSCRIPTION
}
```

### UserInfo (DTO)

```java
public record UserInfo(
    String username,
    List<String> authorities
) {}
```

---

## Verification Plan

### Unit Tests
- `FeedStatusService`: db-scheduler introspection, subscription aggregation, task rescheduling
- `CustomOAuth2UserService`: Email fetching, allowlist checking
- `FeedStatusCard`: Render scheduled vs subscription types correctly

### Integration Tests
- `SecurityConfig`: OAuth2 flow with mock GitHub
- `AdminEndpoint`: Role-based access control (@RolesAllowed)
- Protected routes: Unauthenticated redirect to OAuth2

### E2E Tests (Playwright)
1. Unauthenticated user visits `/admin` → redirected to GitHub
2. Authenticated admin sees feed manager with all feeds listed
3. Scheduled feeds show last/next refresh times
4. Subscription feeds show connection status
5. Admin can trigger manual refresh on scheduled feed
6. Admin can logout and be redirected
7. Non-admin GitHub user sees access denied

### Manual Verification Checklist
- [ ] `./gradlew bootRun` starts without errors
- [ ] Public dashboard loads without authentication prompt
- [ ] `/admin` redirects to GitHub OAuth2
- [ ] After GitHub login, admin dashboard loads
- [ ] All scheduled feeds appear (auto-discovered from db-scheduler)
- [ ] MQTT subscription feed appears with connection status
- [ ] Refresh button triggers feed update
- [ ] Logout returns to public dashboard
- [ ] `npm run validate` passes
- [ ] `./gradlew build` passes

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| db-scheduler API changes | Use stable public API, integration test coverage |
| GitHub rate limits on email API | Cache OAuth2 user attributes in session |
| Session storage in production | Use Spring Session JDBC (config only) |
| Breaking existing endpoints | All current endpoints use @AnonymousAllowed |
| CSRF token issues | Hilla handles automatically |

---

## Dependencies to Add

**build.gradle.kts**:
```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
```

**package.json** (likely already included via Hilla):
```json
"@vaadin/hilla-react-auth": "latest"
```

---

## Files Modified (Existing Code)

| File | Change | Reason |
|------|--------|--------|
| `common/scheduler/RefreshTaskCoordinator.java` | Add `getDisplayName()` method | Provide human-readable names for admin UI |
| `propagation/.../NoaaRefreshTask.java` | Implement `getDisplayName()` | Return "NOAA Solar Indices" |
| `propagation/.../HamQslSolarRefreshTask.java` | Implement `getDisplayName()` | Return "HamQSL Solar" |
| `propagation/.../HamQslBandRefreshTask.java` | Implement `getDisplayName()` | Return "HamQSL Band Conditions" |
| `activations/.../PotaRefreshTask.java` | Implement `getDisplayName()` | Return "POTA Activations" |
| `activations/.../SotaRefreshTask.java` | Implement `getDisplayName()` | Return "SOTA Activations" |
| `contests/.../ContestRefreshTask.java` | Implement `getDisplayName()` | Return "Contest Calendar" |
| `meteors/.../MeteorRefreshTask.java` | Implement `getDisplayName()` | Return "Meteor Showers" |
| `spots/.../PskReporterMqttSource.java` | Implement `SubscriptionStatusProvider` | Expose existing connection state |

**Note**: Each coordinator change is trivial - just add one method returning a string constant. The interface change is backward-compatible if we add a default method:

```java
default String getDisplayName() {
    return getTaskName();  // Fallback to task name
}
```

All other changes are new files in the `admin` module or configuration.

---

## Complexity Tracking

No constitution violations. Interface modifications are justified:

| Change | Justification |
|--------|---------------|
| Add `getDisplayName()` to `RefreshTaskCoordinator` | Backward-compatible (default method). Each implementation adds 1 trivial method returning a string constant. Interface is in `common/`, not activity modules. |
| Add `SubscriptionStatusProvider` interface | New interface in `common/`. Minimal (4 methods). `PskReporterMqttSource` already tracks this state internally - just exposing it. |
| Modify 7 RefreshTask implementations | Each adds 1 line: `return "Display Name";`. Trivial, no behavior change. |
| Modify `PskReporterMqttSource` | Exposes existing state. No new behavior. 4 method implementations, all delegating to existing fields. |

**SOLID Compliance**:
- **Single Responsibility**: Admin aggregates; coordinators provide their own metadata
- **Open-Closed**: New feeds implement interface → auto-discovered → zero admin changes
- **Liskov Substitution**: All coordinators are interchangeable
- **Interface Segregation**: `RefreshTaskCoordinator` remains minimal; `SubscriptionStatusProvider` is separate
- **Dependency Inversion**: Admin depends on abstractions (`common/`), not concretions (activity modules)
