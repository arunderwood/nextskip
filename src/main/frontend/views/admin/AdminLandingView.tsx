import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../auth';

/**
 * Admin landing page with overview and quick links.
 */
export default function AdminLandingView() {
  const { state } = useAuth();

  return (
    <div className="admin-landing">
      <header className="admin-page-header">
        <h2>Welcome, {state.user?.name || 'Admin'}</h2>
        <p>Manage NextSkip data feeds and system status</p>
      </header>

      <div className="admin-quick-links">
        <Link to="/admin/feeds" className="admin-quick-link-card">
          <div className="admin-quick-link-icon">📡</div>
          <h3>Feed Manager</h3>
          <p>View feed status and trigger manual refreshes</p>
        </Link>
      </div>
    </div>
  );
}
