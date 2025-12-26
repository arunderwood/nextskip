import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { FaroRoutes } from '@grafana/faro-react';
import DashboardView from './views/DashboardView';
import './styles/App.css';

// Use FaroRoutes only when Faro is initialized (collector URL configured)
const AppRoutes = import.meta.env.VITE_FARO_COLLECTOR_URL ? FaroRoutes : Routes;

function App() {
  return (
    <div className="app">
      <main className="app-main">
        <AppRoutes>
          <Route path="/" element={<DashboardView />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </AppRoutes>
      </main>

      <footer className="app-footer">
        <p>Data sources: NOAA Space Weather Prediction Center, HamQSL.com</p>
        <p className="footer-note">Conditions update every 5-30 minutes • For reference only</p>
      </footer>
    </div>
  );
}

export default App;
