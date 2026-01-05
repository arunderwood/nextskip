/**
 * DxReachDisplay - Shows max DX distance and path.
 *
 * Single Responsibility: Displays the maximum DX reach for a band.
 */

import React from 'react';
import { Globe } from 'lucide-react';
import './BandModeActivityContent.css';

interface Props {
  /** Maximum DX distance in km */
  maxDxKm?: number;

  /** Description of the max DX path (e.g., "JA1ABC -> W6XYZ") */
  maxDxPath?: string;
}

/**
 * Format distance for display.
 */
function formatDistance(km: number): string {
  if (km >= 1000) {
    return `${(km / 1000).toFixed(1).replace(/\.0$/, '')}k km`;
  }
  return `${km} km`;
}

/**
 * Renders DX reach information.
 */
export function DxReachDisplay({ maxDxKm, maxDxPath }: Props) {
  // No DX data available
  if (!maxDxKm || maxDxKm <= 0) {
    return (
      <div className="dx-reach-container dx-reach--empty">
        <Globe size={14} className="dx-reach-icon" aria-hidden="true" />
        <span className="dx-reach-label">DX Reach</span>
        <span className="dx-reach-value">No DX data</span>
      </div>
    );
  }

  // Clean up path format if present
  const displayPath = maxDxPath?.replace('->', '\u2192') || '';

  return (
    <div className="dx-reach-container">
      <Globe size={14} className="dx-reach-icon" aria-hidden="true" />
      <span className="dx-reach-label">DX Reach</span>
      <span className="dx-reach-value" aria-label={`Maximum DX distance ${maxDxKm} kilometers`}>
        {formatDistance(maxDxKm)}
        {displayPath ? <span className="dx-reach-path">({displayPath})</span> : null}
      </span>
    </div>
  );
}

export default DxReachDisplay;
