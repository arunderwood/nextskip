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
  });

  describe('rating badges', () => {
    it.each([
      [BandConditionRating.GOOD, 'GOOD', '.rating-good'],
      [BandConditionRating.FAIR, 'FAIR', '.rating-fair'],
      [BandConditionRating.POOR, 'POOR', '.rating-poor'],
      [BandConditionRating.UNKNOWN, 'UNKNOWN', '.rating-unknown'],
    ])('should render %s rating with correct class', (rating, expectedText, expectedClass) => {
      const condition = createCondition({ rating });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText(expectedText)).toBeInTheDocument();
      expect(container.querySelector(expectedClass)).toBeInTheDocument();
    });

    it('should handle undefined rating gracefully', () => {
      const condition = { ...createCondition(), rating: undefined } as unknown as BandCondition;
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
      expect(container.querySelector('.rating-unknown')).toBeInTheDocument();
    });
  });

  describe('rating icons', () => {
    it.each([
      [BandConditionRating.GOOD, 'Check'],
      [BandConditionRating.FAIR, 'Minus'],
      [BandConditionRating.POOR, 'X'],
    ])('should render SVG icon for %s rating', (rating) => {
      const condition = createCondition({ rating });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      const icon = container.querySelector('.rating-icon svg');
      expect(icon).toBeInTheDocument();
    });

    it('should render ? for UNKNOWN rating', () => {
      const condition = createCondition({ rating: BandConditionRating.UNKNOWN });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      const iconSpan = container.querySelector('.rating-icon span');
      expect(iconSpan).toHaveTextContent('?');
    });
  });

  describe('CSS structure', () => {
    it('should have band-rating-display container', () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(container.querySelector('.band-rating-display')).toBeInTheDocument();
    });

    it('should have band-condition-container', () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(container.querySelector('.band-condition-container')).toBeInTheDocument();
    });

    it('should have condition-label', () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(container.querySelector('.condition-label')).toHaveTextContent('Current Conditions');
    });

    it('should have rating-prominent section', () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(container.querySelector('.rating-prominent')).toBeInTheDocument();
    });

    it('should have rating-badge with large modifier', () => {
      const condition = createCondition();
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(container.querySelector('.rating-badge--large')).toBeInTheDocument();
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

    it('should hide icons from screen readers', () => {
      const condition = createCondition({ rating: BandConditionRating.GOOD });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      const icon = container.querySelector('.rating-icon svg');
      expect(icon).toHaveAttribute('aria-hidden', 'true');
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
      const { container } = render(<BandRatingDisplay condition={condition} />);

      // getRatingClass normalizes to uppercase
      expect(container.querySelector('.rating-good')).toBeInTheDocument();
    });
  });
});
