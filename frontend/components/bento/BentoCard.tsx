/**
 * BentoCard - Base card wrapper component for bento grid
 *
 * Provides consistent styling, hotness visual indicators, and accessibility
 * features for all activity cards in the dashboard.
 */

import React from 'react';
import type { BentoCardProps } from '../../types/bento';
import { getHotnessLabel } from './usePriorityCalculation';
import './BentoCard.css';

export function BentoCard({
  config,
  title,
  subtitle,
  icon,
  children,
  footer,
  onClick,
  className = '',
  ariaLabel,
}: BentoCardProps) {
  const { size, hotness } = config;

  const cardClasses = [
    'bento-card',
    `bento-card--${size}`,
    `bento-card--${hotness}`,
    onClick ? 'bento-card--interactive' : '',
    className,
  ]
    .filter(Boolean)
    .join(' ');

  // Use button element if clickable, article otherwise
  const CardElement = onClick ? 'button' : 'article';

  return (
    <CardElement
      className={cardClasses}
      onClick={onClick}
      aria-label={ariaLabel || title}
      tabIndex={onClick ? 0 : undefined}
    >
      <header className="bento-card__header">
        <div className="bento-card__header-left">
          {icon && (
            <span className="bento-card__icon" aria-hidden="true">
              {icon}
            </span>
          )}
          <div>
            <h3 className="bento-card__title">{title}</h3>
            {subtitle && <p className="bento-card__subtitle">{subtitle}</p>}
          </div>
        </div>

        <div
          className={`bento-card__hotness-indicator bento-card__hotness-indicator--${hotness}`}
          aria-label={`Conditions: ${getHotnessLabel(hotness)}`}
        >
          {getHotnessLabel(hotness)}
        </div>
      </header>

      <div className="bento-card__content">{children}</div>

      {footer && <footer className="bento-card__footer">{footer}</footer>}
    </CardElement>
  );
}

export default BentoCard;
