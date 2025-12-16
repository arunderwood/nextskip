/**
 * SotaActivationsContent - Content component for SOTA activations card
 *
 * Displays current Summits on the Air (SOTA) activations with:
 * - Total count of activations on air
 * - List of activators with callsign, reference, frequency, mode, time
 */

import React from 'react';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';
import './SotaActivationsContent.css';

interface Props {
  activations: Activation[];
}

function SotaActivationsContent({ activations }: Props) {
  const formatTimeSince = (timestamp: string | undefined): string => {
    if (!timestamp) return 'Unknown';

    const now = new Date().getTime();
    const spotTime = new Date(timestamp).getTime();
    const diffMinutes = Math.floor((now - spotTime) / (1000 * 60));

    if (diffMinutes < 1) return 'Just now';
    if (diffMinutes === 1) return '1 min ago';
    if (diffMinutes < 60) return `${diffMinutes} min ago`;

    const diffHours = Math.floor(diffMinutes / 60);
    if (diffHours === 1) return '1 hour ago';
    return `${diffHours} hours ago`;
  };

  const formatFrequency = (freqKhz: number | undefined): string => {
    if (!freqKhz) return 'Unknown';

    // Convert kHz to MHz for display
    const freqMhz = freqKhz / 1000;
    return `${freqMhz.toFixed(3)} MHz`;
  };

  // Show up to 8 most recent activations (increased for compact mode)
  const displayActivations = activations.slice(0, 8);

  return (
    <div className="sota-content">
      <div className="activation-count">
        <div className="count-number">{activations.length}</div>
        <div className="count-label">on air</div>
      </div>

      {activations.length === 0 ? (
        <div className="no-activations">
          <p>No current SOTA activations</p>
        </div>
      ) : (
        <ul className="activations-list">
          {displayActivations.map((activation) => (
            <li key={activation.spotId} className="activation-item">
              <div className="activation-header">
                <a
                  href={`https://www.qrz.com/db/${activation.activatorCallsign}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="callsign-link"
                >
                  {activation.activatorCallsign}
                </a>
                <span className="reference-code">{activation.reference}</span>
              </div>
              <div className="activation-details">
                <span className="frequency">{formatFrequency(activation.frequency)}</span>
                <span className="mode">{activation.mode || 'Unknown'}</span>
                <span className="time-since">{formatTimeSince(activation.spottedAt)}</span>
              </div>
              {activation.referenceName && (
                <div className="summit-name">{activation.referenceName}</div>
              )}
            </li>
          ))}
        </ul>
      )}

      {activations.length > 8 && (
        <div className="more-activations">
          +{activations.length - 8} more activations
        </div>
      )}
    </div>
  );
}

export default SotaActivationsContent;
