import { test, expect } from '@playwright/test';

/**
 * E2E tests for the NextSkip Dashboard
 *
 * These tests verify that the dashboard loads correctly and displays the expected content.
 *
 * Timeout guidelines:
 * - Simple UI checks: 30s
 * - Tests needing API data: 60s
 * - Tests with page reload/new context: 90s
 */
test.describe('Dashboard', () => {
  test.beforeEach(async ({ page }) => {
    // Set visited flag before navigation to prevent first-visit help modal from blocking tests
    await page.addInitScript(() => {
      localStorage.setItem('nextskip-visited', 'true');
    });
    await page.goto('/');
    // Wait for Vaadin to fully initialize - this is the actual ready signal
    await page.waitForSelector('#outlet > *', { timeout: 10000 });
  });

  test('page loads successfully', { timeout: 30_000 }, async ({ page }) => {
    // Give Vaadin time to set the title dynamically if needed
    await page.waitForFunction(() => document.title !== '', { timeout: 5000 });
    await expect(page).toHaveTitle(/NextSkip/i);
  });

  test('header displays correctly', { timeout: 30_000 }, async ({ page }) => {
    await expect(page.locator('.dashboard-title')).toContainText('NextSkip');
    await expect(page.locator('.dashboard-subtitle')).toContainText('Amateur Radio Activity Dashboard');
  });

  test('dashboard cards render after loading', { timeout: 60_000 }, async ({ page }) => {
    // Wait for loading to complete
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });

    // Verify ActivityGrid has cards
    const cards = page.locator('.activity-card');
    await expect(cards.first()).toBeVisible();
    expect(await cards.count()).toBeGreaterThan(0);
  });

  test('shows last update timestamp', { timeout: 60_000 }, async ({ page }) => {
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });
    await expect(page.locator('.last-update')).toContainText('Updated');
  });

  test('theme toggle button is visible', { timeout: 30_000 }, async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');
    await expect(themeToggle).toBeVisible();
  });

  test('can switch to dark mode', { timeout: 30_000 }, async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Click the toggle to switch to dark mode
    await themeToggle.click();

    // Verify the document has dark theme applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('dark');

    // Verify localStorage was updated
    const storedTheme = await page.evaluate(() => localStorage.getItem('nextskip-theme'));
    expect(storedTheme).toBe('dark');
  });

  test('can switch to light mode', { timeout: 30_000 }, async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Click once to go to dark
    await themeToggle.click();
    await expect(page.locator('html[data-theme="dark"]')).toBeVisible();

    // Click again to go to light
    await themeToggle.click();

    // Verify the document has light theme applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('light');

    // Verify localStorage was updated
    const storedTheme = await page.evaluate(() => localStorage.getItem('nextskip-theme'));
    expect(storedTheme).toBe('light');
  });

  test('theme persists after page reload', { timeout: 90_000 }, async ({ page }) => {
    const themeToggle = page.locator('.theme-toggle');

    // Switch to dark mode
    await themeToggle.click();

    // Reload the page
    await page.reload();
    await page.waitForSelector('#outlet > *', { timeout: 10000 });

    // Verify dark theme is still applied
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('dark');
  });

  test('help modal opens on first visit', { timeout: 90_000 }, async ({ browser }) => {
    // Create a fresh browser context without the visited flag in localStorage
    const freshContext = await browser.newContext();
    const freshPage = await freshContext.newPage();

    await freshPage.goto('/');
    await freshPage.waitForSelector('#outlet > *', { timeout: 10000 });

    // Help modal should be visible on first visit
    const helpModal = freshPage.locator('.help-modal');
    await expect(helpModal).toBeVisible({ timeout: 5000 });

    // Close the modal
    const closeButton = freshPage.locator('.help-modal__close');
    await closeButton.click();

    // Modal should be hidden after closing
    await expect(helpModal).not.toBeVisible();

    await freshContext.close();
  });
});

/**
 * Mobile Responsiveness Tests
 *
 * Test viewports covering all masonry breakpoints:
 * - ≤768px: 1 column (mobile)
 * - ≤1024px: 2 columns (tablet)
 * - >1024px: 4 columns (desktop)
 * - ≥1400px: 6 columns (wide desktop)
 */
const testViewports = [
  { name: 'Mobile (320px - 1 col)', width: 320, height: 568 },
  { name: 'Mobile (375px - 1 col)', width: 375, height: 667 },
  { name: 'Tablet (768px - 1 col edge)', width: 768, height: 1024 },
  { name: 'Tablet (1024px - 2 col)', width: 1024, height: 768 },
  { name: 'Desktop (1280px - 4 col)', width: 1280, height: 800 },
  { name: 'Wide (1400px - 6 col)', width: 1400, height: 900 },
];

for (const viewport of testViewports) {
  test.describe(`Dashboard - ${viewport.name}`, () => {
    test.use({ viewport: { width: viewport.width, height: viewport.height } });

    test('cards do not overflow or clip horizontally', { timeout: 60_000 }, async ({ page }) => {
      // Set visited flag to prevent first-visit help modal
      await page.addInitScript(() => {
        localStorage.setItem('nextskip-visited', 'true');
      });
      await page.goto('/');
      await page.waitForSelector('.activity-card', { timeout: 30000 });

      // Verify no horizontal scrollbar (catches overflow: auto/scroll issues)
      const hasHorizontalScroll = await page.evaluate(
        () => document.documentElement.scrollWidth > document.documentElement.clientWidth,
      );
      expect(hasHorizontalScroll).toBe(false);

      // Verify all cards are fully visible within viewport (catches clipping issues)
      // This detects when overflow-x: hidden masks content being cut off
      const clippedCards = await page.evaluate((viewportWidth) => {
        const cards = document.querySelectorAll('.activity-card');
        const clipped: string[] = [];
        for (const card of cards) {
          const rect = card.getBoundingClientRect();
          // Card should start at or after 0 and end before viewport width
          if (rect.left < 0 || rect.right > viewportWidth) {
            const title = card.querySelector('.activity-card__title')?.textContent || 'unknown';
            clipped.push(`${title}: left=${rect.left.toFixed(0)}, right=${rect.right.toFixed(0)}`);
          }
        }
        return clipped;
      }, viewport.width);

      expect(clippedCards, `Cards clipped at ${viewport.width}px viewport`).toHaveLength(0);
    });
  });
}
