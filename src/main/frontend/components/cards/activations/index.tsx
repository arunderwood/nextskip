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
import type { BentoCardConfig } from 'Frontend/types/bento';
import { BentoCard } from '../../bento';
import { calculatePriority, priorityToHotness } from '../../bento/usePriorityCalculation';
import PotaActivationsContent from './PotaActivationsContent';
import SotaActivationsContent from './SotaActivationsContent';

/* eslint-disable react/jsx-key */

/**
 * POTA Activations Card Definition
 */
const potaActivationsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.activations?.potaActivations);
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
    const rating =
      count >= 10 ? ('GOOD' as const) : count >= 5 ? ('FAIR' as const) : ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score,
      rating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'pota-activations',
      type: 'pota-activations',
      size: 'standard',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: BentoCardConfig) => {
    const potaActivations = data.activations?.potaActivations;
    if (!potaActivations) return null;

    // Filter out undefined values to ensure type safety
    const validActivations = potaActivations.filter((a) => a !== undefined);

    return (
      <BentoCard
        config={config}
        title="POTA Activations"
        icon="🌲"
        subtitle="Parks on the Air"
        footer={
          <div className="info-box">
            <strong>About POTA:</strong> Parks on the Air (POTA) is an amateur radio
            activity encouraging portable operations from parks and public lands. Higher
            activation counts indicate more opportunities for contacts.
          </div>
        }
      >
        <PotaActivationsContent activations={validActivations} />
      </BentoCard>
    );
  },
};

/**
 * SOTA Activations Card Definition
 */
const sotaActivationsCard: CardDefinition = {
  canRender: (data: DashboardData) => {
    return !!(data.activations?.sotaActivations);
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
    const rating =
      count >= 10 ? ('GOOD' as const) : count >= 5 ? ('FAIR' as const) : ('POOR' as const);

    const priority = calculatePriority({
      favorable: isFavorable,
      score,
      rating,
    });

    const hotness = priorityToHotness(priority);

    return {
      id: 'sota-activations',
      type: 'sota-activations',
      size: 'standard',
      priority,
      hotness,
    };
  },

  render: (data: DashboardData, config: BentoCardConfig) => {
    const sotaActivations = data.activations?.sotaActivations;
    if (!sotaActivations) return null;

    // Filter out undefined values to ensure type safety
    const validActivations = sotaActivations.filter((a) => a !== undefined);

    return (
      <BentoCard
        config={config}
        title="SOTA Activations"
        icon="⛰️"
        subtitle="Summits on the Air"
        footer={
          <div className="info-box">
            <strong>About SOTA:</strong> Summits on the Air (SOTA) is an amateur radio
            activity encouraging portable operations from mountain summits. Higher
            activation counts indicate more opportunities for contacts.
          </div>
        }
      >
        <SotaActivationsContent activations={validActivations} />
      </BentoCard>
    );
  },
};

// Register both cards with the global registry
registerCard(potaActivationsCard);
registerCard(sotaActivationsCard);
