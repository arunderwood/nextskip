/**
 * Mock type for ScheduledFeedStatus record.
 */
import type FeedType from './FeedType';
import type HealthStatus from './HealthStatus';

interface ScheduledFeedStatus {
  name: string;
  type: FeedType;
  healthStatus: HealthStatus;
  lastRefreshTime: string | null;
  nextRefreshTime: string | null;
  isCurrentlyRefreshing: boolean;
  consecutiveFailures: number;
  lastFailureTime: string | null;
  refreshIntervalSeconds: number;
}

export default ScheduledFeedStatus;
