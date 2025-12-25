/**
 * Accessibility tests for HelpModal component.
 */

import React from 'react';
import { describe, it, expect, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { HelpModal } from 'Frontend/components/help/HelpModal';
import { HelpButton } from 'Frontend/components/help/HelpButton';
import { clearHelpRegistry, registerHelp } from 'Frontend/components/help/HelpRegistry';
import type { HelpDefinition } from 'Frontend/components/help/types';

expect.extend(toHaveNoViolations);

beforeEach(() => {
  clearHelpRegistry();
});

describe('HelpModal Accessibility', () => {
  const mockHelp: HelpDefinition = {
    id: 'solar-indices',
    title: 'Solar Indices',
    order: 10,
    Content: () => <div>Solar content for testing</div>,
  };

  it('should have no accessibility violations when open', async () => {
    registerHelp(mockHelp);
    const { container } = render(<HelpModal isOpen={true} onClose={() => {}} />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have proper heading hierarchy', () => {
    registerHelp(mockHelp);
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    // Modal title should be h2
    const modalTitle = screen.getByRole('heading', { level: 2, name: /help & about/i });
    expect(modalTitle).toBeInTheDocument();

    // Section titles should be h3
    const sectionTitles = screen.getAllByRole('heading', { level: 3 });
    expect(sectionTitles.length).toBeGreaterThan(0);
  });

  it('should have accessible close button', () => {
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    const closeButton = screen.getByRole('button', { name: /close help/i });
    expect(closeButton).toBeInTheDocument();
    expect(closeButton).toHaveAccessibleName();
  });

  it('should have proper dialog role and labeling', () => {
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    const dialog = screen.getByRole('dialog');
    expect(dialog).toHaveAttribute('aria-labelledby', 'help-modal-title');
  });

  it('should have proper navigation with tablist role', () => {
    registerHelp(mockHelp);
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    const tablist = screen.getByRole('tablist');
    expect(tablist).toBeInTheDocument();

    const tabs = screen.getAllByRole('tab');
    tabs.forEach((tab) => {
      expect(tab).toHaveAttribute('aria-selected');
      expect(tab).toHaveAttribute('aria-controls');
    });
  });

  it('should have accessible sections with aria-labelledby', () => {
    registerHelp(mockHelp);
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    // Check About section
    const aboutSection = document.getElementById('help-section-about');
    expect(aboutSection).toHaveAttribute('aria-labelledby', 'help-section-about-title');

    // Check registered section
    const solarSection = document.getElementById('help-section-solar-indices');
    expect(solarSection).toHaveAttribute('aria-labelledby', 'help-section-solar-indices-title');
  });

  it('close button icon should be hidden from screen readers', () => {
    render(<HelpModal isOpen={true} onClose={() => {}} />);

    const closeButton = screen.getByRole('button', { name: /close help/i });
    const svg = closeButton.querySelector('svg');

    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });
});

describe('HelpButton Accessibility', () => {
  it('should have no accessibility violations', async () => {
    const { container } = render(<HelpButton onClick={() => {}} />);

    const results = await axe(container);
    expect(results).toHaveNoViolations();
  });

  it('should have accessible name', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    expect(button).toHaveAccessibleName(/open help and about/i);
  });

  it('icon should be hidden from screen readers', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    const svg = button.querySelector('svg');

    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });
});
