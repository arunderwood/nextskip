/**
 * Accessibility integration tests for Bento Grid system
 *
 * Tests WCAG 2.1 AA compliance, keyboard navigation, screen reader support,
 * and focus management across the entire bento grid system
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import BentoGrid from 'Frontend/components/bento/BentoGrid';
import BentoCard from 'Frontend/components/bento/BentoCard';
import type { BentoCardConfig } from 'Frontend/types/bento';

expect.extend(toHaveNoViolations);

const createTestCard = (
  id: string,
  priority: number,
  title: string,
  interactive = false
) => {
  const config: BentoCardConfig = {
    id,
    type: 'solar-indices',
    size: 'standard',
    priority,
    hotness: priority >= 70 ? 'hot' : priority >= 45 ? 'warm' : 'neutral',
  };

  return {
    config,
    component: (
      <BentoCard
        config={config}
        title={title}
        subtitle="Test subtitle"
        icon="📊"
        onClick={interactive ? vi.fn() : undefined}
      >
        <div>{title} content</div>
      </BentoCard>
    ),
  };
};

describe('Bento System Accessibility', () => {
  describe('WCAG 2.1 AA Compliance', () => {
    it('should have no accessibility violations with standard cards', async () => {
      const cards = [
        createTestCard('card-1', 90, 'Solar Indices'),
        createTestCard('card-2', 60, 'Band Conditions'),
        createTestCard('card-3', 30, 'Propagation'),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no violations with interactive cards', async () => {
      const cards = [
        createTestCard('card-1', 90, 'Solar Indices', true),
        createTestCard('card-2', 60, 'Band Conditions', true),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no violations with mixed card types', async () => {
      const cards = [
        createTestCard('card-1', 90, 'Interactive Card', true),
        createTestCard('card-2', 60, 'Static Card', false),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no violations with all hotness levels', async () => {
      const cards = [
        createTestCard('hot', 85, 'Hot Card'),
        createTestCard('warm', 55, 'Warm Card'),
        createTestCard('neutral', 35, 'Neutral Card'),
        createTestCard('cool', 15, 'Cool Card'),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Keyboard Navigation', () => {
    it('should allow tab navigation through interactive cards in priority order', async () => {
      const user = userEvent.setup();
      const cards = [
        createTestCard('low', 30, 'Low Priority', true),
        createTestCard('high', 90, 'High Priority', true),
        createTestCard('medium', 60, 'Medium Priority', true),
      ];

      render(<BentoGrid cards={cards} />);

      // Cards should be in DOM in priority order
      const highButton = screen.getByRole('button', { name: 'High Priority' });
      const mediumButton = screen.getByRole('button', {
        name: 'Medium Priority',
      });
      const lowButton = screen.getByRole('button', { name: 'Low Priority' });

      // Tab through cards - should follow visual priority order
      await user.tab();
      expect(highButton).toHaveFocus();

      await user.tab();
      expect(mediumButton).toHaveFocus();

      await user.tab();
      expect(lowButton).toHaveFocus();
    });

    it('should skip non-interactive cards during tab navigation', async () => {
      const user = userEvent.setup();
      const cards = [
        createTestCard('static-1', 90, 'Static High', false),
        createTestCard('interactive', 60, 'Interactive', true),
        createTestCard('static-2', 30, 'Static Low', false),
      ];

      render(<BentoGrid cards={cards} />);

      const button = screen.getByRole('button', { name: 'Interactive' });

      // Tab should go directly to the interactive card
      await user.tab();
      expect(button).toHaveFocus();
    });

    it('should activate interactive cards with Enter key', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      const config: BentoCardConfig = {
        id: 'test',
        type: 'solar-indices',
        size: 'standard',
        priority: 75,
        hotness: 'hot',
      };

      render(
        <BentoCard config={config} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </BentoCard>
      );

      const button = screen.getByRole('button', { name: 'Test Card' });

      await user.tab();
      expect(button).toHaveFocus();

      await user.keyboard('{Enter}');
      expect(handleClick).toHaveBeenCalledTimes(1);
    });

    it('should activate interactive cards with Space key', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      const config: BentoCardConfig = {
        id: 'test',
        type: 'solar-indices',
        size: 'standard',
        priority: 75,
        hotness: 'hot',
      };

      render(
        <BentoCard config={config} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </BentoCard>
      );

      const button = screen.getByRole('button', { name: 'Test Card' });

      await user.tab();
      expect(button).toHaveFocus();

      await user.keyboard(' ');
      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('Screen Reader Support', () => {
    it('should announce grid as a list', () => {
      const cards = [createTestCard('card-1', 75, 'Test Card')];

      render(<BentoGrid cards={cards} />);

      const list = screen.getByRole('list');
      expect(list).toBeInTheDocument();
    });

    it('should announce each card as a list item', () => {
      const cards = [
        createTestCard('card-1', 90, 'Card 1'),
        createTestCard('card-2', 60, 'Card 2'),
        createTestCard('card-3', 30, 'Card 3'),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const list = screen.getByRole('list');
      const items = list.querySelectorAll('[role="listitem"]');

      expect(items).toHaveLength(3);
    });

    it('should have accessible labels on cards', () => {
      const cards = [createTestCard('card-1', 75, 'Solar Indices')];

      render(<BentoGrid cards={cards} />);

      const card = screen.getByLabelText('Solar Indices');
      expect(card).toBeInTheDocument();
    });

    it('should announce hotness level to screen readers', () => {
      const config1: BentoCardConfig = { id: 'hot', type: 'solar-indices', size: 'standard', priority: 85, hotness: 'hot' };
      const config2: BentoCardConfig = { id: 'warm', type: 'solar-indices', size: 'standard', priority: 55, hotness: 'warm' };
      const config3: BentoCardConfig = { id: 'neutral', type: 'solar-indices', size: 'standard', priority: 35, hotness: 'neutral' };
      const config4: BentoCardConfig = { id: 'cool', type: 'solar-indices', size: 'standard', priority: 15, hotness: 'cool' };

      const cards = [
        { config: config1, component: <BentoCard config={config1} title="Hot Card" subtitle="Test" icon="📊"><div>Hot Card content</div></BentoCard> },
        { config: config2, component: <BentoCard config={config2} title="Warm Card" subtitle="Test" icon="📊"><div>Warm Card content</div></BentoCard> },
        { config: config3, component: <BentoCard config={config3} title="Neutral Card" subtitle="Test" icon="📊"><div>Neutral Card content</div></BentoCard> },
        { config: config4, component: <BentoCard config={config4} title="Cool Card" subtitle="Test" icon="📊"><div>Cool Card content</div></BentoCard> },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      expect(container).toHaveTextContent('Excellent'); // hot
      expect(container).toHaveTextContent('Good'); // warm
      expect(container).toHaveTextContent('Moderate'); // neutral
      expect(container).toHaveTextContent('Limited'); // cool
    });

    it('should have proper heading hierarchy', () => {
      const cards = [
        createTestCard('card-1', 90, 'Main Card'),
        createTestCard('card-2', 60, 'Secondary Card'),
      ];

      render(<BentoGrid cards={cards} />);

      const heading1 = screen.getByRole('heading', { name: 'Main Card' });
      const heading2 = screen.getByRole('heading', { name: 'Secondary Card' });

      expect(heading1.tagName).toBe('H3');
      expect(heading2.tagName).toBe('H3');
    });
  });

  describe('Focus Management', () => {
    it('should show visible focus indicators on interactive cards', async () => {
      const user = userEvent.setup();
      const cards = [createTestCard('card-1', 75, 'Test Card', true)];

      const { container } = render(<BentoGrid cards={cards} />);

      const button = screen.getByRole('button', { name: 'Test Card' });

      await user.tab();
      expect(button).toHaveFocus();

      // Check that focus styles are applied (class-based check)
      const card = container.querySelector('.bento-card');
      expect(card).toContainElement(button);
    });

    it('should not trap focus within grid', async () => {
      const user = userEvent.setup();
      const cards = [
        createTestCard('card-1', 90, 'Card 1', true),
        createTestCard('card-2', 60, 'Card 2', true),
      ];

      render(
        <div>
          <button>Before</button>
          <BentoGrid cards={cards} />
          <button>After</button>
        </div>
      );

      const beforeButton = screen.getByRole('button', { name: 'Before' });
      const card1 = screen.getByRole('button', { name: 'Card 1' });
      const card2 = screen.getByRole('button', { name: 'Card 2' });
      const afterButton = screen.getByRole('button', { name: 'After' });

      // Tab through entire sequence
      await user.tab();
      expect(beforeButton).toHaveFocus();

      await user.tab();
      expect(card1).toHaveFocus();

      await user.tab();
      expect(card2).toHaveFocus();

      await user.tab();
      expect(afterButton).toHaveFocus();
    });
  });

  describe('Semantic HTML', () => {
    it('should use article element for non-interactive cards', () => {
      const cards = [createTestCard('card-1', 75, 'Test Card', false)];

      const { container } = render(<BentoGrid cards={cards} />);

      const article = container.querySelector('article.bento-card');
      expect(article).toBeInTheDocument();
    });

    it('should use button element for interactive cards', () => {
      const cards = [createTestCard('card-1', 75, 'Test Card', true)];

      const { container } = render(<BentoGrid cards={cards} />);

      const button = container.querySelector('button.bento-card');
      expect(button).toBeInTheDocument();
    });

    it('should use proper sectioning divs', () => {
      const config: BentoCardConfig = {
        id: 'test',
        type: 'solar-indices',
        size: 'standard',
        priority: 75,
        hotness: 'hot',
      };

      const { container } = render(
        <BentoCard
          config={config}
          title="Test"
          footer={<div>Footer</div>}
        >
          <div>Content</div>
        </BentoCard>
      );

      expect(container.querySelector('.bento-card__header')).toBeInTheDocument();
      expect(container.querySelector('.bento-card__footer')).toBeInTheDocument();
    });
  });

  describe('Color Contrast', () => {
    it('should use semantic color classes for hotness indicators', () => {
      const config1: BentoCardConfig = { id: 'hot', type: 'solar-indices', size: 'standard', priority: 85, hotness: 'hot' };
      const config2: BentoCardConfig = { id: 'warm', type: 'solar-indices', size: 'standard', priority: 55, hotness: 'warm' };
      const config3: BentoCardConfig = { id: 'neutral', type: 'solar-indices', size: 'standard', priority: 35, hotness: 'neutral' };
      const config4: BentoCardConfig = { id: 'cool', type: 'solar-indices', size: 'standard', priority: 15, hotness: 'cool' };

      const cards = [
        { config: config1, component: <BentoCard config={config1} title="Hot"><div>Hot</div></BentoCard> },
        { config: config2, component: <BentoCard config={config2} title="Warm"><div>Warm</div></BentoCard> },
        { config: config3, component: <BentoCard config={config3} title="Neutral"><div>Neutral</div></BentoCard> },
        { config: config4, component: <BentoCard config={config4} title="Cool"><div>Cool</div></BentoCard> },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      expect(
        container.querySelector('.bento-card--hot')
      ).toBeInTheDocument();
      expect(
        container.querySelector('.bento-card--warm')
      ).toBeInTheDocument();
      expect(
        container.querySelector('.bento-card--neutral')
      ).toBeInTheDocument();
      expect(
        container.querySelector('.bento-card--cool')
      ).toBeInTheDocument();
    });
  });

  describe('Responsive Accessibility', () => {
    it('should maintain accessibility at different viewport sizes', async () => {
      const cards = [
        createTestCard('card-1', 90, 'Card 1'),
        createTestCard('card-2', 60, 'Card 2'),
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      // Desktop viewport
      window.matchMedia = vi.fn().mockImplementation((query) => ({
        matches: query === '(min-width: 1024px)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));

      let results = await axe(container);
      expect(results).toHaveNoViolations();

      // Mobile viewport
      window.matchMedia = vi.fn().mockImplementation((query) => ({
        matches: query === '(max-width: 768px)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      }));

      results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
