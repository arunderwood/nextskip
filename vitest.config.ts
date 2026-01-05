import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';
import os from 'os';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './src/test/frontend/setup.ts',
    include: ['src/test/frontend/**/*.{test,spec}.{ts,tsx}'],
    css: true,
    // Enable parallel test execution
    pool: 'threads',
    maxWorkers: Math.max(1, Math.floor(os.cpus().length * 0.75)),
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'json-summary', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'src/test/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/mockData',
        'src/main/frontend/generated/',
      ],
    },
  },
  resolve: {
    alias: {
      // Mock generated types for tests (must come before Frontend alias)
      'Frontend/generated': resolve(__dirname, './src/test/frontend/mocks/generated'),
      Frontend: resolve(__dirname, './src/main/frontend'),
    },
  },
});
