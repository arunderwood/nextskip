import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { FaroRoutes } from '@grafana/faro-react';
import DashboardView from './views/DashboardView';
import { JsonLdSchema } from './components/seo';
import './styles/App.css';

function App() {
  return (
    <div className="app">
      <JsonLdSchema />
      <main className="app-main">
        <FaroRoutes routesComponent={Routes}>
          <Route path="/" element={<DashboardView />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </FaroRoutes>
      </main>

      <footer className="app-footer">
        <p>Aggregating data from trusted amateur radio sources</p>
        <p className="footer-note">Data refreshes automatically • For reference only</p>
      </footer>
    </div>
  );
}

export default App;
