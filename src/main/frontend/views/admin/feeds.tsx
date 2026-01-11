/**
 * Feed Manager View
 *
 * Admin page for viewing and managing data feed status.
 * Displays all feeds grouped by module with health status indicators.
 */

import React, { useCallback, useEffect, useState } from 'react';
import { AdminFeedEndpoint } from 'Frontend/generated/endpoints';
import type FeedStatusResponse from 'Frontend/generated/io/nextskip/admin/api/FeedStatusResponse';
import type ModuleFeedStatus from 'Frontend/generated/io/nextskip/admin/api/ModuleFeedStatus';
import ScheduledFeedCard from 'Frontend/components/admin/ScheduledFeedCard';
import SubscriptionFeedCard from 'Frontend/components/admin/SubscriptionFeedCard';
import './FeedManagerView.css';

/**
 * Module icon mapping for display.
 */
const MODULE_ICONS: Record<string, string> = {
  propagation: 'wb_sunny',
  activations: 'terrain',
  contests: 'emoji_events',
  meteors: 'auto_awesome',
  spots: 'radio',
};

/**
 * Returns the icon for a module.
 */
function getModuleIcon(moduleName: string): string {
  return MODULE_ICONS[moduleName.toLowerCase()] ?? 'folder';
}

/**
 * Formats ISO timestamp to readable string.
 */
function formatTimestamp(isoTime: string): string {
  const date = new Date(isoTime);
  return date.toLocaleString();
}

/**
 * Counts feeds by health status across all modules.
 */
function countHealthStatus(modules: ModuleFeedStatus[]): { healthy: number; degraded: number; unhealthy: number } {
  let healthy = 0;
  let degraded = 0;
  let unhealthy = 0;

  for (const module of modules) {
    for (const feed of module.scheduledFeeds) {
      if (feed.healthStatus === 'HEALTHY') healthy++;
      else if (feed.healthStatus === 'DEGRADED') degraded++;
      else if (feed.healthStatus === 'UNHEALTHY') unhealthy++;
    }
    for (const feed of module.subscriptionFeeds) {
      if (feed.healthStatus === 'HEALTHY') healthy++;
      else if (feed.healthStatus === 'DEGRADED') degraded++;
      else if (feed.healthStatus === 'UNHEALTHY') unhealthy++;
    }
  }

  return { healthy, degraded, unhealthy };
}

/**
 * Module section component displaying feeds grouped by module.
 */
function ModuleSection({
  module,
  refreshingFeeds,
  onRefresh,
}: {
  module: ModuleFeedStatus;
  refreshingFeeds: Set<string>;
  onRefresh(feedName: string): void;
}) {
  const feedCount = module.scheduledFeeds.length + module.subscriptionFeeds.length;

  return (
    <section className="feed-manager-module" aria-labelledby={`module-${module.moduleName}`}>
      <div className="feed-manager-module-header">
        <div className="feed-manager-module-icon">
          <span className="material-icons" aria-hidden="true">
            {getModuleIcon(module.moduleName)}
          </span>
        </div>
        <h2 className="feed-manager-module-title" id={`module-${module.moduleName}`}>
          {module.moduleName}
        </h2>
        <span className="feed-manager-module-count">
          {feedCount} feed{feedCount !== 1 ? 's' : ''}
        </span>
      </div>

      <div className="feed-manager-feeds">
        {module.scheduledFeeds.map((feed) => (
          <ScheduledFeedCard
            key={feed.name}
            feed={feed}
            isRefreshing={refreshingFeeds.has(feed.name)}
            onRefresh={onRefresh}
          />
        ))}
        {module.subscriptionFeeds.map((feed) => (
          <SubscriptionFeedCard key={feed.name} feed={feed} />
        ))}
      </div>
    </section>
  );
}

export default function FeedManagerView() {
  const [data, setData] = useState<FeedStatusResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshingFeeds, setRefreshingFeeds] = useState<Set<string>>(new Set());

  /**
   * Fetches feed status data from the backend.
   */
  const fetchFeedStatuses = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await AdminFeedEndpoint.getFeedStatuses();
      setData(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load feed statuses');
    } finally {
      setLoading(false);
    }
  }, []);

  /**
   * Triggers a manual refresh for a specific feed.
   */
  const handleRefresh = useCallback(
    async (feedName: string) => {
      try {
        setRefreshingFeeds((prev) => new Set(prev).add(feedName));
        const result = await AdminFeedEndpoint.triggerFeedRefresh(feedName);

        if (result?.success) {
          // Refetch to get updated status
          await fetchFeedStatuses();
        } else {
          console.error('Refresh failed:', result?.message);
        }
      } catch (err) {
        console.error('Refresh error:', err);
      } finally {
        setRefreshingFeeds((prev) => {
          const next = new Set(prev);
          next.delete(feedName);
          return next;
        });
      }
    },
    [fetchFeedStatuses],
  );

  // Fetch data on mount
  useEffect(() => {
    fetchFeedStatuses();
  }, [fetchFeedStatuses]);

  // Loading state
  if (loading && !data) {
    return (
      <div className="feed-manager">
        <div className="feed-manager-loading">
          <div className="admin-spinner" aria-hidden="true" />
          <p>Loading feed statuses...</p>
        </div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="feed-manager">
        <div className="feed-manager-error">
          <span className="material-icons" aria-hidden="true">
            error_outline
          </span>
          <p>{error}</p>
          <button type="button" onClick={fetchFeedStatuses}>
            Try Again
          </button>
        </div>
      </div>
    );
  }

  // Empty state
  if (!data || data.modules.length === 0) {
    return (
      <div className="feed-manager">
        <div className="feed-manager-empty">
          <span className="material-icons" aria-hidden="true">
            rss_feed
          </span>
          <p>No feeds configured</p>
        </div>
      </div>
    );
  }

  // Count healthy/unhealthy feeds
  const healthCounts = countHealthStatus(data.modules);

  return (
    <main className="feed-manager">
      <header className="feed-manager-header">
        <h1>Feed Manager</h1>
        <p className="feed-manager-description">Monitor and manage data feed health across all modules.</p>

        <div className="feed-manager-stats">
          <div className="feed-manager-stat">
            <span className="feed-manager-stat-value">{data.totalFeeds}</span>
            <span className="feed-manager-stat-label">Total Feeds</span>
          </div>
          <div className="feed-manager-stat">
            <span className="feed-manager-stat-value feed-manager-stat-value--healthy">{healthCounts.healthy}</span>
            <span className="feed-manager-stat-label">Healthy</span>
          </div>
          {healthCounts.degraded > 0 ? (
            <div className="feed-manager-stat">
              <span className="feed-manager-stat-value feed-manager-stat-value--degraded">{healthCounts.degraded}</span>
              <span className="feed-manager-stat-label">Degraded</span>
            </div>
          ) : null}
          {healthCounts.unhealthy > 0 ? (
            <div className="feed-manager-stat">
              <span className="feed-manager-stat-value feed-manager-stat-value--unhealthy">
                {healthCounts.unhealthy}
              </span>
              <span className="feed-manager-stat-label">Unhealthy</span>
            </div>
          ) : null}
        </div>
      </header>

      <div className="feed-manager-modules">
        {data.modules.map((module) => (
          <ModuleSection
            key={module.moduleName}
            module={module}
            refreshingFeeds={refreshingFeeds}
            onRefresh={handleRefresh}
          />
        ))}
      </div>

      <div className="feed-manager-timestamp">
        <span className="material-icons" aria-hidden="true">
          schedule
        </span>
        <span>Last updated: {formatTimestamp(data.timestamp)}</span>
      </div>
    </main>
  );
}
