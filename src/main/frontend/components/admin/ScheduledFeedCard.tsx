/**
 * ScheduledFeedCard - Card component for scheduled/polling feeds
 *
 * Displays status information for feeds managed by db-scheduler:
 * - Last refresh time
 * - Next scheduled refresh
 * - Refresh interval
 * - Consecutive failures (if any)
 * - Refresh trigger button
 */

import React, { useCallback } from 'react';
import type ScheduledFeedStatus from 'Frontend/generated/io/nextskip/common/admin/ScheduledFeedStatus';
import FeedCard, { FeedCardRow, FeedCardAlert } from './FeedCard';

export interface ScheduledFeedCardProps {
  /** Feed status data */
  feed: ScheduledFeedStatus;
  /** Whether a refresh is currently being triggered */
  isRefreshing?: boolean;
  /** Callback when refresh button is clicked */
  onRefresh?(feedName: string): void;
}

/**
 * Formats an ISO timestamp to a human-readable relative time or absolute time.
 */
function formatTime(isoTime: string | null): string {
  if (!isoTime) {
    return 'Never';
  }

  const date = new Date(isoTime);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSeconds = Math.floor(diffMs / 1000);
  const diffMinutes = Math.floor(diffSeconds / 60);
  const diffHours = Math.floor(diffMinutes / 60);

  // For past times
  if (diffMs >= 0) {
    if (diffSeconds < 60) {
      return 'Just now';
    }
    if (diffMinutes < 60) {
      return `${diffMinutes}m ago`;
    }
    if (diffHours < 24) {
      return `${diffHours}h ago`;
    }
    return date.toLocaleString();
  }

  // For future times
  const futureDiffSeconds = Math.abs(diffSeconds);
  const futureDiffMinutes = Math.floor(futureDiffSeconds / 60);
  const futureDiffHours = Math.floor(futureDiffMinutes / 60);

  if (futureDiffSeconds < 60) {
    return 'In a few seconds';
  }
  if (futureDiffMinutes < 60) {
    return `In ${futureDiffMinutes}m`;
  }
  if (futureDiffHours < 24) {
    return `In ${futureDiffHours}h`;
  }
  return date.toLocaleString();
}

/**
 * Formats refresh interval seconds to a human-readable string.
 */
function formatInterval(seconds: number): string {
  if (seconds < 60) {
    return `${seconds}s`;
  }
  if (seconds < 3600) {
    const minutes = Math.round(seconds / 60);
    return `${minutes}m`;
  }
  const hours = Math.round(seconds / 3600);
  return `${hours}h`;
}

/**
 * Footer component with refresh interval and trigger button.
 */
function ScheduledFeedFooter({
  feed,
  isRefreshing,
  onRefresh,
}: {
  feed: ScheduledFeedStatus;
  isRefreshing: boolean;
  onRefresh(): void;
}) {
  const isDisabled = isRefreshing || feed.isCurrentlyRefreshing;

  return (
    <>
      <div className="feed-card-interval">
        <span className="material-icons" aria-hidden="true">
          repeat
        </span>
        <span>Every {formatInterval(feed.refreshIntervalSeconds)}</span>
      </div>

      <button
        type="button"
        className={`feed-card-refresh ${isRefreshing ? 'feed-card-refresh--refreshing' : ''}`}
        onClick={onRefresh}
        disabled={isDisabled}
        aria-label={`Trigger refresh for ${feed.name}`}
      >
        <span className="material-icons" aria-hidden="true">
          refresh
        </span>
        {isRefreshing ? 'Refreshing...' : 'Refresh Now'}
      </button>
    </>
  );
}

export default function ScheduledFeedCard({ feed, isRefreshing = false, onRefresh }: ScheduledFeedCardProps) {
  const handleRefresh = useCallback(() => {
    onRefresh?.(feed.name);
  }, [feed.name, onRefresh]);

  const hasFailures = feed.consecutiveFailures > 0;

  return (
    <FeedCard
      name={feed.name}
      icon="schedule"
      healthStatus={feed.healthStatus}
      footer={<ScheduledFeedFooter feed={feed} isRefreshing={isRefreshing} onRefresh={handleRefresh} />}
    >
      <FeedCardRow
        label="Last Refresh"
        value={feed.isCurrentlyRefreshing ? 'Refreshing...' : formatTime(feed.lastRefreshTime)}
        muted={!feed.lastRefreshTime && !feed.isCurrentlyRefreshing}
      />

      <FeedCardRow label="Next Refresh" value={formatTime(feed.nextRefreshTime)} muted={!feed.nextRefreshTime} />

      {hasFailures ? (
        <FeedCardAlert
          message={`${feed.consecutiveFailures} consecutive failure${feed.consecutiveFailures > 1 ? 's' : ''} - Last failed ${formatTime(feed.lastFailureTime)}`}
        />
      ) : null}
    </FeedCard>
  );
}
