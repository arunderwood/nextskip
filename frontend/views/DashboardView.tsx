import React, { useEffect, useState } from 'react';
import { PropagationEndpoint } from 'Frontend/generated/endpoints';
import type { PropagationResponse } from 'Frontend/generated/io/nextskip/propagation/api/PropagationEndpoint';
import SolarIndicesCard from '../components/SolarIndicesCard';
import BandConditionsTable from '../components/BandConditionsTable';
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
        {data?.solarIndices && (
          <SolarIndicesCard solarIndices={data.solarIndices} />
        )}

        {data?.bandConditions && data.bandConditions.length > 0 && (
          <BandConditionsTable bandConditions={data.bandConditions} />
        )}

        {(!data?.solarIndices && !data?.bandConditions) && (
          <div className="no-data">
            <p>No propagation data available at this time.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default DashboardView;
