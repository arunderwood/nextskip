/**
 * Component tests for BentoGrid
 *
 * Tests grid rendering, card sorting by priority, responsive behavior,
 * and accessibility
 */

import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import BentoGrid from 'Frontend/components/bento/BentoGrid';
import BentoCard from 'Frontend/components/bento/BentoCard';
import type { BentoCardConfig } from 'Frontend/types/bento';

expect.extend(toHaveNoViolations);

const createCard = (
  id: string,
  priority: number,
  hotness: 'hot' | 'warm' | 'neutral' | 'cool',
  size: 'standard' | 'wide' | 'tall' | 'hero' = 'standard'
): BentoCardConfig => ({
  id,
  type: 'solar-indices',
  size,
  priority,
  hotness,
});

describe('BentoGrid', () => {
  describe('rendering', () => {
    it('should render empty grid with no cards', () => {
      const { container } = render(<BentoGrid cards={[]} />);

      const grid = container.querySelector('.bento-grid');
      expect(grid).toBeInTheDocument();
      expect(grid?.children).toHaveLength(0);
    });

    it('should render single card', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content 1</div>
            </BentoCard>
          ),
        },
      ];

      render(<BentoGrid cards={cards} />);

      expect(screen.getByText('Card 1')).toBeInTheDocument();
      expect(screen.getByText('Content 1')).toBeInTheDocument();
    });

    it('should render multiple cards', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content 1</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <BentoCard
              config={createCard('card-2', 50, 'warm')}
              title="Card 2"
            >
              <div>Content 2</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-3', 25, 'neutral'),
          component: (
            <BentoCard
              config={createCard('card-3', 25, 'neutral')}
              title="Card 3"
            >
              <div>Content 3</div>
            </BentoCard>
          ),
        },
      ];

      render(<BentoGrid cards={cards} />);

      expect(screen.getByText('Card 1')).toBeInTheDocument();
      expect(screen.getByText('Card 2')).toBeInTheDocument();
      expect(screen.getByText('Card 3')).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(
        <BentoGrid cards={cards} className="custom-grid" />
      );

      const grid = container.querySelector('.bento-grid');
      expect(grid).toHaveClass('custom-grid');
    });
  });

  describe('priority-based sorting', () => {
    it('should sort cards by priority in descending order', () => {
      const cards = [
        {
          config: createCard('low', 20, 'neutral'),
          component: (
            <BentoCard config={createCard('low', 20, 'neutral')} title="Low">
              <div>Low priority</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('high', 90, 'hot'),
          component: (
            <BentoCard config={createCard('high', 90, 'hot')} title="High">
              <div>High priority</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('medium', 50, 'warm'),
          component: (
            <BentoCard
              config={createCard('medium', 50, 'warm')}
              title="Medium"
            >
              <div>Medium priority</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const grid = container.querySelector('.bento-grid');
      const items = grid?.querySelectorAll('.bento-grid__card-wrapper');

      expect(items).toHaveLength(3);

      // Cards should be rendered in priority order: high, medium, low
      expect(items?.[0]).toHaveTextContent('High priority');
      expect(items?.[1]).toHaveTextContent('Medium priority');
      expect(items?.[2]).toHaveTextContent('Low priority');
    });

    it('should maintain stable sort for equal priorities', () => {
      const cards = [
        {
          config: createCard('card-a', 50, 'warm'),
          component: (
            <BentoCard config={createCard('card-a', 50, 'warm')} title="A">
              <div>Card A</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-b', 50, 'warm'),
          component: (
            <BentoCard config={createCard('card-b', 50, 'warm')} title="B">
              <div>Card B</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-c', 50, 'warm'),
          component: (
            <BentoCard config={createCard('card-c', 50, 'warm')} title="C">
              <div>Card C</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const grid = container.querySelector('.bento-grid');
      const items = grid?.querySelectorAll('.bento-grid__card-wrapper');

      // Order should be preserved when priorities are equal
      expect(items?.[0]).toHaveTextContent('Card A');
      expect(items?.[1]).toHaveTextContent('Card B');
      expect(items?.[2]).toHaveTextContent('Card C');
    });

    it('should re-sort when card priorities change', () => {
      const initialCards = [
        {
          config: createCard('card-1', 30, 'neutral'),
          component: (
            <BentoCard
              config={createCard('card-1', 30, 'neutral')}
              title="Card 1"
            >
              <div>Content 1</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-2', 70, 'hot'),
          component: (
            <BentoCard config={createCard('card-2', 70, 'hot')} title="Card 2">
              <div>Content 2</div>
            </BentoCard>
          ),
        },
      ];

      const { container, rerender } = render(
        <BentoGrid cards={initialCards} />
      );

      let items = container.querySelectorAll('.bento-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Content 2'); // Higher priority first

      // Update priorities (swap them)
      const updatedCards = [
        {
          config: createCard('card-1', 80, 'hot'),
          component: (
            <BentoCard config={createCard('card-1', 80, 'hot')} title="Card 1">
              <div>Content 1</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-2', 40, 'neutral'),
          component: (
            <BentoCard
              config={createCard('card-2', 40, 'neutral')}
              title="Card 2"
            >
              <div>Content 2</div>
            </BentoCard>
          ),
        },
      ];

      rerender(<BentoGrid cards={updatedCards} />);

      items = container.querySelectorAll('.bento-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Content 1'); // Now card-1 has higher priority
    });
  });

  describe('card size variants', () => {
    it('should apply correct size classes', () => {
      const cards = [
        {
          config: createCard('standard', 90, 'hot', 'standard'),
          component: (
            <BentoCard
              config={createCard('standard', 90, 'hot', 'standard')}
              title="Standard"
            >
              <div>Standard</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('wide', 80, 'hot', 'wide'),
          component: (
            <BentoCard
              config={createCard('wide', 80, 'hot', 'wide')}
              title="Wide"
            >
              <div>Wide</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('tall', 70, 'hot', 'tall'),
          component: (
            <BentoCard
              config={createCard('tall', 70, 'hot', 'tall')}
              title="Tall"
            >
              <div>Tall</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('hero', 60, 'warm', 'hero'),
          component: (
            <BentoCard
              config={createCard('hero', 60, 'warm', 'hero')}
              title="Hero"
            >
              <div>Hero</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const wrappers = container.querySelectorAll('.bento-grid__card-wrapper');
      expect(wrappers[0]).toHaveClass('bento-grid__card--standard');
      expect(wrappers[1]).toHaveClass('bento-grid__card--wide');
      expect(wrappers[2]).toHaveClass('bento-grid__card--tall');
      expect(wrappers[3]).toHaveClass('bento-grid__card--hero');
    });
  });

  describe('grid configuration', () => {
    it('should apply custom column count', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} columns={6} />);

      const grid = container.querySelector('.bento-grid') as HTMLElement;
      const style = grid?.style;

      expect(style.getPropertyValue('--bento-columns-desktop')).toBe('6');
    });

    it('should apply custom gap multiplier', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} gap={5} />);

      const grid = container.querySelector('.bento-grid') as HTMLElement;
      const style = grid?.style;

      expect(style.getPropertyValue('--bento-gap')).toBe(
        'calc(var(--spacing-unit) * 5)'
      );
    });

    it('should apply custom animation duration', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(
        <BentoGrid cards={cards} animationDuration={500} />
      );

      const grid = container.querySelector('.bento-grid') as HTMLElement;
      const style = grid?.style;

      expect(style.getPropertyValue('--bento-transition-duration')).toBe(
        '500ms'
      );
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations', async () => {
      const cards = [
        {
          config: createCard('card-1', 90, 'hot'),
          component: (
            <BentoCard config={createCard('card-1', 90, 'hot')} title="Card 1">
              <div>Content 1</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <BentoCard
              config={createCard('card-2', 50, 'warm')}
              title="Card 2"
            >
              <div>Content 2</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have role="list" on grid', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      render(<BentoGrid cards={cards} />);

      const list = screen.getByRole('list');
      expect(list).toBeInTheDocument();
      expect(list).toHaveClass('bento-grid');
    });

    it('should have role="listitem" on card wrappers', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-1', 75, 'hot')}
              title="Card 1"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <BentoCard
              config={createCard('card-2', 50, 'warm')}
              title="Card 2"
            >
              <div>Content</div>
            </BentoCard>
          ),
        },
      ];

      render(<BentoGrid cards={cards} />);

      const list = screen.getByRole('list');
      const items = within(list).getAllByRole('listitem');

      expect(items).toHaveLength(2);
    });
  });

  describe('edge cases', () => {
    it('should handle cards with priority 0', () => {
      const cards = [
        {
          config: createCard('card-zero', 0, 'cool'),
          component: (
            <BentoCard
              config={createCard('card-zero', 0, 'cool')}
              title="Zero Priority"
            >
              <div>Zero</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-low', 10, 'cool'),
          component: (
            <BentoCard
              config={createCard('card-low', 10, 'cool')}
              title="Low Priority"
            >
              <div>Low</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const items = container.querySelectorAll('.bento-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Low'); // 10 > 0
      expect(items[1]).toHaveTextContent('Zero');
    });

    it('should handle cards with priority 100', () => {
      const cards = [
        {
          config: createCard('card-max', 100, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-max', 100, 'hot')}
              title="Max Priority"
            >
              <div>Max</div>
            </BentoCard>
          ),
        },
        {
          config: createCard('card-high', 95, 'hot'),
          component: (
            <BentoCard
              config={createCard('card-high', 95, 'hot')}
              title="High Priority"
            >
              <div>High</div>
            </BentoCard>
          ),
        },
      ];

      const { container } = render(<BentoGrid cards={cards} />);

      const items = container.querySelectorAll('.bento-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Max'); // 100 > 95
      expect(items[1]).toHaveTextContent('High');
    });
  });
});
