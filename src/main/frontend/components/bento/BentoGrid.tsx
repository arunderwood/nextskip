/**
 * BentoGrid - Grid container component for bento layout
 *
 * Displays cards in a responsive CSS Grid layout, automatically sorting
 * by priority (highest priority cards appear first/top-left).
 */

import React, { useMemo } from 'react';
import type { BentoGridProps } from '../../types/bento';
import './BentoGrid.css';

export function BentoGrid({
  cards,
  columns = 4,
  gap = 3,
  animateReorder = true,
  animationDuration = 300,
  className = '',
}: BentoGridProps) {
  // Sort cards by priority (descending) - highest priority first
  const sortedCards = useMemo(() => {
    return [...cards].sort((a, b) => b.config.priority - a.config.priority);
  }, [cards]);

  // CSS custom properties for grid configuration
  const gridStyle = {
    '--bento-columns-desktop': columns,
    '--bento-gap': `calc(var(--spacing-unit) * ${gap})`,
    '--bento-transition-duration': `${animationDuration}ms`,
  } as React.CSSProperties;

  return (
    <div
      className={`bento-grid ${className}`}
      style={gridStyle}
      role="list"
      aria-label="Dashboard activity cards"
    >
      {sortedCards.map(({ config, component }) => (
        <div
          key={config.id}
          className={`bento-grid__card-wrapper bento-grid__card--${config.size}`}
          role="listitem"
        >
          {component}
        </div>
      ))}
    </div>
  );
}

export default BentoGrid;
