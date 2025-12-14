import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import { resolve } from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: './frontend/test/setup.ts',
    include: ['frontend/tests/**/*.{test,spec}.{ts,tsx}'],
    css: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'frontend/test/',
        'frontend/tests/',
        '**/*.d.ts',
        '**/*.config.*',
        '**/mockData',
        'src/main/frontend/generated/',
      ],
    },
  },
  resolve: {
    alias: {
      Frontend: resolve(__dirname, './frontend'),
    },
  },
});
