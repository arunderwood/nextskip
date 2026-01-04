import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { BandRatingDisplay } from 'Frontend/components/cards/propagation/BandRatingDisplay';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';
import { createMockBandCondition } from '../../../fixtures/mockFactories';

expect.extend(toHaveNoViolations);

describe('BandRatingDisplay', () => {
  // Use shared factory - alias for backward compatibility
  const createCondition = createMockBandCondition;

  describe('rendering', () => {
    it('should render the rating badge', () => {
      const condition = createCondition();

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('GOOD')).toBeInTheDocument();
    });

    it('should display Current Conditions label', () => {
      const condition = createCondition();

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('Current Conditions')).toBeInTheDocument();
    });
  });

  describe('rating badges', () => {
    it.each([
      [BandConditionRating.GOOD, 'GOOD'],
      [BandConditionRating.FAIR, 'FAIR'],
      [BandConditionRating.POOR, 'POOR'],
      [BandConditionRating.UNKNOWN, 'UNKNOWN'],
    ])('should render %s rating text', (rating, expectedText) => {
      const condition = createCondition({ rating });

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText(expectedText)).toBeInTheDocument();
    });

    it('should handle undefined rating gracefully', () => {
      const condition = { ...createCondition(), rating: undefined } as unknown as BandCondition;

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
    });
  });

  describe('rating icons', () => {
    it('should render ? for UNKNOWN rating', () => {
      const condition = createCondition({ rating: BandConditionRating.UNKNOWN });

      render(<BandRatingDisplay condition={condition} />);

      // The ? is visible in the UI for unknown ratings
      expect(screen.getByText('?')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations', async () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have aria-label on rating badge', () => {
      const condition = createCondition({ rating: BandConditionRating.GOOD });

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByRole('status')).toHaveAttribute('aria-label', 'Propagation rating: GOOD');
    });

    it('should have role="status" on rating badge', () => {
      const condition = createCondition();

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByRole('status')).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('should handle all fields undefined', () => {
      const condition: BandCondition = {
        band: undefined,
        rating: undefined,
        confidence: undefined,
        notes: undefined,
        favorable: undefined,
        score: undefined,
      } as unknown as BandCondition;

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
    });

    it('should handle lowercase rating', () => {
      const condition = createCondition({ rating: 'good' as unknown as BandConditionRating });

      render(<BandRatingDisplay condition={condition} />);

      // Component displays the rating value - lowercase passed, lowercase shown
      expect(screen.getByText('good')).toBeInTheDocument();
    });
  });
});
