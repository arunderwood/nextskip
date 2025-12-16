/**
 * ThemeToggle - Button to toggle between light and dark themes
 */

import React from 'react';
import { useTheme } from '../hooks/useTheme';
import './ThemeToggle.css';

export function ThemeToggle() {
  const { isDark, toggleTheme } = useTheme();

  return (
    <button
      className="theme-toggle"
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
    >
      <span className="theme-toggle__icon" aria-hidden="true">
        {isDark ? '☀️' : '🌙'}
      </span>
    </button>
  );
}
