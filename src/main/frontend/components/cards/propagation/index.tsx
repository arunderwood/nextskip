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
import { ActivityCard } from '../../activity';
import { calculatePriority, priorityToHotness } from '../../activity/usePriorityCalculation';
import SolarIndicesContent from './SolarIndicesContent';
import BandConditionsContent, { BandConditionsLegend } from './BandConditionsContent';

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
 * Band Conditions Card Definition
 */
const bandConditionsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.propagation?.bandConditions && data.propagation.bandConditions.length > 0);
  },

  createConfig: (data: DashboardData) => {
    const bandConditions = data.propagation?.bandConditions;
    if (!bandConditions || bandConditions.length === 0) return null;

    // Calculate average score from all bands
    const validConditions = bandConditions.filter((c) => c !== undefined);
    const avgScore = validConditions.reduce((sum, c) => sum + (c?.score ?? 0), 0) / validConditions.length;

    // Count favorable bands
    const favorableCount = validConditions.filter((c) => c?.favorable === true).length;
    const isFavorable = favorableCount > validConditions.length / 2;

    // Use best rating among bands
    const hasGood = validConditions.some((c) => c?.rating === 'GOOD');
    const hasFair = validConditions.some((c) => c?.rating === 'FAIR');
    const bestRating = hasGood ? ('GOOD' as const) : hasFair ? ('FAIR' as const) : ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score: avgScore,
      rating: bestRating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'band-conditions',
      type: 'band-conditions',
      size: 'hero',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const bandConditions = data.propagation?.bandConditions;
    if (!bandConditions || bandConditions.length === 0) return null;

    const validBandConditions = bandConditions.filter(
      (c): c is import('Frontend/generated/io/nextskip/propagation/model/BandCondition').default => c !== undefined,
    );

    return (
      <ActivityCard
        config={config}
        title="HF Band Conditions"
        icon={<Radio size={20} />}
        subtitle="Current propagation by amateur radio band"
        footer={<BandConditionsLegend />}
      >
        <BandConditionsContent bandConditions={validBandConditions} />
      </ActivityCard>
    );
  },
};

// Register both cards with the global registry
registerCard(solarIndicesCard);
registerCard(bandConditionsCard);
