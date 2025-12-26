import { test, expect } from '@playwright/test';

/**
 * E2E tests for the NextSkip Help System
 *
 * Tests the help modal functionality including opening, closing,
 * navigation, accessibility, and responsive behavior.
 *
 * Timeout guidelines:
 * - Simple UI checks: 30s
 * - Tests needing API data: 60s
 * - Tests with page reload/new context: 90s
 */
test.describe('Help System', () => {
  test.beforeEach(async ({ page }) => {
    // Set visited flag before navigation to prevent first-visit help modal from auto-opening
    await page.addInitScript(() => {
      localStorage.setItem('nextskip-visited', 'true');
    });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('#outlet > *', { timeout: 10000 });
    // Wait for loading to complete
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });
  });

  test('help button is visible in header', { timeout: 30_000 }, async ({ page }) => {
    const helpButton = page.locator('.help-button');
    await expect(helpButton).toBeVisible();
  });

  test('help button has accessible label', { timeout: 30_000 }, async ({ page }) => {
    const helpButton = page.locator('.help-button');
    await expect(helpButton).toHaveAttribute('aria-label', 'Open help and about');
  });

  test('modal opens when help button clicked', { timeout: 30_000 }, async ({ page }) => {
    const helpButton = page.locator('.help-button');
    await helpButton.click();

    const modal = page.locator('dialog.help-modal');
    await expect(modal).toHaveAttribute('open');
    await expect(modal).toBeVisible();
  });

  test('modal displays correct title', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const title = page.locator('.help-modal__title');
    await expect(title).toHaveText('Help & About');
  });

  test('modal closes on close button click', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();
    await expect(page.locator('dialog.help-modal')).toHaveAttribute('open');

    await page.locator('.help-modal__close').click();

    await expect(page.locator('dialog.help-modal')).not.toHaveAttribute('open');
  });

  test('modal closes on Escape key', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();
    await expect(page.locator('dialog.help-modal')).toHaveAttribute('open');

    await page.keyboard.press('Escape');

    await expect(page.locator('dialog.help-modal')).not.toHaveAttribute('open');
  });

  test('modal closes on backdrop click', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();
    const modal = page.locator('dialog.help-modal');
    await expect(modal).toHaveAttribute('open');

    // Click outside the modal container (on the backdrop)
    // The dialog is centered, so clicking at the far left edge of viewport hits backdrop
    await page.mouse.click(5, 300);

    await expect(modal).not.toHaveAttribute('open');
  });

  test('all navigation tabs are visible', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const tablist = page.locator('[role="tablist"]');
    await expect(tablist).toBeVisible();

    // About should always be present
    await expect(page.locator('[role="tab"]', { hasText: 'About' })).toBeVisible();

    // At least some activity tabs should be present
    const tabs = page.locator('[role="tab"]');
    expect(await tabs.count()).toBeGreaterThanOrEqual(2);
  });

  test('About section is first in content', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const aboutSection = page.locator('#help-section-about');
    await expect(aboutSection).toBeVisible();

    // About section should contain expected content
    await expect(aboutSection.locator('h3')).toHaveText('About NextSkip');
  });

  test('clicking navigation tab scrolls to section', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    // Click on a tab that's not About
    const solarTab = page.locator('[role="tab"]', { hasText: 'Solar Indices' });
    if (await solarTab.isVisible()) {
      await solarTab.click();

      // Give time for smooth scroll
      await page.waitForTimeout(500);

      // The section should be in view
      const solarSection = page.locator('#help-section-solar-indices');
      await expect(solarSection).toBeInViewport();
    }
  });

  test('modal has proper ARIA attributes', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const modal = page.locator('dialog.help-modal');
    await expect(modal).toHaveAttribute('aria-labelledby', 'help-modal-title');

    // Close button should have accessible name
    const closeButton = page.locator('.help-modal__close');
    await expect(closeButton).toHaveAttribute('aria-label', 'Close help');
  });

  test('can navigate tabs with keyboard', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    // Focus on the first tab
    const firstTab = page.locator('[role="tab"]').first();
    await firstTab.focus();
    await expect(firstTab).toBeFocused();

    // Tab to close button
    await page.keyboard.press('Tab');

    // Should be able to navigate without errors
    await expect(page.locator('dialog.help-modal')).toHaveAttribute('open');
  });

  test('modal respects dark theme', { timeout: 30_000 }, async ({ page }) => {
    // Switch to dark mode first
    const themeToggle = page.locator('.theme-toggle');
    await themeToggle.click();

    // Verify dark mode is active
    const theme = await page.evaluate(() => document.documentElement.getAttribute('data-theme'));
    expect(theme).toBe('dark');

    // Open help modal
    await page.locator('.help-button').click();

    // Modal should be visible in dark mode
    await expect(page.locator('dialog.help-modal')).toHaveAttribute('open');
    await expect(page.locator('.help-modal__container')).toBeVisible();
  });
});

test.describe('Help System - Mobile', () => {
  test.use({ viewport: { width: 375, height: 667 } }); // iPhone SE

  test.beforeEach(async ({ page }) => {
    // Set visited flag before navigation to prevent first-visit help modal from auto-opening
    await page.addInitScript(() => {
      localStorage.setItem('nextskip-visited', 'true');
    });
    await page.goto('/');
    await page.waitForLoadState('networkidle');
    await page.waitForSelector('#outlet > *', { timeout: 10000 });
    await page.waitForSelector('.loading', { state: 'hidden', timeout: 30000 });
  });

  test('modal displays as full-screen sheet', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const modal = page.locator('dialog.help-modal');
    await expect(modal).toHaveAttribute('open');

    // On mobile, modal should take full viewport
    const box = await modal.boundingBox();
    expect(box).not.toBeNull();
    if (box) {
      // Should be close to full viewport width (375px minus scrollbar ~19px)
      expect(box.width).toBeGreaterThanOrEqual(350);
      // Should be close to full height (667px minus browser chrome)
      expect(box.height).toBeGreaterThanOrEqual(630);
    }
  });

  test('navigation tabs are horizontally scrollable', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const navList = page.locator('.help-navigation__list');
    await expect(navList).toBeVisible();

    // The list should have horizontal overflow
    const overflowX = await navList.evaluate((el) => {
      const style = window.getComputedStyle(el);
      return style.overflowX;
    });
    expect(overflowX).toBe('auto');
  });

  test('close button remains accessible', { timeout: 30_000 }, async ({ page }) => {
    await page.locator('.help-button').click();

    const closeButton = page.locator('.help-modal__close');
    await expect(closeButton).toBeVisible();
    await expect(closeButton).toBeEnabled();

    // Should be able to close
    await closeButton.click();
    await expect(page.locator('dialog.help-modal')).not.toHaveAttribute('open');
  });
});
