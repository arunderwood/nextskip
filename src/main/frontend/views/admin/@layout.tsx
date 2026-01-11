/**
 * Admin Layout - Layout wrapper for all /admin/* routes
 *
 * This file follows Hilla's file-based routing convention.
 * The @layout.tsx file automatically wraps all sibling and nested views.
 *
 * Provides:
 * - Authentication check with redirect to OAuth2 login
 * - Loading state while auth is being checked
 * - Navigation sidebar with links to admin sections
 * - Common layout structure for all admin pages
 */

import React, { createContext, useContext, useMemo } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import { useAdminAuth } from '../../hooks/useAdminAuth';
import AdminNav from '../../components/admin/AdminNav';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';
import '../../styles/admin.css';
import './admin.css';

// Context for sharing user info with child components
interface AdminContextType {
  user: AdminUserInfo | null;
  logout(): Promise<void>;
}

const AdminContext = createContext<AdminContextType>({
  user: null,
  logout: () => Promise.resolve(),
});

/**
 * Hook to access admin context from child routes.
 * Provides access to the authenticated user and logout function.
 */
export function useAdminContext() {
  return useContext(AdminContext);
}

/**
 * Admin layout component that wraps all /admin/* routes.
 *
 * On mount, checks if user is authenticated as admin:
 * - If loading: shows loading spinner
 * - If not authenticated: redirects to OAuth2 login
 * - If authenticated: renders child routes via Outlet
 */
export default function AdminLayout() {
  const { user, loading, isAuthenticated, login, logout } = useAdminAuth();
  const location = useLocation();

  // Memoize context value to prevent unnecessary re-renders
  // Must be called unconditionally (before any early returns)
  const contextValue = useMemo(() => ({ user, logout }), [user, logout]);

  // Show loading state while checking auth
  if (loading) {
    return (
      <div className="admin-layout admin-loading">
        <div className="admin-loading-content">
          <div className="admin-spinner" />
          <p>Checking authentication...</p>
        </div>
      </div>
    );
  }

  // Redirect to OAuth2 login if not authenticated
  if (!isAuthenticated) {
    // Store the intended destination for after login
    const returnUrl = location.pathname + location.search;
    sessionStorage.setItem('admin-return-url', returnUrl);

    // Redirect to OAuth2 login
    login();

    return (
      <div className="admin-layout admin-redirecting">
        <div className="admin-loading-content">
          <div className="admin-spinner" />
          <p>Redirecting to login...</p>
        </div>
      </div>
    );
  }

  return (
    <AdminContext.Provider value={contextValue}>
      <div className="admin-layout admin-layout--with-nav">
        <AdminNav user={user} onLogout={logout} />
        <main className="admin-main">
          {/* Outlet renders the matched child route */}
          <Outlet />
        </main>
      </div>
    </AdminContext.Provider>
  );
}

// Export view configuration for Hilla
export const config = {
  title: 'Admin',
};
