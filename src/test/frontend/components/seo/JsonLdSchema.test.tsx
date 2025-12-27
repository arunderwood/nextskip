/**
 * Unit tests for JsonLdSchema component.
 */

import React from 'react';
import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { JsonLdSchema } from 'Frontend/components/seo/JsonLdSchema';
import { APP_METADATA } from 'Frontend/components/seo/constants';

describe('JsonLdSchema', () => {
  it('should render a script tag with type application/ld+json', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    expect(scriptTag).toBeInTheDocument();
  });

  it('should contain valid JSON-LD with WebApplication type', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    expect(scriptTag).toBeInTheDocument();

    const jsonLd = JSON.parse(scriptTag!.textContent || '{}');

    expect(jsonLd['@context']).toBe('https://schema.org');
    expect(jsonLd['@type']).toBe('WebApplication');
    expect(jsonLd.name).toBe(APP_METADATA.name);
    expect(jsonLd.description).toBe(APP_METADATA.description);
  });

  it('should include the application URL', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    const jsonLd = JSON.parse(scriptTag!.textContent || '{}');

    expect(jsonLd.url).toBe(APP_METADATA.url);
  });

  it('should include free pricing offer', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    const jsonLd = JSON.parse(scriptTag!.textContent || '{}');

    expect(jsonLd.offers).toBeDefined();
    expect(jsonLd.offers['@type']).toBe('Offer');
    expect(jsonLd.offers.price).toBe('0');
    expect(jsonLd.offers.priceCurrency).toBe('USD');
  });

  it('should include application category and requirements', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    const jsonLd = JSON.parse(scriptTag!.textContent || '{}');

    expect(jsonLd.applicationCategory).toBe(APP_METADATA.applicationCategory);
    expect(jsonLd.operatingSystem).toBe(APP_METADATA.operatingSystem);
    expect(jsonLd.browserRequirements).toBe(APP_METADATA.browserRequirements);
  });

  it('should include keywords for SEO', () => {
    render(<JsonLdSchema />);

    const scriptTag = document.querySelector('script[type="application/ld+json"]');
    const jsonLd = JSON.parse(scriptTag!.textContent || '{}');

    expect(jsonLd.keywords).toBeDefined();
    expect(jsonLd.keywords).toContain('amateur radio');
    expect(jsonLd.keywords).toContain('ham radio');
  });
});
