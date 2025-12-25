/**
 * SolarIndicesContent - Content component for solar indices card
 *
 * Extracted from SolarIndicesCard.tsx to work with ActivityCard wrapper.
 * Displays solar weather data: SFI, K-index, A-index, Sunspot Number.
 */

import React from 'react';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
import { getSolarFluxLevel, getGeomagneticLevel } from 'Frontend/utils/solarIndices';
import '../../SolarIndicesCard.css'; // Reuse existing styles

interface Props {
  solarIndices: SolarIndices;
}

function SolarIndicesContent({ solarIndices }: Props) {
  const sfiLevel = getSolarFluxLevel(solarIndices.solarFluxIndex || 0);
  const geoLevel = getGeomagneticLevel(solarIndices.kIndex || 0);

  return (
    <div className="indices-grid">
      <div className="index-item">
        <div className="index-label">Solar Flux Index (SFI)</div>
        <div className="index-value">{solarIndices.solarFluxIndex?.toFixed(1) || 'N/A'}</div>
        <div className={`index-status ${sfiLevel.className}`}>{sfiLevel.label}</div>
      </div>

      <div className="index-item">
        <div className="index-label">K-Index</div>
        <div className="index-value">{solarIndices.kIndex ?? 'N/A'}</div>
        <div className={`index-status ${geoLevel.className}`}>{geoLevel.label}</div>
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
        <div className="index-value">{solarIndices.sunspotNumber ?? 'N/A'}</div>
        <div className="index-description">
          {solarIndices.sunspotNumber !== undefined && solarIndices.sunspotNumber > 100
            ? 'High solar activity'
            : solarIndices.sunspotNumber !== undefined && solarIndices.sunspotNumber > 50
              ? 'Moderate activity'
              : 'Low solar activity'}
        </div>
      </div>
    </div>
  );
}

export default SolarIndicesContent;
