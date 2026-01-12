import React from 'react';
import type FeedStatus from '../../generated/io/nextskip/admin/model/FeedStatus';
import FeedStatusCard from './FeedStatusCard';
import './FeedStatusGrid.css';

interface FeedStatusGridProps {
  feeds: FeedStatus[];
  onRefresh(feedId: string): Promise<boolean>;
  loading?: boolean;
}

/**
 * Displays a responsive grid of feed status cards.
 */
export default function FeedStatusGrid({ feeds, onRefresh, loading }: FeedStatusGridProps) {
  if (loading) {
    return (
      <div className="feed-status-grid-loading">
        <div className="feed-status-grid-spinner" />
        <p>Loading feed statuses...</p>
      </div>
    );
  }

  if (feeds.length === 0) {
    return (
      <div className="feed-status-grid-empty">
        <p>No feeds found.</p>
      </div>
    );
  }

  return (
    <div className="feed-status-grid">
      {feeds.map((feed) => (
        <FeedStatusCard key={feed.id} feed={feed} onRefresh={onRefresh} />
      ))}
    </div>
  );
}
