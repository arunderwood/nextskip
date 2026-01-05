import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import PotaActivationsContent from 'Frontend/components/cards/activations/PotaActivationsContent';
import { createMockPotaActivation } from '../../../fixtures/mockFactories';

// Extend Vitest's expect with jest-axe matchers
expect.extend(toHaveNoViolations);

describe('PotaActivationsContent', () => {
  it('should render activation count', () => {
    const activations = [
      createMockPotaActivation({ spotId: '1' }),
      createMockPotaActivation({ spotId: '2' }),
      createMockPotaActivation({ spotId: '3' }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('on air')).toBeInTheDocument();
  });

  it('should display callsign with QRZ link', () => {
    const activations = [createMockPotaActivation()];

    render(<PotaActivationsContent activations={activations} />);

    const link = screen.getByRole('link', { name: 'W1ABC' });
    expect(link).toHaveAttribute('href', 'https://www.qrz.com/db/W1ABC');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('should display park reference and name', () => {
    const activations = [
      createMockPotaActivation({
        location: {
          reference: 'US-0001',
          name: 'Yellowstone National Park',
          regionCode: 'WY',
        },
      }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('US-0001')).toBeInTheDocument();
    expect(screen.getByText('Yellowstone National Park, WY')).toBeInTheDocument();
  });

  it('should display frequency and mode', () => {
    const activations = [
      createMockPotaActivation({
        frequency: 14250,
        mode: 'SSB',
      }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('14.250 MHz')).toBeInTheDocument();
    expect(screen.getByText('SSB')).toBeInTheDocument();
  });

  it('should format frequency correctly', () => {
    const activations = [
      createMockPotaActivation({ frequency: 7200 }), // 7.200 MHz
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('7.200 MHz')).toBeInTheDocument();
  });

  it('should show empty state when no activations', () => {
    render(<PotaActivationsContent activations={[]} />);

    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText('on air')).toBeInTheDocument();
    expect(screen.getByText('No current POTA activations')).toBeInTheDocument();
  });

  it('should format time since spotted', () => {
    // Mock current time
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const fiveMinutesAgo = new Date('2025-01-15T11:55:00Z');
    const activations = [createMockPotaActivation({ spottedAt: fiveMinutesAgo.toISOString() })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('5 min ago')).toBeInTheDocument();

    // Restore real time
    vi.useRealTimers();
  });

  it('should limit display to 250 activations with overflow message', () => {
    const manyActivations = Array.from({ length: 260 }, (_, i) =>
      createMockPotaActivation({ spotId: `${i + 1}`, activatorCallsign: `W${i}ABC` }),
    );

    render(<PotaActivationsContent activations={manyActivations} />);

    // Should show count of 260
    expect(screen.getByText('260')).toBeInTheDocument();

    // Should show "+10 more activations" message (260 total - 250 displayed)
    expect(screen.getByText('+10 more activations')).toBeInTheDocument();

    // Should render exactly 250 activation items (each with 2 links: callsign + reference)
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(500); // 250 activations * 2 links each
  });

  it('should be WCAG 2.1 AA compliant', async () => {
    const activations = [
      createMockPotaActivation({
        spotId: '1',
        activatorCallsign: 'W1ABC',
        location: {
          reference: 'US-0001',
          name: 'Test Park',
          regionCode: 'MA',
        },
      }),
    ];

    const { container } = render(<PotaActivationsContent activations={activations} />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should format time as "Just now" for spots under 1 minute', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const thirtySecondsAgo = new Date('2025-01-15T11:59:30Z');
    const activations = [createMockPotaActivation({ spottedAt: thirtySecondsAgo.toISOString() })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('Just now')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time as "1 min ago" for exactly 1 minute', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const oneMinuteAgo = new Date('2025-01-15T11:59:00Z');
    const activations = [createMockPotaActivation({ spottedAt: oneMinuteAgo.toISOString() })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('1 min ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time in hours for spots over 60 minutes', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const twoHoursAgo = new Date('2025-01-15T10:00:00Z');
    const activations = [createMockPotaActivation({ spottedAt: twoHoursAgo.toISOString() })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('2 hours ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time as "1 hour ago" for exactly 60 minutes', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const oneHourAgo = new Date('2025-01-15T11:00:00Z');
    const activations = [createMockPotaActivation({ spottedAt: oneHourAgo.toISOString() })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('1 hour ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should display "Unknown" for null timestamp', () => {
    const activations = [createMockPotaActivation({ spottedAt: undefined })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('should display "Unknown" for null frequency', () => {
    const activations = [createMockPotaActivation({ frequency: undefined })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('should handle missing park name gracefully', () => {
    const activations = [
      createMockPotaActivation({
        location: {
          reference: 'US-0001',
          name: undefined,
          regionCode: 'MA',
        },
      }),
    ];

    const { container } = render(<PotaActivationsContent activations={activations} />);

    // Should not display park-name div when name is undefined
    expect(container.querySelector('.park-name')).not.toBeInTheDocument();
  });

  it('should display "Unknown" for null mode', () => {
    const activations = [createMockPotaActivation({ mode: undefined })];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });
});
