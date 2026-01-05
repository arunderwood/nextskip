/**
 * ConditionBadge - Small badge showing propagation rating.
 *
 * Single Responsibility: Displays condition rating as a compact badge.
 */

import React from 'react';
import { Check, Minus, X, HelpCircle } from 'lucide-react';
import './BandModeActivityContent.css';

type Rating = 'GOOD' | 'FAIR' | 'POOR' | 'UNKNOWN';

interface Props {
  /** Propagation rating from BandCondition */
  rating?: Rating | string;
}

/**
 * Get the appropriate icon for a rating level.
 */
function getRatingIcon(rating: string): React.ReactNode {
  const iconSize = 12;

  switch (rating.toUpperCase()) {
    case 'GOOD':
      return <Check size={iconSize} aria-hidden="true" />;
    case 'FAIR':
      return <Minus size={iconSize} aria-hidden="true" />;
    case 'POOR':
      return <X size={iconSize} aria-hidden="true" />;
    default:
      return <HelpCircle size={iconSize} aria-hidden="true" />;
  }
}

/**
 * Get the CSS class for rating styling.
 */
function getRatingClass(rating: string): string {
  switch (rating.toUpperCase()) {
    case 'GOOD':
      return 'condition-badge--good';
    case 'FAIR':
      return 'condition-badge--fair';
    case 'POOR':
      return 'condition-badge--poor';
    default:
      return 'condition-badge--unknown';
  }
}

/**
 * Renders a compact condition badge.
 */
export function ConditionBadge({ rating }: Props) {
  if (!rating) {
    return null;
  }

  const normalizedRating = rating.toUpperCase();

  return (
    <div className="condition-badge-container">
      <span className="condition-badge-label">Forecast</span>
      <span
        className={`condition-badge ${getRatingClass(normalizedRating)}`}
        role="status"
        aria-label={`Propagation forecast: ${normalizedRating}`}
      >
        {getRatingIcon(normalizedRating)}
        <span className="condition-badge-text">{normalizedRating}</span>
      </span>
    </div>
  );
}

export default ConditionBadge;
