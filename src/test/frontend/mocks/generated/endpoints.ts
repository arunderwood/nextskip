/**
 * Mock stub for Hilla-generated endpoints.
 * Used in tests to avoid dependency on Gradle code generation.
 */

import { vi, type Mock } from 'vitest';

// AdminAuthEndpoint mock
export const AdminAuthEndpoint = {
  getCurrentUser: vi.fn() as Mock,
  logout: vi.fn() as Mock,
};

// AdminFeedEndpoint mock
export const AdminFeedEndpoint = {
  getFeedStatuses: vi.fn() as Mock,
  triggerFeedRefresh: vi.fn() as Mock,
};

// Re-export for other endpoints as needed
export const PropagationEndpoint = {
  getPropagationData: vi.fn(),
};

export const ActivationsEndpoint = {
  getActivations: vi.fn(),
};

export const ContestEndpoint = {
  getContests: vi.fn(),
};

export const MeteorEndpoint = {
  getMeteorShowers: vi.fn(),
};

export const SpotsEndpoint = {
  getBandActivity: vi.fn(),
};
