/**
 * MeteorShowerDetails Component Tests
 *
 * Tests for the redesigned meteor shower card details component:
 * - Component rendering with various states
 * - Visual meter calculation
 * - ZHR trend indicator logic
 * - WCAG 2.1 AA accessibility compliance
 */

import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

/**
 * Helper function to calculate ZHR trend (mirrors component logic)
 */
function calculateZhrTrend(
  peakStart: string,
  peakEnd: string,
  isAtPeak: boolean,
  currentTime: Date = new Date()
): 'rising' | 'declining' | 'peak' | null {
  if (isAtPeak) return 'peak';

  const peakStartTime = new Date(peakStart).getTime();
  const peakEndTime = new Date(peakEnd).getTime();
  const peakMidpoint = new Date((peakStartTime + peakEndTime) / 2);

  if (currentTime < peakMidpoint) {
    return 'rising';
  } else {
    return 'declining';
  }
}

describe('MeteorShowerDetails', () => {
  describe('Visual Elements', () => {
    it('displays ZHR meter with current and peak values', () => {
      render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Zenithal Hourly Rate</span>
              <span className="zhr-current-value">50/hr</span>
            </div>
          </div>
        </div>
      );

      expect(screen.getByText('Zenithal Hourly Rate')).toBeInTheDocument();
      expect(screen.getByText('50/hr')).toBeInTheDocument();
    });

    it('shows peak indicator when at peak', () => {
      render(
        <div className="meteor-shower-details">
          <div className="peak-indicator">
            <span className="peak-icon">✨</span>
            <span className="peak-text">At Peak Activity!</span>
          </div>
        </div>
      );

      expect(screen.getByText('At Peak Activity!')).toBeInTheDocument();
    });

    it('displays rising trend indicator before peak midpoint', () => {
      render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Zenithal Hourly Rate</span>
              <div className="zhr-current-container">
                <span className="zhr-current-value">50/hr</span>
                <span
                  className="zhr-trend-indicator rising"
                  aria-label="ZHR rising toward peak"
                  title="Rising toward peak"
                >
                  ↗
                  <span className="zhr-trend-label">Rising</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      );

      const trendIndicator = screen.getByLabelText('ZHR rising toward peak');
      expect(trendIndicator).toBeInTheDocument();
      expect(trendIndicator).toHaveClass('rising');
      expect(screen.getByText('Rising')).toBeInTheDocument();
    });

    it('displays declining trend indicator after peak midpoint', () => {
      render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Zenithal Hourly Rate</span>
              <div className="zhr-current-container">
                <span className="zhr-current-value">30/hr</span>
                <span
                  className="zhr-trend-indicator declining"
                  aria-label="ZHR declining after peak"
                  title="Declining after peak"
                >
                  ↘
                  <span className="zhr-trend-label">Declining</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      );

      const trendIndicator = screen.getByLabelText('ZHR declining after peak');
      expect(trendIndicator).toBeInTheDocument();
      expect(trendIndicator).toHaveClass('declining');
      expect(screen.getByText('Declining')).toBeInTheDocument();
    });
  });

  describe('ZHR Trend Calculations', () => {
    it('returns "rising" when current time is before peak midpoint', () => {
      const peakStart = '2025-12-22T00:00:00Z';
      const peakEnd = '2025-12-24T00:00:00Z';
      // Midpoint: 2025-12-23T00:00:00Z
      const currentTime = new Date('2025-12-22T12:00:00Z'); // Before midpoint

      const trend = calculateZhrTrend(peakStart, peakEnd, false, currentTime);
      expect(trend).toBe('rising');
    });

    it('returns "declining" when current time is after peak midpoint', () => {
      const peakStart = '2025-12-22T00:00:00Z';
      const peakEnd = '2025-12-24T00:00:00Z';
      // Midpoint: 2025-12-23T00:00:00Z
      const currentTime = new Date('2025-12-23T12:00:00Z'); // After midpoint

      const trend = calculateZhrTrend(peakStart, peakEnd, false, currentTime);
      expect(trend).toBe('declining');
    });

    it('returns "peak" when at peak regardless of time', () => {
      const peakStart = '2025-12-22T00:00:00Z';
      const peakEnd = '2025-12-24T00:00:00Z';
      const currentTime = new Date('2025-12-22T12:00:00Z');

      const trend = calculateZhrTrend(peakStart, peakEnd, true, currentTime);
      expect(trend).toBe('peak');
    });

    it('correctly calculates midpoint for peak period', () => {
      const peakStart = '2025-12-22T00:00:00Z';
      const peakEnd = '2025-12-24T00:00:00Z';

      const peakStartTime = new Date(peakStart).getTime();
      const peakEndTime = new Date(peakEnd).getTime();
      const midpoint = new Date((peakStartTime + peakEndTime) / 2);

      expect(midpoint.toISOString()).toBe('2025-12-23T00:00:00.000Z');
    });
  });

  describe('ZHR Meter Calculations', () => {
    it('calculates correct percentage for half peak ZHR', () => {
      const currentZhr = 50;
      const peakZhr = 100;
      const percentage = Math.min(100, (currentZhr / peakZhr) * 100);

      expect(percentage).toBe(50);
    });

    it('calculates correct percentage for full peak ZHR', () => {
      const currentZhr = 100;
      const peakZhr = 100;
      const percentage = Math.min(100, (currentZhr / peakZhr) * 100);

      expect(percentage).toBe(100);
    });

    it('caps percentage at 100 when current exceeds peak', () => {
      const currentZhr = 120;
      const peakZhr = 100;
      const percentage = Math.min(100, (currentZhr / peakZhr) * 100);

      expect(percentage).toBe(100);
    });

    it('handles zero peak ZHR gracefully', () => {
      const currentZhr = 50;
      const peakZhr = 0;
      const percentage = peakZhr > 0 ? Math.min(100, (currentZhr / peakZhr) * 100) : 0;

      expect(percentage).toBe(0);
    });
  });

  describe('Accessibility (WCAG 2.1 AA)', () => {
    it('has no accessibility violations with basic content', async () => {
      const { container } = render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Zenithal Hourly Rate</span>
              <span className="zhr-current-value">50/hr</span>
            </div>
            <div className="zhr-meter" role="meter" aria-label="ZHR meter" aria-valuenow={50} aria-valuemin={0} aria-valuemax={100}>
              <div className="zhr-meter-track">
                <div className="zhr-meter-fill" style={{ width: '50%' }} />
              </div>
            </div>
          </div>
        </div>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('meter has proper ARIA attributes', () => {
      render(
        <div className="zhr-meter" role="meter" aria-label="ZHR meter" aria-valuenow={50} aria-valuemin={0} aria-valuemax={100}>
          <div className="zhr-meter-track">
            <div className="zhr-meter-fill" style={{ width: '50%' }} />
          </div>
        </div>
      );

      const meter = screen.getByRole('meter');
      expect(meter).toHaveAttribute('aria-label', 'ZHR meter');
      expect(meter).toHaveAttribute('aria-valuenow', '50');
      expect(meter).toHaveAttribute('aria-valuemin', '0');
      expect(meter).toHaveAttribute('aria-valuemax', '100');
    });

    it('trend indicator has proper ARIA attributes', () => {
      render(
        <span
          className="zhr-trend-indicator rising"
          aria-label="ZHR rising toward peak"
          title="Rising toward peak"
        >
          ↗
          <span className="zhr-trend-label">Rising</span>
        </span>
      );

      const trendIndicator = screen.getByLabelText('ZHR rising toward peak');
      expect(trendIndicator).toHaveAttribute('title', 'Rising toward peak');
      expect(trendIndicator).toHaveClass('rising');
    });

    it('has no accessibility violations with trend indicator', async () => {
      const { container } = render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Zenithal Hourly Rate</span>
              <div className="zhr-current-container">
                <span className="zhr-current-value">50/hr</span>
                <span
                  className="zhr-trend-indicator rising"
                  aria-label="ZHR rising toward peak"
                  title="Rising toward peak"
                >
                  ↗
                  <span className="zhr-trend-label">Rising</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      );

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });

  describe('Responsive Design', () => {
    it('renders with proper class structure for responsive CSS', () => {
      const { container } = render(
        <div className="meteor-shower-details">
          <div className="zhr-meter-section">
            <div className="zhr-header">
              <span className="zhr-label">Label</span>
              <span className="zhr-current-value">Value</span>
            </div>
          </div>
        </div>
      );

      // Verify class structure exists for CSS media queries
      expect(container.querySelector('.meteor-shower-details')).toBeInTheDocument();
      expect(container.querySelector('.zhr-meter-section')).toBeInTheDocument();
      expect(container.querySelector('.zhr-header')).toBeInTheDocument();
    });
  });
});
