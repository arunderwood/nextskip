/**
 * Activations Cards - Card registration for POTA and SOTA activations.
 *
 * This module registers the activations activity cards (POTA and SOTA) with
 * the global registry, enabling them to appear in the dashboard without
 * modifying core files.
 */

import React from 'react';
import { registerCard } from '../CardRegistry';
import type { CardDefinition, DashboardData } from '../types';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import { ActivityCard } from '../../activity';
import { calculatePriority, priorityToHotness } from '../../activity/usePriorityCalculation';
import PotaActivationsContent from './PotaActivationsContent';
import SotaActivationsContent from './SotaActivationsContent';

/**
 * POTA Activations Card Definition
 */
const potaActivationsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!data.activations?.potaActivations;
  },

  createConfig: (data: DashboardData) => {
    const potaActivations = data.activations?.potaActivations;
    if (!potaActivations) return null;

    const count = potaActivations.length;

    // Calculate priority based on number of activations
    // Score: count * 5 (capped at 100)
    // Favorable: 3+ activations
    const score = Math.min(100, count * 5);
    const isFavorable = count >= 3;

    // Rating based on count
    const rating = count >= 10 ? ('GOOD' as const) : count >= 5 ? ('FAIR' as const) : ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score,
      rating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'pota-activations',
      type: 'pota-activations',
      size: '1x2',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const potaActivations = data.activations?.potaActivations;
    if (!potaActivations) return null;

    // Filter out undefined values to ensure type safety
    const validActivations = potaActivations.filter((a) => a !== undefined);

    return (
      <ActivityCard config={config} title="POTA Activations" icon="🌲" subtitle="Parks on the Air">
        <PotaActivationsContent activations={validActivations} />
      </ActivityCard>
    );
  },
};

/**
 * SOTA Activations Card Definition
 */
const sotaActivationsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!data.activations?.sotaActivations;
  },

  createConfig: (data: DashboardData) => {
    const sotaActivations = data.activations?.sotaActivations;
    if (!sotaActivations) return null;

    const count = sotaActivations.length;

    // Calculate priority based on number of activations
    // Score: count * 5 (capped at 100)
    // Favorable: 3+ activations
    const score = Math.min(100, count * 5);
    const isFavorable = count >= 3;

    // Rating based on count
    const rating = count >= 10 ? ('GOOD' as const) : count >= 5 ? ('FAIR' as const) : ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score,
      rating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'sota-activations',
      type: 'sota-activations',
      size: '1x2',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: ActivityCardConfig) => {
    const sotaActivations = data.activations?.sotaActivations;
    if (!sotaActivations) return null;

    // Filter out undefined values to ensure type safety
    const validActivations = sotaActivations.filter((a) => a !== undefined);

    return (
      <ActivityCard config={config} title="SOTA Activations" icon="⛰️" subtitle="Summits on the Air">
        <SotaActivationsContent activations={validActivations} />
      </ActivityCard>
    );
  },
};

// Register both cards with the global registry
registerCard(potaActivationsCard);
registerCard(sotaActivationsCard);
