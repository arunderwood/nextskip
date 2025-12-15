import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import PotaActivationsContent from 'Frontend/components/cards/activations/PotaActivationsContent';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';

// Extend Vitest's expect with jest-axe matchers
expect.extend(toHaveNoViolations);

describe('PotaActivationsContent', () => {
  const createMockActivation = (overrides?: Partial<Activation>): Activation => ({
    spotId: '123456',
    activatorCallsign: 'W1ABC',
    reference: 'US-0001',
    referenceName: 'Test Park',
    type: 'POTA',
    frequency: 14250,
    mode: 'SSB',
    grid: 'FN42',
    latitude: 42.5,
    longitude: -71.3,
    spottedAt: new Date().toISOString(),
    qsoCount: 15,
    source: 'POTA API',
    ...overrides,
  });

  it('should render activation count', () => {
    const activations = [
      createMockActivation({ spotId: '1' }),
      createMockActivation({ spotId: '2' }),
      createMockActivation({ spotId: '3' }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('on air')).toBeInTheDocument();
  });

  it('should display callsign with QRZ link', () => {
    const activations = [createMockActivation()];

    render(<PotaActivationsContent activations={activations} />);

    const link = screen.getByRole('link', { name: 'W1ABC' });
    expect(link).toHaveAttribute('href', 'https://www.qrz.com/db/W1ABC');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('should display park reference and name', () => {
    const activations = [
      createMockActivation({
        reference: 'US-0001',
        referenceName: 'Yellowstone National Park',
      }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('US-0001')).toBeInTheDocument();
    expect(screen.getByText('Yellowstone National Park')).toBeInTheDocument();
  });

  it('should display frequency and mode', () => {
    const activations = [
      createMockActivation({
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
      createMockActivation({ frequency: 7200 }), // 7.200 MHz
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
    const activations = [
      createMockActivation({ spottedAt: fiveMinutesAgo.toISOString() }),
    ];

    render(<PotaActivationsContent activations={activations} />);

    expect(screen.getByText('5 min ago')).toBeInTheDocument();

    // Restore real time
    vi.useRealTimers();
  });

  it('should limit display to 5 activations with overflow message', () => {
    const manyActivations = Array.from({ length: 10 }, (_, i) =>
      createMockActivation({ spotId: `${i + 1}`, activatorCallsign: `W${i}ABC` })
    );

    render(<PotaActivationsContent activations={manyActivations} />);

    // Should show count of 10
    expect(screen.getByText('10')).toBeInTheDocument();

    // Should show "+5 more activations" message
    expect(screen.getByText('+5 more activations')).toBeInTheDocument();

    // Should render exactly 5 callsign links
    const links = screen.getAllByRole('link');
    expect(links).toHaveLength(5);
  });

  it('should be WCAG 2.1 AA compliant', async () => {
    const activations = [
      createMockActivation({
        spotId: '1',
        activatorCallsign: 'W1ABC',
        reference: 'US-0001',
      }),
    ];

    const { container } = render(<PotaActivationsContent activations={activations} />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });
});
