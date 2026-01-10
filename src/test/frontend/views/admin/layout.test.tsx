/**
 * Tests for Admin Layout component.
 *
 * Tests the admin layout wrapper including:
 * - Loading state display
 * - Redirect to OAuth2 login when not authenticated
 * - Rendering child routes when authenticated
 * - Context provider for child components
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';

// Mock useAdminAuth hook
const mockUseAdminAuth = vi.fn();
vi.mock('Frontend/hooks/useAdminAuth', () => ({
  useAdminAuth: () => mockUseAdminAuth(),
}));

// Import after mocking
import AdminLayout, { useAdminContext } from 'Frontend/views/admin/@layout';

// Mock window.location
const mockLocation = {
  href: '',
  pathname: '/admin',
  search: '',
};

Object.defineProperty(window, 'location', {
  value: mockLocation,
  writable: true,
});

// Helper to render with router
function renderWithRouter(ui: React.ReactElement, { route = '/admin' } = {}) {
  return render(<MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>);
}

// Test child component that uses context
function TestChild() {
  const { user, logout } = useAdminContext();
  return (
    <div data-testid="child-content">
      <span data-testid="user-email">{user?.email ?? 'no user'}</span>
      <button type="button" data-testid="logout-btn" onClick={logout}>
        Logout
      </button>
    </div>
  );
}

describe('AdminLayout', () => {
  const mockUser: AdminUserInfo = {
    email: 'admin@example.com',
    name: 'Admin User',
    avatarUrl: 'https://github.com/avatar.png',
  };

  const mockLogin = vi.fn();
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    mockLocation.href = '';
    // Default to loading state
    mockUseAdminAuth.mockReturnValue({
      user: null,
      loading: true,
      isAuthenticated: false,
      login: mockLogin,
      logout: mockLogout,
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('loading state', () => {
    it('should display loading spinner when checking auth', () => {
      renderWithRouter(
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
      );

      expect(screen.getByText('Checking authentication...')).toBeInTheDocument();
      expect(screen.queryByTestId('child-content')).not.toBeInTheDocument();
    });
  });

  describe('unauthenticated state', () => {
    it('should redirect to OAuth2 login when not authenticated', () => {
      mockUseAdminAuth.mockReturnValue({
        user: null,
        loading: false,
        isAuthenticated: false,
        login: mockLogin,
        logout: mockLogout,
      });

      renderWithRouter(
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
      );

      expect(mockLogin).toHaveBeenCalledOnce();
      expect(screen.getByText('Redirecting to login...')).toBeInTheDocument();
    });

    it('should store return URL in sessionStorage', () => {
      const sessionStorageSetItem = vi.spyOn(Storage.prototype, 'setItem');
      mockLocation.pathname = '/admin/feeds';
      mockLocation.search = '?filter=active';

      mockUseAdminAuth.mockReturnValue({
        user: null,
        loading: false,
        isAuthenticated: false,
        login: mockLogin,
        logout: mockLogout,
      });

      renderWithRouter(
        <Routes>
          <Route path="/admin/feeds" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
        { route: '/admin/feeds?filter=active' },
      );

      expect(sessionStorageSetItem).toHaveBeenCalledWith('admin-return-url', '/admin/feeds?filter=active');
    });
  });

  describe('authenticated state', () => {
    it('should render child routes when authenticated', async () => {
      mockUseAdminAuth.mockReturnValue({
        user: mockUser,
        loading: false,
        isAuthenticated: true,
        login: mockLogin,
        logout: mockLogout,
      });

      renderWithRouter(
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
      );

      await waitFor(() => {
        expect(screen.getByTestId('child-content')).toBeInTheDocument();
      });
    });

    it('should provide user context to child components', async () => {
      mockUseAdminAuth.mockReturnValue({
        user: mockUser,
        loading: false,
        isAuthenticated: true,
        login: mockLogin,
        logout: mockLogout,
      });

      renderWithRouter(
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
      );

      await waitFor(() => {
        expect(screen.getByTestId('user-email')).toHaveTextContent('admin@example.com');
      });
    });

    it('should provide logout function via context', async () => {
      mockUseAdminAuth.mockReturnValue({
        user: mockUser,
        loading: false,
        isAuthenticated: true,
        login: mockLogin,
        logout: mockLogout,
      });

      renderWithRouter(
        <Routes>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<TestChild />} />
          </Route>
        </Routes>,
      );

      await waitFor(() => {
        expect(screen.getByTestId('logout-btn')).toBeInTheDocument();
      });

      screen.getByTestId('logout-btn').click();
      expect(mockLogout).toHaveBeenCalledOnce();
    });
  });
});
