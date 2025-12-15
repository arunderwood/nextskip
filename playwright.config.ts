import { defineConfig, devices } from '@playwright/test';

const isCI = !!process.env.CI;

/**
 * Playwright configuration for NextSkip E2E tests.
 * See https://playwright.dev/docs/test-configuration.
 */
export default defineConfig({
  testDir: './src/test/e2e',
  outputDir: './build/playwright-reports/tests',
  /* Run tests in files in parallel */
  fullyParallel: true,
  /* Fail the build on CI if you accidentally left test.only in the source code */
  forbidOnly: isCI,
  /* Retry on CI only */
  retries: isCI ? 2 : 0,
  /* Use 1 worker in CI for stability (recommended by Playwright docs) */
  workers: isCI ? 1 : undefined,
  /* Reporter configuration */
  reporter: [
    ['html', { outputFolder: 'build/playwright-reports/html', open: !isCI }],
    ['junit', { outputFile: 'build/playwright-reports/TEST-e2e-report.xml' }]
  ],
  use: {
    /* Base URL for page.goto('/') */
    baseURL: 'http://localhost:8080',
    /* Collect trace when retrying the failed test */
    trace: 'on-first-retry',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  /* Start the app before running tests (local development only) */
  webServer: isCI ? undefined : {
    command: './gradlew bootRun -Dvaadin.launch-browser=false',
    url: 'http://localhost:8080',
    reuseExistingServer: true,
    timeout: 180000, // 3 minutes for Gradle + Spring Boot startup
    stdout: 'pipe',
    stderr: 'pipe',
  },
});
