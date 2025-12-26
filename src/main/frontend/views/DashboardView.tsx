import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Radio, AlertTriangle } from 'lucide-react';
import {
  PropagationEndpoint,
  ActivationsEndpoint,
  ContestEndpoint,
  MeteorEndpoint,
} from 'Frontend/generated/endpoints';
import type PropagationResponse from 'Frontend/generated/io/nextskip/propagation/api/PropagationResponse';
import type ActivationsResponse from 'Frontend/generated/io/nextskip/activations/api/ActivationsResponse';
import type ContestsResponse from 'Frontend/generated/io/nextskip/contests/api/ContestsResponse';
import type MeteorShowersResponse from 'Frontend/generated/io/nextskip/meteors/api/MeteorShowersResponse';
import type { DashboardData } from '../components/cards/types';
import { ActivityGrid } from '../components/activity';
import { useDashboardCards } from '../hooks/useDashboardCards';
import { getRegisteredCards } from '../components/cards/CardRegistry';
import { ThemeToggle } from '../components/ThemeToggle';
import { HelpButton, HelpModal } from '../components/help';
import './DashboardView.css';

// Import card modules to trigger registration
import '../components/cards/propagation';
import '../components/cards/activations';
import '../components/cards/contests';
import '../components/cards/meteor-showers';

function DashboardView() {
  const [propagationData, setPropagationData] = useState<PropagationResponse | undefined>(undefined);
  const [activationsData, setActivationsData] = useState<ActivationsResponse | undefined>(undefined);
  const [contestsData, setContestsData] = useState<ContestsResponse | undefined>(undefined);
  const [meteorShowersData, setMeteorShowersData] = useState<MeteorShowersResponse | undefined>(undefined);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdate, setLastUpdate] = useState<Date>(new Date());
  const [isHelpOpen, setIsHelpOpen] = useState(() => {
    if (typeof window === 'undefined') return false;
    const hasVisited = localStorage.getItem('nextskip-visited');
    if (!hasVisited) {
      localStorage.setItem('nextskip-visited', 'true');
      return true; // Open help on first visit
    }
    return false;
  });

  // Memoize fetchData to avoid recreation on every render
  // Hilla pattern: Keep async/await with generated endpoint methods
  const fetchData = useCallback(async () => {
    try {
      setError(null);

      // Fetch data from all Hilla endpoints in parallel
      const [propagation, activations, contests, meteorShowers] = await Promise.all([
        PropagationEndpoint.getPropagationData(),
        ActivationsEndpoint.getActivations(),
        ContestEndpoint.getContests(),
        MeteorEndpoint.getMeteorShowers(),
      ]);

      setPropagationData(propagation);
      setActivationsData(activations);
      setContestsData(contests);
      setMeteorShowersData(meteorShowers);
      setLastUpdate(new Date());
    } catch (err) {
      console.error('Error fetching dashboard data:', err);
      setError('Failed to fetch dashboard data. Please try again later.');
    } finally {
      setLoading(false);
    }
  }, []); // Empty deps: setState functions are stable, Hilla endpoint methods are static

  useEffect(() => {
    // Set document title
    document.title = 'NextSkip - Amateur Radio Activity Dashboard';

    // Initial fetch
    fetchData();

    // Auto-refresh every 5 minutes
    const interval = setInterval(fetchData, 5 * 60 * 1000);

    return () => clearInterval(interval);
  }, [fetchData]);

  // Memoize dashboardData to prevent unnecessary recalculations in useDashboardCards
  // Hilla pattern: DashboardData interface references Frontend/generated/ types
  const dashboardData: DashboardData = useMemo(
    () => ({
      propagation: propagationData,
      activations: activationsData,
      contests: contestsData,
      meteorShowers: meteorShowersData,
      // Future modules will add their data here:
      // satellites: satellitesData,
    }),
    [propagationData, activationsData, contestsData, meteorShowersData],
  );

  // Get card configurations from registry (must be called before conditional returns)
  const cardConfigs = useDashboardCards(dashboardData);

  // Build activity grid cards using the registry (memoized to avoid O(n*m) recalculation)
  // IMPORTANT: Must be called before conditional returns to satisfy Rules of Hooks
  const activityCards = useMemo(() => {
    const cards = getRegisteredCards();

    return cardConfigs.map((config) => {
      // Match the card definition by checking if its createConfig produces this config's ID
      const cardDef = cards.find((c) => {
        const cfgResult = c.createConfig(dashboardData);
        // Handle both single config and array of configs
        if (Array.isArray(cfgResult)) {
          return cfgResult.some((cfg) => cfg.id === config.id);
        }
        return cfgResult?.id === config.id;
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
  }, [cardConfigs, dashboardData]);

  if (loading && !propagationData) {
    return (
      <div className="loading">
        <div className="loading-spinner">
          <div className="spinner" />
          <p>Loading propagation conditions...</p>
        </div>
      </div>
    );
  }

  if (error && !propagationData) {
    return (
      <div className="error">
        <h3>
          {/* eslint-disable-next-line react-perf/jsx-no-new-object-as-prop -- One-time error display */}
          <AlertTriangle size={20} style={{ verticalAlign: 'middle', marginRight: '8px' }} />
          Error
        </h3>
        <p>{error}</p>
        <button
          type="button"
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

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-info">
          <div className="header-row">
            <h1 className="dashboard-title">
              <Radio className="dashboard-icon" size={28} aria-hidden="true" />
              NextSkip
            </h1>
            <div className="header-actions">
              <HelpButton onClick={() => setIsHelpOpen(true)} />
              <ThemeToggle />
            </div>
          </div>
          <p className="dashboard-subtitle">
            Amateur Radio Activity Dashboard
            <span className="last-update" aria-live="polite" aria-atomic="true">
              Updated {lastUpdate.toLocaleTimeString()}
            </span>
          </p>
        </div>
      </header>

      {error ? (
        <div className="error-banner" role="alert">
          <p>{error}</p>
        </div>
      ) : null}

      {activityCards.length > 0 ? (
        <ActivityGrid cards={activityCards} />
      ) : (
        <div className="no-data">
          <p>No propagation data available at this time.</p>
        </div>
      )}

      <HelpModal isOpen={isHelpOpen} onClose={() => setIsHelpOpen(false)} />
    </div>
  );
}

export default DashboardView;
