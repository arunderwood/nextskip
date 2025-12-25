import typescript from 'eslint-config-vaadin/typescript';
import react from 'eslint-config-vaadin/react';
import prettier from 'eslint-config-vaadin/prettier';
import testing from 'eslint-config-vaadin/testing';

export default [
  {
    ignores: [
      'node_modules/**',
      'build/**',
      'src/main/frontend/generated/**',
      '*.config.js',
      '*.config.ts',
    ],
  },
  ...typescript,
  ...react,
  ...prettier,
  ...testing,
  {
    files: ['src/main/frontend/**/*.{ts,tsx}', 'src/test/frontend/**/*.{ts,tsx}'],
    settings: {
      react: {
        version: 'detect',
      },
    },
    rules: {
      // Fix invalid config in eslint-config-vaadin - the rule schema forbids
      // setting both html:'ignore' and custom:'ignore' with empty exceptions.
      'react/jsx-props-no-spreading': ['error', {
        html: 'ignore',
        custom: 'enforce',
        explicitSpread: 'enforce',
      }],
      // Disable jsx-no-literals - this is for i18n-heavy apps, not applicable here
      'react/jsx-no-literals': 'off',
      // Allow implicit return types for React components (TypeScript infers them well)
      '@typescript-eslint/explicit-module-boundary-types': 'off',
      // Props are effectively readonly in React, TypeScript enforces this
      'react/prefer-read-only-props': 'off',
      // We use role attributes intentionally for better masonry grid accessibility
      'jsx-a11y/prefer-tag-over-role': 'off',
      // Allow prop-types to be disabled (we use TypeScript for type checking)
      'react/prop-types': 'off',
      'react/require-default-props': 'off',
      // Allow function components without optimization (React 18+ handles this)
      'react/require-optimization': 'off',
      // Allow JSX newlines (handled by Prettier)
      'react/jsx-newline': 'off',
      // Allow dynamic import() type annotations (used in lazy loading)
      '@typescript-eslint/consistent-type-imports': 'off',
    },
  },
  // Relaxed rules for test files - performance optimization not needed in tests
  {
    files: ['src/test/frontend/**/*.{ts,tsx}'],
    rules: {
      'react-perf/jsx-no-new-object-as-prop': 'off',
      'react-perf/jsx-no-new-array-as-prop': 'off',
      'react-perf/jsx-no-new-function-as-prop': 'off',
    },
  },
];
