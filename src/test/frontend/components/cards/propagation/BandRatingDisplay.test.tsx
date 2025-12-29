import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { BandRatingDisplay } from 'Frontend/components/cards/propagation/BandRatingDisplay';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import BandConditionRating from 'Frontend/generated/io/nextskip/propagation/model/BandConditionRating';
import FrequencyBand from 'Frontend/generated/io/nextskip/common/model/FrequencyBand';

expect.extend(toHaveNoViolations);

describe('BandRatingDisplay', () => {
  const createCondition = (overrides?: Partial<BandCondition>): BandCondition => ({
    band: FrequencyBand.BAND_20M,
    rating: BandConditionRating.GOOD,
    confidence: 1.0,
    notes: undefined,
    favorable: true,
    score: 100,
    ...overrides,
  });

  describe('rendering', () => {
    it('should render the rating badge', () => {
      const condition = createCondition();

      render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('GOOD')).toBeInTheDocument();
    });
  });

  describe('rating badges', () => {
    it('should render GOOD rating with correct class', () => {
      const condition = createCondition({ rating: BandConditionRating.GOOD });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('GOOD')).toBeInTheDocument();
      expect(container.querySelector('.rating-good')).toBeInTheDocument();
    });

    it('should render FAIR rating with correct class', () => {
      const condition = createCondition({ rating: BandConditionRating.FAIR, score: 60 });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('FAIR')).toBeInTheDocument();
      expect(container.querySelector('.rating-fair')).toBeInTheDocument();
    });

    it('should render POOR rating with correct class', () => {
      const condition = createCondition({ rating: BandConditionRating.POOR, score: 20 });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('POOR')).toBeInTheDocument();
      expect(container.querySelector('.rating-poor')).toBeInTheDocument();
    });

    it('should render UNKNOWN rating with correct class', () => {
      const condition = createCondition({ rating: BandConditionRating.UNKNOWN, score: 0 });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
      expect(container.querySelector('.rating-unknown')).toBeInTheDocument();
    });

    it('should handle undefined rating gracefully', () => {
      const condition = { ...createCondition(), rating: undefined } as unknown as BandCondition;
      const { container } = render(<BandRatingDisplay condition={condition} />);

      expect(screen.getByText('UNKNOWN')).toBeInTheDocument();
      expect(container.querySelector('.rating-unknown')).toBeInTheDocument();
    });
  });

  describe('rating icons', () => {
    it('should render Check icon for GOOD rating', () => {
      const condition = createCondition({ rating: BandConditionRating.GOOD });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      // lucide-react renders SVG with data-lucide attribute
      const icon = container.querySelector('.rating-icon svg');
      expect(icon).toBeInTheDocument();
    });

    it('should render Minus icon for FAIR rating', () => {
      const condition = createCondition({ rating: BandConditionRating.FAIR });
      const { container } = render(<BandRatingDisplay condition={condition} />);

      const icon = container.querySelector('.rating-icon svg');
      expect(icon).toBeInTheDocument();
    });

    it('should render X icon for POOR rating', () => {
      const condition = createCondition({ rating: BandConditionRating.POOR });
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
