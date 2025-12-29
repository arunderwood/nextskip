/**
 * Propagation Cards - Card registration for solar indices and band conditions.
 *
 * This module registers the propagation activity cards with the global registry,
 * enabling them to appear in the dashboard without modifying core files.
 */

import React from 'react';
import { Sun, Radio } from 'lucide-react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import { ActivityCard } from '../../activity';
import { calculatePriority, priorityToHotness } from '../../activity/usePriorityCalculation';
import { formatBandName } from 'Frontend/utils/bandConditions';
import SolarIndicesContent from './SolarIndicesContent';
import { BandRatingDisplay } from './BandRatingDisplay';

/**
 * Solar Indices Card Definition
 */
const solarIndicesCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!data.propagation?.solarIndices;
  },

  createConfig: (data: DashboardData) => {
    if (!data.propagation?.solarIndices) return null;

    const solarIndices = data.propagation.solarIndices;

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
      size: 'standard',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const solarIndices = data.propagation?.solarIndices;
    if (!solarIndices) return null;

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

/**
 * Band Condition Cards - Individual cards for each HF band.
 *
 * Creates separate cards for each band, allowing independent sorting
 * based on individual band propagation quality.
 *
 * SOLID Compliance:
 * - Single Responsibility: Each card represents one band's conditions
 * - Open-Closed: New bands are automatically included from backend data
 * - Liskov Substitution: All BandCondition implementations work with this card
 * - Dependency Inversion: Depends on BandCondition abstraction from generated types
 */
const bandConditionCards: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.propagation?.bandConditions && data.propagation.bandConditions.length > 0);
  },

  createConfig: (data: DashboardData): ActivityCardConfig[] | null => {
    const bandConditions = data.propagation?.bandConditions;
    if (!bandConditions || bandConditions.length === 0) return null;

    // Filter valid conditions and create individual card for each band
    return bandConditions
      .filter((c): c is BandCondition => c !== undefined)
      .map((condition) => {
        // Use backend score directly (0-100 from BandCondition.getScore())
        const score = condition.score ?? 0;

        return {
          id: `band-${condition.band}`,
          type: 'band-condition',
          size: 'standard' as const,
          priority: score,
          hotness: priorityToHotness(score),
        };
      });
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const bandConditions = data.propagation?.bandConditions;
    if (!bandConditions) return null;

    // Find band by matching config ID
    const condition = bandConditions.find((c) => c && `band-${c.band}` === config.id);
    if (!condition) return null;

    return (
      <ActivityCard
        config={config}
        title={formatBandName(condition.band ?? '')}
        icon={<Radio size={20} />}
        subtitle="Band Forecast"
      >
        <BandRatingDisplay condition={condition} />
      </ActivityCard>
    );
  },
};

// Register cards with the global registry
registerCard(solarIndicesCard);
registerCard(bandConditionCards);
