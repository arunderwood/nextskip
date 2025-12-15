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
});
