/**
 * Time Formatting Utilities
 *
 * Shared utilities for formatting time-related data across the application.
 * Used by contests, events, and activations components.
 */

/**
 * Format time remaining from seconds to human-readable string
 * Examples: "45m", "2h 30m", "3d 5h"
 */
export function formatTimeRemaining(seconds: number | undefined): string {
  if (seconds === undefined || seconds === null) return '';

  const totalSeconds = Math.abs(seconds);
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);

  if (hours < 1) return `${minutes}m`;
  if (hours < 24) return `${hours}h ${minutes}m`;

  const days = Math.floor(hours / 24);
  return `${days}d ${hours % 24}h`;
}

/**
 * Format time since timestamp as human-readable string
 * Examples: "Just now", "5 min ago", "2 hours ago"
 */
export function formatTimeSince(timestamp: string | undefined): string {
  if (!timestamp) return 'Unknown';

  const now = new Date().getTime();
  const spotTime = new Date(timestamp).getTime();
  const diffMinutes = Math.floor((now - spotTime) / (1000 * 60));

  if (diffMinutes < 1) return 'Just now';
  if (diffMinutes === 1) return '1 min ago';
  if (diffMinutes < 60) return `${diffMinutes} min ago`;

  const diffHours = Math.floor(diffMinutes / 60);
  if (diffHours === 1) return '1 hour ago';
  return `${diffHours} hours ago`;
}
