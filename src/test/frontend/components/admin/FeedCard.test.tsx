/**
 * Tests for FeedCard component.
 *
 * Tests the base feed card including:
 * - Header rendering (name, icon, status)
 * - Health status display
 * - Body content slot
 * - Footer content slot
 * - Accessibility
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import FeedCard, { FeedCardRow, FeedCardAlert } from 'Frontend/components/admin/FeedCard';

describe('FeedCard', () => {
  describe('header', () => {
    it('should render feed name', () => {
      render(
        <FeedCard name="Test Feed" icon="rss_feed" healthStatus="HEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByRole('heading', { name: 'Test Feed' })).toBeInTheDocument();
    });

    it('should render icon', () => {
      render(
        <FeedCard name="Test Feed" icon="schedule" healthStatus="HEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByText('schedule')).toBeInTheDocument();
    });

    it('should have accessible label for feed card', () => {
      render(
        <FeedCard name="My Feed" icon="rss_feed" healthStatus="HEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByRole('article', { name: 'My Feed feed status' })).toBeInTheDocument();
    });
  });

  describe('health status', () => {
    it('should display healthy status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="HEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByRole('status', { name: 'Health status: Healthy' })).toBeInTheDocument();
      expect(screen.getByText('Healthy')).toBeInTheDocument();
    });

    it('should display degraded status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="DEGRADED">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByRole('status', { name: 'Health status: Degraded' })).toBeInTheDocument();
      expect(screen.getByText('Degraded')).toBeInTheDocument();
    });

    it('should display unhealthy status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="UNHEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      expect(screen.getByRole('status', { name: 'Health status: Unhealthy' })).toBeInTheDocument();
      expect(screen.getByText('Unhealthy')).toBeInTheDocument();
    });

    it('should apply correct CSS class for healthy status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="HEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      const status = screen.getByRole('status');
      expect(status).toHaveClass('feed-card-status--healthy');
    });

    it('should apply correct CSS class for degraded status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="DEGRADED">
          <div>Content</div>
        </FeedCard>
      );

      const status = screen.getByRole('status');
      expect(status).toHaveClass('feed-card-status--degraded');
    });

    it('should apply correct CSS class for unhealthy status', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="UNHEALTHY">
          <div>Content</div>
        </FeedCard>
      );

      const status = screen.getByRole('status');
      expect(status).toHaveClass('feed-card-status--unhealthy');
    });
  });

  describe('content slots', () => {
    it('should render body content', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="HEALTHY">
          <div data-testid="body-content">Body Content</div>
        </FeedCard>
      );

      expect(screen.getByTestId('body-content')).toBeInTheDocument();
    });

    it('should render footer content when provided', () => {
      render(
        <FeedCard
          name="Test"
          icon="rss_feed"
          healthStatus="HEALTHY"
          footer={<div data-testid="footer-content">Footer</div>}
        >
          <div>Body</div>
        </FeedCard>
      );

      expect(screen.getByTestId('footer-content')).toBeInTheDocument();
    });

    it('should not render footer when not provided', () => {
      render(
        <FeedCard name="Test" icon="rss_feed" healthStatus="HEALTHY">
          <div>Body</div>
        </FeedCard>
      );

      expect(screen.queryByRole('contentinfo')).not.toBeInTheDocument();
    });
  });
});

describe('FeedCardRow', () => {
  it('should render label and value', () => {
    render(<FeedCardRow label="Last Refresh" value="5m ago" />);

    expect(screen.getByText('Last Refresh')).toBeInTheDocument();
    expect(screen.getByText('5m ago')).toBeInTheDocument();
  });

  it('should apply muted class when muted is true', () => {
    render(<FeedCardRow label="Status" value="Never" muted />);

    const value = screen.getByText('Never');
    expect(value).toHaveClass('feed-card-value--muted');
  });

  it('should not apply muted class when muted is false', () => {
    render(<FeedCardRow label="Status" value="Active" muted={false} />);

    const value = screen.getByText('Active');
    expect(value).not.toHaveClass('feed-card-value--muted');
  });

  it('should render React node as value', () => {
    render(<FeedCardRow label="Connection" value={<span data-testid="custom-value">Connected</span>} />);

    expect(screen.getByTestId('custom-value')).toBeInTheDocument();
  });
});

describe('FeedCardAlert', () => {
  it('should render alert message', () => {
    render(<FeedCardAlert message="3 consecutive failures" />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('3 consecutive failures')).toBeInTheDocument();
  });

  it('should display warning icon', () => {
    render(<FeedCardAlert message="Error occurred" />);

    expect(screen.getByText('warning')).toBeInTheDocument();
  });
});
