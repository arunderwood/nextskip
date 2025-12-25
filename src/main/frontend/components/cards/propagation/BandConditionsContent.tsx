/**
 * BandConditionsContent - Content component for band conditions card
 *
 * Extracted from BandConditionsTable.tsx to work with ActivityCard wrapper.
 * Displays HF band propagation conditions in table format.
 */

import React, { useMemo } from 'react';
import { Check, Minus, X } from 'lucide-react';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import { getRatingClass, getBandDescription, formatBandName, sortBandConditions } from 'Frontend/utils/bandConditions';
import '../../BandConditionsTable.css'; // Reuse existing styles

interface Props {
  bandConditions: BandCondition[];
}

function BandConditionsContent({ bandConditions }: Props) {
  const getRatingIcon = (rating: string): React.ReactElement => {
    switch (rating?.toUpperCase()) {
      case 'GOOD':
        return <Check size={14} />;
      case 'FAIR':
        return <Minus size={14} />;
      case 'POOR':
        return <X size={14} />;
      default:
        return <span>?</span>;
    }
  };

  // Sort bands by frequency (memoized to avoid re-sorting on every render)
  const sortedConditions = useMemo(() => sortBandConditions(bandConditions), [bandConditions]);

  return (
    <div className="table-container">
      <table className="conditions-table">
        <thead>
          <tr>
            <th>Band</th>
            <th>Condition</th>
            <th className="description-col">Notes</th>
          </tr>
        </thead>
        <tbody>
          {sortedConditions.map((condition) => (
            <tr key={condition.band} className="band-row">
              <td className="band-name">
                <span className="band-label">{formatBandName(condition.band || '')}</span>
              </td>
              {/* eslint-disable-next-line jsx-a11y/control-has-associated-label -- This is a data cell, not a control */}
              <td>
                <span className={`rating-badge ${getRatingClass(condition.rating || '')}`}>
                  <span className="rating-icon">{getRatingIcon(condition.rating || '')}</span>
                  <span className="rating-text">{condition.rating || 'Unknown'}</span>
                </span>
              </td>
              <td className="description-col">{getBandDescription(condition.band || '')}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default BandConditionsContent;

/** Legend component for use in ActivityCard footer */
export function BandConditionsLegend() {
  return (
    <div className="legend">
      <div className="legend-title">Legend:</div>
      <div className="legend-items">
        <div className="legend-item">
          <span className="rating-badge rating-good">
            <span className="rating-icon">
              <Check size={14} />
            </span>{' '}
            Good
          </span>
          <span className="legend-desc">Excellent propagation</span>
        </div>
        <div className="legend-item">
          <span className="rating-badge rating-fair">
            <span className="rating-icon">
              <Minus size={14} />
            </span>{' '}
            Fair
          </span>
          <span className="legend-desc">Moderate propagation</span>
        </div>
        <div className="legend-item">
          <span className="rating-badge rating-poor">
            <span className="rating-icon">
              <X size={14} />
            </span>{' '}
            Poor
          </span>
          <span className="legend-desc">Limited propagation</span>
        </div>
      </div>
    </div>
  );
}
