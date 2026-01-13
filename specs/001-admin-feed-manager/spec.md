# Feature Specification: Admin Feed Manager

**Feature Branch**: `001-admin-feed-manager`
**Created**: 2026-01-09
**Status**: Draft
**Input**: User description: "Add an authenticated admin-only page to the nextskip site. Once logged in the admin should hit a landing page with a modern and fresh feeling navigation menu to access the different admin functions. The initial admin function is a feed manager view that allows an admin to see the last time each feed refreshed if its a scheduled task, or to see the connection status if its a subscription type task. An admin should be able to click to force an immediate refresh of a scheduled task."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Admin Authentication and Access (Priority: P1)

As a site administrator, I need to securely log in to the admin area so that I can access administrative functions that are not available to regular users.

**Why this priority**: Authentication is the foundational gate that protects all admin functionality. Without secure authentication, no other admin features can be safely exposed.

**Independent Test**: Can be fully tested by attempting login with valid/invalid credentials and verifying access control to admin routes. Delivers secure access control.

**Acceptance Scenarios**:

1. **Given** I am an unauthenticated user, **When** I navigate to the admin URL, **Then** I am redirected to a login page
2. **Given** I am on the admin login page with valid admin credentials, **When** I submit my credentials, **Then** I am authenticated and redirected to the admin landing page
3. **Given** I am on the admin login page with invalid credentials, **When** I submit my credentials, **Then** I see an error message and remain on the login page
4. **Given** I am a logged-in regular user (non-admin), **When** I try to access the admin area, **Then** I see an "access denied" message or am redirected away
5. **Given** I am a logged-in admin, **When** I click the logout button, **Then** my session ends and I am redirected to the login page

---

### User Story 2 - Admin Landing Page with Navigation (Priority: P2)

As a logged-in administrator, I need to see a clean landing page with navigation to various admin functions so that I can easily discover and access the tools I need.

**Why this priority**: The navigation structure is the backbone of the admin experience. It must be in place before any specific admin tools can be accessed, but authentication must come first.

**Independent Test**: Can be tested by logging in as admin and verifying the landing page displays with correct navigation structure and visual styling.

**Acceptance Scenarios**:

1. **Given** I am a logged-in admin, **When** I arrive at the admin landing page, **Then** I see a modern, visually appealing navigation menu
2. **Given** I am on the admin landing page, **When** I view the navigation, **Then** I see a "Feed Manager" option clearly visible
3. **Given** I am on the admin landing page, **When** I click on "Feed Manager" in the navigation, **Then** I am taken to the Feed Manager view
4. **Given** I am anywhere in the admin area, **When** I want to return to the landing page, **Then** I can click a "home" or logo element to return

---

### User Story 3 - View Feed Status (Priority: P3)

As an administrator, I need to see the current status of all data feeds so that I can monitor system health and identify any feeds that may need attention.

**Why this priority**: Viewing feed status is the core informational function that enables administrators to understand system state before taking any action.

**Independent Test**: Can be tested by navigating to Feed Manager and verifying all known feeds display with their current status information.

**Acceptance Scenarios**:

1. **Given** I am on the Feed Manager view, **When** the page loads, **Then** I see a list of all data feeds in the system
2. **Given** I am viewing a scheduled feed entry, **When** I look at its details, **Then** I see the last refresh timestamp and the next scheduled refresh time
3. **Given** I am viewing a subscription feed entry, **When** I look at its details, **Then** I see the connection status (connected, disconnected, reconnecting)
4. **Given** a feed has not refreshed within its expected interval, **When** I view the Feed Manager, **Then** that feed is visually highlighted as potentially problematic
5. **Given** I am viewing the Feed Manager, **When** feed status changes in the background, **Then** the display updates to reflect the new status

---

### User Story 4 - Force Feed Refresh (Priority: P4)

As an administrator, I need to manually trigger an immediate refresh of a scheduled feed so that I can recover from stale data or verify that a feed is working correctly after troubleshooting.

**Why this priority**: This is an action capability that depends on the viewing functionality being in place first. It provides operational control over the system.

**Independent Test**: Can be tested by clicking the refresh button on a scheduled feed and verifying the feed refresh is triggered and new data appears.

**Acceptance Scenarios**:

1. **Given** I am viewing a scheduled feed in the Feed Manager, **When** I click the "Refresh Now" button, **Then** the system immediately triggers a refresh for that feed
2. **Given** I have triggered a manual refresh, **When** the refresh completes successfully, **Then** I see the last refresh timestamp update to the current time
3. **Given** I have triggered a manual refresh, **When** the refresh fails, **Then** I see an error message indicating what went wrong
4. **Given** a feed refresh is in progress, **When** I view that feed's entry, **Then** I see a visual indicator that refresh is in progress
5. **Given** I am viewing a subscription-type feed, **When** I look at available actions, **Then** I do NOT see a "Refresh Now" button (subscriptions are continuous, not scheduled)

---

### Edge Cases

- What happens when an admin's session expires while viewing the Feed Manager? (Should redirect to login with a session expired message)
- How does the system handle simultaneous manual refresh requests from multiple admins for the same feed? (Should either queue or show "already refreshing" status)
- What if a feed source is temporarily unavailable? (Should show error status and allow retry)
- How does the system behave if there are no feeds configured? (Should show an empty state with helpful message)
- What happens if a subscription feed loses connection repeatedly? (Should show reconnection attempts and last successful connection time)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a dedicated admin URL path that is separate from the public site
- **FR-002**: System MUST require authentication before granting access to any admin functionality
- **FR-003**: System MUST verify that authenticated users have admin privileges (email matches configured allowlist) before showing admin content
- **FR-004**: System MUST provide a logout mechanism that terminates the admin session
- **FR-005**: System MUST display a landing page after successful admin login with navigation to admin functions
- **FR-006**: System MUST provide navigation that allows admins to access the Feed Manager view
- **FR-007**: System MUST display all configured data feeds in the Feed Manager view
- **FR-008**: System MUST distinguish between scheduled feeds and subscription feeds in the display
- **FR-009**: System MUST show last refresh timestamp for scheduled feeds
- **FR-010**: System MUST show next scheduled refresh time for scheduled feeds
- **FR-011**: System MUST show connection status for subscription feeds
- **FR-012**: System MUST provide a manual refresh action for scheduled feeds
- **FR-013**: System MUST execute feed refresh when admin triggers manual refresh
- **FR-014**: System MUST provide feedback during refresh operations (loading state, success, or failure)
- **FR-015**: System MUST visually indicate feeds that may be in an unhealthy state

### Key Entities

- **Admin User**: A user account with elevated privileges that grants access to the admin area. Distinguished from regular users by an admin role or permission.
- **Data Feed**: Represents an external data source that NextSkip consumes. Has a name, type (scheduled or subscription), and current status.
- **Scheduled Feed**: A type of data feed that refreshes on a recurring schedule. Tracks last refresh time and next scheduled refresh.
- **Subscription Feed**: A type of data feed that maintains a persistent connection to receive real-time updates. Tracks connection state.
- **Feed Status**: The current operational state of a feed including health indicators, timestamps, and any error information.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Admin users can access the admin area within 10 seconds of entering correct credentials
- **SC-002**: Unauthorized users cannot access any admin functionality (100% access control)
- **SC-003**: Admin can view status of all feeds within 3 seconds of navigating to Feed Manager
- **SC-004**: Manual feed refresh is triggered within 2 seconds of clicking the refresh button
- **SC-005**: Feed status updates are visible to admin within 5 seconds of the status change occurring
- **SC-006**: 100% of scheduled feeds display last refresh timestamp
- **SC-007**: 100% of subscription feeds display current connection status
- **SC-008**: Admin can identify unhealthy feeds at a glance without reading detailed status for each feed

## Clarifications

### Session 2026-01-09

- Q: Where do admin credentials come from? → A: External identity provider (OAuth2/OIDC)
- Q: How is admin authorization determined? → A: Allowlist by email from Spring property or environment variable
- Q: Which OAuth2 provider for MVP? → A: GitHub (simplest, audience fit); Auth0 recommended for future JWT migration

## Assumptions

- Authentication delegates to GitHub OAuth2 for MVP; no local user/password management required
- Admin authorization is determined by email allowlist configured via Spring property or environment variable
- Future JWT migration path: Auth0 (when needed, not in MVP scope)
- The existing db-scheduler infrastructure provides access to feed scheduling information
- Subscription feeds (like MQTT connections) expose connection state that can be queried
- The admin area will use the same React/Vaadin Hilla stack as the main application
- Session-based authentication is acceptable for the admin area (standard web application pattern)
- A single admin role is sufficient initially (no need for granular admin permissions)
- The admin navigation will be expandable to accommodate future admin functions beyond Feed Manager
