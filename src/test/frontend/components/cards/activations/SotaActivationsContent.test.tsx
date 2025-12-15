import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import SotaActivationsContent from 'Frontend/components/cards/activations/SotaActivationsContent';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';

// Extend Vitest's expect with jest-axe matchers
expect.extend(toHaveNoViolations);

describe('SotaActivationsContent', () => {
  const createMockActivation = (overrides?: Partial<Activation>): Activation => ({
    spotId: '654321',
    activatorCallsign: 'K2DEF/P',
    reference: 'W7W/LC-001',
    referenceName: 'Mount Test',
    type: 'SOTA',
    frequency: 7200,
    mode: 'CW',
    grid: undefined,
    latitude: undefined,
    longitude: undefined,
    spottedAt: new Date().toISOString(),
    qsoCount: undefined,
    source: 'SOTA API',
    ...overrides,
  });

  it('should render activation count', () => {
    const activations = [
      createMockActivation({ spotId: '1' }),
      createMockActivation({ spotId: '2' }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(screen.getByText('on air')).toBeInTheDocument();
  });

  it('should display callsign with QRZ link', () => {
    const activations = [createMockActivation()];

    render(<SotaActivationsContent activations={activations} />);

    const link = screen.getByRole('link', { name: 'K2DEF/P' });
    expect(link).toHaveAttribute('href', 'https://www.qrz.com/db/K2DEF/P');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('should display summit reference and name', () => {
    const activations = [
      createMockActivation({
        reference: 'W7W/LC-001',
        referenceName: 'Mount Rainier',
      }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('W7W/LC-001')).toBeInTheDocument();
    expect(screen.getByText('Mount Rainier')).toBeInTheDocument();
  });

  it('should display frequency and mode', () => {
    const activations = [
      createMockActivation({
        frequency: 7200,
        mode: 'CW',
      }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('7.200 MHz')).toBeInTheDocument();
    expect(screen.getByText('CW')).toBeInTheDocument();
  });

  it('should show empty state when no activations', () => {
    render(<SotaActivationsContent activations={[]} />);

    expect(screen.getByText('0')).toBeInTheDocument();
    expect(screen.getByText('on air')).toBeInTheDocument();
    expect(screen.getByText('No current SOTA activations')).toBeInTheDocument();
  });

  it('should format time since spotted', () => {
    // Mock current time
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const tenMinutesAgo = new Date('2025-01-15T11:50:00Z');
    const activations = [
      createMockActivation({ spottedAt: tenMinutesAgo.toISOString() }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('10 min ago')).toBeInTheDocument();

    // Restore real time
    vi.useRealTimers();
  });

  it('should limit display to 5 activations with overflow message', () => {
    const manyActivations = Array.from({ length: 8 }, (_, i) =>
      createMockActivation({ spotId: `${i + 1}`, activatorCallsign: `K${i}DEF/P` })
    );

    render(<SotaActivationsContent activations={manyActivations} />);

    // Should show count of 8
    expect(screen.getByText('8')).toBeInTheDocument();

    // Should show "+3 more activations" message
    expect(screen.getByText('+3 more activations')).toBeInTheDocument();

    // Should render exactly 5 callsign links
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(5);
  });

  it('should be WCAG 2.1 AA compliant', async () => {
    const activations = [
      createMockActivation({
        spotId: '1',
        activatorCallsign: 'K2DEF/P',
        reference: 'W7W/LC-001',
      }),
    ];

    const { container } = render(<SotaActivationsContent activations={activations} />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should format time as "Just now" for spots under 1 minute', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const fortyFiveSecondsAgo = new Date('2025-01-15T11:59:15Z');
    const activations = [
      createMockActivation({ spottedAt: fortyFiveSecondsAgo.toISOString() }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('Just now')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time as "1 min ago" for exactly 1 minute', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const oneMinuteAgo = new Date('2025-01-15T11:59:00Z');
    const activations = [
      createMockActivation({ spottedAt: oneMinuteAgo.toISOString() }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('1 min ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time in hours for spots over 60 minutes', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const threeHoursAgo = new Date('2025-01-15T09:00:00Z');
    const activations = [
      createMockActivation({ spottedAt: threeHoursAgo.toISOString() }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('3 hours ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should format time as "1 hour ago" for exactly 60 minutes', () => {
    const now = new Date('2025-01-15T12:00:00Z');
    vi.setSystemTime(now);

    const oneHourAgo = new Date('2025-01-15T11:00:00Z');
    const activations = [
      createMockActivation({ spottedAt: oneHourAgo.toISOString() }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('1 hour ago')).toBeInTheDocument();

    vi.useRealTimers();
  });

  it('should display "Unknown" for null timestamp', () => {
    const activations = [
      createMockActivation({ spottedAt: undefined }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('should display "Unknown" for null frequency', () => {
    const activations = [
      createMockActivation({ frequency: undefined }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });

  it('should handle missing summit name gracefully', () => {
    const activations = [
      createMockActivation({ referenceName: undefined }),
    ];

    const { container } = render(<SotaActivationsContent activations={activations} />);

    // Should not display summit-name div when referenceName is undefined
    expect(container.querySelector('.summit-name')).not.toBeInTheDocument();
  });

  it('should display "Unknown" for null mode', () => {
    const activations = [
      createMockActivation({ mode: undefined }),
    ];

    render(<SotaActivationsContent activations={activations} />);

    expect(screen.getByText('Unknown')).toBeInTheDocument();
  });
});
