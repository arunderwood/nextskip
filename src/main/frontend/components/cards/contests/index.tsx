/**
 * Contests Card - Individual card registration for each amateur radio contest.
 *
 * This module registers individual contest cards with the global registry,
 * enabling each contest to be sorted independently by its own score.
 *
 * SOLID Compliance:
 * - Single Responsibility: Each card represents one contest
 * - Open-Closed: New event types can be added without modifying this code
 * - Liskov Substitution: All Event implementations work with EventCard
 * - Dependency Inversion: Depends on Event abstraction, not Contest concrete type
 */

import React from 'react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import { priorityToHotness } from '../../activity/usePriorityCalculation';
import { EventCard } from '../events/EventCard';
import EventStatus from 'Frontend/generated/io/nextskip/common/model/EventStatus';
import type Contest from 'Frontend/generated/io/nextskip/contests/model/Contest';

/**
 * Contests Card Definition - Creates individual cards for each contest
 */
const contestsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.contests?.contests && data.contests.contests.length > 0);
  },

  createConfig: (data: DashboardData): ActivityCardConfig[] | null => {
    const contests = data.contests?.contests;
    if (!contests || contests.length === 0) return null;

    // Filter out ended contests and undefined values
    const activeContests = contests.filter((c): c is Contest => c !== undefined && c.status !== EventStatus.ENDED);

    if (activeContests.length === 0) return null;

    // Create individual card config for each contest
    // Use name + startTime as unique identifier to avoid index mismatches
    return activeContests.map((contest) => {
      // Use the contest's own score directly from the backend
      // Contest.getScore() returns 0-100 based on timing and status
      const score = contest.score ?? 0;

      // Priority for individual event cards: use backend score more directly
      // The backend already calculated the score based on activity and timing
      const priority = score;

      // Use name + startTime for unique ID to safely look up contest later
      const uniqueId = `contest-${contest.name}-${contest.startTime}`.replace(/[^a-zA-Z0-9-]/g, '-');

      return {
        id: uniqueId,
        type: 'contests',
        size: '1x1' as const,
        priority,
        hotness: priorityToHotness(priority),
      };
    });
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const contests = data.contests?.contests;
    if (!contests) return null;

    // Filter to only active contests (same filter as createConfig)
    const activeContests = contests.filter((c): c is Contest => c !== undefined && c.status !== EventStatus.ENDED);

    // Find the contest by matching the config ID
    const contest = activeContests.find((c) => {
      const uniqueId = `contest-${c.name}-${c.startTime}`.replace(/[^a-zA-Z0-9-]/g, '-');
      return uniqueId === config.id;
    });

    if (!contest) return null;

    return <EventCard event={contest} eventType="contest" config={config} />;
  },
};

// Register the card with the global registry
registerCard(contestsCard);
