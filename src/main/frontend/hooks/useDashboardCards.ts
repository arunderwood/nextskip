/**
 * useDashboardCards - Hook to create card configurations from dashboard data
 *
 * Uses the card registry to dynamically generate configurations for all
 * registered activity cards, following the Open-Closed Principle.
 */

import { useMemo } from 'react';
import type { BentoCardConfig } from '../types/bento';
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
export function useDashboardCards(data: DashboardData): BentoCardConfig[] {
  return useMemo(() => {
    const cards = getRegisteredCards();
    const configs: BentoCardConfig[] = [];

    for (const card of cards) {
      // Check if this card can render with the available data
      if (!card.canRender(data)) {
        continue;
      }

      // Create the card configuration
      const config = card.createConfig(data);
      if (config) {
        configs.push(config);
      }
    }

    // Configs are already sorted by priority in BentoGrid, but we return them here
    return configs;
  }, [data]);
}
