/**
 * Card Registry - Enables dynamic card registration following the Open-Closed Principle.
 *
 * New activity modules can register their cards without modifying core dashboard code.
 * Each module's index.ts calls registerCard() to add its cards to the registry.
 */

import type { CardDefinition } from './types';

/**
 * Global registry of all activity cards.
 *
 * Populated at module load time by each activity module's index.ts.
 */
const cardRegistry: CardDefinition[] = [];

/**
 * Register a card definition with the global registry.
 *
 * Called by each activity module during initialization to make
 * its cards available to the dashboard.
 *
 * @param definition - The card definition to register
 *
 * @example
 * ```typescript
 * // In propagation/index.ts
 * registerCard(solarIndicesCardDefinition);
 * registerCard(bandConditionsCardDefinition);
 * ```
 */
export function registerCard(definition: CardDefinition): void {
  cardRegistry.push(definition);
}

/**
 * Get all registered card definitions.
 *
 * Called by useDashboardCards to generate configs for all available cards.
 *
 * @returns Array of all registered card definitions
 */
export function getRegisteredCards(): readonly CardDefinition[] {
  return cardRegistry;
}

/**
 * Clear all registered cards.
 *
 * Primarily used for testing to ensure a clean slate between test cases.
 */
export function clearRegistry(): void {
  cardRegistry.length = 0;
}
