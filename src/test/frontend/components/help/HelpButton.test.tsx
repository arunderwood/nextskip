/**
 * Unit tests for HelpButton component.
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { HelpButton } from 'Frontend/components/help/HelpButton';

describe('HelpButton', () => {
  it('should render with correct aria-label', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button', { name: /open help and about/i });
    expect(button).toBeInTheDocument();
  });

  it('should render with correct title attribute', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('title', 'Help & About');
  });

  it('should call onClick when clicked', () => {
    const handleClick = vi.fn();
    render(<HelpButton onClick={handleClick} />);

    const button = screen.getByRole('button');
    fireEvent.click(button);

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('should have the correct CSS class', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    expect(button).toHaveClass('help-button');
  });

  it('should have button type attribute', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    expect(button).toHaveAttribute('type', 'button');
  });

  it('should contain an icon with aria-hidden', () => {
    render(<HelpButton onClick={() => {}} />);

    const button = screen.getByRole('button');
    const svg = button.querySelector('svg');

    expect(svg).toBeInTheDocument();
    expect(svg).toHaveAttribute('aria-hidden', 'true');
  });
});
