# Data Model: Admin Feed Manager

**Feature**: 001-admin-feed-manager
**Date**: 2026-01-09

## Overview

This feature introduces models for admin authentication state and feed status monitoring. No new database entities are required—all data is derived from existing infrastructure (OAuth2 session, db-scheduler tables, MQTT connection state).

The architecture follows the existing `RefreshTaskCoordinator` pattern: a shared interface in `common/admin/` with implementations distributed across each activity module.

## Core Interface

### AdminStatusProvider (in `common/admin/`)

**Location**: `io.nextskip.common.admin.AdminStatusProvider`

```java
/**
 * Interface for modules to expose their feed status to the admin UI.
 * Follows the same discovery pattern as RefreshTaskCoordinator.
 */
public interface AdminStatusProvider {

    /**
     * Returns status of all feeds managed by this module.
     */
    List<FeedStatus> getFeedStatuses();

    /**
     * Triggers immediate refresh of a scheduled feed.
     * @param feedName the feed identifier
     * @return result of the trigger attempt, or empty if feed not found
     */
    Optional<TriggerRefreshResult> triggerRefresh(String feedName);

    /**
     * Returns the module name for grouping in the UI.
     */
    String getModuleName();
}
```

**Implementations**:
| Module | Class | Feeds Exposed |
|--------|-------|---------------|
| propagation | `PropagationAdminProvider` | NOAA Solar, HamQSL Solar, HamQSL Band |
| activations | `ActivationsAdminProvider` | POTA, SOTA |
| contests | `ContestsAdminProvider` | WA7BNM Contests |
| meteors | `MeteorsAdminProvider` | Meteor Showers |
| spots | `SpotsAdminProvider` | Band Activity (scheduled), PSKReporter MQTT (subscription) |

## Entities

### 1. AdminUser (Transient - from OAuth2 Session)

Represents the currently authenticated admin user. Derived from the OAuth2 identity token, not persisted.

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| `email` | String | OIDC `email` claim | User's email (used for authorization) |
| `name` | String | OIDC `name` claim | Display name |
| `picture` | String | OIDC `picture` claim | Avatar URL (optional) |
| `roles` | Set<String> | Computed | Contains "ADMIN" if email in allowlist |

**Validation Rules**:
- Email must be in configured allowlist to receive ADMIN role
- Session expires per OAuth2 provider settings (typically 1 hour)

**State Transitions**: N/A (session-based, no persistent state)

---

### 2. FeedStatus (Base - Abstract)

Abstract representation of a data feed's operational status.

| Field | Type | Description |
|-------|------|-------------|
| `name` | String | Human-readable feed name |
| `type` | FeedType | SCHEDULED or SUBSCRIPTION |
| `healthStatus` | HealthStatus | HEALTHY, DEGRADED, or UNHEALTHY |

**Enum: FeedType**
```java
public enum FeedType {
    SCHEDULED,    // Polling on interval (db-scheduler)
    SUBSCRIPTION  // Persistent connection (MQTT)
}
```

**Enum: HealthStatus**
```java
public enum HealthStatus {
    HEALTHY,    // Operating normally
    DEGRADED,   // Working but with warnings (e.g., reconnecting)
    UNHEALTHY   // Failed or not responding
}
```

---

### 3. ScheduledFeedStatus (extends FeedStatus)

Status for scheduled/polling feeds managed by db-scheduler.

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| `name` | String | RefreshTaskCoordinator.getTaskName() | Feed identifier |
| `type` | FeedType | Constant | Always SCHEDULED |
| `lastRefreshTime` | Instant | scheduled_tasks.last_success | Last successful refresh |
| `nextRefreshTime` | Instant | scheduled_tasks.execution_time | Next scheduled run |
| `isCurrentlyRefreshing` | boolean | scheduled_tasks.picked | Task in progress |
| `consecutiveFailures` | int | scheduled_tasks.consecutive_failures | Failure count |
| `lastFailureTime` | Instant | scheduled_tasks.last_failure | Last failed attempt |
| `refreshIntervalSeconds` | long | RefreshTaskCoordinator config | Normal refresh interval |

**Health Status Calculation**:
```
if consecutiveFailures >= 3:
    UNHEALTHY
elif consecutiveFailures >= 1:
    DEGRADED
else:
    HEALTHY
```

**Actions Available**: `triggerRefresh()` - Reschedule for immediate execution

---

### 4. SubscriptionFeedStatus (extends FeedStatus)

Status for subscription/streaming feeds (e.g., MQTT).

| Field | Type | Source | Description |
|-------|------|--------|-------------|
| `name` | String | SpotSource.getSourceName() | Feed identifier |
| `type` | FeedType | Constant | Always SUBSCRIPTION |
| `connectionState` | ConnectionState | SpotSource | Current connection status |
| `lastMessageTime` | Instant | AbstractSpotSource.lastMessageTime | Last data received |
| `consecutiveReconnectAttempts` | int | AbstractSpotSource.consecutiveFailures | Reconnect count |

**Enum: ConnectionState**
```java
public enum ConnectionState {
    CONNECTED,      // Active connection, receiving data
    DISCONNECTED,   // Not connected
    RECONNECTING,   // Attempting to reconnect
    STALE           // Connected but not receiving messages
}
```

**Health Status Calculation**:
```
if connectionState == DISCONNECTED:
    UNHEALTHY
elif connectionState == STALE or connectionState == RECONNECTING:
    DEGRADED
else:
    HEALTHY
```

**Actions Available**: None (subscription feeds auto-reconnect)

---

### 5. FeedStatusResponse (API Response)

Combined response for the Feed Manager view.

| Field | Type | Description |
|-------|------|-------------|
| `scheduledFeeds` | List<ScheduledFeedStatus> | All scheduled feed statuses |
| `subscriptionFeeds` | List<SubscriptionFeedStatus> | All subscription feed statuses |
| `timestamp` | Instant | Response generation time |

---

## Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                     FeedStatusResponse                       │
├─────────────────────────────────────────────────────────────┤
│  timestamp: Instant                                         │
│  scheduledFeeds: List<ScheduledFeedStatus>                  │
│  subscriptionFeeds: List<SubscriptionFeedStatus>            │
└─────────────────────────────────────────────────────────────┘
                          │
           ┌──────────────┴──────────────┐
           ▼                             ▼
┌─────────────────────┐      ┌─────────────────────────┐
│  ScheduledFeedStatus │      │  SubscriptionFeedStatus  │
├─────────────────────┤      ├─────────────────────────┤
│  ◄extends FeedStatus│      │  ◄extends FeedStatus     │
│  lastRefreshTime    │      │  connectionState         │
│  nextRefreshTime    │      │  lastMessageTime         │
│  isCurrentlyRefreshing│    │  consecutiveReconnectAttempts│
│  consecutiveFailures│      └─────────────────────────┘
│  lastFailureTime    │
│  refreshIntervalSeconds│
└─────────────────────┘
           │
           │ data from
           ▼
┌─────────────────────────────────────────────────────────────┐
│                    scheduled_tasks (table)                   │
├─────────────────────────────────────────────────────────────┤
│  task_name, execution_time, picked, last_success,           │
│  last_failure, consecutive_failures                          │
└─────────────────────────────────────────────────────────────┘
```

---

## Data Sources

| Model | Data Source | Access Pattern |
|-------|-------------|----------------|
| AdminUser | OAuth2 OIDC token | Spring Security session |
| ScheduledFeedStatus | db-scheduler Scheduler API | `scheduler.getScheduledExecutions()` |
| SubscriptionFeedStatus | SpotSource interface | `spotsService.isConnected()`, etc. |

---

## No New Database Entities

This feature does **not** require new database tables. All data derives from:
1. OAuth2 session (in-memory, managed by Spring Security)
2. Existing `scheduled_tasks` table (managed by db-scheduler)
3. In-memory connection state (managed by AbstractSpotSource)

This aligns with the principle of minimal complexity—we leverage existing infrastructure rather than introducing redundant persistence.
