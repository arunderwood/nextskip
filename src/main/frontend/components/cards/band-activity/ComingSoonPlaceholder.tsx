/**
 * ComingSoonPlaceholder - Displayed for unsupported modes.
 *
 * Single Responsibility: Shows placeholder for modes not yet supported.
 */

import React from 'react';
import { Clock } from 'lucide-react';
import './BandModeActivityContent.css';

interface Props {
  /** Mode name to display */
  modeName?: string;
}

/**
 * Renders a placeholder for coming soon features.
 */
export function ComingSoonPlaceholder({ modeName }: Props) {
  return (
    <div className="placeholder-container placeholder--coming-soon">
      <Clock size={20} className="placeholder-icon" aria-hidden="true" />
      <span className="placeholder-text">Coming Soon</span>
      {modeName ? <span className="placeholder-hint">{modeName} support in development</span> : null}
    </div>
  );
}

export default ComingSoonPlaceholder;
