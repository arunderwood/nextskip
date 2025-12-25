/**
 * Help Registry - Enables dynamic help content registration following the Open-Closed Principle.
 *
 * New activity modules can register their help content without modifying core help system code.
 * Each module's help file calls registerHelp() to add its content to the registry.
 */

import type { HelpDefinition } from './types';

/**
 * Global registry of all help sections.
 *
 * Populated at module load time by each activity module's help file.
 */
const helpRegistry: HelpDefinition[] = [];

/**
 * Register a help definition with the global registry.
 *
 * Called by each activity module during initialization to make
 * its help content available to the help modal.
 *
 * @param definition - The help definition to register
 *
 * @example
 * ```typescript
 * // In propagation/PropagationHelp.tsx
 * registerHelp(solarIndicesHelp);
 * registerHelp(bandConditionsHelp);
 * ```
 */
export function registerHelp(definition: HelpDefinition): void {
  // Prevent duplicate registrations
  if (!helpRegistry.some((h) => h.id === definition.id)) {
    helpRegistry.push(definition);
  }
}

/**
 * Get all registered help definitions, sorted by order.
 *
 * Called by HelpModal to render all available help sections.
 *
 * @returns Array of all registered help definitions, sorted by order
 */
export function getRegisteredHelp(): readonly HelpDefinition[] {
  return [...helpRegistry].sort((a, b) => a.order - b.order);
}

/**
 * Clear all registered help sections.
 *
 * Primarily used for testing to ensure a clean slate between test cases.
 */
export function clearHelpRegistry(): void {
  helpRegistry.length = 0;
}
