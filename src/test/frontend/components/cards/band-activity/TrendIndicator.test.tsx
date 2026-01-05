import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { TrendIndicator } from 'Frontend/components/cards/band-activity/TrendIndicator';

expect.extend(toHaveNoViolations);

describe('TrendIndicator', () => {
  describe('positive trends', () => {
    it('should show plus sign for positive trend', () => {
      render(<TrendIndicator trendPercentage={50} />);

      expect(screen.getByText(/\+50%/)).toBeInTheDocument();
    });

    it('should apply positive styling', () => {
      const { container } = render(<TrendIndicator trendPercentage={50} />);

      const indicator = container.querySelector('.trend-indicator');
      expect(indicator).toHaveClass('trend-positive');
    });

    it('should show up arrow icon for positive trend', () => {
      const { container } = render(<TrendIndicator trendPercentage={50} />);

      const upIcon = container.querySelector('.trend-icon--up');
      expect(upIcon).toBeInTheDocument();
    });
  });

  describe('negative trends', () => {
    it('should show negative trend with minus sign', () => {
      render(<TrendIndicator trendPercentage={-30} />);

      expect(screen.getByText(/-30%/)).toBeInTheDocument();
    });

    it('should apply negative styling', () => {
      const { container } = render(<TrendIndicator trendPercentage={-30} />);

      const indicator = container.querySelector('.trend-indicator');
      expect(indicator).toHaveClass('trend-negative');
    });

    it('should show down arrow icon for negative trend', () => {
      const { container } = render(<TrendIndicator trendPercentage={-30} />);

      const downIcon = container.querySelector('.trend-icon--down');
      expect(downIcon).toBeInTheDocument();
    });
  });

  describe('flat trends', () => {
    it('should show 0% for zero trend', () => {
      render(<TrendIndicator trendPercentage={0} />);

      expect(screen.getByText(/0%/)).toBeInTheDocument();
    });

    it('should apply flat styling for zero', () => {
      const { container } = render(<TrendIndicator trendPercentage={0} />);

      const indicator = container.querySelector('.trend-indicator');
      expect(indicator).toHaveClass('trend-flat');
    });

    it('should show flat icon for zero trend', () => {
      const { container } = render(<TrendIndicator trendPercentage={0} />);

      const flatIcon = container.querySelector('.trend-icon--flat');
      expect(flatIcon).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('should handle very large positive values', () => {
      render(<TrendIndicator trendPercentage={999} />);

      expect(screen.getByText(/\+999%/)).toBeInTheDocument();
    });

    it('should handle very large negative values', () => {
      render(<TrendIndicator trendPercentage={-99} />);

      expect(screen.getByText(/-99%/)).toBeInTheDocument();
    });

    it('should show label', () => {
      render(<TrendIndicator trendPercentage={50} />);

      expect(screen.getByText(/Trend/)).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations for positive trend', async () => {
      const { container } = render(<TrendIndicator trendPercentage={50} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for negative trend', async () => {
      const { container } = render(<TrendIndicator trendPercentage={-30} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for zero trend', async () => {
      const { container } = render(<TrendIndicator trendPercentage={0} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
