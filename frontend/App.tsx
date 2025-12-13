import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import DashboardView from './views/DashboardView';
import './styles/App.css';

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <div className="header-content">
          <h1>📡 NextSkip</h1>
          <p className="subtitle">Ham Radio Propagation Dashboard</p>
        </div>
      </header>

      <main className="app-main">
        <Routes>
          <Route path="/" element={<DashboardView />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <footer className="app-footer">
        <p>
          Data sources: NOAA Space Weather Prediction Center, HamQSL.com
        </p>
        <p className="footer-note">
          Conditions update every 5-30 minutes • For reference only
        </p>
      </footer>
    </div>
  );
}

export default App;
