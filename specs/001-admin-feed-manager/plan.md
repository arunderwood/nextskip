# Implementation Plan: Admin Feed Manager

**Branch**: `001-admin-feed-manager` | **Date**: 2026-01-10 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-admin-feed-manager/spec.md`

## Summary

Add an authenticated admin-only area to NextSkip using GitHub OAuth2 for authentication and Hilla's native security integration. The admin area provides a Feed Manager view showing status of all data feeds (scheduled and subscription types) with the ability to force-refresh scheduled feeds.

**Key Design Choice**: Uses db-scheduler introspection for automatic feed discovery - new scheduled feeds are auto-detected without modifying any existing code (Open-Closed Principle compliant).

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
| V. SOLID Design | PASS | db-scheduler introspection preserves O/C principle |
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
│   │   ├── ScheduledFeedDiscovery.java  # Introspects db-scheduler
│   │   ├── SubscriptionRegistry.java    # Tracks live connections
│   │   └── FeedRefreshService.java      # Triggers manual refreshes
│   └── model/                           # DTOs
│       ├── FeedStatus.java              # Feed status record
│       ├── FeedType.java                # SCHEDULED | SUBSCRIPTION enum
│       └── UserInfo.java                # User info for frontend
├── common/
│   └── subscription/                    # NEW: Shared subscription tracking
│       └── SubscriptionLifecycleAware.java  # Interface for self-registration
├── spots/
│   └── internal/
│       └── MqttSpotClient.java          # MODIFIED: implements SubscriptionLifecycleAware
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

**Structure Decision**: Extends existing modular monolith pattern. Admin module follows same `api/internal/model` structure as other activity modules. Uses db-scheduler introspection to automatically discover scheduled feeds without modifying existing modules.

---

## Architecture Design

### SOLID-Compliant Feed Discovery

#### For Scheduled Feeds: db-scheduler Introspection

The existing scheduled feeds (NOAA, HamQSL, POTA, WA7BNM) all use db-scheduler. We introspect the scheduler directly:

```java
@Component
public class ScheduledFeedDiscovery {
    private final Scheduler scheduler;
    private final ScheduledExecutionsRepository scheduledRepo;

    public List<FeedStatus> getScheduledFeedStatuses() {
        // Query db-scheduler for all registered tasks
        List<ScheduledExecution<Object>> executions = scheduler.getScheduledExecutions();

        return executions.stream()
            .map(exec -> new FeedStatus(
                exec.getTaskInstance().getTaskName(),
                extractDisplayName(exec),
                FeedType.SCHEDULED,
                getLastExecutionTime(exec),
                exec.getExecutionTime(),  // next scheduled
                null,  // no connection status for scheduled
                isHealthy(exec)
            ))
            .toList();
    }

    public void triggerRefresh(String taskName) {
        // Reschedule task to run immediately
        scheduler.reschedule(
            taskInstanceOf(taskName),
            Instant.now()
        );
    }
}
```

**Open-Closed Compliance**:
- Adding a new scheduled feed → Just register with db-scheduler (already required)
- Admin automatically discovers it → Zero admin module changes
- Existing feed modules → Zero changes

#### For Subscription Feeds: Self-Registration Pattern

Subscription feeds (like MQTT) self-register their connection status:

```java
// In common module - minimal interface
public interface SubscriptionLifecycleAware {
    String getSubscriptionId();
    String getDisplayName();
    boolean isConnected();
    Instant getLastConnectedTime();
}

// Registry in admin module collects self-registered subscriptions
@Component
public class SubscriptionRegistry {
    private final List<SubscriptionLifecycleAware> subscriptions;

    // Spring autowires all beans implementing the interface
    public SubscriptionRegistry(List<SubscriptionLifecycleAware> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<FeedStatus> getSubscriptionStatuses() {
        return subscriptions.stream()
            .map(sub -> new FeedStatus(
                sub.getSubscriptionId(),
                sub.getDisplayName(),
                FeedType.SUBSCRIPTION,
                sub.getLastConnectedTime(),
                null,  // no next refresh for subscriptions
                sub.isConnected(),
                sub.isConnected()  // healthy = connected
            ))
            .toList();
    }
}
```

**Open-Closed Compliance**:
- Adding a new subscription feed → Implement `SubscriptionLifecycleAware` (4 methods)
- Admin automatically discovers it → Zero admin module changes
- Interface is minimal and stable

**Note**: The existing `MqttSpotClient` already tracks connection status internally. Adding the interface requires only exposing existing state, not adding new functionality.

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

### 1. db-scheduler for Scheduled Feed Discovery
Query the scheduler directly instead of requiring feeds to implement an interface. This means:
- All existing scheduled feeds auto-discovered
- New scheduled feeds auto-discovered
- Zero changes to existing modules

### 2. Minimal Interface for Subscriptions
`SubscriptionLifecycleAware` has only 4 methods - the minimum needed. The existing MQTT client already tracks this state internally.

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
- `ScheduledFeedDiscovery`: db-scheduler introspection, task name extraction
- `SubscriptionRegistry`: Collects all registered subscriptions
- `FeedRefreshService`: Reschedules task to immediate execution
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

Only one existing file needs modification:

| File | Change | Reason |
|------|--------|--------|
| `MqttSpotClient.java` | Implement `SubscriptionLifecycleAware` | Expose existing connection state |

All other changes are new files in the `admin` module or configuration.

---

## Complexity Tracking

No constitution violations. The `SubscriptionLifecycleAware` interface modification to `MqttSpotClient` is justified:

| Change | Justification |
|--------|---------------|
| Add interface to MqttSpotClient | Exposes existing state (isConnected, lastConnected) that client already tracks. No new behavior added. Interface is stable and minimal (4 methods). |

This maintains Open-Closed for future feeds - they implement the interface when created, and admin discovers them automatically.
