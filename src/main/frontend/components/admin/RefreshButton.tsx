import React, { useState } from 'react';
import './RefreshButton.css';

interface RefreshButtonProps {
  feedId: string;
  onRefresh(feedId: string): Promise<boolean>;
}

type RefreshState = 'idle' | 'loading' | 'success' | 'error';

/**
 * Button to trigger a manual feed refresh.
 *
 * Shows visual feedback for loading, success, and error states.
 */
export default function RefreshButton({ feedId, onRefresh }: RefreshButtonProps) {
  const [state, setState] = useState<RefreshState>('idle');

  const handleClick = async () => {
    if (state === 'loading') return;

    setState('loading');
    try {
      const success = await onRefresh(feedId);
      setState(success ? 'success' : 'error');
    } catch {
      setState('error');
    }

    // Reset to idle after 2 seconds
    setTimeout(() => setState('idle'), 2000);
  };

  const getButtonContent = () => {
    switch (state) {
      case 'loading':
        return (
          <>
            <span className="refresh-spinner" />
            Refreshing...
          </>
        );
      case 'success':
        return (
          <>
            <span className="refresh-icon">✓</span>
            Refreshed
          </>
        );
      case 'error':
        return (
          <>
            <span className="refresh-icon">✕</span>
            Failed
          </>
        );
      default:
        return (
          <>
            <span className="refresh-icon">↻</span>
            Refresh Now
          </>
        );
    }
  };

  return (
    <button type="button" className={`refresh-button ${state}`} onClick={handleClick} disabled={state === 'loading'}>
      {getButtonContent()}
    </button>
  );
}
