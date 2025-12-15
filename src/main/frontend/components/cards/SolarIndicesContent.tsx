/**
 * SolarIndicesContent - Content component for solar indices card
 *
 * Extracted from SolarIndicesCard.tsx to work with BentoCard wrapper.
 * Displays solar weather data: SFI, K-index, A-index, Sunspot Number.
 */

import React from 'react';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
import '../SolarIndicesCard.css'; // Reuse existing styles

interface Props {
  solarIndices: SolarIndices;
}

function SolarIndicesContent({ solarIndices }: Props) {
  const getSolarFluxLevel = (
    sfi: number
  ): { label: string; className: string } => {
    if (sfi >= 200) return { label: 'Very High', className: 'status-good' };
    if (sfi >= 150) return { label: 'High', className: 'status-good' };
    if (sfi >= 100) return { label: 'Moderate', className: 'status-fair' };
    if (sfi >= 70) return { label: 'Low', className: 'status-poor' };
    return { label: 'Very Low', className: 'status-poor' };
  };

  const getGeomagneticLevel = (
    kIndex: number
  ): { label: string; className: string } => {
    if (kIndex === 0) return { label: 'Quiet', className: 'status-good' };
    if (kIndex <= 2) return { label: 'Settled', className: 'status-good' };
    if (kIndex <= 4) return { label: 'Unsettled', className: 'status-fair' };
    if (kIndex <= 6) return { label: 'Active', className: 'status-fair' };
    if (kIndex <= 8) return { label: 'Storm', className: 'status-poor' };
    return { label: 'Severe Storm', className: 'status-poor' };
  };

  const sfiLevel = getSolarFluxLevel(solarIndices.solarFluxIndex || 0);
  const geoLevel = getGeomagneticLevel(solarIndices.kIndex || 0);

  return (
    <div className="indices-grid">
      <div className="index-item">
        <div className="index-label">Solar Flux Index (SFI)</div>
        <div className="index-value">
          {solarIndices.solarFluxIndex?.toFixed(1) || 'N/A'}
        </div>
        <div className={`index-status ${sfiLevel.className}`}>
          {sfiLevel.label}
        </div>
      </div>

      <div className="index-item">
        <div className="index-label">K-Index</div>
        <div className="index-value">{solarIndices.kIndex ?? 'N/A'}</div>
        <div className={`index-status ${geoLevel.className}`}>
          {geoLevel.label}
        </div>
      </div>

      <div className="index-item">
        <div className="index-label">A-Index</div>
        <div className="index-value">{solarIndices.aIndex ?? 'N/A'}</div>
        <div className="index-description">
          {solarIndices.aIndex !== undefined && solarIndices.aIndex < 20
            ? 'Quiet conditions'
            : solarIndices.aIndex !== undefined && solarIndices.aIndex < 50
              ? 'Unsettled conditions'
              : 'Disturbed conditions'}
        </div>
      </div>

      <div className="index-item">
        <div className="index-label">Sunspot Number</div>
        <div className="index-value">
          {solarIndices.sunspotNumber ?? 'N/A'}
        </div>
        <div className="index-description">
          {solarIndices.sunspotNumber !== undefined &&
          solarIndices.sunspotNumber > 100
            ? 'High solar activity'
            : solarIndices.sunspotNumber !== undefined &&
                solarIndices.sunspotNumber > 50
              ? 'Moderate activity'
              : 'Low solar activity'}
        </div>
      </div>
    </div>
  );
}

export default SolarIndicesContent;
