/**
 * HelpButton - Opens the help modal.
 *
 * Styled to match ThemeToggle for visual consistency in the header.
 */

import React from 'react';
import { HelpCircle } from 'lucide-react';
import type { HelpButtonProps } from './types';
import './HelpButton.css';

export function HelpButton({ onClick }: HelpButtonProps) {
  return (
    <button
      type="button"
      className="help-button"
      onClick={onClick}
      aria-label="Open help and about"
      title="Help & About"
    >
      <HelpCircle size={20} aria-hidden="true" />
    </button>
  );
}
