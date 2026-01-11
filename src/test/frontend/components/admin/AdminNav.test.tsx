/**
 * Tests for AdminNav component.
 *
 * Tests the admin navigation sidebar including:
 * - Brand/logo rendering
 * - Navigation links
 * - User info display
 * - Logout functionality
 * - Accessibility
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import AdminNav from 'Frontend/components/admin/AdminNav';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';

// Helper to render with router
function renderWithRouter(ui: React.ReactElement, { route = '/admin' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

describe('AdminNav', () => {
  const mockUser: AdminUserInfo = {
    email: 'admin@example.com',
    name: 'Admin User',
    avatarUrl: 'https://github.com/avatar.png',
  };

  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('brand section', () => {
    it('should render the brand logo', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByText('NS')).toBeInTheDocument();
      expect(screen.getByText('NextSkip Admin')).toBeInTheDocument();
    });

    it('should have brand link to admin home', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      const brandLink = screen.getByRole('link', { name: /nextskip admin/i });
      expect(brandLink).toHaveAttribute('href', '/admin');
    });
  });

  describe('navigation links', () => {
    it('should render Dashboard link', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      const dashboardLink = screen.getByRole('link', { name: /dashboard/i });
      expect(dashboardLink).toHaveAttribute('href', '/admin');
    });

    it('should render Feed Manager link', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      const feedsLink = screen.getByRole('link', { name: /feed manager/i });
      expect(feedsLink).toHaveAttribute('href', '/admin/feeds');
    });

    it('should highlight active link', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />, {
        route: '/admin',
      });

      const dashboardLink = screen.getByRole('link', { name: /dashboard/i });
      expect(dashboardLink).toHaveClass('admin-nav-link--active');
    });

    it('should highlight Feed Manager when on feeds route', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />, {
        route: '/admin/feeds',
      });

      const feedsLink = screen.getByRole('link', { name: /feed manager/i });
      expect(feedsLink).toHaveClass('admin-nav-link--active');
    });
  });

  describe('user section', () => {
    it('should display user avatar', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      const avatar = document.querySelector('.admin-nav-avatar') as HTMLImageElement;
      expect(avatar).toBeInTheDocument();
      expect(avatar).toHaveAttribute('src', mockUser.avatarUrl);
    });

    it('should display user name', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByText('Admin User')).toBeInTheDocument();
    });

    it('should display user email', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByText('admin@example.com')).toBeInTheDocument();
    });

    it('should show placeholder avatar when no avatarUrl', () => {
      const userWithoutAvatar: AdminUserInfo = {
        email: 'admin@example.com',
        name: 'Admin User',
      };

      renderWithRouter(<AdminNav user={userWithoutAvatar} onLogout={mockLogout} />);

      const placeholder = screen.getByText('A');
      expect(placeholder).toHaveClass('admin-nav-avatar--placeholder');
    });

    it('should use email initial when no name or avatar', () => {
      const userWithEmailOnly: AdminUserInfo = {
        email: 'zebra@example.com',
      };

      renderWithRouter(<AdminNav user={userWithEmailOnly} onLogout={mockLogout} />);

      expect(screen.getByText('z')).toBeInTheDocument();
    });
  });

  describe('logout button', () => {
    it('should render logout button', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByRole('button', { name: /log out/i })).toBeInTheDocument();
    });

    it('should call onLogout when clicked', async () => {
      const user = userEvent.setup();
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      await user.click(screen.getByRole('button', { name: /log out/i }));

      expect(mockLogout).toHaveBeenCalledOnce();
    });
  });

  describe('accessibility', () => {
    it('should have navigation landmark', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByRole('navigation', { name: /admin navigation/i })).toBeInTheDocument();
    });

    it('should have list for navigation links', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      expect(screen.getByRole('list')).toBeInTheDocument();
    });

    it('should hide decorative icons from screen readers', () => {
      renderWithRouter(<AdminNav user={mockUser} onLogout={mockLogout} />);

      const icons = document.querySelectorAll('[aria-hidden="true"]');
      expect(icons.length).toBeGreaterThan(0);
    });
  });

  describe('null user', () => {
    it('should not render user section when user is null', () => {
      renderWithRouter(<AdminNav user={null} onLogout={mockLogout} />);

      expect(screen.queryByRole('button', { name: /log out/i })).not.toBeInTheDocument();
      expect(screen.queryByText('admin@example.com')).not.toBeInTheDocument();
    });
  });
});
