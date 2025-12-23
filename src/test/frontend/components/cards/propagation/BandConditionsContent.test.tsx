import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import BandConditionsContent, {
  BandConditionsLegend,
} from 'Frontend/components/cards/propagation/BandConditionsContent';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';

describe('BandConditionsContent', () => {
  const createCondition = (
    band: string,
    rating: string,
    score: number,
    favorable: boolean
  ): BandCondition => ({
    band,
    rating,
    score,
    favorable,
  });

  describe('rendering', () => {
    it('should render table with band conditions', () => {
      const conditions = [
        createCondition('BAND_160M', 'GOOD', 85, true),
        createCondition('BAND_80M', 'FAIR', 60, false),
        createCondition('BAND_40M', 'POOR', 30, false),
      ];

      render(<BandConditionsContent bandConditions={conditions} />);

      // Check table headers
      expect(screen.getByText('Band')).toBeInTheDocument();
      expect(screen.getByText('Condition')).toBeInTheDocument();
      expect(screen.getByText('Notes')).toBeInTheDocument();

      // Check band names (should be formatted - BAND_ prefix removed)
      expect(screen.getByText('160M')).toBeInTheDocument();
      expect(screen.getByText('80M')).toBeInTheDocument();
      expect(screen.getByText('40M')).toBeInTheDocument();

      // Check ratings
      expect(screen.getByText('GOOD')).toBeInTheDocument();
      expect(screen.getByText('FAIR')).toBeInTheDocument();
      expect(screen.getByText('POOR')).toBeInTheDocument();
    });

    it('should display band descriptions', () => {
      const conditions = [
        createCondition('BAND_20M', 'GOOD', 85, true),
        createCondition('BAND_40M', 'FAIR', 60, false),
      ];

      render(<BandConditionsContent bandConditions={conditions} />);

      expect(screen.getByText('DX powerhouse')).toBeInTheDocument();
      expect(screen.getByText('All-around workhorse')).toBeInTheDocument();
    });

    it('should render empty table when no conditions provided', () => {
      const { container } = render(<BandConditionsContent bandConditions={[]} />);

      // Headers should still be present
      expect(screen.getByText('Band')).toBeInTheDocument();
      expect(screen.getByText('Condition')).toBeInTheDocument();
      expect(screen.getByText('Notes')).toBeInTheDocument();

      // No band rows should be rendered
      const bandRows = container.querySelectorAll('.band-row');
      expect(bandRows.length).toBe(0);
    });
  });

  describe('rating icons', () => {
    it('should show check icon for GOOD rating', () => {
      const conditions = [createCondition('BAND_20M', 'GOOD', 85, true)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      // Check for lucide-react check icon
      expect(container.querySelector('.rating-icon svg')).toBeInTheDocument();
      expect(screen.getByText('GOOD')).toBeInTheDocument();
    });

    it('should show minus icon for FAIR rating', () => {
      const conditions = [createCondition('BAND_20M', 'FAIR', 60, false)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-icon svg')).toBeInTheDocument();
      expect(screen.getByText('FAIR')).toBeInTheDocument();
    });

    it('should show X icon for POOR rating', () => {
      const conditions = [createCondition('BAND_20M', 'POOR', 30, false)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-icon svg')).toBeInTheDocument();
      expect(screen.getByText('POOR')).toBeInTheDocument();
    });

    it('should show question mark for unknown rating', () => {
      const conditions = [createCondition('BAND_20M', 'UNKNOWN', 0, false)];
      render(<BandConditionsContent bandConditions={conditions} />);

      expect(screen.getByText('?')).toBeInTheDocument();
      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
    });

    it('should handle undefined rating', () => {
      const conditions = [
        {
          band: 'BAND_20M',
          rating: undefined,
          score: 0,
          favorable: false,
        } as BandCondition,
      ];

      render(<BandConditionsContent bandConditions={conditions} />);

      expect(screen.getByText('?')).toBeInTheDocument();
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });
  });

  describe('CSS classes', () => {
    it('should apply rating-good class for GOOD rating', () => {
      const conditions = [createCondition('BAND_20M', 'GOOD', 85, true)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-good')).toBeInTheDocument();
    });

    it('should apply rating-fair class for FAIR rating', () => {
      const conditions = [createCondition('BAND_20M', 'FAIR', 60, false)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-fair')).toBeInTheDocument();
    });

    it('should apply rating-poor class for POOR rating', () => {
      const conditions = [createCondition('BAND_20M', 'POOR', 30, false)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-poor')).toBeInTheDocument();
    });

    it('should apply rating-unknown class for unknown rating', () => {
      const conditions = [createCondition('BAND_20M', 'UNKNOWN', 0, false)];
      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      expect(container.querySelector('.rating-unknown')).toBeInTheDocument();
    });
  });

  describe('sorting', () => {
    it('should sort bands in correct frequency order', () => {
      const conditions = [
        createCondition('BAND_10M', 'GOOD', 85, true),
        createCondition('BAND_160M', 'FAIR', 60, false),
        createCondition('BAND_40M', 'POOR', 30, false),
        createCondition('BAND_20M', 'GOOD', 80, true),
      ];

      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      const rows = container.querySelectorAll('.band-row');
      expect(rows).toHaveLength(4);

      // Should be sorted: 160M, 40M, 20M, 10M
      expect(rows[0].textContent).toContain('160M');
      expect(rows[1].textContent).toContain('40M');
      expect(rows[2].textContent).toContain('20M');
      expect(rows[3].textContent).toContain('10M');
    });

    it('should handle all bands in correct order', () => {
      const conditions = [
        createCondition('BAND_6M', 'GOOD', 85, true),
        createCondition('BAND_10M', 'GOOD', 85, true),
        createCondition('BAND_12M', 'GOOD', 85, true),
        createCondition('BAND_15M', 'GOOD', 85, true),
        createCondition('BAND_17M', 'GOOD', 85, true),
        createCondition('BAND_20M', 'GOOD', 85, true),
        createCondition('BAND_30M', 'GOOD', 85, true),
        createCondition('BAND_40M', 'GOOD', 85, true),
        createCondition('BAND_80M', 'GOOD', 85, true),
        createCondition('BAND_160M', 'GOOD', 85, true),
      ];

      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      const rows = container.querySelectorAll('.band-row');
      expect(rows).toHaveLength(10);

      // Should be in order: 160M, 80M, 40M, 30M, 20M, 17M, 15M, 12M, 10M, 6M
      const expectedOrder = ['160M', '80M', '40M', '30M', '20M', '17M', '15M', '12M', '10M', '6M'];
      rows.forEach((row, index) => {
        expect(row.textContent).toContain(expectedOrder[index]);
      });
    });
  });

  describe('memoization', () => {
    it('should memoize sorted conditions', () => {
      const conditions = [
        createCondition('BAND_10M', 'GOOD', 85, true),
        createCondition('BAND_20M', 'FAIR', 60, false),
      ];

      const { rerender, container } = render(
        <BandConditionsContent bandConditions={conditions} />
      );

      const initialRows = container.querySelectorAll('.band-row');

      // Rerender with same conditions (should use memoized value)
      rerender(<BandConditionsContent bandConditions={conditions} />);

      const rerenderedRows = container.querySelectorAll('.band-row');
      expect(rerenderedRows).toHaveLength(initialRows.length);
    });
  });

  describe('edge cases', () => {
    it('should handle undefined band name', () => {
      const conditions = [
        {
          band: undefined,
          rating: 'GOOD',
          score: 85,
          favorable: true,
        } as BandCondition,
      ];

      render(<BandConditionsContent bandConditions={conditions} />);

      // Should show "Unknown" for undefined band
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });

    it('should handle empty band name', () => {
      const conditions = [createCondition('', 'GOOD', 85, true)];

      render(<BandConditionsContent bandConditions={conditions} />);

      // Should show "Unknown" for empty band
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });

    it('should handle band without description', () => {
      const conditions = [createCondition('UNKNOWN_BAND', 'GOOD', 85, true)];

      const { container } = render(<BandConditionsContent bandConditions={conditions} />);

      // Description cell should be empty for unknown bands (select only tbody cells, not header)
      const bandRow = container.querySelector('.band-row');
      const descriptionCell = bandRow?.querySelector('.description-col');
      expect(descriptionCell?.textContent).toBe('');
    });
  });
});

describe('BandConditionsLegend', () => {
  it('should render legend with title', () => {
    render(<BandConditionsLegend />);

    expect(screen.getByText('Legend:')).toBeInTheDocument();
  });

  it('should render all rating types', () => {
    render(<BandConditionsLegend />);

    expect(screen.getByText('Good')).toBeInTheDocument();
    expect(screen.getByText('Fair')).toBeInTheDocument();
    expect(screen.getByText('Poor')).toBeInTheDocument();
  });

  it('should render rating descriptions', () => {
    render(<BandConditionsLegend />);

    expect(screen.getByText('Excellent propagation')).toBeInTheDocument();
    expect(screen.getByText('Moderate propagation')).toBeInTheDocument();
    expect(screen.getByText('Limited propagation')).toBeInTheDocument();
  });

  it('should render rating icons', () => {
    const { container } = render(<BandConditionsLegend />);

    // Should have 3 icons (one for each rating type)
    const icons = container.querySelectorAll('.rating-icon svg');
    expect(icons).toHaveLength(3);
  });

  it('should apply correct CSS classes to legend items', () => {
    const { container } = render(<BandConditionsLegend />);

    expect(container.querySelector('.legend')).toBeInTheDocument();
    expect(container.querySelector('.legend-title')).toBeInTheDocument();
    expect(container.querySelector('.legend-items')).toBeInTheDocument();
    expect(container.querySelectorAll('.legend-item')).toHaveLength(3);
    expect(container.querySelector('.rating-good')).toBeInTheDocument();
    expect(container.querySelector('.rating-fair')).toBeInTheDocument();
    expect(container.querySelector('.rating-poor')).toBeInTheDocument();
  });
});
