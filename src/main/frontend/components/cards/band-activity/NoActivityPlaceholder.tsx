/**
 * NoActivityPlaceholder - Displayed when activity data is unavailable.
 *
 * Single Responsibility: Shows placeholder for missing activity data.
 */

import React from 'react';
import { Radio } from 'lucide-react';
import './BandModeActivityContent.css';

/**
 * Renders a placeholder when no activity data is available.
 */
export function NoActivityPlaceholder() {
  return (
    <div className="placeholder-container placeholder--no-activity">
      <Radio size={24} className="placeholder-icon" aria-hidden="true" />
      <span className="placeholder-text">No Activity Data</span>
      <span className="placeholder-hint">Waiting for spots...</span>
    </div>
  );
}

export default NoActivityPlaceholder;
