/**
 * Component tests for ActivityGrid
 *
 * Tests grid rendering, card sorting by priority, responsive behavior,
 * and accessibility
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import ActivityGrid from 'Frontend/components/activity/ActivityGrid';
import ActivityCard from 'Frontend/components/activity/ActivityCard';
import type { ActivityCardConfig } from 'Frontend/components/activity';
import { createMockActivityCardConfig } from '../../fixtures/mockFactories';

expect.extend(toHaveNoViolations);

// Wrapper to maintain existing test API while using shared factory
const createCard = (
  id: string,
  priority: number,
  hotness: 'hot' | 'warm' | 'neutral' | 'cool',
  size: '1x1' | '2x1' | '1x2' | '2x2' = '1x1',
): ActivityCardConfig => createMockActivityCardConfig({ id, priority, hotness, size });

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

  describe('responsive frame width calculation', () => {
    const originalInnerWidth = window.innerWidth;

    beforeEach(() => {
      // Reset any mocks
      vi.restoreAllMocks();
    });

    afterEach(() => {
      // Restore original window.innerWidth
      Object.defineProperty(window, 'innerWidth', {
        value: originalInnerWidth,
        writable: true,
        configurable: true,
      });
    });

    const setViewportWidth = (width: number) => {
      Object.defineProperty(window, 'innerWidth', {
        value: width,
        writable: true,
        configurable: true,
      });
      window.dispatchEvent(new Event('resize'));
    };

    it('should render at mobile viewport width (<=768px)', () => {
      setViewportWidth(375);

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

      const { container } = render(<ActivityGrid cards={cards} />);

      // Grid should render without error at mobile width
      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
      expect(screen.getByText('Card 1')).toBeInTheDocument();
    });

    it('should render at tablet viewport width (<=1024px)', () => {
      setViewportWidth(1024);

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

      const { container } = render(<ActivityGrid cards={cards} />);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
    });

    it('should render at wide desktop viewport width (>=1400px)', () => {
      setViewportWidth(1400);

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

      const { container } = render(<ActivityGrid cards={cards} />);

      // Grid should render at wide desktop width (6 columns)
      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
      expect(screen.getByText('Card 1')).toBeInTheDocument();
    });

    it('should use fallback width calculation when container not found', () => {
      setViewportWidth(500);

      // Mock querySelector to return null for .activity-grid initially
      const originalQuerySelector = document.querySelector.bind(document);
      let callCount = 0;
      vi.spyOn(document, 'querySelector').mockImplementation((selector: string) => {
        if (selector === '.activity-grid' && callCount < 1) {
          callCount++;
          return null;
        }
        return originalQuerySelector(selector);
      });

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

      const { container } = render(<ActivityGrid cards={cards} />);

      // Should still render using fallback calculation
      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
    });

    it('should recalculate width on window resize', () => {
      setViewportWidth(1280);

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

      const { container } = render(<ActivityGrid cards={cards} />);

      // Trigger resize to different width
      setViewportWidth(768);

      const grid = container.querySelector('.activity-grid');
      expect(grid).toBeInTheDocument();
    });
  });
});
