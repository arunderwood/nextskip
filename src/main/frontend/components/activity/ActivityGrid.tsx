/**
 * ActivityGrid - Grid container component for activity card layout
 *
 * Displays cards in a responsive masonry layout, automatically sorting
 * by priority (highest priority cards appear first/top-left).
 */

import React, { useMemo, useEffect, useState } from 'react';
import { RegularMasonryGrid, Frame } from '@masonry-grid/react';
import type { ActivityGridProps, ActivityCardSize } from '../../types/activity';
import './ActivityGrid.css';

// Map card size to Frame aspect ratio
const SIZE_TO_ASPECT: Record<ActivityCardSize, { width: number; height: number }> = {
  standard: { width: 1, height: 1 },
  wide: { width: 2, height: 1 },
  tall: { width: 1, height: 2 },
  hero: { width: 2, height: 2 },
};

/**
 * Hook to calculate responsive frame width based on viewport and column count
 */
function useResponsiveFrameWidth(baseColumns: number): number {
  const [frameWidth, setFrameWidth] = useState(200);

  useEffect(() => {
    const calculateWidth = () => {
      // Get container width or fallback to window width
      const container = document.querySelector('.activity-grid');
      const containerWidth = container?.clientWidth || window.innerWidth;
      const gap = 24; // Default gap in pixels

      // Determine columns based on breakpoints
      let cols = baseColumns;
      if (window.innerWidth <= 768) {
        cols = 1; // mobile
      } else if (window.innerWidth <= 1024) {
        cols = 2; // tablet
      } else if (window.innerWidth >= 1400) {
        cols = 6; // wide desktop
      }

      // Calculate frame width: (containerWidth - totalGaps) / columns
      const totalGaps = (cols - 1) * gap;
      const width = Math.floor((containerWidth - totalGaps) / cols);
      setFrameWidth(Math.max(150, width)); // minimum 150px
    };

    calculateWidth();
    window.addEventListener('resize', calculateWidth);
    return () => window.removeEventListener('resize', calculateWidth);
  }, [baseColumns]);

  return frameWidth;
}

export function ActivityGrid({
  cards,
  columns = 4,
  gap = 3,
  animateReorder = true,
  animationDuration = 300,
  className = '',
}: ActivityGridProps) {
  // Sort cards by priority (descending) - highest priority first
  const sortedCards = useMemo(() => {
    return [...cards].sort((a, b) => b.config.priority - a.config.priority);
  }, [cards]);

  // Calculate responsive frame width
  const frameWidth = useResponsiveFrameWidth(columns);

  // Convert gap from spacing units to pixels
  const gapPx = 8 * gap; // spacing-unit = 8px

  return (
    <div
      className={`activity-grid ${className}`}
      role="list"
      aria-label="Dashboard activity cards"
      style={{
        '--activity-transition-duration': `${animationDuration}ms`,
      } as React.CSSProperties}
    >
      <RegularMasonryGrid frameWidth={frameWidth} gap={gapPx}>
        {sortedCards.map(({ config, component }) => {
          const aspect = SIZE_TO_ASPECT[config.size];
          return (
            <Frame key={config.id} width={aspect.width} height={aspect.height}>
              <div
                className={`activity-grid__card-wrapper activity-grid__card--${config.size}`}
                role="listitem"
              >
                {component}
              </div>
            </Frame>
          );
        })}
      </RegularMasonryGrid>
    </div>
  );
}

export default ActivityGrid;
