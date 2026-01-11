/**
 * useAdminAuth - Hook for managing admin authentication state
 *
 * Handles:
 * - Fetching current user info on mount
 * - Redirecting to OAuth2 login when not authenticated
 * - Logout functionality
 */

import { useState, useEffect, useCallback } from 'react';
import { AdminAuthEndpoint } from 'Frontend/generated/endpoints';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';

export type AdminAuthState = {
  /** The authenticated admin user, or null if not authenticated */
  user: AdminUserInfo | null;
  /** Whether the auth state is being loaded */
  loading: boolean;
  /** Error message if auth check failed */
  error: string | null;
  /** Whether the user is authenticated as admin */
  isAuthenticated: boolean;
};

const OAUTH2_LOGIN_URL = '/oauth2/authorization/github';

/**
 * Hook for managing admin authentication.
 *
 * On mount, attempts to fetch the current user's info from the backend.
 * If the request fails with a 401, redirects to OAuth2 login.
 *
 * @returns AuthState object with user info, loading state, and auth methods
 */
export function useAdminAuth() {
  const [user, setUser] = useState<AdminUserInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const checkAuth = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const currentUser = await AdminAuthEndpoint.getCurrentUser();
      setUser(currentUser ?? null);
    } catch {
      // Hilla throws on 401 - this means user is not authenticated
      // We'll let the component decide whether to redirect
      setUser(null);
      setError('Not authenticated');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  /**
   * Redirects to OAuth2 login page.
   * Call this when user needs to authenticate.
   */
  const login = useCallback(() => {
    window.location.href = OAUTH2_LOGIN_URL;
  }, []);

  /**
   * Logs out the current user and redirects to login.
   */
  const logout = useCallback(async () => {
    try {
      await AdminAuthEndpoint.logout();
    } catch {
      // Ignore logout errors - session might already be invalid
    }
    setUser(null);
    // Redirect to admin root which will trigger OAuth2 login
    window.location.href = '/admin';
  }, []);

  return {
    user,
    loading,
    error,
    isAuthenticated: user !== null,
    login,
    logout,
    refreshAuth: checkAuth,
  };
}

export type UseAdminAuthReturn = ReturnType<typeof useAdminAuth>;
