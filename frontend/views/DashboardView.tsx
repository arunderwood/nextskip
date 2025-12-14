import React, { useEffect, useState } from 'react';
import { PropagationEndpoint } from 'Frontend/generated/endpoints';
import type { PropagationResponse } from 'Frontend/generated/io/nextskip/propagation/api/PropagationEndpoint';
import { BentoGrid, BentoCard } from '../components/bento';
import { useDashboardCards } from '../hooks/useDashboardCards';
import SolarIndicesContent from '../components/cards/SolarIndicesContent';
import BandConditionsContent, {
  BandConditionsLegend,
} from '../components/cards/BandConditionsContent';
import './DashboardView.css';

function DashboardView() {
  const [data, setData] = useState<PropagationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());

  const fetchData = async () => {
    try {
      setError(null);
      const response = await PropagationEndpoint.getPropagationData();
      setData(response);
      setLastUpdate(new Date());
    } catch (err) {
      console.error('Error fetching propagation data:', err);
      setError('Failed to fetch propagation data. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Initial fetch
    fetchData();

    // Auto-refresh every 5 minutes
    const interval = setInterval(fetchData, 5 * 60 * 1000);

    return () => clearInterval(interval);
  }, []);

  const handleRefresh = () => {
    setLoading(true);
    fetchData();
  };

  if (loading && !data) {
    return (
      <div className="loading">
        <div className="loading-spinner">
          <div className="spinner"></div>
          <p>Loading propagation conditions...</p>
        </div>
      </div>
    );
  }

  if (error && !data) {
    return (
      <div className="error">
        <h3>⚠️ Error</h3>
        <p>{error}</p>
        <button onClick={handleRefresh} className="retry-button">
          Retry
        </button>
      </div>
    );
  }

  // Get card configurations from dashboard data
  const cardConfigs = useDashboardCards(data);

  // Build bento grid cards
  const bentoCards = cardConfigs.map((config) => {
    let component: React.ReactNode;

    switch (config.type) {
      case 'solar-indices':
        if (data?.solarIndices) {
          component = (
            <BentoCard
              config={config}
              title="Solar Indices"
              icon="☀️"
              subtitle={data.solarIndices.source}
              footer={
                <div className="info-box">
                  <strong>What this means:</strong>
                  <ul>
                    <li>
                      <strong>SFI:</strong> Higher values (150+) indicate better
                      HF propagation
                    </li>
                    <li>
                      <strong>K-Index:</strong> Lower values (0-2) mean quieter,
                      more stable conditions
                    </li>
                    <li>
                      <strong>A-Index:</strong> 24-hour average geomagnetic
                      activity (lower is better)
                    </li>
                  </ul>
                </div>
              }
            >
              <SolarIndicesContent solarIndices={data.solarIndices} />
            </BentoCard>
          );
        }
        break;

      case 'band-conditions':
        if (data?.bandConditions && data.bandConditions.length > 0) {
          component = (
            <BentoCard
              config={config}
              title="HF Band Conditions"
              icon="📻"
              subtitle="Current propagation by amateur radio band"
              footer={<BandConditionsLegend />}
            >
              <BandConditionsContent bandConditions={data.bandConditions} />
            </BentoCard>
          );
        }
        break;
    }

    return {
      config,
      component,
    };
  });

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <div className="header-info">
          <h2>Current Conditions</h2>
          <p className="last-update">
            Last updated: {lastUpdate.toLocaleTimeString()}
          </p>
        </div>
        <button
          onClick={handleRefresh}
          disabled={loading}
          className="refresh-button"
          title="Refresh data"
        >
          {loading ? '⟳' : '↻'} Refresh
        </button>
      </div>

      {error && (
        <div className="error-banner">
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
