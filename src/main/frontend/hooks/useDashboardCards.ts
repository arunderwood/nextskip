/**
 * useDashboardCards - Hook to create card configurations from dashboard data
 *
 * Orchestrates conversion of PropagationResponse data into BentoCardConfig
 * objects with calculated priorities and hotness levels.
 */

import { useMemo } from 'react';
import type { PropagationResponse } from 'Frontend/generated/io/nextskip/propagation/api/PropagationEndpoint';
import type { BentoCardConfig } from '../types/bento';
import { usePriorityCalculation } from '../components/bento';

/**
 * Create card configurations from dashboard data
 *
 * Calculates priorities for each activity based on current conditions
 * and returns configurations ready for BentoGrid.
 *
 * @param data - Propagation response from backend
 * @returns Array of card configurations
 */
export function useDashboardCards(
  data: PropagationResponse | null
): BentoCardConfig[] {
  // Solar indices card configuration
  const solarIndicesConfig = useMemo((): BentoCardConfig | null => {
    if (!data?.solarIndices) return null;

    const { priority, hotness } = usePriorityCalculation({
      favorable: data.solarIndices.favorable ?? false,
      score: data.solarIndices.solarFluxIndex,
      rating:
        data.solarIndices.solarFluxIndex !== undefined
          ? data.solarIndices.solarFluxIndex >= 150
            ? 'GOOD'
            : data.solarIndices.solarFluxIndex >= 100
              ? 'FAIR'
              : 'POOR'
          : 'UNKNOWN',
    });

    return {
      id: 'solar-indices',
      type: 'solar-indices',
      size: 'standard', // 1x1 for compact display
      priority,
      hotness,
    };
  }, [data?.solarIndices]);

  // Band conditions card configuration
  const bandConditionsConfig = useMemo((): BentoCardConfig | null => {
    if (!data?.bandConditions || data.bandConditions.length === 0) return null;

    // Calculate average score from all bands
    const validConditions = data.bandConditions.filter((c) => c !== undefined);
    const avgScore =
      validConditions.reduce((sum, c) => sum + (c?.score ?? 0), 0) /
      validConditions.length;

    // Count favorable bands
    const favorableCount = validConditions.filter(
      (c) => c?.favorable === true
    ).length;
    const isFavorable = favorableCount > validConditions.length / 2;

    // Use best rating among bands
    const hasGood = validConditions.some((c) => c?.rating === 'GOOD');
    const hasFair = validConditions.some((c) => c?.rating === 'FAIR');
    const bestRating = hasGood ? 'GOOD' : hasFair ? 'FAIR' : 'POOR';

    const { priority, hotness } = usePriorityCalculation({
      favorable: isFavorable,
      score: avgScore,
      rating: bestRating,
    });

    return {
      id: 'band-conditions',
      type: 'band-conditions',
      size: 'wide', // 2x1 for table display
      priority,
      hotness,
    };
  }, [data?.bandConditions]);

  // Combine all card configs (filter out null values)
  return useMemo(() => {
    return [solarIndicesConfig, bandConditionsConfig].filter(
      (config): config is BentoCardConfig => config !== null
    );
  }, [solarIndicesConfig, bandConditionsConfig]);
}
