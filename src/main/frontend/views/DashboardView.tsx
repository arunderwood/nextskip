import React, { useEffect, useState } from 'react';
import { PropagationEndpoint, ActivationsEndpoint } from 'Frontend/generated/endpoints';
import type PropagationResponse from 'Frontend/generated/io/nextskip/propagation/api/PropagationResponse';
import type ActivationsResponse from 'Frontend/generated/io/nextskip/activations/api/ActivationsResponse';
import type { DashboardData } from '../components/cards/types';
import { BentoGrid } from '../components/bento';
import { useDashboardCards } from '../hooks/useDashboardCards';
import { getRegisteredCards } from '../components/cards/CardRegistry';
import { ThemeToggle } from '../components/ThemeToggle';
import './DashboardView.css';

// Import card modules to trigger registration
import '../components/cards/propagation';
import '../components/cards/activations';

function DashboardView() {
  const [propagationData, setPropagationData] = useState<PropagationResponse | undefined>(
    undefined
  );
  const [activationsData, setActivationsData] = useState<ActivationsResponse | undefined>(
    undefined
  );
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const fetchData = async () => {
    try {
      setError(null);

      // Fetch data from all modules in parallel
      const [propagation, activations] = await Promise.all([
        PropagationEndpoint.getPropagationData(),
        ActivationsEndpoint.getActivations(),
      ]);

      setPropagationData(propagation);
      setActivationsData(activations);
      setLastUpdate(new Date());
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to fetch dashboard data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Set document title
    document.title = 'NextSkip - HF Propagation Dashboard';

    // Initial fetch
    fetchData();

    // Auto-refresh every 5 minutes
    const interval = setInterval(fetchData, 5 * 60 * 1000);

    return () => clearInterval(interval);
  }, []);

  // Combine all module data into DashboardData structure
  const dashboardData: DashboardData = {
    propagation: propagationData,
    activations: activationsData,
    // Future modules will add their data here:
    // contests: contestsData,
    // satellites: satellitesData,
  };

  // Get card configurations from registry (must be called before conditional returns)
  const cardConfigs = useDashboardCards(dashboardData);

  if (loading && !propagationData) {
    return (
      <div className="loading">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading propagation conditions...</p>
        </div>
      </div>
    );
  }

  if (error && !propagationData) {
    return (
      <div className="error">
        <h3>⚠️ Error</h3>
        <p>{error}</p>
        <button
          onClick={() => {
            setLoading(true);
            fetchData();
          }}
          className="retry-button"
        >
          Retry
        </button>
      </div>
    );
  }

  // Build bento grid cards using the registry
  const cards = getRegisteredCards();
  const bentoCards = cardConfigs.map((config) => {
    // Match the card definition by checking if its createConfig produces this config's ID
    const cardDef = cards.find((c) => {
      const cfg = c.createConfig(dashboardData);
      return cfg?.id === config.id;
    });

    if (!cardDef) {
      return { config, component: null };
    }

    const component = cardDef.render(dashboardData, config);

    return {
      config,
      component,
    };
  });

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-info">
          <div className="header-row">
            <h1 className="dashboard-title">
              <span className="dashboard-icon" aria-hidden="true">
                📡
              </span>
              NextSkip
            </h1>
            <ThemeToggle />
          </div>
          <p className="dashboard-subtitle">
            HF Propagation Dashboard
            <span className="last-update" aria-live="polite" aria-atomic="true">
              Updated {lastUpdate.toLocaleTimeString()}
            </span>
          </p>
        </div>
      </header>

      {error && (
        <div className="error-banner" role="alert">
          <p>{error}</p>
        </div>
      )}

      <div className="dashboard-content">
        {bentoCards.length > 0 ? (
          <BentoGrid cards={bentoCards} />
        ) : (
          <div className="no-data">
            <p>No propagation data available at this time.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default DashboardView;
