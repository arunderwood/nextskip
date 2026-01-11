/**
 * FeedCard - Base component for feed status display
 *
 * Provides common structure for feed cards including:
 * - Header with name and health status indicator
 * - Body slot for feed-specific content
 * - Footer with refresh interval and optional refresh button
 */

import React from 'react';
import type HealthStatus from 'Frontend/generated/io/nextskip/common/admin/HealthStatus';
import './FeedCard.css';

export interface FeedCardProps {
  /** Human-readable feed name */
  name: string;
  /** Feed icon (Material Icons name) */
  icon: string;
  /** Current health status */
  healthStatus: HealthStatus;
  /** Feed-specific content */
  children: React.ReactNode;
  /** Optional footer content (e.g., refresh button, interval) */
  footer?: React.ReactNode;
}

/**
 * Returns CSS class modifier for health status.
 */
function getStatusClassName(status: HealthStatus): string {
  const statusLower = status.toLowerCase();
  return `feed-card-status--${statusLower}`;
}

/**
 * Returns display label for health status.
 */
function getStatusLabel(status: HealthStatus): string {
  switch (status) {
    case 'HEALTHY':
      return 'Healthy';
    case 'DEGRADED':
      return 'Degraded';
    case 'UNHEALTHY':
      return 'Unhealthy';
    default:
      return status;
  }
}

export default function FeedCard({ name, icon, healthStatus, children, footer }: FeedCardProps) {
  return (
    <article className="feed-card" aria-label={`${name} feed status`}>
      <header className="feed-card-header">
        <h3 className="feed-card-title">
          <span className="material-icons feed-card-icon" aria-hidden="true">
            {icon}
          </span>
          {name}
        </h3>
        <div
          className={`feed-card-status ${getStatusClassName(healthStatus)}`}
          role="status"
          aria-label={`Health status: ${getStatusLabel(healthStatus)}`}
        >
          <span className="feed-card-status-dot" aria-hidden="true" />
          {getStatusLabel(healthStatus)}
        </div>
      </header>

      <div className="feed-card-body">{children}</div>

      {footer ? <footer className="feed-card-footer">{footer}</footer> : null}
    </article>
  );
}

/**
 * Helper component for displaying a row of label + value.
 */
export function FeedCardRow({
  label,
  value,
  muted = false,
}: {
  label: string;
  value: React.ReactNode;
  muted?: boolean;
}) {
  return (
    <div className="feed-card-row">
      <span className="feed-card-label">{label}</span>
      <span className={`feed-card-value ${muted ? 'feed-card-value--muted' : ''}`}>{value}</span>
    </div>
  );
}

/**
 * Helper component for displaying failure alerts.
 */
export function FeedCardAlert({ message }: { message: string }) {
  return (
    <div className="feed-card-alert" role="alert">
      <span className="material-icons" aria-hidden="true">
        warning
      </span>
      <span className="feed-card-alert-text">{message}</span>
    </div>
  );
}
