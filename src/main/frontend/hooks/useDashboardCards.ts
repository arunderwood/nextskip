/**
 * useDashboardCards - Hook to create card configurations from dashboard data
 *
 * Orchestrates conversion of PropagationResponse data into BentoCardConfig
 * objects with calculated priorities and hotness levels.
 */

import { useMemo } from 'react';
import type PropagationResponse from 'Frontend/generated/io/nextskip/propagation/api/PropagationResponse';
import type { BentoCardConfig } from '../types/bento';
import { calculatePriority, priorityToHotness } from '../components/bento/usePriorityCalculation';

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
  data: PropagationResponse | undefined
): BentoCardConfig[] {
  // Prepare solar indices input (always calculate, even if no data)
  const solarInput = useMemo(() => {
    if (!data?.solarIndices) {
      return { favorable: false, score: undefined, rating: 'UNKNOWN' as const };
    }

    return {
      favorable: data.solarIndices.favorable ?? false,
      score: data.solarIndices.solarFluxIndex,
      rating:
        data.solarIndices.solarFluxIndex !== undefined
          ? data.solarIndices.solarFluxIndex >= 150
            ? ('GOOD' as const)
            : data.solarIndices.solarFluxIndex >= 100
              ? ('FAIR' as const)
              : ('POOR' as const)
          : ('UNKNOWN' as const),
    };
  }, [data?.solarIndices]);

  // Prepare band conditions input (always calculate, even if no data)
  const bandInput = useMemo(() => {
    if (!data?.bandConditions || data.bandConditions.length === 0) {
      return { favorable: false, score: undefined, rating: 'UNKNOWN' as const };
    }

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
    const bestRating = hasGood ? ('GOOD' as const) : hasFair ? ('FAIR' as const) : ('POOR' as const);

    return {
      favorable: isFavorable,
      score: avgScore,
      rating: bestRating,
    };
  }, [data?.bandConditions]);

  // Solar indices card configuration
  const solarIndicesConfig = useMemo((): BentoCardConfig | null => {
    if (!data?.solarIndices) return null;

    const priority = calculatePriority(solarInput);
    const hotness = priorityToHotness(priority);

    return {
      id: 'solar-indices',
      type: 'solar-indices',
      size: 'standard', // 1x1 for compact display
      priority,
      hotness,
    };
  }, [data?.solarIndices, solarInput]);

  // Band conditions card configuration
  const bandConditionsConfig = useMemo((): BentoCardConfig | null => {
    if (!data?.bandConditions || data.bandConditions.length === 0) return null;

    const priority = calculatePriority(bandInput);
    const hotness = priorityToHotness(priority);

    return {
      id: 'band-conditions',
      type: 'band-conditions',
      size: 'wide', // 2x1 for table display
      priority,
      hotness,
    };
  }, [data?.bandConditions, bandInput]);

  // Combine all card configs (filter out null values)
  return useMemo(() => {
    return [solarIndicesConfig, bandConditionsConfig].filter(
      (config): config is BentoCardConfig => config !== null
    );
  }, [solarIndicesConfig, bandConditionsConfig]);
}
