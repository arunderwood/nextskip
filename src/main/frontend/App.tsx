import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { FaroRoutes } from '@grafana/faro-react';
import DashboardView from './views/DashboardView';
import { JsonLdSchema } from './components/seo';
import { AuthProvider } from './auth';
import './styles/App.css';

// Lazy load admin views for code splitting
const AdminLayout = React.lazy(() => import('./views/admin/AdminLayout'));
const AdminLandingView = React.lazy(() => import('./views/admin/AdminLandingView'));
const FeedManagerView = React.lazy(() => import('./views/admin/FeedManagerView'));

function App() {
  return (
    <AuthProvider>
      <div className="app">
        <JsonLdSchema />
        <main className="app-main">
          <FaroRoutes routesComponent={Routes}>
            {/* Public dashboard */}
            <Route path="/" element={<DashboardView />} />

            {/* Admin routes - protected by AdminLayout */}
            <Route
              path="/admin"
              element={
                <React.Suspense fallback={<div>Loading admin...</div>}>
                  <AdminLayout />
                </React.Suspense>
              }
            >
              <Route index element={<AdminLandingView />} />
              <Route path="feeds" element={<FeedManagerView />} />
            </Route>

            <Route path="*" element={<Navigate to="/" replace />} />
          </FaroRoutes>
        </main>

        <footer className="app-footer">
          <p>Aggregating data from trusted amateur radio sources</p>
          <p className="footer-note">Data refreshes automatically • For reference only</p>
        </footer>
      </div>
    </AuthProvider>
  );
}

export default App;
