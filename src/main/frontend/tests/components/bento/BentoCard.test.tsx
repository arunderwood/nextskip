/**
 * Component tests for BentoCard
 *
 * Tests card rendering, hotness variants, accessibility, and user interaction
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import BentoCard from 'Frontend/components/bento/BentoCard';
import type { BentoCardConfig } from 'Frontend/types/bento';

expect.extend(toHaveNoViolations);

const mockConfig: BentoCardConfig = {
  id: 'test-card',
  type: 'solar-indices',
  size: 'standard',
  priority: 75,
  hotness: 'hot',
};

describe('BentoCard', () => {
  describe('rendering', () => {
    it('should render with required props', () => {
      render(
        <BentoCard config={mockConfig} title="Test Card">
          <div>Test content</div>
        </BentoCard>
      );

      expect(screen.getByText('Test Card')).toBeInTheDocument();
      expect(screen.getByText('Test content')).toBeInTheDocument();
    });

    it('should render icon when provided', () => {
      render(
        <BentoCard config={mockConfig} title="Test Card" icon="☀️">
          <div>Content</div>
        </BentoCard>
      );

      expect(screen.getByText('☀️')).toBeInTheDocument();
    });

    it('should render subtitle when provided', () => {
      render(
        <BentoCard
          config={mockConfig}
          title="Test Card"
          subtitle="Test subtitle"
        >
          <div>Content</div>
        </BentoCard>
      );

      expect(screen.getByText('Test subtitle')).toBeInTheDocument();
    });

    it('should render footer when provided', () => {
      render(
        <BentoCard
          config={mockConfig}
          title="Test Card"
          footer={<div>Footer content</div>}
        >
          <div>Content</div>
        </BentoCard>
      );

      expect(screen.getByText('Footer content')).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const { container } = render(
        <BentoCard
          config={mockConfig}
          title="Test Card"
          className="custom-class"
        >
          <div>Content</div>
        </BentoCard>
      );

      const card = container.querySelector('.bento-card');
      expect(card).toHaveClass('custom-class');
    });
  });

  describe('card sizes', () => {
    it.each([
      ['standard', 'bento-card--standard'],
      ['wide', 'bento-card--wide'],
      ['tall', 'bento-card--tall'],
      ['hero', 'bento-card--hero'],
    ] as const)('should apply %s size class', (size, expectedClass) => {
      const config = { ...mockConfig, size };
      const { container } = render(
        <BentoCard config={config} title="Test Card">
          <div>Content</div>
        </BentoCard>
      );

      const card = container.querySelector('.bento-card');
      expect(card).toHaveClass(expectedClass);
    });
  });

  describe('hotness variants', () => {
    it.each([
      ['hot', 'bento-card--hot', 'Excellent'],
      ['warm', 'bento-card--warm', 'Good'],
      ['neutral', 'bento-card--neutral', 'Moderate'],
      ['cool', 'bento-card--cool', 'Limited'],
    ] as const)(
      'should apply %s hotness class and show %s label',
      (hotness, expectedClass, expectedLabel) => {
        const config = { ...mockConfig, hotness };
        const { container } = render(
          <BentoCard config={config} title="Test Card">
            <div>Content</div>
          </BentoCard>
        );

        const card = container.querySelector('.bento-card');
        expect(card).toHaveClass(expectedClass);
        expect(screen.getByText(expectedLabel)).toBeInTheDocument();
      }
    );
  });

  describe('interactive behavior', () => {
    it('should render as article when not interactive', () => {
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card">
          <div>Content</div>
        </BentoCard>
      );

      const article = container.querySelector('article.bento-card');
      expect(article).toBeInTheDocument();
    });

    it('should render as button when onClick is provided', () => {
      const handleClick = vi.fn();
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </BentoCard>
      );

      const button = container.querySelector('button.bento-card');
      expect(button).toBeInTheDocument();
      expect(button).toHaveClass('bento-card--interactive');
    });

    it('should call onClick when clicked', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(
        <BentoCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </BentoCard>
      );

      const button = screen.getByRole('button', { name: 'Test Card' });
      await user.click(button);

      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations (non-interactive)', async () => {
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card" subtitle="Subtitle">
          <div>Content</div>
        </BentoCard>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations (interactive)', async () => {
      const handleClick = vi.fn();
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </BentoCard>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should use custom aria-label when provided', () => {
      render(
        <BentoCard
          config={mockConfig}
          title="Test Card"
          ariaLabel="Custom label"
        >
          <div>Content</div>
        </BentoCard>
      );

      const article = screen.getByLabelText('Custom label');
      expect(article).toBeInTheDocument();
    });

    it('should fall back to title for aria-label', () => {
      render(
        <BentoCard config={mockConfig} title="Default Label">
          <div>Content</div>
        </BentoCard>
      );

      const article = screen.getByLabelText('Default Label');
      expect(article).toBeInTheDocument();
    });

    it('should have proper heading hierarchy', () => {
      render(
        <BentoCard config={mockConfig} title="Card Title">
          <div>Content</div>
        </BentoCard>
      );

      const heading = screen.getByRole('heading', { name: 'Card Title' });
      expect(heading.tagName).toBe('H3');
    });
  });

  describe('hotness indicator', () => {
    it('should show hotness indicator for all levels', () => {
      const { container, rerender } = render(
        <BentoCard config={{ ...mockConfig, hotness: 'hot' }} title="Test">
          <div>Content</div>
        </BentoCard>
      );

      let indicator = container.querySelector(
        '.bento-card__hotness-indicator--hot'
      );
      expect(indicator).toBeInTheDocument();
      expect(indicator).toHaveTextContent('Excellent');

      rerender(
        <BentoCard config={{ ...mockConfig, hotness: 'warm' }} title="Test">
          <div>Content</div>
        </BentoCard>
      );
      indicator = container.querySelector(
        '.bento-card__hotness-indicator--warm'
      );
      expect(indicator).toHaveTextContent('Good');

      rerender(
        <BentoCard config={{ ...mockConfig, hotness: 'neutral' }} title="Test">
          <div>Content</div>
        </BentoCard>
      );
      indicator = container.querySelector(
        '.bento-card__hotness-indicator--neutral'
      );
      expect(indicator).toHaveTextContent('Moderate');

      rerender(
        <BentoCard config={{ ...mockConfig, hotness: 'cool' }} title="Test">
          <div>Content</div>
        </BentoCard>
      );
      indicator = container.querySelector(
        '.bento-card__hotness-indicator--cool'
      );
      expect(indicator).toHaveTextContent('Limited');
    });
  });

  describe('card structure', () => {
    it('should have header, content, and footer sections', () => {
      const { container } = render(
        <BentoCard
          config={mockConfig}
          title="Test Card"
          footer={<div>Footer</div>}
        >
          <div>Content</div>
        </BentoCard>
      );

      expect(
        container.querySelector('.bento-card__header')
      ).toBeInTheDocument();
      expect(
        container.querySelector('.bento-card__content')
      ).toBeInTheDocument();
      expect(
        container.querySelector('.bento-card__footer')
      ).toBeInTheDocument();
    });

    it('should not render footer when not provided', () => {
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card">
          <div>Content</div>
        </BentoCard>
      );

      expect(container.querySelector('.bento-card__footer')).not.toBeInTheDocument();
    });

    it('should render header with icon and title', () => {
      const { container } = render(
        <BentoCard config={mockConfig} title="Test Card" icon="📊">
          <div>Content</div>
        </BentoCard>
      );

      const header = container.querySelector('.bento-card__header');
      expect(header).toBeInTheDocument();
      expect(header).toHaveTextContent('📊');
      expect(header).toHaveTextContent('Test Card');
    });
  });
});
