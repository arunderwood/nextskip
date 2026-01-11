/**
 * Tests for AdminLandingView component.
 *
 * Tests the admin landing page including:
 * - Welcome message with user name
 * - Navigation cards for admin sections
 * - Disabled cards for upcoming features
 * - Link navigation
 * - Accessibility
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import React, { createContext, useContext } from 'react';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';

// Import component
import AdminLandingView from 'Frontend/views/admin/@index';

// Create a test wrapper that provides the AdminContext
const AdminContext = createContext<{
  user: AdminUserInfo | null;
  logout(): Promise<void>;
}>({
  user: null,
  logout: () => Promise.resolve(),
});

// Mock the useAdminContext hook
vi.mock('Frontend/views/admin/@layout', () => ({
  useAdminContext: () => useContext(AdminContext),
}));

// Helper to render with context and router
function renderWithContext(ui: React.ReactElement, { user = null }: { user?: AdminUserInfo | null } = {}) {
  const mockLogout = vi.fn();
  return render(
    <AdminContext.Provider value={{ user, logout: mockLogout }}>
      <MemoryRouter initialEntries={['/admin']}>{ui}</MemoryRouter>
    </AdminContext.Provider>,
  );
}

describe('AdminLandingView', () => {
  const mockUser: AdminUserInfo = {
    email: 'admin@example.com',
    name: 'Admin User',
    avatarUrl: 'https://github.com/avatar.png',
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('welcome message', () => {
    it('should display personalized welcome when user has name', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByText(/welcome back, admin user/i)).toBeInTheDocument();
    });

    it('should display generic welcome when user has no name', () => {
      const userWithoutName: AdminUserInfo = {
        email: 'admin@example.com',
      };

      renderWithContext(<AdminLandingView />, { user: userWithoutName });

      expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Welcome back');
      expect(screen.queryByText(/welcome back,/i)).not.toBeInTheDocument();
    });

    it('should display subtitle description', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByText(/manage nextskip feeds/i)).toBeInTheDocument();
    });
  });

  describe('navigation cards', () => {
    it('should display Feed Manager card', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByRole('heading', { name: /feed manager/i })).toBeInTheDocument();
      expect(screen.getByText(/monitor feed health/i)).toBeInTheDocument();
    });

    it('should link Feed Manager card to /admin/feeds', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      const feedManagerLink = screen.getByRole('link', { name: /feed manager/i });
      expect(feedManagerLink).toHaveAttribute('href', '/admin/feeds');
    });

    it('should display System Status card as coming soon', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByRole('heading', { name: /system status/i })).toBeInTheDocument();
      // Should show "Coming soon" instead of a link
      const comingSoonElements = screen.getAllByText(/coming soon/i);
      expect(comingSoonElements.length).toBeGreaterThan(0);
    });

    it('should display User Management card as coming soon', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByRole('heading', { name: /user management/i })).toBeInTheDocument();
    });

    it('should not make disabled cards clickable links', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      // System Status and User Management should not be links
      expect(screen.queryByRole('link', { name: /system status/i })).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: /user management/i })).not.toBeInTheDocument();
    });
  });

  describe('accessibility', () => {
    it('should have navigation landmark for admin sections', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      expect(screen.getByRole('navigation', { name: /admin sections/i })).toBeInTheDocument();
    });

    it('should have heading hierarchy', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      // h1 for page title
      expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument();

      // h2 for card titles
      const cardHeadings = screen.getAllByRole('heading', { level: 2 });
      expect(cardHeadings.length).toBe(3); // Feed Manager, System Status, User Management
    });

    it('should hide decorative icons from screen readers', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      const icons = document.querySelectorAll('[aria-hidden="true"]');
      expect(icons.length).toBeGreaterThan(0);
    });
  });

  describe('page structure', () => {
    it('should render page header', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      const header = document.querySelector('.admin-page-header');
      expect(header).toBeInTheDocument();
    });

    it('should render navigation cards grid', () => {
      renderWithContext(<AdminLandingView />, { user: mockUser });

      const cardsGrid = document.querySelector('.admin-nav-cards');
      expect(cardsGrid).toBeInTheDocument();
    });
  });
});
