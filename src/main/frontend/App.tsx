import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import DashboardView from './views/DashboardView';
import './styles/App.css';

function App() {
  return (
    <div className="app">
      <main className="app-main">
        <Routes>
          <Route path="/" element={<DashboardView />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <footer className="app-footer">
        <p>Aggregating data from trusted amateur radio sources</p>
        <p className="footer-note">Data refreshes automatically • For reference only</p>
      </footer>
    </div>
  );
}

export default App;
