/**
 * Unit tests for HelpRegistry.
 */

import { describe, it, expect, beforeEach } from 'vitest';
import { registerHelp, getRegisteredHelp, clearHelpRegistry } from 'Frontend/components/help/HelpRegistry';
import type { HelpDefinition } from 'Frontend/components/help/types';

describe('HelpRegistry', () => {
  beforeEach(() => {
    clearHelpRegistry();
  });

  const createMockHelp = (id: string, order: number): HelpDefinition => ({
    id: id as HelpDefinition['id'],
    title: `Test ${id}`,
    order,
    Content: () => null,
  });

  describe('registerHelp', () => {
    it('should add a help definition to the registry', () => {
      const help = createMockHelp('solar-indices', 10);
      registerHelp(help);

      const registered = getRegisteredHelp();
      expect(registered).toHaveLength(1);
      expect(registered[0].id).toBe('solar-indices');
    });

    it('should allow multiple registrations', () => {
      registerHelp(createMockHelp('solar-indices', 10));
      registerHelp(createMockHelp('band-conditions', 20));

      const registered = getRegisteredHelp();
      expect(registered).toHaveLength(2);
    });

    it('should prevent duplicate registrations with same ID', () => {
      const help1 = createMockHelp('solar-indices', 10);
      const help2 = createMockHelp('solar-indices', 20);

      registerHelp(help1);
      registerHelp(help2);

      const registered = getRegisteredHelp();
      expect(registered).toHaveLength(1);
      expect(registered[0].order).toBe(10); // First registration wins
    });
  });

  describe('getRegisteredHelp', () => {
    it('should return an empty array when no help is registered', () => {
      expect(getRegisteredHelp()).toHaveLength(0);
    });

    it('should return help definitions sorted by order', () => {
      registerHelp(createMockHelp('band-conditions', 20));
      registerHelp(createMockHelp('solar-indices', 10));
      registerHelp(createMockHelp('contests', 40));

      const registered = getRegisteredHelp();
      expect(registered[0].id).toBe('solar-indices');
      expect(registered[1].id).toBe('band-conditions');
      expect(registered[2].id).toBe('contests');
    });

    it('should return a copy of the registry', () => {
      registerHelp(createMockHelp('solar-indices', 10));

      const registered1 = getRegisteredHelp();
      const registered2 = getRegisteredHelp();

      expect(registered1).not.toBe(registered2);
      expect(registered1).toEqual(registered2);
    });
  });

  describe('clearHelpRegistry', () => {
    it('should remove all registered help definitions', () => {
      registerHelp(createMockHelp('solar-indices', 10));
      registerHelp(createMockHelp('band-conditions', 20));

      expect(getRegisteredHelp()).toHaveLength(2);

      clearHelpRegistry();

      expect(getRegisteredHelp()).toHaveLength(0);
    });
  });
});
