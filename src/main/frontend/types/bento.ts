/**
 * Type definitions for the NextSkip Bento Grid Design System
 *
 * The bento grid displays dashboard cards arranged by "hotness" -
 * how favorable conditions are for each activity at the moment.
 */

import type { ReactNode } from 'react';

/**
 * Card size variants for bento grid layout
 * - standard: 1x1 (single cell) - Single metrics, utilities
 * - wide: 2x1 (spans 2 columns) - Charts, tables
 * - tall: 1x2 (spans 2 rows) - Lists, activity feeds
 * - hero: 2x2 (spans 2 columns and 2 rows) - Primary KPIs, featured metrics
 */
export type BentoCardSize = 'standard' | 'wide' | 'tall' | 'hero';

/**
 * Hotness level determines visual emphasis and border glow intensity
 * Derived from priority score:
 * - hot (70-100): Green glow, "Excellent" badge - best conditions
 * - warm (45-69): Orange tint, "Good" badge - favorable conditions
 * - neutral (20-44): Blue tint, "Moderate" badge - acceptable conditions
 * - cool (0-19): Gray, "Limited" badge - poor conditions
 */
export type HotnessLevel = 'hot' | 'warm' | 'neutral' | 'cool';

/**
 * Activity types for card variants
 * Extensible for future modules (POTA/SOTA, satellites, contests, spots)
 */
export type ActivityType =
  | 'propagation'
  | 'solar-indices'
  | 'band-conditions'
  | 'pota-activations'
  | 'sota-activations'
  | 'contests'
  | 'satellite-passes'
  | 'contest-calendar'
  | 'real-time-spots';

/**
 * Configuration for a single bento card
 */
export interface BentoCardConfig {
  /** Unique identifier for the card */
  id: string;
  /** Type of activity this card displays */
  type: ActivityType;
  /** Card size variant */
  size: BentoCardSize;
  /** Priority score (0-100) - higher = more important/favorable */
  priority: number;
  /** Computed hotness level based on priority */
  hotness: HotnessLevel;
  /** Whether the card should animate on priority changes */
  animateOnChange?: boolean;
}

/**
 * Props for BentoCard base component
 */
export interface BentoCardProps {
  /** Card configuration */
  config: BentoCardConfig;
  /** Title displayed in card header */
  title: string;
  /** Optional subtitle/source label */
  subtitle?: string;
  /** Optional icon (emoji or component) */
  icon?: ReactNode;
  /** Card content */
  children: ReactNode;
  /** Optional footer content */
  footer?: ReactNode;
  /** Click handler for card interactions */
  onClick?: () => void;
  /** Additional CSS class names */
  className?: string;
  /** Accessibility label */
  ariaLabel?: string;
}

/**
 * Props for BentoGrid container component
 */
export interface BentoGridProps {
  /** Array of card configurations with components */
  cards: Array<{
    config: BentoCardConfig;
    component: ReactNode;
  }>;
  /** Number of columns at desktop breakpoint (default: 4) */
  columns?: number;
  /** Gap between cards in spacing units (default: 3) */
  gap?: number;
  /** Whether to animate card reordering (default: true) */
  animateReorder?: boolean;
  /** Animation duration in ms (default: 300) */
  animationDuration?: number;
  /** Additional CSS class names */
  className?: string;
}

/**
 * Priority calculation input - generic interface for any data source
 * Maps existing backend data to priority score
 */
export interface PriorityInput {
  /** Whether conditions are favorable (from backend) - 40% weight */
  favorable: boolean;
  /** Numeric score (0-100 scale, from backend) - 35% weight */
  score?: number;
  /** Rating enum value (GOOD/FAIR/POOR/UNKNOWN) - 20% weight */
  rating?: 'GOOD' | 'FAIR' | 'POOR' | 'UNKNOWN';
  /** Optional: recency factor (more recent = higher priority) - 5% weight */
  lastUpdated?: Date;
  /** Optional: user preference weight multiplier (0-1) */
  userWeight?: number;
}
