import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SolarIndicesContent from 'Frontend/components/cards/propagation/SolarIndicesContent';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
import { createMockSolarIndices } from '../../../fixtures/mockFactories';

describe('SolarIndicesContent', () => {
  // Create a wrapper with test-specific defaults (SFI=150 to match existing tests)
  const createIndices = (overrides?: Partial<SolarIndices>): SolarIndices =>
    createMockSolarIndices({ solarFluxIndex: 150, sunspotNumber: 75, ...overrides });

  describe('rendering', () => {
    it('should render all index labels', () => {
      const indices = createIndices();

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Solar Flux Index (SFI)')).toBeInTheDocument();
      expect(screen.getByText('K-Index')).toBeInTheDocument();
      expect(screen.getByText('A-Index')).toBeInTheDocument();
      expect(screen.getByText('Sunspot Number')).toBeInTheDocument();
    });

    it('should render all index values', () => {
      const indices = createIndices({ solarFluxIndex: 150.5 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('150.5')).toBeInTheDocument();
      expect(screen.getByText('2')).toBeInTheDocument(); // K-index
      expect(screen.getByText('10')).toBeInTheDocument(); // A-index (from shared factory default)
      expect(screen.getByText('75')).toBeInTheDocument(); // Sunspot
    });

    it('should display N/A for undefined values', () => {
      // Cast to SolarIndices to test undefined handling in component
      const indices = {
        favorable: true,
        score: 80,
        source: 'Test Source',
      } as unknown as SolarIndices;

      render(<SolarIndicesContent solarIndices={indices} />);

      const naElements = screen.getAllByText('N/A');
      expect(naElements).toHaveLength(4);
    });
  });

  describe('Solar Flux Index (SFI)', () => {
    it('should format SFI to 1 decimal place', () => {
      const indices = createIndices({ solarFluxIndex: 142.6789 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('142.7')).toBeInTheDocument();
    });

    it.each([
      [200, 'Very High'],
      [150, 'High'],
      [100, 'Moderate'],
      [70, 'Low'],
      [50, 'Very Low'],
    ])('should show %s status for SFI=%d', (sfi, expectedStatus) => {
      const indices = createIndices({ solarFluxIndex: sfi });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText(expectedStatus)).toBeInTheDocument();
    });

    it('should apply status-good class for high SFI', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const statusElements = container.querySelectorAll('.status-good');
      expect(statusElements.length).toBeGreaterThan(0);
    });

    it('should apply status-poor class for low SFI', () => {
      const indices = createIndices({ solarFluxIndex: 50 });
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const statusElements = container.querySelectorAll('.status-poor');
      expect(statusElements.length).toBeGreaterThan(0);
    });
  });

  describe('K-Index', () => {
    it.each([
      [0, 'Quiet'],
      [2, 'Settled'],
      [4, 'Unsettled'],
      [6, 'Active'],
      [8, 'Storm'],
      [9, 'Severe Storm'],
    ])('should show %s status for K=%d', (kIndex, expectedStatus) => {
      const indices = createIndices({ kIndex });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText(expectedStatus)).toBeInTheDocument();
    });

    it('should apply status-good class for low K-index', () => {
      const indices = createIndices({ kIndex: 1 });
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const statusElements = container.querySelectorAll('.status-good');
      expect(statusElements.length).toBeGreaterThan(0);
    });

    it('should apply status-poor class for high K-index', () => {
      const indices = createIndices({ kIndex: 9 });
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const statusElements = container.querySelectorAll('.status-poor');
      expect(statusElements.length).toBeGreaterThan(0);
    });
  });

  describe('A-Index', () => {
    it.each([
      [0, 'Quiet conditions'],
      [15, 'Quiet conditions'],
      [20, 'Unsettled conditions'],
      [30, 'Unsettled conditions'],
      [50, 'Disturbed conditions'],
      [60, 'Disturbed conditions'],
    ])('should show "%s" for A-index=%d', (aIndex, expectedStatus) => {
      const indices = createIndices({ aIndex });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText(expectedStatus)).toBeInTheDocument();
    });

    it('should show "Disturbed conditions" when A-index is undefined', () => {
      // Cast to test undefined handling in component
      const indices = { ...createIndices(), aIndex: undefined } as unknown as SolarIndices;

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Disturbed conditions')).toBeInTheDocument();
    });
  });

  describe('Sunspot Number', () => {
    it.each([
      [0, 'Low solar activity'],
      [30, 'Low solar activity'],
      [50, 'Low solar activity'],
      [51, 'Moderate activity'],
      [75, 'Moderate activity'],
      [100, 'Moderate activity'],
      [101, 'High solar activity'],
      [150, 'High solar activity'],
    ])('should show "%s" for sunspot=%d', (sunspotNumber, expectedStatus) => {
      const indices = createIndices({ sunspotNumber });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText(expectedStatus)).toBeInTheDocument();
    });

    it('should show "Low solar activity" when sunspots is undefined', () => {
      // Cast to test undefined handling in component
      const indices = { ...createIndices(), sunspotNumber: undefined } as unknown as SolarIndices;

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Low solar activity')).toBeInTheDocument();
    });
  });

  describe('CSS structure', () => {
    it('should have indices-grid container', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      expect(container.querySelector('.indices-grid')).toBeInTheDocument();
    });

    it('should have 4 index-item elements', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const items = container.querySelectorAll('.index-item');
      expect(items).toHaveLength(4);
    });

    it('should have index-label for each item', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const labels = container.querySelectorAll('.index-label');
      expect(labels).toHaveLength(4);
    });

    it('should have index-value for each item', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const values = container.querySelectorAll('.index-value');
      expect(values).toHaveLength(4);
    });

    it('should have index-status for SFI and K-index', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const statuses = container.querySelectorAll('.index-status');
      expect(statuses).toHaveLength(2);
    });

    it('should have index-description for A-index and Sunspot', () => {
      const indices = createIndices();
      const { container } = render(<SolarIndicesContent solarIndices={indices} />);

      const descriptions = container.querySelectorAll('.index-description');
      expect(descriptions).toHaveLength(2);
    });
  });

  describe('edge cases', () => {
    it('should handle all values as zero', () => {
      const indices = createIndices({ solarFluxIndex: 0, kIndex: 0, aIndex: 0, sunspotNumber: 0 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('0.0')).toBeInTheDocument(); // SFI
      expect(screen.getAllByText('0')).toHaveLength(3); // K, A, Sunspot
    });

    it('should handle very large values', () => {
      const indices = createIndices({ solarFluxIndex: 999.9, kIndex: 9, aIndex: 999, sunspotNumber: 999 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('999.9')).toBeInTheDocument();
      expect(screen.getByText('9')).toBeInTheDocument();
      // "999" appears twice (A-index and Sunspot), so use getAllByText
      const nineNineNine = screen.getAllByText('999');
      expect(nineNineNine).toHaveLength(2);
    });

    it('should handle negative values gracefully', () => {
      // Although negative values shouldn't occur in practice
      const indices = createIndices({ solarFluxIndex: -10, kIndex: -1, aIndex: -5, sunspotNumber: -10 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('-10.0')).toBeInTheDocument();
    });
  });
});
