/**
 * TrendIndicator - Shows trend arrow + percentage.
 *
 * Single Responsibility: Displays trend direction and magnitude.
 */

import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import './BandModeActivityContent.css';

interface Props {
  /** Percentage change vs baseline (-100 to +infinity) */
  trendPercentage: number;
}

/**
 * Get the appropriate icon for a trend direction.
 */
function getTrendIcon(percentage: number): React.ReactNode {
  const iconSize = 16;

  if (percentage > 5) {
    return <TrendingUp size={iconSize} className="trend-icon trend-icon--up" aria-hidden="true" />;
  }
  if (percentage < -5) {
    return <TrendingDown size={iconSize} className="trend-icon trend-icon--down" aria-hidden="true" />;
  }
  return <Minus size={iconSize} className="trend-icon trend-icon--flat" aria-hidden="true" />;
}

/**
 * Get the CSS class for trend styling.
 */
function getTrendClass(percentage: number): string {
  if (percentage > 5) return 'trend-positive';
  if (percentage < -5) return 'trend-negative';
  return 'trend-flat';
}

/**
 * Format the trend percentage for display.
 */
function formatTrend(percentage: number): string {
  const sign = percentage > 0 ? '+' : '';
  return `${sign}${Math.round(percentage)}%`;
}

/**
 * Get accessible label for screen readers.
 */
function getTrendLabel(percentage: number): string {
  if (percentage > 5) return `Trend up ${Math.round(percentage)} percent versus baseline`;
  if (percentage < -5) return `Trend down ${Math.abs(Math.round(percentage))} percent versus baseline`;
  return 'Trend flat versus baseline';
}

/**
 * Renders a trend indicator with direction icon and percentage.
 */
export function TrendIndicator({ trendPercentage }: Props) {
  return (
    <div className={`trend-indicator ${getTrendClass(trendPercentage)}`}>
      <span className="trend-label">Trend</span>
      <span className="trend-value" aria-label={getTrendLabel(trendPercentage)}>
        {getTrendIcon(trendPercentage)}
        <span className="trend-percentage">{formatTrend(trendPercentage)} vs. baseline</span>
      </span>
    </div>
  );
}

export default TrendIndicator;
