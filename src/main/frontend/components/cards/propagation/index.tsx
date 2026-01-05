/**
 * Propagation Cards - Card registration for solar indices.
 *
 * This module registers the solar indices card with the global registry.
 * Band condition cards have been replaced by the band-activity module
 * which merges activity data with conditions.
 */

import React from 'react';
import { Sun } from 'lucide-react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import { ActivityCard } from '../../activity';
import { calculatePriority, priorityToHotness } from '../../activity/usePriorityCalculation';
import SolarIndicesContent from './SolarIndicesContent';

/** Style for loading state message */
const loadingStyle: React.CSSProperties = {
  padding: '1rem',
  textAlign: 'center',
  color: 'var(--color-text-secondary)',
};

/**
 * Solar Indices Card Definition
 *
 * Always shows this card, displaying a loading state when data is not yet available.
 * This ensures users see the card is expected and data is being fetched.
 */
const solarIndicesCard: CardDefinition = {
  canRender: () => true, // Always show this card

  createConfig: (data: DashboardData) => {
    const solarIndices = data.propagation?.solarIndices;

    // When no data, return config with low priority (card still shows with loading state)
    if (!solarIndices) {
      return {
        id: 'solar-indices',
        type: 'solar-indices',
        size: '1x1',
        priority: 0,
        hotness: 'neutral',
      };
    }

    // Calculate priority from solar indices
    const priority = calculatePriority({
      favorable: solarIndices.favorable ?? false,
      score: solarIndices.solarFluxIndex,
      rating:
        solarIndices.solarFluxIndex !== undefined
          ? solarIndices.solarFluxIndex >= 150
            ? ('GOOD' as const)
            : solarIndices.solarFluxIndex >= 100
              ? ('FAIR' as const)
              : ('POOR' as const)
          : ('UNKNOWN' as const),
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'solar-indices',
      type: 'solar-indices',
      size: '1x1',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const solarIndices = data.propagation?.solarIndices;

    // Show loading state when data is not yet available
    if (!solarIndices) {
      return (
        <ActivityCard config={config} title="Solar Indices" icon={<Sun size={20} />} subtitle="Loading...">
          <div className="loading-state" style={loadingStyle}>
            Fetching solar data...
          </div>
        </ActivityCard>
      );
    }

    return (
      <ActivityCard
        config={config}
        title="Solar Indices"
        icon={<Sun size={20} />}
        subtitle={solarIndices.source}
        footer={
          <div className="info-box">
            <strong>What this means:</strong>
            <ul>
              <li>
                <strong>SFI:</strong> Higher values (150+) indicate better HF propagation
              </li>
              <li>
                <strong>K-Index:</strong> Lower values (0-2) mean quieter, more stable conditions
              </li>
              <li>
                <strong>A-Index:</strong> 24-hour average geomagnetic activity (lower is better)
              </li>
            </ul>
          </div>
        }
      >
        <SolarIndicesContent solarIndices={solarIndices} />
      </ActivityCard>
    );
  },
};

// Register cards with the global registry
registerCard(solarIndicesCard);
