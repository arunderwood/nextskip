/**
 * Activations Utilities
 *
 * Shared utilities for working with POTA/SOTA activations.
 * Used by activation content components.
 */

/**
 * Format frequency from kHz to MHz display format
 * Example: 14250 kHz -> "14.250 MHz"
 */
export function formatFrequency(freqKhz: number | undefined): string {
  if (!freqKhz) return 'Unknown';

  // Convert kHz to MHz for display
  const freqMhz = freqKhz / 1000;
  return `${freqMhz.toFixed(3)} MHz`;
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
