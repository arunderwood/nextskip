/**
 * Tests for BandActivityHelp - DX distance threshold help content.
 * Focus: Rendering and accessibility, not exact content.
 */
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';

// Import the component to test - triggers help registration as side effect
import 'Frontend/components/cards/band-activity/BandActivityHelp';
import { getRegisteredHelp } from 'Frontend/components/help/HelpRegistry';

describe('BandActivityHelp', () => {
  // Get the registered section - registration happens on import
  const sections = getRegisteredHelp();
  const bandDxSection = sections.find((s) => s.id === 'band-dx-thresholds');

  describe('help registration', () => {
    it('should register band-dx-thresholds help section', () => {
      expect(bandDxSection).toBeDefined();
      expect(bandDxSection?.title).toBe('DX Distance Scoring');
    });
  });

  describe('BandDxThresholdsHelpContent', () => {
    it('should render threshold table with accessible markup', () => {
      const { Content } = bandDxSection!;
      render(<Content />);

      // Table should have accessible label
      const table = screen.getByRole('table', { name: /band dx distance thresholds/i });
      expect(table).toBeInTheDocument();
    });

    it('should render column headers for threshold categories', () => {
      const { Content } = bandDxSection!;
      render(<Content />);

      expect(screen.getByRole('columnheader', { name: /band/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /excellent/i })).toBeInTheDocument();
      expect(screen.getByRole('columnheader', { name: /good/i })).toBeInTheDocument();
    });

    it('should render PSKReporter data source link', () => {
      const { Content } = bandDxSection!;
      render(<Content />);

      const link = screen.getByRole('link', { name: /pskreporter/i });
      expect(link).toHaveAttribute('href', 'https://pskreporter.info/');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });
  });
});
