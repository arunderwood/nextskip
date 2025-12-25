/**
 * Help System - Barrel exports for the help modal and related components.
 */

// Main components used by DashboardView
export { HelpButton } from './HelpButton';
export { HelpModal } from './HelpModal';

// Registry for activity modules to register their help content
export { registerHelp, getRegisteredHelp, clearHelpRegistry } from './HelpRegistry';

// Types for implementing help content
export type {
  HelpDefinition,
  HelpSectionId,
  HelpButtonProps,
  HelpModalProps,
  HelpNavigationProps,
  HelpSectionProps,
  ScrollspyState,
} from './types';
