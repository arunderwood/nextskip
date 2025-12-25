/**
 * Unit tests for useScrollspy hook.
 */

import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useScrollspy } from 'Frontend/components/help/useScrollspy';
import type { HelpDefinition } from 'Frontend/components/help/types';

describe('useScrollspy', () => {
  const mockSections: HelpDefinition[] = [
    {
      id: 'solar-indices',
      title: 'Solar Indices',
      order: 10,
      Content: () => null,
    },
    {
      id: 'band-conditions',
      title: 'Band Conditions',
      order: 20,
      Content: () => null,
    },
  ];

  it('should return initial active section as "about"', () => {
    const { result } = renderHook(() => useScrollspy(mockSections));

    expect(result.current.activeSectionId).toBe('about');
  });

  it('should return a containerRef', () => {
    const { result } = renderHook(() => useScrollspy(mockSections));

    expect(result.current.containerRef).toBeDefined();
    expect(result.current.containerRef.current).toBeNull();
  });

  it('should return a scrollToSection function', () => {
    const { result } = renderHook(() => useScrollspy(mockSections));

    expect(typeof result.current.scrollToSection).toBe('function');
  });

  it('scrollToSection should be a callable function', () => {
    const { result } = renderHook(() => useScrollspy(mockSections));

    // scrollToSection should not throw when called
    // (element won't be found without DOM, but function should still work)
    expect(() => {
      act(() => {
        result.current.scrollToSection('solar-indices');
      });
    }).not.toThrow();
  });

  it('should handle empty sections array', () => {
    const { result } = renderHook(() => useScrollspy([]));

    expect(result.current.activeSectionId).toBe('about');
    expect(result.current.containerRef).toBeDefined();
  });

  it('should be stable across re-renders (scrollToSection reference)', () => {
    const { result, rerender } = renderHook(() => useScrollspy(mockSections));

    const initialScrollToSection = result.current.scrollToSection;

    rerender();

    expect(result.current.scrollToSection).toBe(initialScrollToSection);
  });

  it('should be stable across re-renders (containerRef reference)', () => {
    const { result, rerender } = renderHook(() => useScrollspy(mockSections));

    const initialContainerRef = result.current.containerRef;

    rerender();

    expect(result.current.containerRef).toBe(initialContainerRef);
  });

  it('should handle multiple scrollToSection calls without errors', () => {
    const { result } = renderHook(() => useScrollspy(mockSections));

    // Multiple calls should not throw
    expect(() => {
      act(() => {
        result.current.scrollToSection('band-conditions');
        result.current.scrollToSection('about');
        result.current.scrollToSection('solar-indices');
      });
    }).not.toThrow();
  });
});
