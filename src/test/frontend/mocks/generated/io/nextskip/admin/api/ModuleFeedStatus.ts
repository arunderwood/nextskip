/**
 * Mock type for ModuleFeedStatus record.
 */
import type ScheduledFeedStatus from '../../common/admin/ScheduledFeedStatus';
import type SubscriptionFeedStatus from '../../common/admin/SubscriptionFeedStatus';

interface ModuleFeedStatus {
  moduleName: string;
  scheduledFeeds: ScheduledFeedStatus[];
  subscriptionFeeds: SubscriptionFeedStatus[];
}

export default ModuleFeedStatus;
