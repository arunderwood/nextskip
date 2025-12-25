/**
 * Type definitions for the help registry system.
 *
 * This module defines the contract for dynamically registering help content
 * for activity modules, following the Open-Closed Principle.
 */

import type { ReactNode, ComponentType, RefObject } from 'react';

/**
 * Unique identifier for help sections.
 * 'about' is always first, followed by activity-specific sections.
 */
export type HelpSectionId =
  | 'about'
  | 'solar-indices'
  | 'band-conditions'
  | 'pota-activations'
  | 'sota-activations'
  | 'contests'
  | 'meteor-showers';

/**
 * Defines a help section that can be registered with the help system.
 *
 * Each activity module implements this interface to register its help content
 * without modifying core help system code.
 */
export interface HelpDefinition {
  /**
   * Unique identifier for this help section.
   * Used for scroll targeting and navigation.
   */
  id: HelpSectionId;

  /**
   * Display title shown in navigation and section header.
   */
  title: string;

  /**
   * Optional icon for navigation (emoji or React component).
   */
  icon?: ReactNode;

  /**
   * Order in which this section appears (lower = earlier).
   * The "About" section is always first (order: 0).
   */
  order: number;

  /**
   * React component that renders the help content.
   * Receives no props - content is self-contained.
   */
  Content: ComponentType;
}

/**
 * Props for the HelpModal component.
 */
export interface HelpModalProps {
  /**
   * Whether the modal is open.
   */
  isOpen: boolean;

  /**
   * Callback when modal requests close (Escape key, backdrop click, close button).
   */
  onClose: () => void;
}

/**
 * Props for the HelpButton component.
 */
export interface HelpButtonProps {
  /**
   * Callback when button is clicked.
   */
  onClick: () => void;
}

/**
 * Props for HelpNavigation component.
 */
export interface HelpNavigationProps {
  /**
   * All registered help sections (including About).
   */
  sections: readonly HelpDefinition[];

  /**
   * Currently active section ID (from scrollspy).
   */
  activeSectionId: HelpSectionId;

  /**
   * Callback when a navigation item is clicked.
   */
  onNavigate: (sectionId: HelpSectionId) => void;
}

/**
 * Props for individual HelpSection wrapper.
 */
export interface HelpSectionProps {
  /**
   * Section definition.
   */
  definition: HelpDefinition;

  /**
   * Children (the actual help content).
   */
  children: ReactNode;
}

/**
 * Return type for useScrollspy hook.
 */
export interface ScrollspyState {
  /**
   * Currently visible section ID.
   */
  activeSectionId: HelpSectionId;

  /**
   * Ref to attach to the scrollable container.
   */
  containerRef: RefObject<HTMLDivElement | null>;

  /**
   * Function to scroll to a specific section.
   */
  scrollToSection: (sectionId: HelpSectionId) => void;
}
