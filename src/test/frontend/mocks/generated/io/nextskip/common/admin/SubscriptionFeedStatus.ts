/**
 * Mock type for SubscriptionFeedStatus record.
 */
import type ConnectionState from './ConnectionState';
import type FeedType from './FeedType';
import type HealthStatus from './HealthStatus';

interface SubscriptionFeedStatus {
  name: string;
  type: FeedType;
  healthStatus: HealthStatus;
  connectionState: ConnectionState;
  lastMessageTime: string | null;
  consecutiveReconnectAttempts: number;
}

export default SubscriptionFeedStatus;
