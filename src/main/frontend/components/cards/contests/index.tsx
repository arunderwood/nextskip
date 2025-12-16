/**
 * Contests Card - Card registration for upcoming amateur radio contests.
 *
 * This module registers the contests activity card with the global registry,
 * enabling it to appear in the dashboard without modifying core files.
 */

import React from 'react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { BentoCardConfig } from 'Frontend/types/bento';
import { BentoCard } from '../../bento';
import { calculatePriority, priorityToHotness } from '../../bento/usePriorityCalculation';
import ContestsContent from './ContestsContent';
import EventStatus from 'Frontend/generated/io/nextskip/common/model/EventStatus';

/* eslint-disable react/jsx-key */

/**
 * Contests Card Definition
 */
const contestsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.contests?.contests);
  },

  createConfig: (data: DashboardData) => {
    const contests = data.contests?.contests;
    if (!contests) return null;

    const activeCount = data.contests?.activeCount ?? 0;
    const upcomingCount = data.contests?.upcomingCount ?? 0;

    // Calculate priority based on active and upcoming contests
    // Score: active contests are worth more (10 points each), upcoming worth 5
    const score = Math.min(100, (activeCount * 10) + (upcomingCount * 5));

    // Favorable: any active contest or 2+ upcoming
    const isFavorable = activeCount > 0 || upcomingCount >= 2;

    // Rating based on active/upcoming counts
    const rating =
      activeCount > 0 ? ('GOOD' as const) :
      upcomingCount >= 2 ? ('FAIR' as const) :
      ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score,
      rating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'contests',
      type: 'contests',
      size: 'standard',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: BentoCardConfig) => {
    const contestsData = data.contests;
    if (!contestsData?.contests) return null;

    // Filter out undefined values to ensure type safety
    const validContests = contestsData.contests.filter((c) => c !== undefined);

    // Sort contests: Active first, then upcoming, then ended
    const sortedContests = [...validContests].sort((a, b) => {
      // Active contests first
      if (a.status === EventStatus.ACTIVE && b.status !== EventStatus.ACTIVE) return -1;
      if (a.status !== EventStatus.ACTIVE && b.status === EventStatus.ACTIVE) return 1;

      // Among active contests, ending soon comes first
      if (a.status === EventStatus.ACTIVE && b.status === EventStatus.ACTIVE) {
        if (a.endingSoon && !b.endingSoon) return -1;
        if (!a.endingSoon && b.endingSoon) return 1;
      }

      // Then by score (higher first)
      return (b.score ?? 0) - (a.score ?? 0);
    });

    return (
      <BentoCard
        config={config}
        title="Contests"
        icon="🏆"
        subtitle="Amateur Radio Competitions"
        footer={
          <div className="info-box">
            <strong>About Contests:</strong> Amateur radio contests are competitive
            operating events where participants make as many contacts as possible.
            Higher counts indicate more contest activity and opportunities.
          </div>
        }
      >
        <ContestsContent
          contests={sortedContests}
          activeCount={contestsData.activeCount}
          upcomingCount={contestsData.upcomingCount}
        />
      </BentoCard>
    );
  },
};

// Register the card with the global registry
registerCard(contestsCard);
