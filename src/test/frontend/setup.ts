/**
 * Vitest test setup
 *
 * Configures global test environment for frontend tests
 */

import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// Cleanup after each test
afterEach(() => {
  cleanup();
});

// Mock window.matchMedia for responsive tests
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {}, // deprecated
    removeListener: () => {}, // deprecated
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => true,
  }),
});

// Mock ResizeObserver for masonry grid tests
global.ResizeObserver = class ResizeObserver {
  /* eslint-disable @typescript-eslint/class-methods-use-this */
  observe() {}
  unobserve() {}
  disconnect() {}
  /* eslint-enable @typescript-eslint/class-methods-use-this */
};

// Mock IntersectionObserver for scrollspy tests
global.IntersectionObserver = class IntersectionObserver {
  readonly root: Element | null = null;
  readonly rootMargin: string = '';
  readonly thresholds: readonly number[] = [];

  /* eslint-disable @typescript-eslint/class-methods-use-this */
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords(): IntersectionObserverEntry[] {
    return [];
  }
  /* eslint-enable @typescript-eslint/class-methods-use-this */
};

// Mock HTMLDialogElement methods for modal tests
HTMLDialogElement.prototype.showModal = function () {
  this.setAttribute('open', '');
};
HTMLDialogElement.prototype.close = function () {
  this.removeAttribute('open');
};
