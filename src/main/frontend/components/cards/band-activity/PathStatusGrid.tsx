/**
 * PathStatusGrid - Displays 6 continent paths with status.
 *
 * Single Responsibility: Shows which continental paths are active.
 */

import React from 'react';
import { Check, Circle } from 'lucide-react';
import './BandModeActivityContent.css';

/**
 * All 6 major HF propagation paths.
 * Order matches ContinentPath enum in backend.
 */
const ALL_PATHS = [
  { id: 'NA_EU', display: 'NA\u2194EU', name: 'Trans-Atlantic' },
  { id: 'NA_AS', display: 'NA\u2194AS', name: 'Trans-Pacific' },
  { id: 'EU_AS', display: 'EU\u2194AS', name: 'Europe-Asia' },
  { id: 'NA_OC', display: 'NA\u2194OC', name: 'North America-Oceania' },
  { id: 'EU_AF', display: 'EU\u2194AF', name: 'Europe-Africa' },
  { id: 'NA_SA', display: 'NA\u2194SA', name: 'North-South America' },
] as const;

interface Props {
  /** Array of active path IDs (e.g., ["NA_EU", "NA_AS"]) */
  activePaths: string[];
}

/**
 * Renders a 3x2 grid of continent paths with active/inactive status.
 */
export function PathStatusGrid({ activePaths }: Props) {
  // Convert to Set for O(1) lookup
  const activeSet = new Set(activePaths.map((p) => p.toUpperCase()));

  return (
    <div className="path-grid-container">
      <span className="path-grid-label">Paths</span>
      <div className="path-grid" role="list" aria-label="Continental propagation paths">
        {ALL_PATHS.map((path) => {
          const isActive = activeSet.has(path.id);
          return (
            <div
              key={path.id}
              className={`path-item ${isActive ? 'path-item--active' : 'path-item--inactive'}`}
              role="listitem"
              aria-label={`${path.name}: ${isActive ? 'active' : 'inactive'}`}
            >
              <span className="path-display">{path.display}</span>
              {isActive ? (
                <Check size={12} className="path-status-icon path-status--active" aria-hidden="true" />
              ) : (
                <Circle size={10} className="path-status-icon path-status--inactive" aria-hidden="true" />
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

export default PathStatusGrid;
