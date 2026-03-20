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
