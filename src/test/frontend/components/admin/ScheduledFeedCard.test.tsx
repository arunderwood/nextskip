/**
 * Tests for ScheduledFeedCard component.
 *
 * Tests the scheduled feed card including:
 * - Feed status display
 * - Time formatting (last refresh, next refresh)
 * - Failure alerts
 * - Refresh button functionality
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ScheduledFeedCard from 'Frontend/components/admin/ScheduledFeedCard';
import type ScheduledFeedStatus from 'Frontend/generated/io/nextskip/common/admin/ScheduledFeedStatus';

function createMockFeed(overrides: Partial<ScheduledFeedStatus> = {}): ScheduledFeedStatus {
  return {
    name: 'Test Feed',
    type: 'SCHEDULED',
    healthStatus: 'HEALTHY',
    lastRefreshTime: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5 minutes ago
    nextRefreshTime: new Date(Date.now() + 5 * 60 * 1000).toISOString(), // 5 minutes from now
    isCurrentlyRefreshing: false,
    consecutiveFailures: 0,
    lastFailureTime: null,
    refreshIntervalSeconds: 300, // 5 minutes
    ...overrides,
  };
}

describe('ScheduledFeedCard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('basic rendering', () => {
    it('should render feed name', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ name: 'NOAA SWPC' })} />);

      expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
    });

    it('should render schedule icon', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('schedule')).toBeInTheDocument();
    });

    it('should display health status', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ healthStatus: 'DEGRADED' })} />);

      expect(screen.getByRole('status', { name: 'Health status: Degraded' })).toBeInTheDocument();
    });
  });

  describe('time display', () => {
    it('should display last refresh time', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('Last Refresh')).toBeInTheDocument();
      // Time display should show "5m ago" or similar
      expect(screen.getByText(/ago|Just now/i)).toBeInTheDocument();
    });

    it('should display next refresh time', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('Next Refresh')).toBeInTheDocument();
      // Time display should show "In 5m" or similar
      expect(screen.getByText(/In \d+m/i)).toBeInTheDocument();
    });

    it('should display "Never" when lastRefreshTime is null', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ lastRefreshTime: null })} />);

      expect(screen.getByText('Never')).toBeInTheDocument();
    });

    it('should display "Refreshing..." when currently refreshing', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ isCurrentlyRefreshing: true })} />);

      expect(screen.getByText('Refreshing...')).toBeInTheDocument();
    });
  });

  describe('refresh interval', () => {
    it('should display refresh interval in minutes', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ refreshIntervalSeconds: 300 })} />);

      expect(screen.getByText('Every 5m')).toBeInTheDocument();
    });

    it('should display refresh interval in hours', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ refreshIntervalSeconds: 3600 })} />);

      expect(screen.getByText('Every 1h')).toBeInTheDocument();
    });

    it('should display refresh interval in seconds for short intervals', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ refreshIntervalSeconds: 30 })} />);

      expect(screen.getByText('Every 30s')).toBeInTheDocument();
    });
  });

  describe('failure alerts', () => {
    it('should not display alert when no failures', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ consecutiveFailures: 0 })} />);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('should display alert for single failure', () => {
      const failureTime = new Date(Date.now() - 2 * 60 * 1000).toISOString();
      render(
        <ScheduledFeedCard
          feed={createMockFeed({
            consecutiveFailures: 1,
            lastFailureTime: failureTime,
          })}
        />
      );

      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText(/1 consecutive failure/i)).toBeInTheDocument();
    });

    it('should display alert for multiple failures', () => {
      const failureTime = new Date(Date.now() - 2 * 60 * 1000).toISOString();
      render(
        <ScheduledFeedCard
          feed={createMockFeed({
            consecutiveFailures: 3,
            lastFailureTime: failureTime,
          })}
        />
      );

      expect(screen.getByText(/3 consecutive failures/i)).toBeInTheDocument();
    });
  });

  describe('refresh button', () => {
    it('should render refresh button', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} />);

      expect(screen.getByRole('button', { name: /refresh/i })).toBeInTheDocument();
    });

    it('should call onRefresh when clicked', async () => {
      const user = userEvent.setup();
      const onRefresh = vi.fn();
      render(<ScheduledFeedCard feed={createMockFeed({ name: 'NOAA SWPC' })} onRefresh={onRefresh} />);

      await user.click(screen.getByRole('button', { name: /trigger refresh for noaa swpc/i }));

      expect(onRefresh).toHaveBeenCalledWith('NOAA SWPC');
    });

    it('should disable button when isRefreshing is true', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} isRefreshing />);

      expect(screen.getByRole('button', { name: /refresh/i })).toBeDisabled();
    });

    it('should disable button when feed is currently refreshing', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ isCurrentlyRefreshing: true })} />);

      expect(screen.getByRole('button', { name: /refresh/i })).toBeDisabled();
    });

    it('should show "Refreshing..." text when isRefreshing is true', () => {
      render(<ScheduledFeedCard feed={createMockFeed()} isRefreshing />);

      expect(screen.getByText('Refreshing...')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have accessible button label', () => {
      render(<ScheduledFeedCard feed={createMockFeed({ name: 'Test Feed' })} />);

      expect(screen.getByRole('button', { name: 'Trigger refresh for Test Feed' })).toBeInTheDocument();
    });
  });
});
