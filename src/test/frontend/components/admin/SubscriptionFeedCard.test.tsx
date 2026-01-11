/**
 * Tests for SubscriptionFeedCard component.
 *
 * Tests the subscription feed card including:
 * - Connection state display
 * - Last message time
 * - Reconnect attempt alerts
 * - Accessibility
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import SubscriptionFeedCard from 'Frontend/components/admin/SubscriptionFeedCard';
import type SubscriptionFeedStatus from 'Frontend/generated/io/nextskip/common/admin/SubscriptionFeedStatus';

function createMockFeed(overrides: Partial<SubscriptionFeedStatus> = {}): SubscriptionFeedStatus {
  return {
    name: 'Test Feed',
    type: 'SUBSCRIPTION',
    healthStatus: 'HEALTHY',
    connectionState: 'CONNECTED',
    lastMessageTime: new Date(Date.now() - 30 * 1000).toISOString(), // 30 seconds ago
    consecutiveReconnectAttempts: 0,
    ...overrides,
  };
}

describe('SubscriptionFeedCard', () => {
  describe('basic rendering', () => {
    it('should render feed name', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ name: 'PSKReporter MQTT' })} />);

      expect(screen.getByRole('heading', { name: 'PSKReporter MQTT' })).toBeInTheDocument();
    });

    it('should render podcasts icon', () => {
      render(<SubscriptionFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('podcasts')).toBeInTheDocument();
    });

    it('should display health status', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ healthStatus: 'DEGRADED' })} />);

      expect(screen.getByRole('status', { name: 'Health status: Degraded' })).toBeInTheDocument();
    });

    it('should show real-time subscription label', () => {
      render(<SubscriptionFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('Real-time subscription')).toBeInTheDocument();
    });
  });

  describe('connection state', () => {
    it('should display CONNECTED state', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ connectionState: 'CONNECTED' })} />);

      expect(screen.getByText('Connected')).toBeInTheDocument();
      expect(screen.getByText('wifi')).toBeInTheDocument();
    });

    it('should display DISCONNECTED state', () => {
      render(
        <SubscriptionFeedCard feed={createMockFeed({ connectionState: 'DISCONNECTED', healthStatus: 'UNHEALTHY' })} />
      );

      expect(screen.getByText('Disconnected')).toBeInTheDocument();
      expect(screen.getByText('wifi_off')).toBeInTheDocument();
    });

    it('should display RECONNECTING state', () => {
      render(
        <SubscriptionFeedCard feed={createMockFeed({ connectionState: 'RECONNECTING', healthStatus: 'DEGRADED' })} />
      );

      expect(screen.getByText('Reconnecting')).toBeInTheDocument();
      expect(screen.getByText('sync')).toBeInTheDocument();
    });

    it('should display STALE state', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ connectionState: 'STALE', healthStatus: 'DEGRADED' })} />);

      expect(screen.getByText('Stale')).toBeInTheDocument();
      expect(screen.getByText('wifi_tethering_error')).toBeInTheDocument();
    });

    it('should apply correct CSS class for CONNECTED state', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ connectionState: 'CONNECTED' })} />);

      const connectionBadge = screen.getByText('Connected').closest('.feed-card-connection');
      expect(connectionBadge).toHaveClass('feed-card-connection--connected');
    });

    it('should apply correct CSS class for DISCONNECTED state', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ connectionState: 'DISCONNECTED' })} />);

      const connectionBadge = screen.getByText('Disconnected').closest('.feed-card-connection');
      expect(connectionBadge).toHaveClass('feed-card-connection--disconnected');
    });
  });

  describe('last message time', () => {
    it('should display last message time', () => {
      render(<SubscriptionFeedCard feed={createMockFeed()} />);

      expect(screen.getByText('Last Message')).toBeInTheDocument();
      expect(screen.getByText('Just now')).toBeInTheDocument();
    });

    it('should display "Never" when lastMessageTime is null', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ lastMessageTime: null })} />);

      expect(screen.getByText('Never')).toBeInTheDocument();
    });

    it('should display time in minutes when older than 1 minute', () => {
      const fiveMinutesAgo = new Date(Date.now() - 5 * 60 * 1000).toISOString();
      render(<SubscriptionFeedCard feed={createMockFeed({ lastMessageTime: fiveMinutesAgo })} />);

      expect(screen.getByText('5m ago')).toBeInTheDocument();
    });

    it('should display time in hours when older than 1 hour', () => {
      const twoHoursAgo = new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString();
      render(<SubscriptionFeedCard feed={createMockFeed({ lastMessageTime: twoHoursAgo })} />);

      expect(screen.getByText('2h ago')).toBeInTheDocument();
    });
  });

  describe('reconnect alerts', () => {
    it('should not display alert when no reconnect attempts', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ consecutiveReconnectAttempts: 0 })} />);

      expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('should display alert for single reconnect attempt', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ consecutiveReconnectAttempts: 1 })} />);

      expect(screen.getByRole('alert')).toBeInTheDocument();
      expect(screen.getByText('1 reconnect attempt')).toBeInTheDocument();
    });

    it('should display alert for multiple reconnect attempts', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ consecutiveReconnectAttempts: 3 })} />);

      expect(screen.getByText('3 reconnect attempts')).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have accessible feed card label', () => {
      render(<SubscriptionFeedCard feed={createMockFeed({ name: 'PSKReporter MQTT' })} />);

      expect(screen.getByRole('article', { name: 'PSKReporter MQTT feed status' })).toBeInTheDocument();
    });
  });
});
