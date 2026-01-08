/**
 * Band Activity Help Content - Help registration for DX distance thresholds.
 *
 * Shows how DX scoring varies by band based on propagation characteristics.
 * For example, 3,000 km is exceptional on 160m but routine on 20m.
 *
 * KEEP IN SYNC with FrequencyBand.java DxThresholds
 * @see src/main/java/io/nextskip/common/model/FrequencyBand.java
 */

import React from 'react';
import { Radio } from 'lucide-react';
import { registerHelp } from '../../help/HelpRegistry';
import type { HelpDefinition } from '../../help/types';
import { bandDxThresholds } from '../../../utils/bandDxThresholds';

/**
 * Band DX Thresholds Help Content
 */
function BandDxThresholdsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Different bands have vastly different propagation characteristics. What counts as &ldquo;excellent DX&rdquo;
        varies by band:
      </p>

      <ul>
        <li>
          <strong>160m</strong> &mdash; 3,000 km is exceptional (requires night skip)
        </li>
        <li>
          <strong>20m</strong> &mdash; 15,000 km is needed for top score (workhorse DX band)
        </li>
        <li>
          <strong>6m</strong> &mdash; 5,000 km is legendary F2 propagation
        </li>
      </ul>

      <p>
        <strong>Scoring:</strong> DX distance contributes 20% to the overall band activity score. Scores scale from 0
        (no DX) to 100 (excellent threshold reached).
      </p>

      <details>
        <summary>
          <strong>Distance Thresholds by Band</strong>
        </summary>
        <table className="help-section__table" role="table" aria-label="Band DX distance thresholds">
          <thead>
            <tr>
              <th scope="col">Band</th>
              <th scope="col">Excellent</th>
              <th scope="col">Good</th>
              <th scope="col">Propagation</th>
            </tr>
          </thead>
          <tbody>
            {bandDxThresholds.map((threshold) => (
              <tr key={threshold.band}>
                <td>{threshold.band}</td>
                <td>{threshold.excellentKm.toLocaleString()}+ km</td>
                <td>{threshold.goodKm.toLocaleString()}+ km</td>
                <td>{threshold.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>

      <p className="data-source">
        Data source:{' '}
        <a href="https://pskreporter.info/" target="_blank" rel="noopener noreferrer">
          PSKReporter
        </a>
      </p>
    </div>
  );
}

// Register help definition
const bandDxThresholdsHelp: HelpDefinition = {
  id: 'band-dx-thresholds',
  title: 'DX Distance Scoring',
  icon: <Radio size={16} />,
  order: 25, // After band conditions (20), before activations (30)
  Content: BandDxThresholdsHelpContent,
};

registerHelp(bandDxThresholdsHelp);
