/**
 * Component tests for ActivityGrid
 *
 * Tests grid rendering, card sorting by priority, responsive behavior,
 * and accessibility
 */

import { describe, it, expect } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import ActivityGrid from 'Frontend/components/activity/ActivityGrid';
import ActivityCard from 'Frontend/components/activity/ActivityCard';
import type { ActivityCardConfig } from 'Frontend/components/activity';

expect.extend(toHaveNoViolations);

const createCard = (
  id: string,
  priority: number,
  hotness: 'hot' | 'warm' | 'neutral' | 'cool',
  size: 'standard' | 'wide' | 'tall' | 'hero' = 'standard',
): ActivityCardConfig => ({
  id,
  type: 'solar-indices',
  size,
  priority,
  hotness,
});

describe('ActivityGrid', () => {
  describe('rendering', () => {
    it('should render empty grid with no cards', () => {
      const { container } = render(<ActivityGrid cards={[]} />);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();

      // Masonry grid creates a wrapper, so check that no card wrappers exist
      const cardWrappers = container.querySelectorAll('.activity-grid__card-wrapper');
      expect(cardWrappers).toHaveLength(0);
    });

    it('should render single card', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content 1</div>
            </ActivityCard>
          ),
        },
      ];

      render(<ActivityGrid cards={cards} />);

      expect(screen.getByText('Card 1')).toBeInTheDocument();
      expect(screen.getByText('Content 1')).toBeInTheDocument();
    });

    it('should render multiple cards', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content 1</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('card-2', 50, 'warm')} title="Card 2">
              <div>Content 2</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-3', 25, 'neutral'),
          component: (
            <ActivityCard config={createCard('card-3', 25, 'neutral')} title="Card 3">
              <div>Content 3</div>
            </ActivityCard>
          ),
        },
      ];

      render(<ActivityGrid cards={cards} />);

      expect(screen.getByText('Card 1')).toBeInTheDocument();
      expect(screen.getByText('Card 2')).toBeInTheDocument();
      expect(screen.getByText('Card 3')).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} className="custom-grid" />);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toHaveClass('custom-grid');
    });
  });

  describe('priority-based sorting', () => {
    it('should sort cards by priority in descending order', () => {
      const cards = [
        {
          config: createCard('low', 20, 'neutral'),
          component: (
            <ActivityCard config={createCard('low', 20, 'neutral')} title="Low">
              <div>Low priority</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('high', 90, 'hot'),
          component: (
            <ActivityCard config={createCard('high', 90, 'hot')} title="High">
              <div>High priority</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('medium', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('medium', 50, 'warm')} title="Medium">
              <div>Medium priority</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} />);

      const grid = container.querySelector('.activity-grid');
      const items = grid?.querySelectorAll('.activity-grid__card-wrapper');

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
            <ActivityCard config={createCard('card-a', 50, 'warm')} title="A">
              <div>Card A</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-b', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('card-b', 50, 'warm')} title="B">
              <div>Card B</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-c', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('card-c', 50, 'warm')} title="C">
              <div>Card C</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} />);

      const grid = container.querySelector('.activity-grid');
      const items = grid?.querySelectorAll('.activity-grid__card-wrapper');

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
            <ActivityCard config={createCard('card-1', 30, 'neutral')} title="Card 1">
              <div>Content 1</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-2', 70, 'hot'),
          component: (
            <ActivityCard config={createCard('card-2', 70, 'hot')} title="Card 2">
              <div>Content 2</div>
            </ActivityCard>
          ),
        },
      ];

      const { container, rerender } = render(<ActivityGrid cards={initialCards} />);

      let items = container.querySelectorAll('.activity-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Content 2'); // Higher priority first

      // Update priorities (swap them)
      const updatedCards = [
        {
          config: createCard('card-1', 80, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 80, 'hot')} title="Card 1">
              <div>Content 1</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-2', 40, 'neutral'),
          component: (
            <ActivityCard config={createCard('card-2', 40, 'neutral')} title="Card 2">
              <div>Content 2</div>
            </ActivityCard>
          ),
        },
      ];

      rerender(<ActivityGrid cards={updatedCards} />);

      items = container.querySelectorAll('.activity-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Content 1'); // Now card-1 has higher priority
    });
  });

  describe('grid configuration', () => {
    it('should render with custom column count prop', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      // Just verify it renders without error - masonry handles columns internally
      const { container } = render(<ActivityGrid cards={cards} columns={6} />);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
      expect(screen.getByText('Card 1')).toBeInTheDocument();
    });

    it('should render with custom gap prop', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      // Just verify it renders without error - masonry handles gap internally
      const { container } = render(<ActivityGrid cards={cards} gap={5} />);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
      expect(screen.getByText('Card 1')).toBeInTheDocument();
    });

    it('should apply custom animation duration', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} animationDuration={500} />);

      const grid = container.querySelector('.activity-grid') as HTMLElement;
      const style = grid?.style;

      expect(style.getPropertyValue('--activity-transition-duration')).toBe('500ms');
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations', async () => {
      const cards = [
        {
          config: createCard('card-1', 90, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 90, 'hot')} title="Card 1">
              <div>Content 1</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('card-2', 50, 'warm')} title="Card 2">
              <div>Content 2</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have role="list" on grid', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      render(<ActivityGrid cards={cards} />);

      const list = screen.getByRole('list');
      expect(list).toBeInTheDocument();
      expect(list).toHaveClass('activity-grid');
    });

    it('should have role="listitem" on card wrappers', () => {
      const cards = [
        {
          config: createCard('card-1', 75, 'hot'),
          component: (
            <ActivityCard config={createCard('card-1', 75, 'hot')} title="Card 1">
              <div>Content</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-2', 50, 'warm'),
          component: (
            <ActivityCard config={createCard('card-2', 50, 'warm')} title="Card 2">
              <div>Content</div>
            </ActivityCard>
          ),
        },
      ];

      render(<ActivityGrid cards={cards} />);

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
            <ActivityCard config={createCard('card-zero', 0, 'cool')} title="Zero Priority">
              <div>Zero</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-low', 10, 'cool'),
          component: (
            <ActivityCard config={createCard('card-low', 10, 'cool')} title="Low Priority">
              <div>Low</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} />);

      const items = container.querySelectorAll('.activity-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Low'); // 10 > 0
      expect(items[1]).toHaveTextContent('Zero');
    });

    it('should handle cards with priority 100', () => {
      const cards = [
        {
          config: createCard('card-max', 100, 'hot'),
          component: (
            <ActivityCard config={createCard('card-max', 100, 'hot')} title="Max Priority">
              <div>Max</div>
            </ActivityCard>
          ),
        },
        {
          config: createCard('card-high', 95, 'hot'),
          component: (
            <ActivityCard config={createCard('card-high', 95, 'hot')} title="High Priority">
              <div>High</div>
            </ActivityCard>
          ),
        },
      ];

      const { container } = render(<ActivityGrid cards={cards} />);

      const items = container.querySelectorAll('.activity-grid__card-wrapper');
      expect(items[0]).toHaveTextContent('Max'); // 100 > 95
      expect(items[1]).toHaveTextContent('High');
    });
  });
});
