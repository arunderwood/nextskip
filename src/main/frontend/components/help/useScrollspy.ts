/**
 * useScrollspy - Tracks scroll position to highlight active navigation.
 *
 * Uses IntersectionObserver for efficient scroll tracking.
 * Returns the currently most-visible section.
 */

import { useState, useRef, useCallback, useEffect } from 'react';
import type { HelpDefinition, HelpSectionId, ScrollspyState } from './types';

export function useScrollspy(sections: readonly HelpDefinition[]): ScrollspyState {
  const [activeSectionId, setActiveSectionId] = useState<HelpSectionId>('about');
  const containerRef = useRef<HTMLDivElement | null>(null);
  const sectionRefs = useRef<Map<HelpSectionId, HTMLElement>>(new Map());

  // Track section visibility with IntersectionObserver
  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    // Store visibility ratios for all sections
    const visibilityMap = new Map<HelpSectionId, number>();

    const observer = new IntersectionObserver(
      (entries) => {
        // Update visibility map with new intersection ratios
        entries.forEach((entry) => {
          const sectionId = entry.target.getAttribute('data-section-id') as HelpSectionId;
          if (sectionId) {
            visibilityMap.set(sectionId, entry.intersectionRatio);
          }
        });

        // Find the most visible section
        let bestId: HelpSectionId | null = null;
        let bestRatio = 0;

        visibilityMap.forEach((ratio, id) => {
          if (ratio > 0 && ratio > bestRatio) {
            bestId = id;
            bestRatio = ratio;
          }
        });

        if (bestId !== null) {
          setActiveSectionId(bestId);
        }
      },
      {
        root: container,
        rootMargin: '-10% 0px -70% 0px', // Bias toward top of viewport
        threshold: [0, 0.1, 0.25, 0.5, 0.75, 1],
      },
    );

    // Observe all sections (including About)
    const allSectionIds: HelpSectionId[] = ['about', ...sections.map((s) => s.id)];

    // Wait for DOM to be ready
    requestAnimationFrame(() => {
      allSectionIds.forEach((id) => {
        const element = container.querySelector(`[data-section-id="${id}"]`);
        if (element) {
          sectionRefs.current.set(id, element as HTMLElement);
          observer.observe(element);
        }
      });
    });

    return () => {
      observer.disconnect();
      visibilityMap.clear();
    };
  }, [sections]);

  const scrollToSection = useCallback((sectionId: HelpSectionId) => {
    const element = sectionRefs.current.get(sectionId);
    if (element) {
      element.scrollIntoView({ behavior: 'smooth', block: 'start' });
      // Immediately update active section for responsive feel
      setActiveSectionId(sectionId);
    }
  }, []);

  return {
    activeSectionId,
    containerRef,
    scrollToSection,
  };
}
