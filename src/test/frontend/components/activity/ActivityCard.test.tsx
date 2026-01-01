/**
 * Component tests for ActivityCard
 *
 * Tests card rendering, hotness variants, accessibility, and user interaction
 */

import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { axe, toHaveNoViolations } from 'jest-axe';
import ActivityCard from 'Frontend/components/activity/ActivityCard';
import type { ActivityCardConfig } from 'Frontend/components/activity';

expect.extend(toHaveNoViolations);

const mockConfig: ActivityCardConfig = {
  id: 'test-card',
  type: 'solar-indices',
  size: '1x1',
  priority: 75,
  hotness: 'hot',
};

describe('ActivityCard', () => {
  describe('rendering', () => {
    it('should render with required props', () => {
      render(
        <ActivityCard config={mockConfig} title="Test Card">
          <div>Test content</div>
        </ActivityCard>,
      );

      expect(screen.getByText('Test Card')).toBeInTheDocument();
      expect(screen.getByText('Test content')).toBeInTheDocument();
    });

    it('should render icon when provided', () => {
      render(
        <ActivityCard config={mockConfig} title="Test Card" icon="☀️">
          <div>Content</div>
        </ActivityCard>,
      );

      expect(screen.getByText('☀️')).toBeInTheDocument();
    });

    it('should render subtitle when provided', () => {
      render(
        <ActivityCard config={mockConfig} title="Test Card" subtitle="Test subtitle">
          <div>Content</div>
        </ActivityCard>,
      );

      expect(screen.getByText('Test subtitle')).toBeInTheDocument();
    });

    it('should render footer when provided', () => {
      render(
        <ActivityCard config={mockConfig} title="Test Card" footer={<div>Footer content</div>}>
          <div>Content</div>
        </ActivityCard>,
      );

      expect(screen.getByText('Footer content')).toBeInTheDocument();
    });

    it('should apply custom className', () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" className="custom-class">
          <div>Content</div>
        </ActivityCard>,
      );

      const card = container.querySelector('.activity-card');
      expect(card).toHaveClass('custom-class');
    });
  });

  describe('card sizes', () => {
    it.each([
      ['1x1', 'activity-card--1x1'],
      ['2x1', 'activity-card--2x1'],
      ['1x2', 'activity-card--1x2'],
      ['2x2', 'activity-card--2x2'],
    ] as const)('should apply %s size class', (size, expectedClass) => {
      const config = { ...mockConfig, size };
      const { container } = render(
        <ActivityCard config={config} title="Test Card">
          <div>Content</div>
        </ActivityCard>,
      );

      const card = container.querySelector('.activity-card');
      expect(card).toHaveClass(expectedClass);
    });
  });

  describe('hotness variants', () => {
    it.each([
      ['hot', 'activity-card--hot', 'Excellent'],
      ['warm', 'activity-card--warm', 'Good'],
      ['neutral', 'activity-card--neutral', 'Moderate'],
      ['cool', 'activity-card--cool', 'Limited'],
    ] as const)('should apply %s hotness class and show %s label', (hotness, expectedClass, expectedLabel) => {
      const config = { ...mockConfig, hotness };
      const { container } = render(
        <ActivityCard config={config} title="Test Card">
          <div>Content</div>
        </ActivityCard>,
      );

      const card = container.querySelector('.activity-card');
      expect(card).toHaveClass(expectedClass);
      expect(screen.getByText(expectedLabel)).toBeInTheDocument();
    });
  });

  describe('interactive behavior', () => {
    it('should render as article when not interactive', () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card">
          <div>Content</div>
        </ActivityCard>,
      );

      const article = container.querySelector('article.activity-card');
      expect(article).toBeInTheDocument();
    });

    it('should render as button when onClick is provided', () => {
      const handleClick = vi.fn();
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </ActivityCard>,
      );

      const button = container.querySelector('button.activity-card');
      expect(button).toBeInTheDocument();
      expect(button).toHaveClass('activity-card--interactive');
    });

    it('should call onClick when clicked', async () => {
      const user = userEvent.setup();
      const handleClick = vi.fn();

      render(
        <ActivityCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </ActivityCard>,
      );

      const button = screen.getByRole('button', { name: 'Test Card' });
      await user.click(button);

      expect(handleClick).toHaveBeenCalledTimes(1);
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations (non-interactive)', async () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" subtitle="Subtitle">
          <div>Content</div>
        </ActivityCard>,
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations (interactive)', async () => {
      const handleClick = vi.fn();
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" onClick={handleClick}>
          <div>Content</div>
        </ActivityCard>,
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should use custom aria-label when provided', () => {
      render(
        <ActivityCard config={mockConfig} title="Test Card" ariaLabel="Custom label">
          <div>Content</div>
        </ActivityCard>,
      );

      const article = screen.getByLabelText('Custom label');
      expect(article).toBeInTheDocument();
    });

    it('should fall back to title for aria-label', () => {
      render(
        <ActivityCard config={mockConfig} title="Default Label">
          <div>Content</div>
        </ActivityCard>,
      );

      const article = screen.getByLabelText('Default Label');
      expect(article).toBeInTheDocument();
    });

    it('should have proper heading hierarchy', () => {
      render(
        <ActivityCard config={mockConfig} title="Card Title">
          <div>Content</div>
        </ActivityCard>,
      );

      const heading = screen.getByRole('heading', { name: 'Card Title' });
      expect(heading.tagName).toBe('H3');
    });
  });

  describe('hotness indicator', () => {
    it('should show hotness indicator for all levels', () => {
      const { container, rerender } = render(
        <ActivityCard config={{ ...mockConfig, hotness: 'hot' }} title="Test">
          <div>Content</div>
        </ActivityCard>,
      );

      let indicator = container.querySelector('.activity-card__hotness-indicator--hot');
      expect(indicator).toBeInTheDocument();
      expect(indicator).toHaveTextContent('Excellent');

      rerender(
        <ActivityCard config={{ ...mockConfig, hotness: 'warm' }} title="Test">
          <div>Content</div>
        </ActivityCard>,
      );
      indicator = container.querySelector('.activity-card__hotness-indicator--warm');
      expect(indicator).toHaveTextContent('Good');

      rerender(
        <ActivityCard config={{ ...mockConfig, hotness: 'neutral' }} title="Test">
          <div>Content</div>
        </ActivityCard>,
      );
      indicator = container.querySelector('.activity-card__hotness-indicator--neutral');
      expect(indicator).toHaveTextContent('Moderate');

      rerender(
        <ActivityCard config={{ ...mockConfig, hotness: 'cool' }} title="Test">
          <div>Content</div>
        </ActivityCard>,
      );
      indicator = container.querySelector('.activity-card__hotness-indicator--cool');
      expect(indicator).toHaveTextContent('Limited');
    });
  });

  describe('card structure', () => {
    it('should have header, content, and footer sections', () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" footer={<div>Footer</div>}>
          <div>Content</div>
        </ActivityCard>,
      );

      expect(container.querySelector('.activity-card__header')).toBeInTheDocument();
      expect(container.querySelector('.activity-card__content')).toBeInTheDocument();
      expect(container.querySelector('.activity-card__footer')).toBeInTheDocument();
    });

    it('should not render footer when not provided', () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card">
          <div>Content</div>
        </ActivityCard>,
      );

      expect(container.querySelector('.activity-card__footer')).not.toBeInTheDocument();
    });

    it('should render header with icon and title', () => {
      const { container } = render(
        <ActivityCard config={mockConfig} title="Test Card" icon="📊">
          <div>Content</div>
        </ActivityCard>,
      );

      const header = container.querySelector('.activity-card__header');
      expect(header).toBeInTheDocument();
      expect(header).toHaveTextContent('📊');
      expect(header).toHaveTextContent('Test Card');
    });
  });
});
