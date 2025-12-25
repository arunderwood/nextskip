/**
 * HelpSection - Wrapper component for individual help sections.
 *
 * Provides consistent styling and data attributes for scrollspy tracking.
 */

import React from 'react';
import type { HelpSectionProps } from './types';
import './HelpSection.css';

export function HelpSection({ definition, children }: HelpSectionProps) {
  const headingId = `help-section-${definition.id}-title`;

  return (
    <section
      id={`help-section-${definition.id}`}
      data-section-id={definition.id}
      className="help-section"
      aria-labelledby={headingId}
    >
      <h3 id={headingId} className="help-section__title">
        {definition.icon ? (
          <span className="help-section__icon" aria-hidden="true">
            {definition.icon}
          </span>
        ) : null}
        {definition.title}
      </h3>

      <div className="help-section__content">{children}</div>
    </section>
  );
}
