/**
 * ActivationsContent - Generic content component for POTA/SOTA activations
 *
 * Displays current activations with:
 * - Total count of activations on air
 * - List of activators with callsign, reference, frequency, mode, time
 *
 * This component consolidates the previously duplicated POTA and SOTA components.
 */

import React, { useMemo } from 'react';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';
import type { ActivationLocationExt } from 'Frontend/types/ActivationLocation';
import { formatTimeSince, formatFrequency } from 'Frontend/utils/activations';
import './PotaActivationsContent.css';
import './SotaActivationsContent.css';

interface Props {
  activations: Activation[];
  type: 'pota' | 'sota';
  emptyMessage: string;
}

const MAX_DISPLAY_ACTIVATIONS = 250;

/**
 * Generate URL for park/summit details page
 */
function getPotaReferenceUrl(reference: string): string {
  return `https://pota.app/#/park/${reference}`;
}

/**
 * Generate SOTA summit URL. The sotl.as URL format requires the full summit reference
 * including the association code (e.g., "JA/ST-013" not just "ST-013").
 */
function getSotaReferenceUrl(reference: string, associationCode?: string): string {
  // If reference already contains a slash, it includes the association (e.g., "W7W/LC-001")
  if (reference.includes('/')) {
    return `https://sotl.as/summits/${reference}`;
  }
  // Otherwise, prepend the association code if available (e.g., "JA" + "ST-013" -> "JA/ST-013")
  if (associationCode) {
    return `https://sotl.as/summits/${associationCode}/${reference}`;
  }
  // Fallback if no association code available
  return `https://sotl.as/summits/${reference}`;
}

function ActivationsContent({ activations, type, emptyMessage }: Props) {
  // Sort by most recent spot time and limit display count (memoized to avoid recalculating)
  const displayActivations = useMemo(
    () =>
      [...activations]
        .sort((a, b) => new Date(b.spottedAt ?? 0).getTime() - new Date(a.spottedAt ?? 0).getTime())
        .slice(0, MAX_DISPLAY_ACTIVATIONS),
    [activations],
  );

  const contentClass = `${type}-content`;
  const locationClass = type === 'pota' ? 'park-name' : 'summit-name';

  return (
    <div className={contentClass}>
      <div className="activation-count">
        <div className="count-number">{activations.length}</div>
        <div className="count-label">on air</div>
      </div>

      {activations.length === 0 ? (
        <div className="no-activations">
          <p>{emptyMessage}</p>
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
                {(activation.location as ActivationLocationExt)?.reference ? (
                  <a
                    href={
                      type === 'pota'
                        ? getPotaReferenceUrl((activation.location as ActivationLocationExt).reference!)
                        : getSotaReferenceUrl(
                            (activation.location as ActivationLocationExt).reference!,
                            (activation.location as ActivationLocationExt).associationCode,
                          )
                    }
                    target="_blank"
                    rel="noopener noreferrer"
                    className="reference-code"
                  >
                    {(activation.location as ActivationLocationExt).reference}
                  </a>
                ) : null}
              </div>
              <div className="activation-details">
                <span className="frequency">{formatFrequency(activation.frequency)}</span>
                <span className="mode">{activation.mode || 'Unknown'}</span>
                <span className="time-since">{formatTimeSince(activation.spottedAt)}</span>
              </div>
              {(activation.location as ActivationLocationExt)?.name ? (
                <div className={locationClass}>
                  {(activation.location as ActivationLocationExt).name}
                  {(activation.location as ActivationLocationExt).regionCode
                    ? `, ${(activation.location as ActivationLocationExt).regionCode}`
                    : null}
                </div>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      {activations.length > MAX_DISPLAY_ACTIVATIONS && (
        <div className="more-activations">+{activations.length - MAX_DISPLAY_ACTIVATIONS} more activations</div>
      )}
    </div>
  );
}

export default ActivationsContent;
