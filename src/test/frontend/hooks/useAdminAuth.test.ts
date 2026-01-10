/**
 * Tests for useAdminAuth hook.
 *
 * Tests the admin authentication state management including:
 * - Fetching current user on mount
 * - Handling authentication errors
 * - Login redirect
 * - Logout functionality
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act, waitFor } from '@testing-library/react';
import { useAdminAuth } from 'Frontend/hooks/useAdminAuth';
import { AdminAuthEndpoint } from 'Frontend/generated/endpoints';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';

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

describe('useAdminAuth', () => {
  const mockUser: AdminUserInfo = {
    email: 'admin@example.com',
    name: 'Admin User',
    avatarUrl: 'https://github.com/avatar.png',
  };

  beforeEach(() => {
    vi.clearAllMocks();
    mockLocation.href = '';
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('initialization', () => {
    it('should start in loading state', () => {
      // Don't resolve the promise yet to test loading state
      AdminAuthEndpoint.getCurrentUser.mockReturnValue(new Promise(() => {}));

      const { result } = renderHook(() => useAdminAuth());

      expect(result.current.loading).toBe(true);
      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('should fetch current user on mount', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(mockUser);

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(AdminAuthEndpoint.getCurrentUser).toHaveBeenCalledOnce();
      expect(result.current.user).toEqual(mockUser);
      expect(result.current.isAuthenticated).toBe(true);
      expect(result.current.error).toBeNull();
    });

    it('should handle undefined user response', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(undefined);

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
    });

    it('should handle authentication error', async () => {
      AdminAuthEndpoint.getCurrentUser.mockRejectedValue(new Error('Unauthorized'));

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(result.current.user).toBeNull();
      expect(result.current.isAuthenticated).toBe(false);
      expect(result.current.error).toBe('Not authenticated');
    });
  });

  describe('login', () => {
    it('should redirect to OAuth2 login URL', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(undefined);

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      act(() => {
        result.current.login();
      });

      expect(mockLocation.href).toBe('/oauth2/authorization/github');
    });
  });

  describe('logout', () => {
    it('should call logout endpoint and clear user', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(mockUser);
      AdminAuthEndpoint.logout.mockResolvedValue(undefined);

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.isAuthenticated).toBe(true);
      });

      await act(async () => {
        await result.current.logout();
      });

      expect(AdminAuthEndpoint.logout).toHaveBeenCalledOnce();
      expect(result.current.user).toBeNull();
      expect(mockLocation.href).toBe('/admin');
    });

    it('should handle logout error gracefully', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(mockUser);
      AdminAuthEndpoint.logout.mockRejectedValue(new Error('Session invalid'));

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.isAuthenticated).toBe(true);
      });

      // Should not throw even if logout fails
      await act(async () => {
        await result.current.logout();
      });

      expect(result.current.user).toBeNull();
      expect(mockLocation.href).toBe('/admin');
    });
  });

  describe('refreshAuth', () => {
    it('should refetch user info when called', async () => {
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(mockUser);

      const { result } = renderHook(() => useAdminAuth());

      await waitFor(() => {
        expect(result.current.loading).toBe(false);
      });

      expect(AdminAuthEndpoint.getCurrentUser).toHaveBeenCalledOnce();

      // Update mock for next call
      const updatedUser = { ...mockUser, name: 'Updated Name' };
      AdminAuthEndpoint.getCurrentUser.mockResolvedValue(updatedUser);

      await act(async () => {
        await result.current.refreshAuth();
      });

      expect(AdminAuthEndpoint.getCurrentUser).toHaveBeenCalledTimes(2);
      expect(result.current.user?.name).toBe('Updated Name');
    });
  });
});
