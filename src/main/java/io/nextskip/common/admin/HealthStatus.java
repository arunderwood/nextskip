package io.nextskip.common.admin;

/**
 * Health status indicator for data feeds.
 */
public enum HealthStatus {

    /**
     * Feed is operating normally with no issues.
     */
    HEALTHY,

    /**
     * Feed is working but with warnings (e.g., reconnecting, recent failures).
     */
    DEGRADED,

    /**
     * Feed has failed or is not responding.
     */
    UNHEALTHY
}
