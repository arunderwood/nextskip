/**
 * Type definitions for the card registry system.
 *
 * This module defines the contract for dynamically registering and rendering
 * activity cards in the dashboard, enabling the Open-Closed Principle.
 */

import type { ReactNode } from 'react';
import type { ActivityCardConfig } from 'Frontend/types/activity';

/**
 * Combined dashboard data from all activity modules.
 *
 * As new modules are added (POTA, SOTA, Contests, Satellites, etc.),
 * extend this type with their response types.
 */
export interface DashboardData {
  propagation?: import('Frontend/generated/io/nextskip/propagation/api/PropagationResponse').default;
  activations?: import('Frontend/generated/io/nextskip/activations/api/ActivationsResponse').default;
  contests?: import('Frontend/generated/io/nextskip/contests/api/ContestsResponse').default;
  meteorShowers?: import('Frontend/generated/io/nextskip/meteors/api/MeteorShowersResponse').default;
  // Future modules will add their types here as they're implemented:
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
   * Create ActivityCardConfig(s) from the dashboard data.
   *
   * This calculates the card's priority and hotness based on the data.
   * Can return a single config, an array of configs (for individual items),
   * or null if cannot create.
   *
   * @param data - The combined dashboard data from all modules
   * @returns Card configuration(s) with priority/hotness, or null if cannot create
   */
  createConfig: (data: DashboardData) => ActivityCardConfig | ActivityCardConfig[] | null;

  /**
   * Render the card's content within an ActivityCard wrapper.
   *
   * @param data - The combined dashboard data from all modules
   * @param config - The card configuration created by createConfig
   * @returns React element for the complete card (including ActivityCard wrapper)
   */
  render: (data: DashboardData, config: ActivityCardConfig) => ReactNode;
}
