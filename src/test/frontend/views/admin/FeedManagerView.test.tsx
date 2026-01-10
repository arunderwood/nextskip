/**
 * Tests for FeedManagerView component.
 *
 * Tests the feed manager view including:
 * - Loading state
 * - Error state
 * - Empty state
 * - Feed status display grouped by module
 * - Health statistics
 * - Force refresh functionality
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FeedManagerView from 'Frontend/views/admin/feeds';
import { AdminFeedEndpoint } from 'Frontend/generated/endpoints';
import type FeedStatusResponse from 'Frontend/generated/io/nextskip/admin/api/FeedStatusResponse';
import type TriggerRefreshResult from 'Frontend/generated/io/nextskip/common/admin/TriggerRefreshResult';

// Mock the AdminFeedEndpoint
vi.mock('Frontend/generated/endpoints', () => ({
  AdminFeedEndpoint: {
    getFeedStatuses: vi.fn(),
    triggerFeedRefresh: vi.fn(),
  },
}));

const mockGetFeedStatuses = AdminFeedEndpoint.getFeedStatuses as ReturnType<typeof vi.fn>;
const mockTriggerFeedRefresh = AdminFeedEndpoint.triggerFeedRefresh as ReturnType<typeof vi.fn>;

function createMockResponse(overrides: Partial<FeedStatusResponse> = {}): FeedStatusResponse {
  return {
    modules: [
      {
        moduleName: 'propagation',
        scheduledFeeds: [
          {
            name: 'NOAA SWPC',
            type: 'SCHEDULED',
            healthStatus: 'HEALTHY',
            lastRefreshTime: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
            nextRefreshTime: new Date(Date.now() + 5 * 60 * 1000).toISOString(),
            isCurrentlyRefreshing: false,
            consecutiveFailures: 0,
            lastFailureTime: null,
            refreshIntervalSeconds: 300,
          },
        ],
        subscriptionFeeds: [],
      },
      {
        moduleName: 'spots',
        scheduledFeeds: [],
        subscriptionFeeds: [
          {
            name: 'PSKReporter MQTT',
            type: 'SUBSCRIPTION',
            healthStatus: 'HEALTHY',
            connectionState: 'CONNECTED',
            lastMessageTime: new Date(Date.now() - 30 * 1000).toISOString(),
            consecutiveReconnectAttempts: 0,
          },
        ],
      },
    ],
    timestamp: new Date().toISOString(),
    totalFeeds: 2,
    ...overrides,
  };
}

describe('FeedManagerView', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('loading state', () => {
    it('should show loading spinner initially', () => {
      mockGetFeedStatuses.mockReturnValue(new Promise(() => {})); // Never resolves

      render(<FeedManagerView />);

      expect(screen.getByText('Loading feed statuses...')).toBeInTheDocument();
    });
  });

  describe('error state', () => {
    it('should display error message when fetch fails', async () => {
      mockGetFeedStatuses.mockRejectedValueOnce(new Error('Network error'));

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByText('Network error')).toBeInTheDocument();
      });
    });

    it('should show Try Again button on error', async () => {
      mockGetFeedStatuses.mockRejectedValueOnce(new Error('Network error'));

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Try Again' })).toBeInTheDocument();
      });
    });

    it('should retry fetch when Try Again is clicked', async () => {
      const user = userEvent.setup();
      mockGetFeedStatuses.mockRejectedValueOnce(new Error('Network error'));
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('button', { name: 'Try Again' })).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: 'Try Again' }));

      await waitFor(() => {
        expect(screen.getByText('Feed Manager')).toBeInTheDocument();
      });

      expect(mockGetFeedStatuses).toHaveBeenCalledTimes(2);
    });
  });

  describe('empty state', () => {
    it('should show empty message when no feeds', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce({
        modules: [],
        timestamp: new Date().toISOString(),
        totalFeeds: 0,
      });

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByText('No feeds configured')).toBeInTheDocument();
      });
    });
  });

  describe('feed display', () => {
    it('should render page header', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'Feed Manager', level: 1 })).toBeInTheDocument();
      });
    });

    it('should display modules grouped by name', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'propagation', level: 2 })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'spots', level: 2 })).toBeInTheDocument();
      });
    });

    it('should display feed cards', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'PSKReporter MQTT' })).toBeInTheDocument();
      });
    });

    it('should display feed count per module', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        // Both modules have 1 feed each
        const feedCounts = screen.getAllByText('1 feed');
        expect(feedCounts).toHaveLength(2);
      });
    });
  });

  describe('health statistics', () => {
    it('should display total feeds count', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByText('Total Feeds')).toBeInTheDocument();
      });

      // Stats section shows both total (2) and healthy (2) - verify via labels
      const statLabels = screen.getAllByText(/Total Feeds|Healthy/);
      expect(statLabels.length).toBeGreaterThanOrEqual(2);
    });

    it('should display healthy feeds count', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        // "Healthy" appears in both stats section and feed cards
        const healthyElements = screen.getAllByText('Healthy');
        expect(healthyElements.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('should display degraded count when present', async () => {
      const response = createMockResponse();
      response.modules[0].scheduledFeeds[0].healthStatus = 'DEGRADED';

      mockGetFeedStatuses.mockResolvedValueOnce(response);

      render(<FeedManagerView />);

      await waitFor(() => {
        // Both stats section and feed card will show "Degraded"
        const degradedElements = screen.getAllByText('Degraded');
        expect(degradedElements.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('should display unhealthy count when present', async () => {
      const response = createMockResponse();
      response.modules[0].scheduledFeeds[0].healthStatus = 'UNHEALTHY';

      mockGetFeedStatuses.mockResolvedValueOnce(response);

      render(<FeedManagerView />);

      await waitFor(() => {
        // Both stats section and feed card will show "Unhealthy"
        const unhealthyElements = screen.getAllByText('Unhealthy');
        expect(unhealthyElements.length).toBeGreaterThanOrEqual(1);
      });
    });

    it('should not display degraded count when zero', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByText('Feed Manager')).toBeInTheDocument();
      });

      expect(screen.queryByText('Degraded')).not.toBeInTheDocument();
    });
  });

  describe('force refresh', () => {
    it('should trigger refresh when refresh button is clicked', async () => {
      const user = userEvent.setup();
      mockGetFeedStatuses.mockResolvedValue(createMockResponse());

      const successResult: TriggerRefreshResult = {
        success: true,
        message: 'Refresh scheduled',
        feedName: 'NOAA SWPC',
        scheduledFor: new Date().toISOString(),
      };
      mockTriggerFeedRefresh.mockResolvedValueOnce(successResult);

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
      });

      const refreshButton = screen.getByRole('button', { name: /trigger refresh for noaa swpc/i });
      await user.click(refreshButton);

      expect(mockTriggerFeedRefresh).toHaveBeenCalledWith('NOAA SWPC');
    });

    it('should refetch statuses after successful refresh', async () => {
      const user = userEvent.setup();
      mockGetFeedStatuses.mockResolvedValue(createMockResponse());

      const successResult: TriggerRefreshResult = {
        success: true,
        message: 'Refresh scheduled',
        feedName: 'NOAA SWPC',
        scheduledFor: new Date().toISOString(),
      };
      mockTriggerFeedRefresh.mockResolvedValueOnce(successResult);

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
      });

      // Clear call count after initial load
      mockGetFeedStatuses.mockClear();

      const refreshButton = screen.getByRole('button', { name: /trigger refresh for noaa swpc/i });
      await user.click(refreshButton);

      await waitFor(() => {
        expect(mockGetFeedStatuses).toHaveBeenCalled();
      });
    });

    it('should handle refresh failure gracefully', async () => {
      const user = userEvent.setup();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      mockGetFeedStatuses.mockResolvedValue(createMockResponse());

      const failureResult: TriggerRefreshResult = {
        success: false,
        message: 'Feed not found',
        feedName: 'NOAA SWPC',
        scheduledFor: null,
      };
      mockTriggerFeedRefresh.mockResolvedValueOnce(failureResult);

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
      });

      const refreshButton = screen.getByRole('button', { name: /trigger refresh for noaa swpc/i });
      await user.click(refreshButton);

      await waitFor(() => {
        expect(consoleSpy).toHaveBeenCalledWith('Refresh failed:', 'Feed not found');
      });

      consoleSpy.mockRestore();
    });

    it('should handle refresh error gracefully', async () => {
      const user = userEvent.setup();
      const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      mockGetFeedStatuses.mockResolvedValue(createMockResponse());
      mockTriggerFeedRefresh.mockRejectedValueOnce(new Error('Network error'));

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByRole('heading', { name: 'NOAA SWPC' })).toBeInTheDocument();
      });

      const refreshButton = screen.getByRole('button', { name: /trigger refresh for noaa swpc/i });
      await user.click(refreshButton);

      await waitFor(() => {
        expect(consoleSpy).toHaveBeenCalled();
      });

      consoleSpy.mockRestore();
    });
  });

  describe('timestamp', () => {
    it('should display last updated timestamp', async () => {
      mockGetFeedStatuses.mockResolvedValueOnce(createMockResponse());

      render(<FeedManagerView />);

      await waitFor(() => {
        expect(screen.getByText(/Last updated:/)).toBeInTheDocument();
      });
    });
  });
});
