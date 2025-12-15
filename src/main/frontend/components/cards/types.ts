/**
 * Type definitions for the card registry system.
 *
 * This module defines the contract for dynamically registering and rendering
 * activity cards in the dashboard, enabling the Open-Closed Principle.
 */

import type { ReactNode } from 'react';
import type { BentoCardConfig } from 'Frontend/types/bento';

/**
 * Combined dashboard data from all activity modules.
 *
 * As new modules are added (POTA, SOTA, Contests, Satellites, etc.),
 * extend this type with their response types.
 */
export interface DashboardData {
  propagation?: import('Frontend/generated/io/nextskip/propagation/api/PropagationResponse').default;
  activations?: import('Frontend/generated/io/nextskip/activations/api/ActivationsResponse').default;
  // Future modules will add their types here as they're implemented:
  // contests?: ContestsResponse;
  // satellites?: SatellitesResponse;
  // pskreporter?: PskReporterResponse;
}

/**
 * Defines a card type that can be registered with the dashboard.
 *
 * Each activity module implements this interface to register its cards
 * without modifying core dashboard code.
 *
 * @template TData - The specific data type for this card (e.g., PropagationResponse)
 */
export interface CardDefinition<TData = unknown> {
  /**
   * Check if this card can be rendered with the given data.
   *
   * @param data - The combined dashboard data from all modules
   * @returns true if the card has sufficient data to render
   */
  canRender: (data: DashboardData) => boolean;

  /**
   * Create a BentoCardConfig from the dashboard data.
   *
   * This calculates the card's priority and hotness based on the data.
   *
   * @param data - The combined dashboard data from all modules
   * @returns Card configuration with priority/hotness, or null if cannot create
   */
  createConfig: (data: DashboardData) => BentoCardConfig | null;

  /**
   * Render the card's content within a BentoCard wrapper.
   *
   * @param data - The combined dashboard data from all modules
   * @param config - The card configuration created by createConfig
   * @returns React element for the complete card (including BentoCard wrapper)
   */
  render: (data: DashboardData, config: BentoCardConfig) => ReactNode;
}
