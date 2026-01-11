# API Contract: Admin Feed Manager

**Feature**: 001-admin-feed-manager
**Date**: 2026-01-09
**Protocol**: Vaadin Hilla `@BrowserCallable` (Type-safe RPC)

## Overview

Admin API endpoints are protected with `@RolesAllowed("ADMIN")`. Authentication flows through Spring Security OAuth2; authorization via email allowlist.

**Architecture**: The `AdminFeedEndpoint` aggregates feed status from all `AdminStatusProvider` implementations across modules. Each activity module (propagation, activations, etc.) implements `AdminStatusProvider` to expose its own feed status—the admin endpoint does NOT contain feed-specific logic.

---

## Endpoints

### AdminAuthEndpoint

**Class**: `io.nextskip.admin.api.AdminAuthEndpoint`
**Security**: `@RolesAllowed("ADMIN")`

#### getCurrentUser()

Returns the currently authenticated admin user's information.

**Request**: None (uses session)

**Response**: `AdminUserInfo`
```typescript
interface AdminUserInfo {
    email: string;
    name: string;
    picture?: string;
}
```

**Errors**:
- 401 Unauthorized - No valid session
- 403 Forbidden - Authenticated but not admin

---

#### logout()

Invalidates the current admin session.

**Request**: None

**Response**: `void`

**Side Effects**: Session invalidated, client should redirect to `/`

---

### AdminFeedEndpoint

**Class**: `io.nextskip.admin.api.AdminFeedEndpoint`
**Security**: `@RolesAllowed("ADMIN")`

#### getFeedStatuses()

Returns status of all data feeds (scheduled and subscription).

**Request**: None

**Response**: `FeedStatusResponse`
```typescript
interface FeedStatusResponse {
    modules: ModuleFeedStatus[];  // Grouped by module
    timestamp: string; // ISO-8601
}

interface ModuleFeedStatus {
    moduleName: string;  // e.g., "propagation", "activations"
    scheduledFeeds: ScheduledFeedStatus[];
    subscriptionFeeds: SubscriptionFeedStatus[];
}

interface ScheduledFeedStatus {
    name: string;
    type: 'SCHEDULED';
    healthStatus: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
    lastRefreshTime: string | null; // ISO-8601
    nextRefreshTime: string | null; // ISO-8601
    isCurrentlyRefreshing: boolean;
    consecutiveFailures: number;
    lastFailureTime: string | null; // ISO-8601
    refreshIntervalSeconds: number;
}

interface SubscriptionFeedStatus {
    name: string;
    type: 'SUBSCRIPTION';
    healthStatus: 'HEALTHY' | 'DEGRADED' | 'UNHEALTHY';
    connectionState: 'CONNECTED' | 'DISCONNECTED' | 'RECONNECTING' | 'STALE';
    lastMessageTime: string | null; // ISO-8601
    consecutiveReconnectAttempts: number;
}
```

**Example Response**:
```json
{
    "modules": [
        {
            "moduleName": "propagation",
            "scheduledFeeds": [
                {
                    "name": "NOAA Solar Indices",
                    "type": "SCHEDULED",
                    "healthStatus": "HEALTHY",
                    "lastRefreshTime": "2026-01-09T10:30:00Z",
                    "nextRefreshTime": "2026-01-09T10:35:00Z",
                    "isCurrentlyRefreshing": false,
                    "consecutiveFailures": 0,
                    "lastFailureTime": null,
                    "refreshIntervalSeconds": 300
                }
            ],
            "subscriptionFeeds": []
        },
        {
            "moduleName": "activations",
            "scheduledFeeds": [
                {
                    "name": "POTA Activations",
                    "type": "SCHEDULED",
                    "healthStatus": "DEGRADED",
                    "lastRefreshTime": "2026-01-09T10:29:00Z",
                    "nextRefreshTime": "2026-01-09T10:30:00Z",
                    "isCurrentlyRefreshing": false,
                    "consecutiveFailures": 1,
                    "lastFailureTime": "2026-01-09T10:30:00Z",
                    "refreshIntervalSeconds": 60
                }
            ],
            "subscriptionFeeds": []
        },
        {
            "moduleName": "spots",
            "scheduledFeeds": [],
            "subscriptionFeeds": [
                {
                    "name": "PSKReporter MQTT",
                    "type": "SUBSCRIPTION",
                    "healthStatus": "HEALTHY",
                    "connectionState": "CONNECTED",
                    "lastMessageTime": "2026-01-09T10:30:45Z",
                    "consecutiveReconnectAttempts": 0
                }
            ]
        }
    ],
    "timestamp": "2026-01-09T10:30:50Z"
}
```

---

#### triggerFeedRefresh(feedName: string)

Triggers an immediate refresh of a scheduled feed.

**Request**:
```typescript
feedName: string  // e.g., "noaa-refresh"
```

**Response**: `TriggerRefreshResult`
```typescript
interface TriggerRefreshResult {
    success: boolean;
    message: string;
    feedName: string;
    scheduledFor: string; // ISO-8601 (should be ~now)
}
```

**Example Response** (success):
```json
{
    "success": true,
    "message": "Refresh triggered successfully",
    "feedName": "noaa-refresh",
    "scheduledFor": "2026-01-09T10:31:00Z"
}
```

**Example Response** (already refreshing):
```json
{
    "success": false,
    "message": "Feed is already refreshing",
    "feedName": "noaa-refresh",
    "scheduledFor": null
}
```

**Errors**:
- 400 Bad Request - Unknown feed name
- 401 Unauthorized - No valid session
- 403 Forbidden - Authenticated but not admin

**Validation**:
- `feedName` must match an existing `RefreshTaskCoordinator.getTaskName()`
- Cannot trigger refresh for subscription feeds (returns 400)

---

## Authentication Flow

### OAuth2 Login (GitHub - MVP)

**URL**: `/oauth2/authorization/github`

**Flow**:
1. Frontend redirects to `/oauth2/authorization/github`
2. User authenticates with GitHub
3. GitHub redirects to `/login/oauth2/code/github` with auth code
4. Spring Security exchanges code for access token
5. `GitHubAdminUserService` fetches user info and checks email allowlist
6. On success: session created, redirect to `/admin`
7. On failure: redirect to `/login?error=unauthorized`

**GitHub OAuth App Requirements**:
- Scope: `user:email` (to access user's email for allowlist check)
- Callback URL: `{baseUrl}/login/oauth2/code/github`

**Future Migration (Auth0)**:
When JWT support is needed, swap GitHub for Auth0:
- Change provider config in `application.yml`
- Auth0 returns JWT access tokens by default
- Minimal code changes to `SecurityConfig`

### Session Check

**URL**: `/api/auth/user` (optional REST endpoint for initial load)

Used by frontend to check authentication status on page load before making `@BrowserCallable` calls.

---

## Error Responses

All endpoints return standard Hilla error format on failure:

```typescript
interface HillaError {
    type: string;       // Exception class name
    message: string;    // Human-readable message
    detail?: object;    // Additional context (optional)
}
```

**Common Errors**:
| HTTP Status | Type | Cause |
|-------------|------|-------|
| 401 | `UnauthorizedException` | No valid session |
| 403 | `AccessDeniedException` | Session valid but missing ADMIN role |
| 400 | `IllegalArgumentException` | Invalid feedName |

---

## TypeScript Client Generation

Hilla automatically generates TypeScript clients in `src/main/frontend/generated/`:

```typescript
// Generated: AdminAuthEndpoint.ts
export async function getCurrentUser(): Promise<AdminUserInfo> { ... }
export async function logout(): Promise<void> { ... }

// Generated: AdminFeedEndpoint.ts
export async function getFeedStatuses(): Promise<FeedStatusResponse> { ... }
export async function triggerFeedRefresh(feedName: string): Promise<TriggerRefreshResult> { ... }
```

Frontend usage:
```typescript
import { getFeedStatuses, triggerFeedRefresh } from 'Frontend/generated/AdminFeedEndpoint';

const statuses = await getFeedStatuses();
const result = await triggerFeedRefresh('pota-refresh');
```
