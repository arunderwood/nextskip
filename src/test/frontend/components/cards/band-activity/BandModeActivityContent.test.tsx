import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { BandModeActivityContent } from 'Frontend/components/cards/band-activity/BandModeActivityContent';
import type { ModeConfig } from 'Frontend/config/modeRegistry';
import type { BandModeActivity } from 'Frontend/types/spotterSource';
import { createMockBandCondition, createMockBandActivity, BandConditionRating } from '../../../fixtures/mockFactories';

expect.extend(toHaveNoViolations);

describe('BandModeActivityContent', () => {
  const supportedModeConfig: ModeConfig = {
    mode: 'FT8',
    isSupported: true,
    displayName: 'FT8',
  };

  const unsupportedModeConfig: ModeConfig = {
    mode: 'SSB',
    isSupported: false,
    displayName: 'SSB',
  };

  const createActivity = (overrides?: Partial<BandModeActivity>): BandModeActivity => {
    const mockActivity = createMockBandActivity(overrides);
    return {
      band: mockActivity.band ?? '20m',
      mode: mockActivity.mode ?? 'FT8',
      spotCount: mockActivity.spotCount ?? 150,
      baselineSpotCount: mockActivity.baselineSpotCount ?? 100,
      trendPercentage: mockActivity.trendPercentage ?? 50,
      maxDxKm: mockActivity.maxDxKm,
      maxDxPath: mockActivity.maxDxPath,
      activePaths: mockActivity.activePaths?.map((p) => String(p)) ?? [],
      score: mockActivity.score ?? 75,
      windowMinutes: mockActivity.windowMinutes,
    };
  };

  describe('full data rendering', () => {
    it('should render all components when activity and condition provided', () => {
      const activity = createActivity();
      const condition = createMockBandCondition();

      render(
        <BandModeActivityContent
          activity={activity}
          condition={condition}
          modeConfig={supportedModeConfig}
          band="20m"
        />,
      );

      // Activity bar should show spot count
      expect(screen.getByText(/150/)).toBeInTheDocument();

      // Trend indicator should show percentage
      expect(screen.getByText(/50%/)).toBeInTheDocument();

      // DX reach should show distance (8432 km -> "8.4k km" format)
      expect(screen.getByText(/8\.4k km/)).toBeInTheDocument();

      // Condition badge should show rating
      expect(screen.getByText('GOOD')).toBeInTheDocument();
    });

    it('should render activity data without condition', () => {
      const activity = createActivity();

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      // Activity bar should show spot count
      expect(screen.getByText(/150/)).toBeInTheDocument();

      // Condition badge should not be present
      expect(screen.queryByText('GOOD')).not.toBeInTheDocument();
    });
  });

  describe('no activity handling', () => {
    it('should render nothing when supported mode has no activity', () => {
      const condition = createMockBandCondition();

      const { container } = render(
        <BandModeActivityContent condition={condition} modeConfig={supportedModeConfig} band="20m" />,
      );

      // Should render nothing (cards without activity are filtered at registration level)
      expect(container).toBeEmptyDOMElement();
    });

    it('should render nothing when no activity and no condition', () => {
      const { container } = render(<BandModeActivityContent modeConfig={supportedModeConfig} band="20m" />);

      // Should render nothing
      expect(container).toBeEmptyDOMElement();
    });
  });

  describe('coming soon placeholder', () => {
    it('should show coming soon for unsupported modes', () => {
      render(<BandModeActivityContent modeConfig={unsupportedModeConfig} band="20m" />);

      expect(screen.getByText(/Coming soon/i)).toBeInTheDocument();
    });

    it('should not show activity data for unsupported modes', () => {
      const activity = createActivity();
      const condition = createMockBandCondition();

      render(
        <BandModeActivityContent
          activity={activity}
          condition={condition}
          modeConfig={unsupportedModeConfig}
          band="20m"
        />,
      );

      // Should show coming soon, not activity data
      expect(screen.getByText(/Coming soon/i)).toBeInTheDocument();
      expect(screen.queryByText(/150/)).not.toBeInTheDocument();
    });
  });

  describe('trend indicators', () => {
    it('should show positive trend with up arrow', () => {
      const activity = createActivity({ trendPercentage: 50 });

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      expect(screen.getByText(/\+50%/)).toBeInTheDocument();
    });

    it('should show negative trend with down arrow', () => {
      const activity = createActivity({ trendPercentage: -30 });

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      expect(screen.getByText(/-30%/)).toBeInTheDocument();
    });

    it('should show flat trend for zero', () => {
      const activity = createActivity({ trendPercentage: 0 });

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      expect(screen.getByText(/0%/)).toBeInTheDocument();
    });
  });

  describe('condition badges', () => {
    it.each([
      [BandConditionRating.GOOD, 'GOOD'],
      [BandConditionRating.FAIR, 'FAIR'],
      [BandConditionRating.POOR, 'POOR'],
      [BandConditionRating.UNKNOWN, 'UNKNOWN'],
    ])('should display %s rating correctly', (rating, expectedText) => {
      const activity = createActivity();
      const condition = createMockBandCondition({ rating });

      render(
        <BandModeActivityContent
          activity={activity}
          condition={condition}
          modeConfig={supportedModeConfig}
          band="20m"
        />,
      );

      expect(screen.getByText(expectedText)).toBeInTheDocument();
    });
  });

  describe('DX reach display', () => {
    it('should show max DX with path', () => {
      const activity = createActivity({
        maxDxKm: 12000,
        maxDxPath: 'VK3XYZ -> K1ABC',
      });

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      // formatDistance displays as "12k km" for 12000 km
      expect(screen.getByText(/12k km/)).toBeInTheDocument();
      // The -> is converted to → in the display
      expect(screen.getByText(/VK3XYZ → K1ABC/)).toBeInTheDocument();
    });

    it('should show placeholder when no DX data', () => {
      const activity = createActivity({
        maxDxKm: undefined,
        maxDxPath: undefined,
      });

      render(<BandModeActivityContent activity={activity} modeConfig={supportedModeConfig} band="20m" />);

      // Should show empty state (no distance)
      expect(screen.getByText(/No DX/i)).toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations with full data', async () => {
      const activity = createActivity();
      const condition = createMockBandCondition();

      const { container } = render(
        <BandModeActivityContent
          activity={activity}
          condition={condition}
          modeConfig={supportedModeConfig}
          band="20m"
        />,
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations with no activity', async () => {
      const { container } = render(<BandModeActivityContent modeConfig={supportedModeConfig} band="20m" />);

      // Component renders nothing for no activity, but axe should still pass
      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations for unsupported mode', async () => {
      const { container } = render(<BandModeActivityContent modeConfig={unsupportedModeConfig} band="20m" />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
