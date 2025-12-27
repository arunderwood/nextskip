import { Helmet } from '@dr.pogodin/react-helmet';

interface SEOProps {
  title: string;
  description?: string;
  canonicalUrl?: string;
  ogImage?: string;
}

const DEFAULT_DESCRIPTION =
  'Find the best amateur radio opportunities. Track propagation, portable activations, contests, satellites, and more in one dashboard.';

export function Seo({ title, description, canonicalUrl, ogImage }: SEOProps) {
  const fullTitle = `${title} | NextSkip`;
  const desc = description || DEFAULT_DESCRIPTION;
  const url = canonicalUrl || 'https://nextskip.io';
  const image = ogImage || 'https://nextskip.io/og-image.svg';

  return (
    <Helmet>
      <title>{fullTitle}</title>
      <meta name="description" content={desc} />
      <link rel="canonical" href={url} />

      {/* Open Graph */}
      <meta property="og:title" content={fullTitle} />
      <meta property="og:description" content={desc} />
      <meta property="og:url" content={url} />
      <meta property="og:image" content={image} />
      <meta property="og:type" content="website" />

      {/* Twitter Card */}
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={fullTitle} />
      <meta name="twitter:description" content={desc} />
      <meta name="twitter:image" content={image} />
    </Helmet>
  );
}
