import React, { useEffect, useState, useCallback } from 'react';
import { AdminEndpoint } from '../../generated/endpoints';
import type FeedStatus from '../../generated/io/nextskip/admin/model/FeedStatus';
import FeedStatusGrid from '../../components/admin/FeedStatusGrid';
import './FeedManagerView.css';

// Polling interval for status updates (5 seconds per spec)
const POLL_INTERVAL = 5000;

/**
 * Feed Manager view - displays all data feeds with their status.
 *
 * Features:
 * - Displays all scheduled and subscription feeds
 * - Auto-refreshes status every 5 seconds
 * - Allows manual refresh of scheduled feeds
 * - Shows last update timestamp
 */
export default function FeedManagerView() {
  const [feeds, setFeeds] = useState<FeedStatus[]>([]);
  const [loading, setLoading] = useState(true);
  const [lastUpdate, setLastUpdate] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  const fetchFeeds = useCallback(async () => {
    try {
      const statuses = await AdminEndpoint.getFeedStatuses();
      // Filter out undefined values from the response
      const validStatuses = (statuses ?? []).filter((s): s is FeedStatus => s !== undefined);
      setFeeds(validStatuses);
      setLastUpdate(new Date());
      setError(null);
    } catch (e) {
      setError('Failed to load feed statuses');
      console.error('Error fetching feed statuses:', e);
    } finally {
      setLoading(false);
    }
  }, []);

  // Initial fetch and polling
  useEffect(() => {
    fetchFeeds();
    const interval = setInterval(fetchFeeds, POLL_INTERVAL);
    return () => clearInterval(interval);
  }, [fetchFeeds]);

  const handleRefresh = async (feedId: string): Promise<boolean> => {
    try {
      const success = await AdminEndpoint.triggerRefresh(feedId);
      // Refresh the list after triggering
      if (success) {
        setTimeout(fetchFeeds, 500);
      }
      return success;
    } catch (e) {
      console.error('Error triggering refresh:', e);
      return false;
    }
  };

  const formatLastUpdate = () => {
    if (!lastUpdate) return 'Never';
    return lastUpdate.toLocaleTimeString();
  };

  // Count healthy vs unhealthy feeds
  const healthyCount = feeds.filter((f) => f.healthy).length;
  const totalCount = feeds.length;

  return (
    <div className="feed-manager">
      <header className="admin-page-header">
        <div className="feed-manager-title-row">
          <h2>Feed Manager</h2>
          <div className="feed-manager-stats">
            <span className={healthyCount === totalCount ? 'all-healthy' : 'some-unhealthy'}>
              {healthyCount}/{totalCount} healthy
            </span>
          </div>
        </div>
        <p>Monitor and manage data feed status</p>
        {error ? <div className="feed-manager-error">{error}</div> : null}
      </header>

      <div className="feed-manager-meta">
        <span className="feed-manager-update">Last updated: {formatLastUpdate()}</span>
        <span className="feed-manager-interval">Auto-refresh: every 5s</span>
      </div>

      <FeedStatusGrid feeds={feeds} onRefresh={handleRefresh} loading={loading} />
    </div>
  );
}
