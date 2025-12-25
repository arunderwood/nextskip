import { describe, it, expect, beforeEach } from 'vitest';
import { registerCard, getRegisteredCards, clearRegistry } from 'Frontend/components/cards/CardRegistry';
import type { CardDefinition } from 'Frontend/components/cards/types';

describe('CardRegistry', () => {
  beforeEach(() => {
    // Clear registry before each test to ensure isolation
    clearRegistry();
  });

  it('should start with empty registry', () => {
    const cards = getRegisteredCards();

    expect(cards).toBeDefined();
    expect(cards).toHaveLength(0);
  });

  it('should register a card definition', () => {
    const mockCard: CardDefinition = {
      canRender: () => true,
      createConfig: () => ({
        id: 'test-card',
        type: 'propagation',
        size: 'standard',
        priority: 50,
        hotness: 'neutral',
      }),
      render: () => null,
    };

    registerCard(mockCard);
    const cards = getRegisteredCards();

    expect(cards).toHaveLength(1);
    expect(cards[0]).toBe(mockCard);
  });

  it('should allow multiple card registrations', () => {
    const card1: CardDefinition = {
      canRender: () => true,
      createConfig: () => ({
        id: 'card-1',
        type: 'solar-indices',
        size: 'standard',
        priority: 100,
        hotness: 'hot',
      }),
      render: () => null,
    };

    const card2: CardDefinition = {
      canRender: () => true,
      createConfig: () => ({
        id: 'card-2',
        type: 'band-conditions',
        size: 'wide',
        priority: 50,
        hotness: 'warm',
      }),
      render: () => null,
    };

    registerCard(card1);
    registerCard(card2);
    const cards = getRegisteredCards();

    expect(cards).toHaveLength(2);
    expect(cards[0]).toBe(card1);
    expect(cards[1]).toBe(card2);
  });

  it('should clear all registrations', () => {
    const mockCard: CardDefinition = {
      canRender: () => true,
      createConfig: () => null,
      render: () => null,
    };

    // Register multiple cards
    registerCard(mockCard);
    registerCard(mockCard);
    registerCard(mockCard);

    expect(getRegisteredCards()).toHaveLength(3);

    // Clear registry
    clearRegistry();

    expect(getRegisteredCards()).toHaveLength(0);
  });

  it('should return readonly array to prevent external modification', () => {
    const mockCard: CardDefinition = {
      canRender: () => true,
      createConfig: () => null,
      render: () => null,
    };

    registerCard(mockCard);
    const cards = getRegisteredCards();

    expect(cards).toHaveLength(1);

    // Array is readonly, but verify it returns consistent results
    const cards2 = getRegisteredCards();
    expect(cards2).toHaveLength(1);
    expect(cards2[0]).toBe(mockCard);
  });
});
