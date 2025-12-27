/**
 * Unit tests for SEO component.
 *
 * Tests meta tag rendering and default values for SEO optimization.
 */

import React from 'react';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, cleanup } from '@testing-library/react';
import { HelmetProvider } from '@dr.pogodin/react-helmet';
import { Seo } from 'Frontend/components/seo/SEO';

/**
 * Helper to render components wrapped with HelmetProvider.
 */
const renderWithHelmet = (ui: React.ReactElement) => render(<HelmetProvider>{ui}</HelmetProvider>);

/**
 * Helper to get meta tag content by name or property.
 */
const getMetaContent = (attr: 'name' | 'property', value: string): string | null => {
  const meta = document.querySelector(`meta[${attr}="${value}"]`);
  return meta?.getAttribute('content') || null;
};

describe('SEO', () => {
  beforeEach(() => {
    // Clear any existing meta tags before each test
    document.head.innerHTML = '';
  });

  afterEach(() => {
    cleanup();
  });

  describe('title handling', () => {
    it('should render title with NextSkip suffix', () => {
      renderWithHelmet(<Seo title="Test Page" />);

      expect(document.title).toBe('Test Page | NextSkip');
    });

    it('should update title when prop changes', () => {
      const { rerender } = renderWithHelmet(<Seo title="First" />);
      expect(document.title).toBe('First | NextSkip');

      rerender(
        <HelmetProvider>
          <Seo title="Second" />
        </HelmetProvider>,
      );
      expect(document.title).toBe('Second | NextSkip');
    });
  });

  describe('description meta tag', () => {
    it('should use default description when none provided', () => {
      renderWithHelmet(<Seo title="Test" />);

      const description = getMetaContent('name', 'description');
      expect(description).toContain('amateur radio opportunities');
    });

    it('should use custom description when provided', () => {
      renderWithHelmet(<Seo title="Test" description="Custom description" />);

      const description = getMetaContent('name', 'description');
      expect(description).toBe('Custom description');
    });
  });

  describe('canonical URL', () => {
    it('should set default canonical URL', () => {
      renderWithHelmet(<Seo title="Test" />);

      const canonical = document.querySelector('link[rel="canonical"]');
      expect(canonical?.getAttribute('href')).toBe('https://nextskip.io');
    });

    it('should use custom canonical URL when provided', () => {
      renderWithHelmet(<Seo title="Test" canonicalUrl="https://nextskip.io/page" />);

      const canonical = document.querySelector('link[rel="canonical"]');
      expect(canonical?.getAttribute('href')).toBe('https://nextskip.io/page');
    });
  });

  describe('Open Graph meta tags', () => {
    it('should render og:title', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('property', 'og:title')).toBe('Test | NextSkip');
    });

    it('should render og:description', () => {
      renderWithHelmet(<Seo title="Test" />);

      const ogDesc = getMetaContent('property', 'og:description');
      expect(ogDesc).toContain('amateur radio opportunities');
    });

    it('should render og:url', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('property', 'og:url')).toBe('https://nextskip.io');
    });

    it('should render og:image', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('property', 'og:image')).toBe('https://nextskip.io/og-image.svg');
    });

    it('should render og:type as website', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('property', 'og:type')).toBe('website');
    });

    it('should use custom og:image when provided', () => {
      renderWithHelmet(<Seo title="Test" ogImage="https://example.com/custom.png" />);

      expect(getMetaContent('property', 'og:image')).toBe('https://example.com/custom.png');
    });
  });

  describe('Twitter Card meta tags', () => {
    it('should render twitter:card as summary_large_image', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('name', 'twitter:card')).toBe('summary_large_image');
    });

    it('should render twitter:title', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('name', 'twitter:title')).toBe('Test | NextSkip');
    });

    it('should render twitter:description', () => {
      renderWithHelmet(<Seo title="Test" />);

      const twitterDesc = getMetaContent('name', 'twitter:description');
      expect(twitterDesc).toContain('amateur radio opportunities');
    });

    it('should render twitter:image', () => {
      renderWithHelmet(<Seo title="Test" />);

      expect(getMetaContent('name', 'twitter:image')).toBe('https://nextskip.io/og-image.svg');
    });
  });

  describe('default description content', () => {
    it('should mention key features in default description', () => {
      renderWithHelmet(<Seo title="Test" />);

      const description = getMetaContent('name', 'description');
      expect(description).toContain('propagation');
      expect(description).toContain('activations');
      expect(description).toContain('contests');
      expect(description).toContain('satellites');
    });
  });
});
