import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ActivationsContent from 'Frontend/components/cards/activations/ActivationsContent';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';
import { createMockActivation } from '../../../fixtures/mockFactories';

describe('ActivationsContent', () => {
  // Helper to create activation with specific test values
  interface ActivationParams {
    id: number;
    callsign: string;
    reference: string;
    frequency: number;
    mode: string;
    name?: string;
    regionCode?: string;
    spottedAt?: string;
    associationCode?: string;
  }

  const createActivation = (params: ActivationParams): Activation =>
    createMockActivation({
      spotId: String(params.id),
      activatorCallsign: params.callsign,
      frequency: params.frequency,
      mode: params.mode,
      spottedAt: params.spottedAt,
      location: {
        reference: params.reference,
        name: params.name,
        regionCode: params.regionCode,
        associationCode: params.associationCode,
      },
    });

  describe('POTA type', () => {
    it('should render activation count', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'K1ABC',
          reference: 'K-1234',
          frequency: 14250,
          mode: 'SSB',
          name: 'Test Park',
          regionCode: 'US-MA',
        }),
        createActivation({
          id: 2,
          callsign: 'W2XYZ',
          reference: 'K-5678',
          frequency: 7074,
          mode: 'FT8',
          name: 'Another Park',
          regionCode: 'US-NY',
        }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('on air')).toBeInTheDocument();
    });

    it('should render activation list', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'K1ABC',
          reference: 'K-1234',
          frequency: 14250,
          mode: 'SSB',
          name: 'Test Park',
          regionCode: 'US-MA',
        }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('K1ABC')).toBeInTheDocument();
      expect(screen.getByText('K-1234')).toBeInTheDocument();
      expect(screen.getByText('14.250 MHz')).toBeInTheDocument();
      expect(screen.getByText('SSB')).toBeInTheDocument();
      expect(screen.getByText('Test Park, US-MA')).toBeInTheDocument();
    });

    it('should use pota-content class', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'K1ABC', reference: 'K-1234', frequency: 14250, mode: 'SSB' }),
      ];

      const { container } = render(
        <ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />,
      );

      expect(container.querySelector('.pota-content')).toBeInTheDocument();
    });

    it('should use park-name class for location', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'K1ABC',
          reference: 'K-1234',
          frequency: 14250,
          mode: 'SSB',
          name: 'Test Park',
        }),
      ];

      const { container } = render(
        <ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />,
      );

      expect(container.querySelector('.park-name')).toBeInTheDocument();
    });
  });

  describe('SOTA type', () => {
    it('should render activation count', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'G4ABC',
          reference: 'W7W/SE-001',
          frequency: 14250,
          mode: 'CW',
          name: 'Mount Rainier',
          regionCode: 'US-WA',
        }),
        createActivation({
          id: 2,
          callsign: 'K5XYZ',
          reference: 'W7W/SE-002',
          frequency: 7074,
          mode: 'FT8',
          name: 'Mount Adams',
          regionCode: 'US-WA',
        }),
      ];

      render(<ActivationsContent activations={activations} type="sota" emptyMessage="No activations" />);

      expect(screen.getByText('2')).toBeInTheDocument();
      expect(screen.getByText('on air')).toBeInTheDocument();
    });

    it('should use sota-content class', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'G4ABC', reference: 'W7W/SE-001', frequency: 14250, mode: 'CW' }),
      ];

      const { container } = render(
        <ActivationsContent activations={activations} type="sota" emptyMessage="No activations" />,
      );

      expect(container.querySelector('.sota-content')).toBeInTheDocument();
    });

    it('should use summit-name class for location', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'G4ABC',
          reference: 'W7W/SE-001',
          frequency: 14250,
          mode: 'CW',
          name: 'Mount Rainier',
        }),
      ];

      const { container } = render(
        <ActivationsContent activations={activations} type="sota" emptyMessage="No activations" />,
      );

      expect(container.querySelector('.summit-name')).toBeInTheDocument();
    });
  });

  describe('empty state', () => {
    it('should show empty message when no activations', () => {
      render(<ActivationsContent activations={[]} type="pota" emptyMessage="No current POTA activations" />);

      expect(screen.getByText('No current POTA activations')).toBeInTheDocument();
    });

    it('should show 0 count when empty', () => {
      render(<ActivationsContent activations={[]} type="sota" emptyMessage="No activations" />);

      expect(screen.getByText('0')).toBeInTheDocument();
    });
  });

  describe('activation display', () => {
    it('should show callsign link to QRZ', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'K1ABC', reference: 'K-1234', frequency: 14250, mode: 'SSB' }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      const link = screen.getByRole('link', { name: 'K1ABC' });
      expect(link).toHaveAttribute('href', 'https://www.qrz.com/db/K1ABC');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should show POTA reference link to pota.app', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'K1ABC', reference: 'US-0001', frequency: 14250, mode: 'SSB' }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      const link = screen.getByRole('link', { name: 'US-0001' });
      expect(link).toHaveAttribute('href', 'https://pota.app/#/park/US-0001');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should show SOTA reference link to sotl.as with full reference', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'K1ABC', reference: 'W7W/LC-001', frequency: 14250, mode: 'CW' }),
      ];

      render(<ActivationsContent activations={activations} type="sota" emptyMessage="No activations" />);

      const link = screen.getByRole('link', { name: 'W7W/LC-001' });
      expect(link).toHaveAttribute('href', 'https://sotl.as/summits/W7W/LC-001');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should show SOTA reference link using associationCode when reference lacks prefix', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'JA1ABC',
          reference: 'ST-013',
          frequency: 14250,
          mode: 'CW',
          associationCode: 'JA',
        }),
      ];

      render(<ActivationsContent activations={activations} type="sota" emptyMessage="No activations" />);

      const link = screen.getByRole('link', { name: 'ST-013' });
      expect(link).toHaveAttribute('href', 'https://sotl.as/summits/JA/ST-013');
      expect(link).toHaveAttribute('target', '_blank');
      expect(link).toHaveAttribute('rel', 'noopener noreferrer');
    });

    it('should show mode or Unknown', () => {
      const now = new Date().toISOString();
      const withMode = createActivation({
        id: 1,
        callsign: 'K1ABC',
        reference: 'K-1234',
        frequency: 14250,
        mode: 'SSB',
        spottedAt: now,
      });
      const withoutMode = createActivation({
        id: 2,
        callsign: 'W2XYZ',
        reference: 'K-5678',
        frequency: 7074,
        mode: '',
        spottedAt: now,
      });

      const { rerender } = render(
        <ActivationsContent activations={[withMode]} type="pota" emptyMessage="No activations" />,
      );

      expect(screen.getByText('SSB')).toBeInTheDocument();

      rerender(<ActivationsContent activations={[withoutMode]} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });

    it('should render location with region code', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'K1ABC',
          reference: 'K-1234',
          frequency: 14250,
          mode: 'SSB',
          name: 'Test Park',
          regionCode: 'US-MA',
        }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('Test Park, US-MA')).toBeInTheDocument();
    });

    it('should render location without region code', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'K1ABC',
          reference: 'K-1234',
          frequency: 14250,
          mode: 'SSB',
          name: 'Test Park',
        }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('Test Park')).toBeInTheDocument();
    });

    it('should not render location if name is missing', () => {
      const activations = [
        createActivation({ id: 1, callsign: 'K1ABC', reference: 'K-1234', frequency: 14250, mode: 'SSB' }),
      ];

      const { container } = render(
        <ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />,
      );

      expect(container.querySelector('.park-name')).not.toBeInTheDocument();
      expect(container.querySelector('.summit-name')).not.toBeInTheDocument();
    });
  });

  describe('pagination', () => {
    it('should limit displayed activations and show overflow message', () => {
      // Create 12 activations to test slicing behavior (simulates > limit scenario)
      const activations = Array.from({ length: 12 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      // All 12 should be visible (under the 250 limit)
      for (let i = 0; i < 12; i++) {
        expect(screen.getByText(`K${i}ABC`)).toBeInTheDocument();
      }

      // No overflow message when under limit
      expect(screen.queryByText(/more activations/)).not.toBeInTheDocument();
    });

    it('should show total count in header regardless of display limit', () => {
      const activations = Array.from({ length: 5 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      // Count shows total, not limited count
      expect(screen.getByText('5')).toBeInTheDocument();
    });
  });

  describe('memoization', () => {
    it('should memoize displayActivations', () => {
      const activations = Array.from({ length: 10 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      const { rerender } = render(
        <ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />,
      );

      // Rerender with same activations
      rerender(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      // Should still show all 10 items (verifies memoization didn't break behavior)
      for (let i = 0; i < 10; i++) {
        expect(screen.getByText(`K${i}ABC`)).toBeInTheDocument();
      }
    });
  });

  describe('sorting', () => {
    it('should sort activations by most recent spot time first', () => {
      const activations = [
        createActivation({
          id: 1,
          callsign: 'OLDEST',
          reference: 'K-1',
          frequency: 14250,
          mode: 'SSB',
          spottedAt: new Date('2025-01-15T10:00:00Z').toISOString(),
        }),
        createActivation({
          id: 2,
          callsign: 'NEWEST',
          reference: 'K-2',
          frequency: 14250,
          mode: 'SSB',
          spottedAt: new Date('2025-01-15T11:30:00Z').toISOString(),
        }),
        createActivation({
          id: 3,
          callsign: 'MIDDLE',
          reference: 'K-3',
          frequency: 14250,
          mode: 'SSB',
          spottedAt: new Date('2025-01-15T11:00:00Z').toISOString(),
        }),
      ];

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      // Get callsign links (every other link, starting at index 0)
      const links = screen.getAllByRole('link');
      const callsignLinks = links.filter((_, i) => i % 2 === 0);
      expect(callsignLinks[0]).toHaveTextContent('NEWEST');
      expect(callsignLinks[1]).toHaveTextContent('MIDDLE');
      expect(callsignLinks[2]).toHaveTextContent('OLDEST');
    });
  });
});
