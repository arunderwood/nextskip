/**
 * ActivityBar - Progress bar showing spot count.
 *
 * Single Responsibility: Displays activity level as a visual progress bar.
 */

import React, { useMemo } from 'react';
import './BandModeActivityContent.css';

/** Maximum spots for full bar (100%) */
const MAX_SPOTS_FOR_FULL_BAR = 100;

interface Props {
  /** Number of spots in current window */
  spotCount: number;

  /** Maximum spots for scaling (defaults to 100) */
  maxSpots?: number;

  /** Duration of the window in minutes */
  windowMinutes?: number;
}

/**
 * Renders a progress bar showing activity level.
 */
export function ActivityBar({ spotCount, maxSpots = MAX_SPOTS_FOR_FULL_BAR, windowMinutes = 15 }: Props) {
  // Calculate percentage (capped at 100%)
  const percentage = Math.min(100, (spotCount / maxSpots) * 100);

  // Memoize style object to satisfy react-perf/jsx-no-new-object-as-prop
  const fillStyle = useMemo(() => ({ width: `${percentage}%` }), [percentage]);

  // Format spot count for display
  const formattedCount = spotCount >= 1000 ? `${(spotCount / 1000).toFixed(1)}k` : spotCount.toString();

  return (
    <div className="activity-bar-container">
      <div className="activity-bar-label">
        <span className="activity-bar-title">Activity</span>
        <span className="activity-bar-value">
          {formattedCount} spots ({windowMinutes} min)
        </span>
      </div>
      <div
        className="activity-bar"
        role="progressbar"
        aria-valuenow={percentage}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`Activity level: ${Math.round(percentage)}%`}
      >
        <div className="activity-bar-fill" style={fillStyle} />
      </div>
    </div>
  );
}

export default ActivityBar;
