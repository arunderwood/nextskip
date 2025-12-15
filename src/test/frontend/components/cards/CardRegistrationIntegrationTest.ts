import { describe, it, expect, beforeEach } from 'vitest';
import { clearRegistry, getRegisteredCards } from 'Frontend/components/cards/CardRegistry';

describe('Card Registration Integration', () => {
  beforeEach(() => {
    // Clear registry before each test to ensure clean state
    clearRegistry();
  });

  it('should auto-register propagation cards on import', async () => {
    // When: Import propagation module
    await import('Frontend/components/cards/propagation');

    // Then: Propagation cards should be registered
    const cards = getRegisteredCards();
    const propagationCards = cards.filter((c) => {
      const config = c.createConfig({});
      return config?.id.startsWith('propagation-');
    });

    expect(propagationCards.length).toBeGreaterThanOrEqual(2);

    // Verify specific cards exist
    const cardIds = propagationCards.map(c => c.createConfig({})?.id);
    expect(cardIds).toContain('propagation-solar-indices');
    expect(cardIds).toContain('propagation-band-conditions');
  });

  it('should auto-register activations cards on import', async () => {
    // When: Import activations module
    await import('Frontend/components/cards/activations');

    // Then: Activations cards should be registered
    const cards = getRegisteredCards();
    const activationCards = cards.filter((c) => {
      const config = c.createConfig({});
      return config?.id.startsWith('activations-');
    });

    expect(activationCards.length).toBeGreaterThanOrEqual(2);

    // Verify specific cards exist
    const cardIds = activationCards.map(c => c.createConfig({})?.id);
    expect(cardIds).toContain('activations-pota');
    expect(cardIds).toContain('activations-sota');
  });

  it('should not have duplicate card IDs across modules', async () => {
    // When: Import all modules
    await import('Frontend/components/cards/propagation');
    await import('Frontend/components/cards/activations');

    // Then: No duplicate card IDs should exist
    const cards = getRegisteredCards();
    const configs = cards
      .map((c) => c.createConfig({}))
      .filter((c) => c !== null && c !== undefined);

    const ids = configs.map((c) => c!.id);
    const uniqueIds = new Set(ids);

    expect(uniqueIds.size).toBe(ids.length);
    expect(ids.length).toBeGreaterThanOrEqual(4); // At least 2 propagation + 2 activations
  });
});
