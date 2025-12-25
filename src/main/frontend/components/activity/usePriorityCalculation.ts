/**
 * Priority calculation hooks for activity grid card ordering
 *
 * Calculates priority score from input data and maps to visual "hotness" levels.
 * Higher priority cards appear first in the grid (top-left position).
 */

import { useMemo } from 'react';
import type { PriorityInput, HotnessLevel } from '../../types/activity';

/**
 * Weight configuration for priority calculation
 * Total weights: 40% + 35% + 20% + 5% = 100%
 */
const PRIORITY_WEIGHTS = {
  favorable: 40, // Base weight for favorable flag
  score: 35, // Weight for numeric score
  rating: 20, // Weight for rating enum
  recency: 5, // Weight for recency (time since update)
};

/**
 * Rating to score mapping
 * Converts GOOD/FAIR/POOR/UNKNOWN enum to 0-100 scale
 */
const RATING_SCORES: Record<string, number> = {
  GOOD: 100,
  FAIR: 60,
  POOR: 20,
  UNKNOWN: 0,
};

/**
 * Calculate priority score from input data
 *
 * @param input - Priority input data (favorable, score, rating, etc.)
 * @returns Priority score from 0-100
 *
 * @example
 * calculatePriority({
 *   favorable: true,
 *   score: 85,
 *   rating: 'GOOD'
 * }) // Returns ~91 (40 + 29.75 + 20 + 0)
 */
export function calculatePriority(input: PriorityInput): number {
  let priority = 0;

  // 1. Favorable flag contribution (0 or 40)
  if (input.favorable) {
    priority += PRIORITY_WEIGHTS.favorable;
  }

  // 2. Numeric score contribution (0-35)
  if (input.score !== undefined) {
    // Normalize score to 0-100 if needed, then scale by weight
    const normalizedScore = Math.min(100, Math.max(0, input.score));
    priority += (normalizedScore / 100) * PRIORITY_WEIGHTS.score;
  }

  // 3. Rating contribution (0-20)
  if (input.rating) {
    const ratingScore = RATING_SCORES[input.rating] ?? 0;
    priority += (ratingScore / 100) * PRIORITY_WEIGHTS.rating;
  }

  // 4. Recency contribution (0-5) - decays over time
  if (input.lastUpdated) {
    const ageMinutes = (Date.now() - input.lastUpdated.getTime()) / 60000;
    // Full points if updated within 5 minutes, decays to 0 over 60 minutes
    const recencyFactor = Math.max(0, 1 - ageMinutes / 60);
    priority += recencyFactor * PRIORITY_WEIGHTS.recency;
  }

  // 5. Apply user weight multiplier if specified
  if (input.userWeight !== undefined) {
    priority *= input.userWeight;
  }

  return Math.round(priority);
}

/**
 * Map priority score to hotness level for visual styling
 *
 * @param priority - Priority score (0-100)
 * @returns Hotness level (hot/warm/neutral/cool)
 *
 * Ranges:
 * - 70-100: 'hot' (green glow, excellent conditions)
 * - 45-69: 'warm' (orange tint, good conditions)
 * - 20-44: 'neutral' (blue tint, moderate conditions)
 * - 0-19: 'cool' (gray, limited conditions)
 */
export function priorityToHotness(priority: number): HotnessLevel {
  if (priority >= 70) return 'hot';
  if (priority >= 45) return 'warm';
  if (priority >= 20) return 'neutral';
  return 'cool';
}

/**
 * Hook to calculate priority and hotness from input data
 *
 * Memoizes the calculation to avoid unnecessary recomputation.
 *
 * @param input - Priority input data
 * @returns Object with priority score and hotness level
 *
 * @example
 * const { priority, hotness } = usePriorityCalculation({
 *   favorable: true,
 *   score: 85,
 *   rating: 'GOOD'
 * });
 * // priority: 91, hotness: 'hot'
 */
export function usePriorityCalculation(input: PriorityInput) {
  return useMemo(() => {
    const priority = calculatePriority(input);
    const hotness = priorityToHotness(priority);
    return { priority, hotness };
  }, [input]);
}

/**
 * Get hotness display label for UI
 *
 * @param hotness - Hotness level
 * @returns Human-readable label
 */
export function getHotnessLabel(hotness: HotnessLevel): string {
  const labels: Record<HotnessLevel, string> = {
    hot: 'Excellent',
    warm: 'Good',
    neutral: 'Moderate',
    cool: 'Limited',
  };
  return labels[hotness];
}
