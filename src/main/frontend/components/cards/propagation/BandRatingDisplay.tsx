/**
 * BandRatingDisplay - Content component for individual band condition cards.
 *
 * Displays the propagation rating for a single HF band.
 * Used within ActivityCard wrapper for the per-band card layout.
 */

import React from 'react';
import { Check, Minus, X } from 'lucide-react';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import { getRatingClass } from 'Frontend/utils/bandConditions';
import './BandRatingDisplay.css';

interface Props {
  condition: BandCondition;
}

/**
 * Get the appropriate icon for a rating level.
 */
function getRatingIcon(rating: string | undefined): React.ReactElement {
  switch (rating?.toUpperCase()) {
    case 'GOOD':
      return <Check size={18} aria-hidden="true" />;
    case 'FAIR':
      return <Minus size={18} aria-hidden="true" />;
    case 'POOR':
      return <X size={18} aria-hidden="true" />;
    default:
      return <span aria-hidden="true">?</span>;
  }
}

/**
 * Get the frequency range for an amateur radio band.
 */
function getFrequencyRange(band: string | undefined): string {
  const ranges: Record<string, string> = {
    '160m': '1.8 – 2.0 MHz',
    '80m': '3.5 – 4.0 MHz',
    '60m': '5 MHz (channelized)',
    '40m': '7.0 – 7.3 MHz',
    '30m': '10.1 – 10.15 MHz',
    '20m': '14.0 – 14.35 MHz',
    '17m': '18.068 – 18.168 MHz',
    '15m': '21.0 – 21.45 MHz',
    '12m': '24.89 – 24.99 MHz',
    '10m': '28.0 – 29.7 MHz',
    '6m': '50.0 – 54.0 MHz',
  };
  return ranges[band ?? ''] ?? '';
}

/**
 * Displays band condition rating prominently.
 */
export function BandRatingDisplay({ condition }: Props) {
  const rating = condition.rating ?? 'UNKNOWN';
  const frequencyRange = getFrequencyRange(condition.band);

  return (
    <div className="band-rating-display">
      <div className="band-condition-container">
        <div className="condition-label">Current Conditions</div>
        <div className="rating-prominent">
          <span
            className={`rating-badge rating-badge--large ${getRatingClass(rating)}`}
            role="status"
            aria-label={`Propagation rating: ${rating}`}
          >
            <span className="rating-icon">{getRatingIcon(rating)}</span>
            <span className="rating-text">{rating}</span>
          </span>
        </div>
        {frequencyRange ? <div className="frequency-range">{frequencyRange}</div> : null}
      </div>
    </div>
  );
}

export default BandRatingDisplay;
