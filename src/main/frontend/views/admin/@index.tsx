/**
 * Admin Landing Page - /admin route
 *
 * This is the default view when navigating to /admin.
 * Displays a welcome message and navigation cards to admin sections.
 */

import React from 'react';
import { Link } from 'react-router-dom';
import { useAdminContext } from './@layout';

/**
 * Navigation cards for the admin dashboard.
 * Each card links to a different admin section.
 */
const adminSections = [
  {
    path: '/admin/feeds',
    title: 'Feed Manager',
    description: 'Monitor feed health, view refresh schedules, and manually trigger feed updates.',
    icon: 'rss_feed',
  },
  {
    path: '/admin/system',
    title: 'System Status',
    description: 'View system health, cache statistics, and application metrics.',
    icon: 'monitor_heart',
    disabled: true,
  },
  {
    path: '/admin/users',
    title: 'User Management',
    description: 'Manage admin users, permissions, and access controls.',
    icon: 'group',
    disabled: true,
  },
];

export default function AdminLandingView() {
  const { user } = useAdminContext();

  return (
    <div className="admin-page">
      {/* Page Header */}
      <header className="admin-page-header">
        <h1 className="admin-page-title">Welcome back{user?.name ? `, ${user.name}` : ''}</h1>
        <p className="admin-page-subtitle">Manage NextSkip feeds, system health, and administrative functions.</p>
      </header>

      {/* Navigation Cards */}
      <nav aria-label="Admin sections">
        <div className="admin-nav-cards">
          {adminSections.map((section) =>
            section.disabled ? (
              <div key={section.path} className="admin-nav-card admin-nav-card--disabled">
                <div className="admin-card">
                  <div className="admin-card-header">
                    <div className="admin-card-icon">
                      <span className="material-icons" aria-hidden="true">
                        {section.icon}
                      </span>
                    </div>
                  </div>
                  <h2 className="admin-card-title">{section.title}</h2>
                  <p className="admin-card-description">{section.description}</p>
                  <div className="admin-nav-card-arrow">
                    <span>Coming soon</span>
                  </div>
                </div>
              </div>
            ) : (
              <Link key={section.path} to={section.path} className="admin-nav-card">
                <div className="admin-card admin-card--clickable">
                  <div className="admin-card-header">
                    <div className="admin-card-icon">
                      <span className="material-icons" aria-hidden="true">
                        {section.icon}
                      </span>
                    </div>
                  </div>
                  <h2 className="admin-card-title">{section.title}</h2>
                  <p className="admin-card-description">{section.description}</p>
                  <div className="admin-nav-card-arrow">
                    <span>Go to {section.title}</span>
                    <span className="material-icons" aria-hidden="true">
                      arrow_forward
                    </span>
                  </div>
                </div>
              </Link>
            ),
          )}
        </div>
      </nav>
    </div>
  );
}

// Export view configuration for Hilla
export const config = {
  title: 'Admin Dashboard',
};
