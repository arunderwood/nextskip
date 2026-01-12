import React from 'react';
import type FeedStatus from '../../generated/io/nextskip/admin/model/FeedStatus';
import RefreshButton from './RefreshButton';
import './FeedStatusCard.css';

interface FeedStatusCardProps {
  feed: FeedStatus;
  onRefresh(feedId: string): Promise<boolean>;
}

/**
 * Displays the status of a single data feed.
 *
 * Shows:
 * - Feed name and type indicator
 * - Health status (healthy/unhealthy)
 * - Last activity timestamp
 * - Status message (for unhealthy feeds or subscription status)
 * - Refresh button (for scheduled feeds only)
 */
export default function FeedStatusCard({ feed, onRefresh }: FeedStatusCardProps) {
  const isScheduled = feed.type === 'SCHEDULED';
  const healthClass = feed.healthy ? 'healthy' : 'unhealthy';

  const formatTimestamp = (timestamp: string | null | undefined) => {
    if (!timestamp) return 'Never';
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString();
  };

  return (
    <article className={`feed-status-card ${healthClass}`}>
      <header className="feed-status-header">
        <div className="feed-status-info">
          <h3 className="feed-status-name">{feed.displayName}</h3>
          <span className={`feed-status-type ${feed.type?.toLowerCase()}`}>
            {isScheduled ? 'Scheduled' : 'Subscription'}
          </span>
        </div>
        <div className={`feed-status-indicator ${healthClass}`} title={feed.healthy ? 'Healthy' : 'Unhealthy'} />
      </header>

      <div className="feed-status-details">
        <div className="feed-status-row">
          <span className="feed-status-label">Last Activity:</span>
          <span className="feed-status-value">{formatTimestamp(feed.lastActivity)}</span>
        </div>
        {feed.statusMessage ? (
          <div className="feed-status-row">
            <span className="feed-status-label">Status:</span>
            <span className="feed-status-value">{feed.statusMessage}</span>
          </div>
        ) : null}
      </div>

      {isScheduled && feed.id ? (
        <footer className="feed-status-actions">
          <RefreshButton feedId={feed.id} onRefresh={onRefresh} />
        </footer>
      ) : null}
    </article>
  );
}
