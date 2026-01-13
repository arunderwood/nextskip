# Research: Admin Feed Manager

**Feature**: 001-admin-feed-manager
**Date**: 2026-01-09
**Stack**: Spring Boot 4.0.1, Vaadin Hilla 25.0.2

## 1. Spring Security OAuth2/OIDC Integration

### Decision: GitHub OAuth2 for MVP

**Rationale**: GitHub OAuth2 is the simplest path for MVP—Spring Security's `CommonOAuth2Provider` has built-in support requiring only client ID/secret. The ham radio + tech audience likely has GitHub accounts. Future JWT migration to Auth0 is straightforward when needed.

**Alternatives Considered**:
- Google OAuth2 (viable, but GitHub better fits audience)
- Auth0 (recommended for JWT phase, overkill for MVP)
- Clerk (good DX, but manual Spring integration)
- Basic auth (rejected: not suitable for production admin access)
- Keycloak (rejected: over-engineered for single-admin use case)

**Migration Path**:
```
Phase 1 (MVP):   GitHub OAuth2 → Session cookie
Phase 2 (Later): Auth0 OAuth2 → JWT in Authorization header
```

### Implementation Approach

**Dependencies**:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
```

**Security Configuration** (GitHub-specific):
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // Public routes (existing dashboard)
                .requestMatchers("/", "/VAADIN/**", "/frontend/**").permitAll()
                // Admin routes require ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth -> oauth
                .defaultSuccessUrl("/admin", true)
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService())  // GitHub uses OAuth2, not OIDC
                )
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
            )
            .build();
    }

    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService() {
        return new GitHubAdminUserService(adminEmailAllowlist);
    }
}
```

**Email Allowlist Authorization** (GitHub-specific):
```java
public class GitHubAdminUserService extends DefaultOAuth2UserService {
    private final List<String> adminEmails;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) {
        OAuth2User user = super.loadUser(request);

        // GitHub returns email in attributes (may need separate API call if private)
        String email = user.getAttribute("email");

        if (email == null || !adminEmails.contains(email)) {
            throw new OAuth2AuthenticationException("User not authorized as admin");
        }

        // Add ADMIN authority
        Set<GrantedAuthority> authorities = new HashSet<>(user.getAuthorities());
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));

        return new DefaultOAuth2User(authorities, user.getAttributes(), "login");
    }
}
```

**Configuration**:
```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email  # Required to access email

nextskip:
  admin:
    allowed-emails:
      - your-email@example.com
```

**GitHub OAuth App Setup**:
1. Go to GitHub → Settings → Developer settings → OAuth Apps
2. Create new OAuth App:
   - Application name: `NextSkip Admin`
   - Homepage URL: `https://nextskip.io` (or localhost for dev)
   - Authorization callback URL: `http://localhost:8080/login/oauth2/code/github`
3. Copy Client ID and generate Client Secret

### Hilla Endpoint Security

**Current State**: All endpoints use `@AnonymousAllowed` for public dashboard.

**Admin Endpoints**: Use `@RolesAllowed("ADMIN")` for new admin-only endpoints:
```java
@BrowserCallable
@RolesAllowed("ADMIN")  // Requires ADMIN role from OAuth flow
public class AdminFeedEndpoint {
    // Admin-only feed management methods
}
```

---

## 2. db-scheduler Task Introspection

### Decision: Direct Scheduler API + scheduled_tasks Table

**Rationale**: The existing `Scheduler` bean provides programmatic access to task state. Combined with the `scheduled_tasks` table, this covers all requirements without additional dependencies.

**Alternatives Considered**:
- bekk/db-scheduler-ui (rejected: separate UI, doesn't integrate with NextSkip design)
- Custom JPA repository on scheduled_tasks (rejected: Scheduler API is cleaner)

### Task Status Query

**Scheduler Bean Access**:
```java
@Autowired
private Scheduler scheduler;

// Get all scheduled executions
List<ScheduledExecution<Object>> executions = new ArrayList<>();
scheduler.getScheduledExecutions(executions::add);

// Get specific task
Optional<ScheduledExecution<Object>> execution = scheduler.getScheduledExecution(
    TaskInstanceId.of("pota-refresh", RecurringTask.INSTANCE)
);

// Extract status
Instant nextRun = execution.map(ScheduledExecution::getExecutionTime).orElse(null);
boolean isRunning = execution.map(ScheduledExecution::isPicked).orElse(false);
```

**Table Schema** (from `007-db-scheduler-table.yaml`):
| Column | Use in Admin UI |
|--------|-----------------|
| `task_name` | Feed identifier |
| `execution_time` | Next scheduled run |
| `picked` | Currently running indicator |
| `last_success` | Last successful refresh |
| `last_failure` | Last failed refresh |
| `consecutive_failures` | Health indicator |

### Triggering Immediate Execution

**Pattern** (from existing `DataRefreshStartupHandler`):
```java
scheduler.reschedule(
    task.instance(RecurringTask.INSTANCE),
    Instant.now()
);
scheduler.triggerCheckForDueExecutions();
```

**Requirement**: Enable `immediate-execution-enabled: true` in application.yml.

### Task Discovery

**Leverage Existing Pattern**: Inject all `RefreshTaskCoordinator` beans:
```java
@Autowired
private List<RefreshTaskCoordinator> coordinators;

// Iterate to build feed status list
coordinators.stream()
    .map(c -> buildFeedStatus(c, scheduler))
    .toList();
```

---

## 3. Subscription Feed Status (MQTT)

### Decision: Expose Existing SpotSource Status

**Rationale**: The `SpotsService` already exposes `isConnected()` and `isReceivingMessages()`. The Feed Manager can query this directly.

**Existing API** (from `SpotsService.java`):
```java
public interface SpotsService {
    boolean isConnected();  // MQTT connection state
    // isReceivingMessages() via SpotSource interface
}
```

**Additional Metadata Needed**:
- Last message timestamp (available via `lastMessageTime` in AbstractSpotSource)
- Reconnection attempts (available via `consecutiveFailures`)

**Extension**: Add getter methods to expose these for admin UI:
```java
// In SpotSource interface
Instant getLastMessageTime();
int getConsecutiveReconnectAttempts();
```

---

## 4. Frontend Authentication Flow

### Decision: Vaadin Hilla AuthStateProvider + React Router

**Rationale**: Hilla provides `useAuth()` hook for authentication state. Combined with React Router's `<Outlet>` pattern, this creates a clean protected route structure.

**Implementation**:
```typescript
// AdminLayout.tsx
import { useAuth } from '@vaadin/hilla-react-auth';

export default function AdminLayout() {
    const { state } = useAuth();

    if (state.loading) return <div>Loading...</div>;
    if (!state.user) {
        window.location.href = '/oauth2/authorization/google';
        return null;
    }

    return (
        <div className="admin-layout">
            <AdminNav />
            <main>
                <Outlet />
            </main>
        </div>
    );
}
```

**Routing** (add to App.tsx):
```typescript
<Route path="/admin" element={<AdminLayout />}>
    <Route index element={<AdminLandingView />} />
    <Route path="feeds" element={<FeedManagerView />} />
</Route>
```

---

## 5. Existing Scheduled Tasks (Feed Registry)

From codebase exploration, NextSkip has these scheduled feeds:

| Task Name | Refresh Interval | Module |
|-----------|------------------|--------|
| `noaa-refresh` | 5 min | propagation |
| `hamqsl-solar-refresh` | 30 min | propagation |
| `hamqsl-band-refresh` | 30 min | propagation |
| `pota-refresh` | 1 min | activations |
| `sota-refresh` | 1 min | activations |
| `contest-refresh` | 24 hours | contests |
| `meteor-refresh` | 4 hours | meteors |
| `band-activity-refresh` | 1 min | spots |
| `spot-cleanup` | 1 hour | spots |

And one subscription feed:
| Source | Type | Module |
|--------|------|--------|
| PSKReporter MQTT | Subscription | spots |

---

## Summary of Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Authentication | Spring Security OAuth2 Client | Native Spring Boot integration |
| Authorization | Email allowlist via custom OidcUserService | Simple, configuration-driven |
| Task Status | db-scheduler Scheduler API | Existing infrastructure, no new dependencies |
| Immediate Refresh | `scheduler.reschedule()` + `triggerCheckForDueExecutions()` | Existing pattern in DataRefreshStartupHandler |
| MQTT Status | Extend SpotsService interface | Already exposes connection state |
| Frontend Auth | Hilla `useAuth()` + React Router | Standard Vaadin Hilla pattern |
