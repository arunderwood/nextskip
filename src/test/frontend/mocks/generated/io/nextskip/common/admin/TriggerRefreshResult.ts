/**
 * Mock type for TriggerRefreshResult record.
 */
interface TriggerRefreshResult {
  success: boolean;
  message: string;
  feedName: string;
  scheduledFor: string | null;
}

export default TriggerRefreshResult;
