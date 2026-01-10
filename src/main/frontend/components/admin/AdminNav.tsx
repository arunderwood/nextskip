/**
 * Admin Navigation Component
 *
 * Provides the main navigation for the admin area including:
 * - Logo/brand link back to admin home
 * - Navigation links to admin sections (Feed Manager, etc.)
 * - User info and logout button
 */

import React from 'react';
import { NavLink } from 'react-router-dom';
import type AdminUserInfo from 'Frontend/generated/io/nextskip/admin/api/AdminUserInfo';
import './AdminNav.css';

interface AdminNavProps {
  user: AdminUserInfo | null;
  onLogout(): Promise<void>;
}

/**
 * Navigation items for the admin sidebar.
 * Add new admin sections here as they are implemented.
 */
const navItems = [
  { path: '/admin', label: 'Dashboard', icon: 'dashboard', exact: true },
  { path: '/admin/feeds', label: 'Feed Manager', icon: 'rss_feed', exact: false },
];

export default function AdminNav({ user, onLogout }: AdminNavProps) {
  return (
    <nav className="admin-nav" aria-label="Admin navigation">
      {/* Brand */}
      <div className="admin-nav-brand">
        <NavLink to="/admin" className="admin-nav-logo">
          <span className="admin-nav-logo-icon" aria-hidden="true">
            NS
          </span>
          <span className="admin-nav-logo-text">NextSkip Admin</span>
        </NavLink>
      </div>

      {/* Navigation Links */}
      <ul className="admin-nav-links">
        {navItems.map((item) => (
          <li key={item.path}>
            <NavLink
              to={item.path}
              end={item.exact}
              className={({ isActive }) => `admin-nav-link ${isActive ? 'admin-nav-link--active' : ''}`}
            >
              <span className="material-icons admin-nav-link-icon" aria-hidden="true">
                {item.icon}
              </span>
              <span className="admin-nav-link-label">{item.label}</span>
            </NavLink>
          </li>
        ))}
      </ul>

      {/* User Section */}
      <div className="admin-nav-user">
        {user ? (
          <>
            <div className="admin-nav-user-info">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt="" className="admin-nav-avatar" aria-hidden="true" />
              ) : (
                <div className="admin-nav-avatar admin-nav-avatar--placeholder" aria-hidden="true">
                  {user.name?.charAt(0) ?? user.email?.charAt(0) ?? '?'}
                </div>
              )}
              <div className="admin-nav-user-details">
                <span className="admin-nav-user-name">{user.name ?? 'Admin'}</span>
                {user.email ? <span className="admin-nav-user-email">{user.email}</span> : null}
              </div>
            </div>
            <button type="button" onClick={onLogout} className="admin-nav-logout" aria-label="Log out of admin area">
              <span className="material-icons" aria-hidden="true">
                logout
              </span>
            </button>
          </>
        ) : null}
      </div>
    </nav>
  );
}
