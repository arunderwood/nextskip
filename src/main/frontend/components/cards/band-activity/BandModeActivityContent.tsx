/**
 * BandModeActivityContent - Composed content for band+mode activity cards.
 *
 * Composes sub-components to display activity data, trends, DX reach,
 * propagation paths, and condition rating.
 *
 * Handles display states:
 * - Full data (activity + condition): Shows all sub-components
 * - Activity only: Hides ConditionBadge
 * - Condition only + supported mode: Shows "No Activity Data"
 * - Unsupported mode: Shows "Coming soon..."
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
import { NoActivityPlaceholder } from './NoActivityPlaceholder';
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
 */
export function BandModeActivityContent({ activity, condition, modeConfig, band: _band }: Props) {
  // Unsupported mode: show coming soon placeholder
  if (!modeConfig.isSupported) {
    return <ComingSoonPlaceholder modeName={modeConfig.displayName ?? String(modeConfig.mode)} />;
  }

  // Supported mode but no activity data: show no activity placeholder
  // (We still render if we have condition data, just with placeholder for activity)
  if (!activity) {
    return (
      <div className="band-mode-activity-content">
        <NoActivityPlaceholder />
        {condition?.rating ? <ConditionBadge rating={condition.rating} /> : null}
      </div>
    );
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
