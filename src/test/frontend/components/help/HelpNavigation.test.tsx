/**
 * Unit tests for HelpNavigation component.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HelpNavigation } from 'Frontend/components/help/HelpNavigation';
import type { HelpDefinition } from 'Frontend/components/help/types';

describe('HelpNavigation', () => {
  const mockSections: HelpDefinition[] = [
    {
      id: 'solar-indices',
      title: 'Solar Indices',
      order: 10,
      Content: () => null,
    },
    {
      id: 'band-conditions',
      title: 'Band Conditions',
      order: 20,
      Content: () => null,
    },
  ];

  it('should render About tab first (hardcoded)', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={() => {}} />);

    const tabs = screen.getAllByRole('tab');
    expect(tabs[0]).toHaveTextContent('About');
  });

  it('should render all section tabs', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={() => {}} />);

    expect(screen.getByRole('tab', { name: /about/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /solar indices/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /band conditions/i })).toBeInTheDocument();
  });

  it('should highlight the active section', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="solar-indices" onNavigate={() => {}} />);

    const activeTab = screen.getByRole('tab', { name: /solar indices/i });
    expect(activeTab).toHaveClass('help-navigation__item--active');
    expect(activeTab).toHaveAttribute('aria-selected', 'true');
  });

  it('should not highlight inactive sections', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={() => {}} />);

    const inactiveTab = screen.getByRole('tab', { name: /solar indices/i });
    expect(inactiveTab).not.toHaveClass('help-navigation__item--active');
    expect(inactiveTab).toHaveAttribute('aria-selected', 'false');
  });

  it('should call onNavigate with section ID when tab is clicked', () => {
    const handleNavigate = vi.fn();
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={handleNavigate} />);

    const tab = screen.getByRole('tab', { name: /solar indices/i });
    fireEvent.click(tab);

    expect(handleNavigate).toHaveBeenCalledWith('solar-indices');
  });

  it('should call onNavigate with about when About tab is clicked', () => {
    const handleNavigate = vi.fn();
    render(<HelpNavigation sections={mockSections} activeSectionId="solar-indices" onNavigate={handleNavigate} />);

    const tab = screen.getByRole('tab', { name: /about/i });
    fireEvent.click(tab);

    expect(handleNavigate).toHaveBeenCalledWith('about');
  });

  it('should have proper tablist role on container', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={() => {}} />);

    expect(screen.getByRole('tablist')).toBeInTheDocument();
  });

  it('should have aria-controls linking to section IDs', () => {
    render(<HelpNavigation sections={mockSections} activeSectionId="about" onNavigate={() => {}} />);

    const solarTab = screen.getByRole('tab', { name: /solar indices/i });
    expect(solarTab).toHaveAttribute('aria-controls', 'help-section-solar-indices');
  });

  it('should render section icons when provided', () => {
    const sectionsWithIcon: HelpDefinition[] = [
      {
        id: 'solar-indices',
        title: 'Solar Indices',
        icon: <span data-testid="test-icon">*</span>,
        order: 10,
        Content: () => null,
      },
    ];

    render(<HelpNavigation sections={sectionsWithIcon} activeSectionId="about" onNavigate={() => {}} />);

    expect(screen.getByTestId('test-icon')).toBeInTheDocument();
  });
});
