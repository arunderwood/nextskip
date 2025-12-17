/**
 * ActivityCard - Base card wrapper component for activity grid
 *
 * Provides consistent styling, hotness visual indicators, and accessibility
 * features for all activity cards in the dashboard.
 */

import React from 'react';
import type { ActivityCardProps } from '../../types/activity';
import { getHotnessLabel } from './usePriorityCalculation';
import './ActivityCard.css';

export function ActivityCard({
  config,
  title,
  subtitle,
  icon,
  children,
  footer,
  onClick,
  className = '',
  ariaLabel,
}: ActivityCardProps) {
  const { size, hotness } = config;

  const cardClasses = [
    'activity-card',
    `activity-card--${size}`,
    `activity-card--${hotness}`,
    onClick ? 'activity-card--interactive' : '',
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
      <div className="activity-card__header">
        <div className="activity-card__header-left">
          {icon && (
            <span className="activity-card__icon" aria-hidden="true">
              {icon}
            </span>
          )}
          <div className="activity-card__title-wrapper">
            <h3 className="activity-card__title">{title}</h3>
            {subtitle && <p className="activity-card__subtitle">{subtitle}</p>}
          </div>
        </div>

        <div
          className={`activity-card__hotness-indicator activity-card__hotness-indicator--${hotness}`}
          aria-label={`Conditions: ${getHotnessLabel(hotness)}`}
        >
          {getHotnessLabel(hotness)}
        </div>
      </div>

      <div className="activity-card__content">{children}</div>

      {footer && <div className="activity-card__footer">{footer}</div>}
    </CardElement>
  );
}

export default ActivityCard;
