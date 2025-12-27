/**
 * JsonLdSchema component.
 *
 * Renders JSON-LD structured data for search engine optimization.
 * Uses WebApplication schema type for the dashboard application.
 */
import type { ReactElement } from 'react';
import { JsonLd } from 'react-schemaorg';
import type { WebApplication, WithContext } from 'schema-dts';
import { APP_METADATA } from './constants';

/** Static JSON-LD schema for NextSkip web application */
const SCHEMA: WithContext<WebApplication> = {
  '@context': 'https://schema.org',
  '@type': 'WebApplication',
  name: APP_METADATA.name,
  description: APP_METADATA.description,
  url: APP_METADATA.url,
  applicationCategory: APP_METADATA.applicationCategory,
  operatingSystem: APP_METADATA.operatingSystem,
  browserRequirements: APP_METADATA.browserRequirements,
  offers: {
    '@type': 'Offer',
    price: '0',
    priceCurrency: 'USD',
  },
  keywords: 'amateur radio, ham radio, HF propagation, radio activity, band conditions',
};

/**
 * Renders JSON-LD structured data for NextSkip.
 *
 * The WebApplication schema helps search engines understand
 * that NextSkip is a web-based amateur radio dashboard.
 */
export function JsonLdSchema(): ReactElement {
  return <JsonLd<WebApplication> item={SCHEMA} />;
}
