/**
 * Activity Grid Design System - Barrel Exports
 *
 * Exports all activity grid components, hooks, and utilities.
 */

// Components
export { ActivityCard } from './ActivityCard';
export { ActivityGrid } from './ActivityGrid';

// Hooks
export {
  usePriorityCalculation,
  calculatePriority,
  priorityToHotness,
  getHotnessLabel,
} from './usePriorityCalculation';

// Types (re-exported from types/activity.ts for convenience)
export type {
  ActivityCardSize,
  HotnessLevel,
  ActivityType,
  ActivityCardConfig,
  ActivityCardProps,
  ActivityGridProps,
  PriorityInput,
} from '../../types/activity';
