/**
 * SubscriptionFeedCard - Card component for subscription/streaming feeds
 *
 * Displays status information for real-time feeds (e.g., MQTT):
 * - Connection state
 * - Last message received time
 * - Reconnect attempts (if any)
 */

import React from 'react';
import type ConnectionState from 'Frontend/generated/io/nextskip/common/admin/ConnectionState';
import type SubscriptionFeedStatus from 'Frontend/generated/io/nextskip/common/admin/SubscriptionFeedStatus';
import FeedCard, { FeedCardRow, FeedCardAlert } from './FeedCard';

export interface SubscriptionFeedCardProps {
  /** Feed status data */
  feed: SubscriptionFeedStatus;
}

/**
 * Returns CSS class modifier for connection state.
 */
function getConnectionClassName(state: ConnectionState): string {
  const stateLower = state.toLowerCase();
  return `feed-card-connection--${stateLower}`;
}

/**
 * Returns display label for connection state.
 */
function getConnectionLabel(state: ConnectionState): string {
  switch (state) {
    case 'CONNECTED':
      return 'Connected';
    case 'DISCONNECTED':
      return 'Disconnected';
    case 'RECONNECTING':
      return 'Reconnecting';
    case 'STALE':
      return 'Stale';
    default:
      return state;
  }
}

/**
 * Returns icon for connection state.
 */
function getConnectionIcon(state: ConnectionState): string {
  switch (state) {
    case 'CONNECTED':
      return 'wifi';
    case 'DISCONNECTED':
      return 'wifi_off';
    case 'RECONNECTING':
      return 'sync';
    case 'STALE':
      return 'wifi_tethering_error';
    default:
      return 'help';
  }
}

/**
 * Formats an ISO timestamp to a human-readable relative time.
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

/**
 * Connection state badge display.
 */
function ConnectionStateDisplay({ state }: { state: ConnectionState }) {
  return (
    <span className={`feed-card-connection ${getConnectionClassName(state)}`}>
      <span className="material-icons feed-card-connection-icon" aria-hidden="true">
        {getConnectionIcon(state)}
      </span>
      {getConnectionLabel(state)}
    </span>
  );
}

export default function SubscriptionFeedCard({ feed }: SubscriptionFeedCardProps) {
  const hasReconnectAttempts = feed.consecutiveReconnectAttempts > 0;

  return (
    <FeedCard
      name={feed.name}
      icon="podcasts"
      healthStatus={feed.healthStatus}
      footer={
        <div className="feed-card-interval">
          <span className="material-icons" aria-hidden="true">
            rss_feed
          </span>
          <span>Real-time subscription</span>
        </div>
      }
    >
      <FeedCardRow label="Connection" value={<ConnectionStateDisplay state={feed.connectionState} />} />

      <FeedCardRow label="Last Message" value={formatTime(feed.lastMessageTime)} muted={!feed.lastMessageTime} />

      {hasReconnectAttempts ? (
        <FeedCardAlert
          message={`${feed.consecutiveReconnectAttempts} reconnect attempt${feed.consecutiveReconnectAttempts > 1 ? 's' : ''}`}
        />
      ) : null}
    </FeedCard>
  );
}
