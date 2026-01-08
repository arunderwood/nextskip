/**
 * BandDxHelp - Help content explaining band-specific DX distance thresholds.
 *
 * Shows how DX scoring varies by band based on propagation characteristics.
 * For example, 3,000 km is exceptional on 160m but routine on 20m.
 *
 * KEEP IN SYNC with FrequencyBand.java DxThresholds
 * @see src/main/java/io/nextskip/common/model/FrequencyBand.java
 */

import React from 'react';
import { bandDxThresholds } from '../../utils/bandDxThresholds';
import './HelpSection.css';

export function BandDxHelp() {
  return (
    <section
      id="help-section-band-dx"
      data-section-id="band-dx"
      className="help-section"
      aria-labelledby="help-section-band-dx-title"
    >
      <h3 id="help-section-band-dx-title" className="help-section__title">
        Band DX Distance Thresholds
      </h3>

      <div className="help-section__content">
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

        <h4>Distance Thresholds by Band</h4>

        <div className="help-section__table-wrapper">
          <table className="help-section__table" role="table" aria-label="Band DX distance thresholds">
            <thead>
              <tr>
                <th scope="col">Band</th>
                <th scope="col">Excellent</th>
                <th scope="col">Good</th>
                <th scope="col">Moderate</th>
                <th scope="col">Propagation</th>
              </tr>
            </thead>
            <tbody>
              {bandDxThresholds.map((threshold) => (
                <tr key={threshold.band}>
                  <td>{threshold.band}</td>
                  <td>{threshold.excellentKm.toLocaleString()}+ km</td>
                  <td>{threshold.goodKm.toLocaleString()}+ km</td>
                  <td>{threshold.moderateKm.toLocaleString()}+ km</td>
                  <td>{threshold.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <h4>Scoring Tiers</h4>
        <p>DX distance contributes 20% to the overall band activity score:</p>
        <ul>
          <li>
            <strong>100 points</strong> &mdash; At or above excellent threshold
          </li>
          <li>
            <strong>70-100 points</strong> &mdash; Between good and excellent
          </li>
          <li>
            <strong>40-70 points</strong> &mdash; Between moderate and good
          </li>
          <li>
            <strong>0-40 points</strong> &mdash; Below moderate threshold
          </li>
        </ul>
      </div>
    </section>
  );
}
