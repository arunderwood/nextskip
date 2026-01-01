/**
 * Meteor Showers Card - Individual card registration for each meteor shower.
 *
 * This module registers individual meteor shower cards with the global registry,
 * enabling each shower to be sorted independently by its own score.
 *
 * SOLID Compliance:
 * - Single Responsibility: Each card represents one meteor shower event
 * - Open-Closed: Reuses EventCard without modification
 * - Liskov Substitution: All Event implementations work with EventCard
 * - Dependency Inversion: Depends on Event abstraction
 */

import React from 'react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import { priorityToHotness } from '../../activity/usePriorityCalculation';
import { EventCard } from '../events/EventCard';
import EventStatus from 'Frontend/generated/io/nextskip/common/model/EventStatus';
import type MeteorShower from 'Frontend/generated/io/nextskip/meteors/model/MeteorShower';
import './MeteorShowerDetails.css';

/**
 * Format ZHR with units
 */
function formatZhr(zhr: number | undefined): string {
  if (zhr === undefined) return 'Unknown';
  return `${zhr}/hr`;
}

/**
 * Calculate ZHR trend direction based on peak midpoint
 */
function calculateZhrTrend(shower: MeteorShower): 'rising' | 'declining' | 'peak' | null {
  if (!shower.peakStart || !shower.peakEnd) return null;
  if (shower.atPeak) return 'peak';

  const peakStartTime = new Date(shower.peakStart).getTime();
  const peakEndTime = new Date(shower.peakEnd).getTime();
  const peakMidpoint = new Date((peakStartTime + peakEndTime) / 2);
  const now = new Date();

  if (now < peakMidpoint) {
    return 'rising';
  } else {
    return 'declining';
  }
}

/**
 * Meteor Shower Details component - renders shower-specific information
 */
function MeteorShowerDetails({ shower }: { shower: MeteorShower }) {
  const currentZhr = shower.currentZhr ?? 0;
  const peakZhr = shower.peakZhr ?? 0;
  const isAtPeak = shower.atPeak ?? false;

  // Calculate percentage for visual meter (0-100%)
  const zhrPercentage = peakZhr > 0 ? Math.min(100, (currentZhr / peakZhr) * 100) : 0;

  // Determine ZHR trend direction
  const trend = calculateZhrTrend(shower);

  return (
    <div className="meteor-shower-details">
      {/* At Peak Indicator */}
      {isAtPeak ? (
        <div className="peak-indicator">
          <span className="peak-icon">✨</span>
          <span className="peak-text">At Peak Activity!</span>
        </div>
      ) : null}

      {/* ZHR Visual Meter */}
      <div className="zhr-meter-section">
        <div className="zhr-header">
          <span className="zhr-label">Zenithal Hourly Rate</span>
          <span className="zhr-current-value">{formatZhr(currentZhr)}</span>
        </div>

        {/* Visual Progress Bar */}
        <div
          className="zhr-meter"
          role="meter"
          aria-label={
            trend === 'peak'
              ? 'ZHR at peak activity'
              : trend === 'rising'
                ? 'ZHR rising toward peak'
                : trend === 'declining'
                  ? 'ZHR declining after peak'
                  : 'ZHR meter'
          }
          aria-valuenow={currentZhr}
          aria-valuemin={0}
          aria-valuemax={peakZhr}
        >
          <div className="zhr-meter-track">
            <div
              className={`zhr-meter-fill ${isAtPeak ? 'at-peak' : trend === 'rising' ? 'rising' : trend === 'declining' ? 'declining' : ''}`}
              style={{ '--zhr-width': `${zhrPercentage}%` } as React.CSSProperties}
            />
          </div>
          <div className="zhr-peak-label">
            <span>Peak: {formatZhr(peakZhr)}</span>
          </div>
        </div>
      </div>

      {/* Peak Timing Info */}
      {shower.peakStart && shower.peakEnd ? (
        <div className="peak-timing-section">
          <span className="timing-icon">📅</span>
          <div className="timing-info">
            <span className="timing-label">Peak Period</span>
            <span className="timing-value">
              {new Date(shower.peakStart).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
              {' - '}
              {new Date(shower.peakEnd).toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
              })}
            </span>
          </div>
        </div>
      ) : null}
    </div>
  );
}

/**
 * Extended EventCard for meteor showers with additional details
 */
function MeteorShowerCard({ shower, config }: { shower: MeteorShower; config: ActivityCardConfig }) {
  return (
    <EventCard event={shower} eventType="meteor-shower" config={config}>
      <MeteorShowerDetails shower={shower} />
    </EventCard>
  );
}

/**
 * Meteor Showers Card Definition - Creates individual cards for each shower
 */
const meteorShowersCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.meteorShowers?.showers && data.meteorShowers.showers.length > 0);
  },

  createConfig: (data: DashboardData): ActivityCardConfig[] | null => {
    const showers = data.meteorShowers?.showers;
    if (!showers || showers.length === 0) return null;

    // Filter out ended showers
    const activeShowers = showers.filter((s): s is MeteorShower => s !== undefined && s.status !== EventStatus.ENDED);

    if (activeShowers.length === 0) return null;

    // Create individual card config for each shower
    return activeShowers.map((shower) => {
      const score = shower.score ?? 0;
      const priority = score;

      // Use code + peakStart for unique ID
      const uniqueId = `meteor-${shower.code}-${shower.peakStart}`.replace(/[^a-zA-Z0-9-]/g, '-');

      return {
        id: uniqueId,
        type: 'event-meteor-shower',
        size: '1x1' as const,
        priority,
        hotness: priorityToHotness(priority),
      };
    });
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const showers = data.meteorShowers?.showers;
    if (!showers) return null;

    // Filter to only active/upcoming showers
    const relevantShowers = showers.filter((s): s is MeteorShower => s !== undefined && s.status !== EventStatus.ENDED);

    // Find the shower by matching the config ID
    const shower = relevantShowers.find((s) => {
      const uniqueId = `meteor-${s.code}-${s.peakStart}`.replace(/[^a-zA-Z0-9-]/g, '-');
      return uniqueId === config.id;
    });

    if (!shower) return null;

    return <MeteorShowerCard shower={shower} config={config} />;
  },
};

// Register the card with the global registry
registerCard(meteorShowersCard);
