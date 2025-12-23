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

/* eslint-disable react/jsx-key */

/**
 * Format ZHR with units
 */
function formatZhr(zhr: number | undefined): string {
  if (zhr === undefined) return 'Unknown';
  return `${zhr}/hr`;
}

/**
 * Meteor Shower Details component - renders shower-specific information
 */
function MeteorShowerDetails({ shower }: { shower: MeteorShower }) {
  const currentZhr = shower.currentZhr ?? 0;
  const peakZhr = shower.peakZhr ?? 0;
  const isAtPeak = shower.atPeak ?? false;

  return (
    <div className="event-details">
      <div className="detail-row">
        <span className="detail-label">Current Rate:</span>
        <span className="detail-value">
          {formatZhr(currentZhr)}
          {isAtPeak && <span className="peak-badge"> (Peak!)</span>}
        </span>
      </div>
      <div className="detail-row">
        <span className="detail-label">Peak Rate:</span>
        <span className="detail-value">{formatZhr(peakZhr)}</span>
      </div>
      {shower.parentBody && (
        <div className="detail-row">
          <span className="detail-label">Parent:</span>
          <span className="detail-value">{shower.parentBody}</span>
        </div>
      )}
    </div>
  );
}

/**
 * Extended EventCard for meteor showers with additional details
 */
function MeteorShowerCard({
  shower,
  config
}: {
  shower: MeteorShower;
  config: ActivityCardConfig;
}) {
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
    const activeShowers = showers.filter(
      (s): s is MeteorShower => s !== undefined && s.status !== EventStatus.ENDED
    );

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
        size: 'standard' as const,
        priority,
        hotness: priorityToHotness(priority),
      };
    });
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const showers = data.meteorShowers?.showers;
    if (!showers) return null;

    // Filter to only active/upcoming showers
    const relevantShowers = showers.filter(
      (s): s is MeteorShower => s !== undefined && s.status !== EventStatus.ENDED
    );

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
