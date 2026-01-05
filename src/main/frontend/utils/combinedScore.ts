/**
 * Combined Score Calculation
 *
 * Calculates merged priority score from activity and condition data.
 * Uses weighted average: 70% activity score, 30% condition score.
 *
 * This weighting prioritizes real-time activity (what's happening now)
 * while still considering propagation forecasts (what should work).
 */

/** Weight for activity score (real-time spot data) */
export const ACTIVITY_WEIGHT = 0.7;

/** Weight for condition score (propagation forecast) */
export const CONDITION_WEIGHT = 0.3;

/**
 * Input for combined score calculation.
 */
export interface CombinedScoreInput {
  /** Activity score from BandActivity (0-100), undefined if no activity data */
  activityScore?: number;

  /** Condition score from BandCondition (0-100), undefined if no condition data */
  conditionScore?: number;
}

/**
 * Calculate combined score for a band+mode card.
 *
 * Scoring logic:
 * - Both present: 70% activity + 30% condition (weighted average)
 * - Activity only: Use activity score directly
 * - Condition only: Use condition score directly
 * - Neither: Return 0
 *
 * @param input - Activity and/or condition scores
 * @returns Combined score 0-100
 */
export function calculateCombinedScore(input: CombinedScoreInput): number {
  const { activityScore, conditionScore } = input;

  // Both scores available: weighted average
  if (activityScore !== undefined && conditionScore !== undefined) {
    const weighted = activityScore * ACTIVITY_WEIGHT + conditionScore * CONDITION_WEIGHT;
    return Math.round(Math.min(100, Math.max(0, weighted)));
  }

  // Activity only
  if (activityScore !== undefined) {
    return Math.round(Math.min(100, Math.max(0, activityScore)));
  }

  // Condition only
  if (conditionScore !== undefined) {
    return Math.round(Math.min(100, Math.max(0, conditionScore)));
  }

  // Neither available
  return 0;
}

/**
 * Determine if conditions are favorable based on combined data.
 *
 * Favorable when:
 * - Activity is favorable (high activity + positive trend + active paths), OR
 * - Condition is favorable (GOOD rating with confidence > 0.5)
 *
 * @param activityFavorable - Whether activity data indicates favorable conditions
 * @param conditionFavorable - Whether condition data indicates favorable conditions
 * @returns true if either source indicates favorable conditions
 */
export function isCombinedFavorable(activityFavorable?: boolean, conditionFavorable?: boolean): boolean {
  // Either source indicating favorable is sufficient
  return activityFavorable === true || conditionFavorable === true;
}

/**
 * Get the activity and condition weights for transparency.
 *
 * @returns Object with weight values
 */
export function getWeights(): { activity: number; condition: number } {
  return {
    activity: ACTIVITY_WEIGHT,
    condition: CONDITION_WEIGHT,
  };
}
