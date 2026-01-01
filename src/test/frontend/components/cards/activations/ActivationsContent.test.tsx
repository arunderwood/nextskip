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
  }

  const createActivation = (params: ActivationParams): Activation =>
    createMockActivation({
      spotId: String(params.id),
      activatorCallsign: params.callsign,
      frequency: params.frequency,
      mode: params.mode,
      location: { reference: params.reference, name: params.name, regionCode: params.regionCode },
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

    it('should show mode or Unknown', () => {
      const withMode = createActivation({
        id: 1,
        callsign: 'K1ABC',
        reference: 'K-1234',
        frequency: 14250,
        mode: 'SSB',
      });
      const withoutMode = createActivation({
        id: 2,
        callsign: 'W2XYZ',
        reference: 'K-5678',
        frequency: 7074,
        mode: '',
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
    it('should show max 8 activations', () => {
      const activations = Array.from({ length: 10 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      // Should show 8 activations
      for (let i = 0; i < 8; i++) {
        expect(screen.getByText(`K${i}ABC`)).toBeInTheDocument();
      }

      // Should not show 9th and 10th
      expect(screen.queryByText('K8ABC')).not.toBeInTheDocument();
      expect(screen.queryByText('K9ABC')).not.toBeInTheDocument();
    });

    it('should show "more activations" message when > 8', () => {
      const activations = Array.from({ length: 12 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.getByText('+4 more activations')).toBeInTheDocument();
    });

    it('should not show "more activations" when <= 8', () => {
      const activations = Array.from({ length: 8 }, (_, i) =>
        createActivation({ id: i, callsign: `K${i}ABC`, reference: `K-${i}`, frequency: 14250, mode: 'SSB' }),
      );

      render(<ActivationsContent activations={activations} type="pota" emptyMessage="No activations" />);

      expect(screen.queryByText(/more activations/)).not.toBeInTheDocument();
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

      // Should still show 8 items (verifies memoization didn't break behavior)
      for (let i = 0; i < 8; i++) {
        expect(screen.getByText(`K${i}ABC`)).toBeInTheDocument();
      }
    });
  });
});
