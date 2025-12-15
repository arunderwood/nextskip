/**
 * Bento Grid Design System - Barrel Exports
 *
 * Exports all bento grid components, hooks, and utilities.
 */

// Components
export { BentoCard } from './BentoCard';
export { BentoGrid } from './BentoGrid';

// Hooks
export {
  usePriorityCalculation,
  calculatePriority,
  priorityToHotness,
  getHotnessLabel,
} from './usePriorityCalculation';

// Types (re-exported from types/bento.ts for convenience)
export type {
  BentoCardSize,
  HotnessLevel,
  ActivityType,
  BentoCardConfig,
  BentoCardProps,
  BentoGridProps,
  PriorityInput,
} from '../../types/bento';
