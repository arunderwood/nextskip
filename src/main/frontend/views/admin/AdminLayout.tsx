import React from 'react';
import { Outlet, NavLink } from 'react-router-dom';
import { useAuth } from '../../auth';
import '../../styles/admin.css';

/**
 * Admin layout component with authentication and navigation.
 *
 * Handles:
 * - Authentication check (redirects to GitHub OAuth if not logged in)
 * - Loading state while checking auth
 * - Sidebar navigation between admin views
 * - Logout functionality
 */
export default function AdminLayout() {
  const { state } = useAuth();

  // Show loading state while checking auth
  if (state.loading) {
    return (
      <div className="admin-loading">
        <div className="admin-loading-spinner" />
        <p>Loading...</p>
      </div>
    );
  }

  // Redirect to GitHub OAuth if not authenticated
  if (!state.user) {
    window.location.href = '/oauth2/authorization/github';
    return null;
  }

  return (
    <div className="admin-layout">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-header">
          <h1 className="admin-title">NextSkip Admin</h1>
          <div className="admin-user">
            <span className="admin-user-name">{state.user.name || state.user.email}</span>
          </div>
        </div>

        <nav className="admin-nav">
          <NavLink
            to="/admin"
            end
            className={({ isActive }) => (isActive ? 'admin-nav-link active' : 'admin-nav-link')}
          >
            Dashboard
          </NavLink>
          <NavLink
            to="/admin/feeds"
            className={({ isActive }) => (isActive ? 'admin-nav-link active' : 'admin-nav-link')}
          >
            Feed Manager
          </NavLink>
        </nav>

        <div className="admin-sidebar-footer">
          <a href="/admin/logout" className="admin-logout-link">
            Logout
          </a>
          <a href="/" className="admin-back-link">
            Back to Dashboard
          </a>
        </div>
      </aside>

      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
