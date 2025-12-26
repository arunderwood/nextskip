/**
 * HelpNavigation - Scrollspy navigation for help sections.
 *
 * Features:
 * - Active section highlighting based on scroll position
 * - Click to jump to section
 * - Horizontal scroll on mobile (pill-style tabs)
 */

import React from 'react';
import type { HelpNavigationProps, HelpSectionId } from './types';
import './HelpNavigation.css';

export function HelpNavigation({ sections, activeSectionId, onNavigate }: HelpNavigationProps) {
  // Include "About" as first item (hardcoded, always present)
  const allSections: Array<{ id: HelpSectionId; title: string; icon?: React.ReactNode }> = [
    { id: 'about', title: 'About', icon: null },
    ...sections.map((s) => ({ id: s.id, title: s.title, icon: s.icon })),
  ];

  return (
    <nav className="help-navigation" aria-label="Help sections">
      <div className="help-navigation__list" role="tablist">
        {allSections.map((section) => (
          <div key={section.id} role="presentation">
            <button
              type="button"
              role="tab"
              className={`help-navigation__item ${
                activeSectionId === section.id ? 'help-navigation__item--active' : ''
              }`}
              onClick={() => onNavigate(section.id)}
              aria-selected={activeSectionId === section.id}
              aria-controls={`help-section-${section.id}`}
            >
              {section.icon ? (
                <span className="help-navigation__icon" aria-hidden="true">
                  {section.icon}
                </span>
              ) : null}
              <span className="help-navigation__label">{section.title}</span>
            </button>
          </div>
        ))}
      </div>
    </nav>
  );
}
