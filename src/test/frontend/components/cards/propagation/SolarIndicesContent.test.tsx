import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SolarIndicesContent from 'Frontend/components/cards/propagation/SolarIndicesContent';
import type SolarIndices from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';

describe('SolarIndicesContent', () => {
  const createIndices = (overrides?: Partial<SolarIndices>): SolarIndices => ({
    solarFluxIndex: 150,
    kIndex: 2,
    aIndex: 15,
    sunspotNumber: 75,
    favorable: true,
    score: 80,
    source: 'Test Source',
    ...overrides,
  });

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
      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('15')).toBeInTheDocument();
      expect(screen.getByText('75')).toBeInTheDocument();
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

    it('should show Very High status for SFI >= 200', () => {
      const indices = createIndices({ solarFluxIndex: 200 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Very High')).toBeInTheDocument();
    });

    it('should show High status for SFI >= 150', () => {
      const indices = createIndices();

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('High')).toBeInTheDocument();
    });

    it('should show Moderate status for SFI >= 100', () => {
      const indices = createIndices({ solarFluxIndex: 100 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Moderate')).toBeInTheDocument();
    });

    it('should show Low status for SFI >= 70', () => {
      const indices = createIndices({ solarFluxIndex: 70 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Low')).toBeInTheDocument();
    });

    it('should show Very Low status for SFI < 70', () => {
      const indices = createIndices({ solarFluxIndex: 50 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Very Low')).toBeInTheDocument();
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
    it('should show Quiet status for K=0', () => {
      const indices = createIndices({ kIndex: 0 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Quiet')).toBeInTheDocument();
    });

    it('should show Settled status for K=1-2', () => {
      const indices = createIndices();

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Settled')).toBeInTheDocument();
    });

    it('should show Unsettled status for K=3-4', () => {
      const indices = createIndices({ kIndex: 4 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Unsettled')).toBeInTheDocument();
    });

    it('should show Active status for K=5-6', () => {
      const indices = createIndices({ kIndex: 6 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Active')).toBeInTheDocument();
    });

    it('should show Storm status for K=7-8', () => {
      const indices = createIndices({ kIndex: 8 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Storm')).toBeInTheDocument();
    });

    it('should show Severe Storm status for K>=9', () => {
      const indices = createIndices({ kIndex: 9 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Severe Storm')).toBeInTheDocument();
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
    it('should show "Quiet conditions" for A-index < 20', () => {
      const indices = createIndices();

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Quiet conditions')).toBeInTheDocument();
    });

    it('should show "Unsettled conditions" for A-index 20-49', () => {
      const indices = createIndices({ aIndex: 30 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Unsettled conditions')).toBeInTheDocument();
    });

    it('should show "Disturbed conditions" for A-index >= 50', () => {
      const indices = createIndices({ aIndex: 60 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Disturbed conditions')).toBeInTheDocument();
    });

    it('should show "Disturbed conditions" when A-index is undefined', () => {
      // Cast to test undefined handling in component
      const indices = { ...createIndices(), aIndex: undefined } as unknown as SolarIndices;

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Disturbed conditions')).toBeInTheDocument();
    });

    it('should handle A-index of 0', () => {
      const indices = createIndices({ aIndex: 0 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('0')).toBeInTheDocument();
      expect(screen.getByText('Quiet conditions')).toBeInTheDocument();
    });

    it('should handle boundary value 20', () => {
      const indices = createIndices({ aIndex: 20 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Unsettled conditions')).toBeInTheDocument();
    });

    it('should handle boundary value 50', () => {
      const indices = createIndices({ aIndex: 50 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Disturbed conditions')).toBeInTheDocument();
    });
  });

  describe('Sunspot Number', () => {
    it('should show "High solar activity" for sunspots > 100', () => {
      const indices = createIndices({ sunspotNumber: 150 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('High solar activity')).toBeInTheDocument();
    });

    it('should show "Moderate activity" for sunspots 51-100', () => {
      const indices = createIndices();

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Moderate activity')).toBeInTheDocument();
    });

    it('should show "Low solar activity" for sunspots <= 50', () => {
      const indices = createIndices({ sunspotNumber: 30 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Low solar activity')).toBeInTheDocument();
    });

    it('should show "Low solar activity" when sunspots is undefined', () => {
      // Cast to test undefined handling in component
      const indices = { ...createIndices(), sunspotNumber: undefined } as unknown as SolarIndices;

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Low solar activity')).toBeInTheDocument();
    });

    it('should handle sunspot number of 0', () => {
      const indices = createIndices({ sunspotNumber: 0 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('0')).toBeInTheDocument();
      expect(screen.getByText('Low solar activity')).toBeInTheDocument();
    });

    it('should handle boundary value 50', () => {
      const indices = createIndices({ sunspotNumber: 50 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Low solar activity')).toBeInTheDocument();
    });

    it('should handle boundary value 51', () => {
      const indices = createIndices({ sunspotNumber: 51 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Moderate activity')).toBeInTheDocument();
    });

    it('should handle boundary value 100', () => {
      const indices = createIndices({ sunspotNumber: 100 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('Moderate activity')).toBeInTheDocument();
    });

    it('should handle boundary value 101', () => {
      const indices = createIndices({ sunspotNumber: 101 });

      render(<SolarIndicesContent solarIndices={indices} />);

      expect(screen.getByText('High solar activity')).toBeInTheDocument();
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
