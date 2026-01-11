/**
 * Mock type for FeedStatusResponse record.
 */
import type ModuleFeedStatus from './ModuleFeedStatus';

interface FeedStatusResponse {
  modules: ModuleFeedStatus[];
  timestamp: string;
  totalFeeds: number;
}

export default FeedStatusResponse;
