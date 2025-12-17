/**
 * BandConditionsContent - Content component for band conditions card
 *
 * Extracted from BandConditionsTable.tsx to work with ActivityCard wrapper.
 * Displays HF band propagation conditions in table format.
 */

import React from 'react';
import { Check, Minus, X } from 'lucide-react';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import '../../BandConditionsTable.css'; // Reuse existing styles

interface Props {
  bandConditions: BandCondition[];
}

function BandConditionsContent({ bandConditions }: Props) {
  const getRatingClass = (rating: string): string => {
    switch (rating?.toUpperCase()) {
      case 'GOOD':
        return 'rating-good';
      case 'FAIR':
        return 'rating-fair';
      case 'POOR':
        return 'rating-poor';
      default:
        return 'rating-unknown';
    }
  };

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

  const getBandDescription = (band: string): string => {
    const descriptions: { [key: string]: string } = {
      BAND_160M: 'Long distance, nighttime',
      BAND_80M: 'Regional to DX, night',
      BAND_40M: 'All-around workhorse',
      BAND_30M: 'Digital modes, quiet',
      BAND_20M: 'DX powerhouse',
      BAND_17M: 'Underutilized gem',
      BAND_15M: 'DX when conditions support',
      BAND_12M: 'Daytime DX',
      BAND_10M: 'Solar cycle dependent',
      BAND_6M: 'Magic band',
    };
    return descriptions[band] || '';
  };

  // Sort bands by frequency (highest to lowest)
  const sortedConditions = [...bandConditions].sort((a, b) => {
    const order = [
      'BAND_160M',
      'BAND_80M',
      'BAND_40M',
      'BAND_30M',
      'BAND_20M',
      'BAND_17M',
      'BAND_15M',
      'BAND_12M',
      'BAND_10M',
      'BAND_6M',
    ];
    return order.indexOf(a.band || '') - order.indexOf(b.band || '');
  });

  const formatBandName = (band: string): string => {
    return band?.replace('BAND_', '') || 'Unknown';
  };

  return (
    <>
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
                  <span className="band-label">
                    {formatBandName(condition.band || '')}
                  </span>
                </td>
                <td>
                  <span
                    className={`rating-badge ${getRatingClass(condition.rating || '')}`}
                  >
                    <span className="rating-icon">
                      {getRatingIcon(condition.rating || '')}
                    </span>
                    <span className="rating-text">
                      {condition.rating || 'Unknown'}
                    </span>
                  </span>
                </td>
                <td className="description-col">
                  {getBandDescription(condition.band || '')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
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
            <span className="rating-icon"><Check size={14} /></span> Good
          </span>
          <span className="legend-desc">Excellent propagation</span>
        </div>
        <div className="legend-item">
          <span className="rating-badge rating-fair">
            <span className="rating-icon"><Minus size={14} /></span> Fair
          </span>
          <span className="legend-desc">Moderate propagation</span>
        </div>
        <div className="legend-item">
          <span className="rating-badge rating-poor">
            <span className="rating-icon"><X size={14} /></span> Poor
          </span>
          <span className="legend-desc">Limited propagation</span>
        </div>
      </div>
    </div>
  );
}
