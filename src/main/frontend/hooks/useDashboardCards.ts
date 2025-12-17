/**
 * useDashboardCards - Hook to create card configurations from dashboard data
 *
 * Uses the card registry to dynamically generate configurations for all
 * registered activity cards, following the Open-Closed Principle.
 */

import { useMemo } from 'react';
import type { ActivityCardConfig } from '../types/activity';
import type { DashboardData } from '../components/cards/types';
import { getRegisteredCards } from '../components/cards/CardRegistry';

/**
 * Create card configurations from dashboard data using the card registry.
 *
 * This hook delegates to registered card definitions, allowing new modules
 * to add cards without modifying this file.
 *
 * @param data - Combined data from all activity modules
 * @returns Array of card configurations sorted by priority
 */
export function useDashboardCards(data: DashboardData): ActivityCardConfig[] {
  return useMemo(() => {
    const cards = getRegisteredCards();
    const configs: ActivityCardConfig[] = [];

    for (const card of cards) {
      // Check if this card can render with the available data
      if (!card.canRender(data)) {
        continue;
      }

      // Create the card configuration(s)
      const configResult = card.createConfig(data);

      if (configResult) {
        // Handle both single config and array of configs
        if (Array.isArray(configResult)) {
          configs.push(...configResult);
        } else {
          configs.push(configResult);
        }
      }
    }

    // Configs are already sorted by priority in ActivityGrid, but we return them here
    return configs;
  }, [data]);
}
