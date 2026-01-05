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
    // Enable parallel test execution (Vitest 4+ top-level options)
    pool: 'threads',
    maxWorkers: Math.max(1, Math.floor(os.cpus().length * 0.75)),
    coverage: {
      provider: 'v8',
      // text-summary for console, lcov for cover-diff (written to file only)
      reporter: [
        ['text-summary', { file: null }], // Console summary
        ['json', { file: 'coverage-final.json' }],
        ['json-summary', { file: 'coverage-summary.json' }],
        ['html', { subdir: 'html' }],
        ['lcov', { file: 'lcov.info' }], // For cover-diff, file only
      ],
      reportsDirectory: './coverage',
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
