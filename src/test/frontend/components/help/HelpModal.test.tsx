/**
 * Unit tests for HelpModal component.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HelpModal } from 'Frontend/components/help/HelpModal';
import { clearHelpRegistry, registerHelp } from 'Frontend/components/help/HelpRegistry';
import type { HelpDefinition } from 'Frontend/components/help/types';

beforeEach(() => {
  clearHelpRegistry();
});

describe('HelpModal', () => {
  it('should not be visible when isOpen is false', () => {
    // eslint-disable-next-line react/jsx-boolean-value -- false values cannot be omitted
    render(<HelpModal isOpen={false} onClose={() => {}} />);

    const dialog = screen.getByRole('dialog', { hidden: true });
    expect(dialog).not.toHaveAttribute('open');
  });

  it('should be visible when isOpen is true', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('open');
  });

  it('should call onClose when close button is clicked', () => {
    const handleClose = vi.fn();
    render(<HelpModal isOpen onClose={handleClose} />);

    const closeButton = screen.getByRole('button', { name: /close help/i });
    fireEvent.click(closeButton);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should have proper aria-labelledby', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-labelledby', 'help-modal-title');
  });

  it('should render the modal title', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    expect(screen.getByText('Help & About')).toBeInTheDocument();
  });

  it('should render the About section', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    expect(screen.getByText('About NextSkip')).toBeInTheDocument();
  });

  it('should render registered help sections', () => {
    const mockHelp: HelpDefinition = {
      id: 'solar-indices',
      title: 'Solar Indices',
      order: 10,
      Content: () => <div data-testid="solar-content">Solar content</div>,
    };
    registerHelp(mockHelp);

    render(<HelpModal isOpen onClose={() => {}} />);

    expect(screen.getByTestId('solar-content')).toBeInTheDocument();
  });

  it('should render navigation tabs', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    expect(screen.getByRole('tablist')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /about/i })).toBeInTheDocument();
  });

  it('should have proper CSS classes', () => {
    render(<HelpModal isOpen onClose={() => {}} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveClass('help-modal');
  });

  it('should close when dialog backdrop is clicked', () => {
    const handleClose = vi.fn();
    render(<HelpModal isOpen onClose={handleClose} />);

    const dialog = screen.getByRole('dialog');
    // Simulate clicking on the dialog element itself (backdrop)
    fireEvent.click(dialog);

    expect(handleClose).toHaveBeenCalledTimes(1);
  });

  it('should not close when modal content is clicked', () => {
    const handleClose = vi.fn();
    render(<HelpModal isOpen onClose={handleClose} />);

    const container = screen.getByRole('dialog').querySelector('.help-modal__container');
    if (container) {
      fireEvent.click(container);
    }

    expect(handleClose).not.toHaveBeenCalled();
  });
});
