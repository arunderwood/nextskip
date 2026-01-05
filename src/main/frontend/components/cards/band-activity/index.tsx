/**
 * Band Activity Cards - Card registration for band+mode activity.
 *
 * This module registers band+mode activity cards with the global registry,
 * enabling them to appear in the dashboard without modifying core files.
 *
 * SOLID Compliance:
 * - Open-Closed: New modes added via modeRegistry, not code changes
 * - Single Responsibility: This file handles card registration only
 * - Dependency Inversion: Depends on ModeConfig abstraction, not concrete modes
 */

import React from 'react';
import { Radio } from 'lucide-react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import type BandActivity from 'Frontend/generated/io/nextskip/spots/model/BandActivity';
import { ActivityCard } from '../../activity';
import { priorityToHotness } from '../../activity/usePriorityCalculation';
import { getAllModes, getModeConfig } from 'Frontend/config/modeRegistry';
import { normalizeBandToString, bandsMatch, compareBands } from 'Frontend/utils/bandMapping';
import { calculateCombinedScore } from 'Frontend/utils/combinedScore';
import { BandModeActivityContent } from './BandModeActivityContent';
import type { BandModeActivity } from 'Frontend/types/spotterSource';

/**
 * Convert backend BandActivity to frontend BandModeActivity interface.
 */
function toBandModeActivity(activity: BandActivity): BandModeActivity {
  return {
    band: activity.band ?? '',
    mode: activity.mode ?? '',
    spotCount: activity.spotCount ?? 0,
    baselineSpotCount: activity.baselineSpotCount ?? 0,
    trendPercentage: activity.trendPercentage ?? 0,
    maxDxKm: activity.maxDxKm ?? undefined,
    maxDxPath: activity.maxDxPath ?? undefined,
    activePaths: activity.activePaths?.map((p) => String(p)) ?? [],
    score: activity.score ?? 0,
    windowMinutes: activity.windowMinutes ?? 15,
  };
}

/**
 * Get unique bands from both condition and activity data.
 */
function getUniqueBands(data: DashboardData): string[] {
  const bands = new Set<string>();

  // Add bands from condition data
  if (data.propagation?.bandConditions) {
    for (const condition of data.propagation.bandConditions) {
      if (condition?.band) {
        bands.add(normalizeBandToString(condition.band));
      }
    }
  }

  // Add bands from activity data
  if (data.spots?.bandActivities) {
    for (const [, activity] of Object.entries(data.spots.bandActivities)) {
      if (activity?.band) {
        bands.add(normalizeBandToString(activity.band));
      }
    }
  }

  // Sort by frequency (lower first)
  return [...bands].sort(compareBands);
}

/**
 * Find condition for a specific band.
 */
function findCondition(data: DashboardData, band: string): BandCondition | undefined {
  return data.propagation?.bandConditions?.find((c) => c && bandsMatch(c.band, band));
}

/**
 * Find activity for a specific band+mode.
 */
function findActivity(data: DashboardData, band: string, mode: string): BandActivity | undefined {
  if (!data.spots?.bandActivities) return undefined;

  for (const [, activity] of Object.entries(data.spots.bandActivities)) {
    if (activity && bandsMatch(activity.band, band) && activity.mode === mode) {
      return activity;
    }
  }
  return undefined;
}

/**
 * Create card ID from band and mode.
 */
function createCardId(band: string, mode: string): string {
  return `band-activity-${band}-${mode}`;
}

/**
 * Parse band and mode from card ID.
 */
function parseCardId(id: string): { band: string; mode: string } | undefined {
  const prefix = 'band-activity-';
  if (!id.startsWith(prefix)) return undefined;

  const rest = id.slice(prefix.length);
  const lastDash = rest.lastIndexOf('-');
  if (lastDash === -1) return undefined;

  return {
    band: rest.slice(0, lastDash),
    mode: rest.slice(lastDash + 1),
  };
}

/**
 * Band Mode Activity Cards - Creates individual cards for each band+mode combo.
 *
 * Replaces the old band condition cards with merged activity+condition cards.
 */
const bandModeActivityCards: CardDefinition = {
  canRender: (data: DashboardData) => {
    // Can render if we have either spots OR propagation data
    return !!(data.spots?.bandActivities || data.propagation?.bandConditions);
  },

  createConfig: (data: DashboardData): ActivityCardConfig[] | null => {
    const configs: ActivityCardConfig[] = [];
    const bands = getUniqueBands(data);
    const modes = getAllModes();

    // For each band, create cards for each mode
    for (const band of bands) {
      const condition = findCondition(data, band);

      for (const modeConfig of modes) {
        const modeId = String(modeConfig.mode);
        const activity = findActivity(data, band, modeId);

        // Only create cards for supported modes OR modes with activity
        if (!modeConfig.isSupported && !activity) {
          continue;
        }

        // Calculate combined score
        const combinedScore = calculateCombinedScore({
          activityScore: activity?.score,
          conditionScore: condition?.score,
        });

        configs.push({
          id: createCardId(band, modeId),
          type: 'band-mode-activity',
          size: '1x1',
          priority: combinedScore,
          hotness: priorityToHotness(combinedScore),
        });
      }
    }

    return configs.length > 0 ? configs : null;
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const parsed = parseCardId(config.id);
    if (!parsed) return null;

    const { band, mode } = parsed;
    const modeConfig = getModeConfig(mode);
    if (!modeConfig) return null;

    const condition = findCondition(data, band);
    const activity = findActivity(data, band, mode);

    // Convert to frontend interface if activity exists
    const frontendActivity = activity ? toBandModeActivity(activity) : undefined;

    // Create title with band and mode
    const title = `${band} ${mode}`;

    return (
      <ActivityCard config={config} title={title} icon={<Radio size={20} />} subtitle="Band Activity">
        <BandModeActivityContent
          activity={frontendActivity}
          condition={condition}
          modeConfig={modeConfig}
          band={band}
        />
      </ActivityCard>
    );
  },
};

// Register cards with the global registry
registerCard(bandModeActivityCards);
