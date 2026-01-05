import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { axe, toHaveNoViolations } from 'jest-axe';
import { PathStatusGrid } from 'Frontend/components/cards/band-activity/PathStatusGrid';
import { ContinentPath } from '../../../fixtures/mockFactories';

expect.extend(toHaveNoViolations);

describe('PathStatusGrid', () => {
  const ALL_PATHS = [
    ContinentPath.NA_EU,
    ContinentPath.NA_AS,
    ContinentPath.EU_AS,
    ContinentPath.NA_OC,
    ContinentPath.EU_AF,
    ContinentPath.NA_SA,
  ];

  describe('rendering', () => {
    it('should render all 6 paths', () => {
      render(<PathStatusGrid activePaths={[]} />);

      // All path labels should be present
      expect(screen.getByText(/NA.*EU/)).toBeInTheDocument();
      expect(screen.getByText(/NA.*AS/)).toBeInTheDocument();
      expect(screen.getByText(/EU.*AS/)).toBeInTheDocument();
      expect(screen.getByText(/NA.*OC/)).toBeInTheDocument();
      expect(screen.getByText(/EU.*AF/)).toBeInTheDocument();
      expect(screen.getByText(/NA.*SA/)).toBeInTheDocument();
    });

    it('should show Paths label', () => {
      render(<PathStatusGrid activePaths={[]} />);

      expect(screen.getByText(/Paths/)).toBeInTheDocument();
    });
  });

  describe('active paths', () => {
    it('should highlight active paths', () => {
      const activePaths = [ContinentPath.NA_EU, ContinentPath.EU_AS];
      const { container } = render(<PathStatusGrid activePaths={activePaths} />);

      // Find active path items
      const activeItems = container.querySelectorAll('.path-item--active');
      expect(activeItems.length).toBe(2);
    });

    it('should not highlight inactive paths', () => {
      const activePaths = [ContinentPath.NA_EU];
      const { container } = render(<PathStatusGrid activePaths={activePaths} />);

      // Find inactive path items
      const inactiveItems = container.querySelectorAll('.path-item--inactive');
      expect(inactiveItems.length).toBe(5); // 6 total - 1 active = 5 inactive
    });

    it('should highlight all paths when all active', () => {
      const { container } = render(<PathStatusGrid activePaths={ALL_PATHS} />);

      const activeItems = container.querySelectorAll('.path-item--active');
      expect(activeItems.length).toBe(6);
    });

    it('should show all inactive when no paths active', () => {
      const { container } = render(<PathStatusGrid activePaths={[]} />);

      const inactiveItems = container.querySelectorAll('.path-item--inactive');
      expect(inactiveItems.length).toBe(6);
    });
  });

  describe('grid layout', () => {
    it('should have correct grid structure', () => {
      const { container } = render(<PathStatusGrid activePaths={[]} />);

      const grid = container.querySelector('.path-grid');
      expect(grid).toBeInTheDocument();

      const items = grid?.querySelectorAll('.path-item');
      expect(items?.length).toBe(6);
    });
  });

  describe('accessibility', () => {
    it('should have no accessibility violations with no active paths', async () => {
      const { container } = render(<PathStatusGrid activePaths={[]} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations with some active paths', async () => {
      const { container } = render(<PathStatusGrid activePaths={[ContinentPath.NA_EU, ContinentPath.EU_AS]} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });

    it('should have no accessibility violations with all active paths', async () => {
      const { container } = render(<PathStatusGrid activePaths={ALL_PATHS} />);

      const results = await axe(container);
      expect(results).toHaveNoViolations();
    });
  });
});
