/**
 * BandModeActivityContent - Composed content for band+mode activity cards.
 *
 * Composes sub-components to display activity data, trends, DX reach,
 * propagation paths, and condition rating.
 *
 * Cards are only rendered when activity data exists (no placeholder states).
 * Unsupported modes with activity still show "Coming soon..." message.
 */

import React from 'react';
import type { ModeConfig } from 'Frontend/config/modeRegistry';
import type { BandModeActivity } from 'Frontend/types/spotterSource';
import type BandCondition from 'Frontend/generated/io/nextskip/propagation/model/BandCondition';
import { ActivityBar } from './ActivityBar';
import { TrendIndicator } from './TrendIndicator';
import { DxReachDisplay } from './DxReachDisplay';
import { PathStatusGrid } from './PathStatusGrid';
import { ConditionBadge } from './ConditionBadge';
import { ComingSoonPlaceholder } from './ComingSoonPlaceholder';
import './BandModeActivityContent.css';

interface Props {
  /** Activity data from spotter source (optional) */
  activity?: BandModeActivity;

  /** Condition data from propagation module (optional) */
  condition?: BandCondition;

  /** Mode configuration from registry */
  modeConfig: ModeConfig;

  /** Band identifier for display */
  band: string;
}

/**
 * Renders the appropriate content based on available data and mode support.
 * Note: Cards without activity are filtered out in createConfig, so activity should always exist.
 */
export function BandModeActivityContent({ activity, condition, modeConfig, band: _band }: Props) {
  // Unsupported mode: show coming soon placeholder
  if (!modeConfig.isSupported) {
    return <ComingSoonPlaceholder modeName={modeConfig.displayName ?? String(modeConfig.mode)} />;
  }

  // Activity should always exist since cards without activity are not created
  // But handle gracefully just in case
  if (!activity) {
    return null;
  }

  // Full content with activity data
  return (
    <div className="band-mode-activity-content">
      <ActivityBar spotCount={activity.spotCount} windowMinutes={activity.windowMinutes} />

      <TrendIndicator trendPercentage={activity.trendPercentage} />

      <DxReachDisplay maxDxKm={activity.maxDxKm} maxDxPath={activity.maxDxPath} />

      <PathStatusGrid activePaths={activity.activePaths} />

      {condition?.rating ? <ConditionBadge rating={condition.rating} /> : null}
    </div>
  );
}

export default BandModeActivityContent;
