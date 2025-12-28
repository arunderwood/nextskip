import { UserConfigFn, loadEnv } from 'vite';
import { overrideVaadinConfig } from './vite.generated';
import { VitePWA } from 'vite-plugin-pwa';

const customConfig: UserConfigFn = (env) => {
  // Detect production mode via Render's RENDER env var or Vite's mode
  // https://render.com/docs/environment-variables#render
  const isProduction = process.env.RENDER === 'true' || env.mode !== 'development';
  const mode = isProduction ? 'production' : 'development';
  const envVars = loadEnv(mode, process.cwd(), 'VITE_');

  return {
    plugins: [
      VitePWA({
        registerType: 'autoUpdate',
        includeAssets: ['favicon.svg', 'icons/*.png'],
        manifest: {
          name: 'NextSkip - Amateur Radio Activity Dashboard',
          short_name: 'NextSkip',
          description: 'Real-time amateur radio activity dashboard for propagation, activations, contests, and more',
          theme_color: '#6366f1',
          background_color: '#0d1117',
          display: 'standalone',
          orientation: 'portrait-primary',
          scope: '/',
          start_url: '/',
          icons: [
            {
              src: 'icons/pwa-192x192.png',
              sizes: '192x192',
              type: 'image/png',
            },
            {
              src: 'icons/pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png',
            },
            {
              src: 'icons/pwa-512x512.png',
              sizes: '512x512',
              type: 'image/png',
              purpose: 'any maskable',
            },
          ],
        },
        workbox: {
          globPatterns: ['**/*.{js,css,html,ico,png,svg,woff,woff2}'],
          runtimeCaching: [
            // Cache-first for hashed JS/CSS bundles (immutable)
            {
              urlPattern: /\.(?:js|css)$/,
              handler: 'CacheFirst',
              options: {
                cacheName: 'static-assets',
                expiration: {
                  maxEntries: 100,
                  maxAgeSeconds: 60 * 60 * 24 * 365, // 1 year
                },
              },
            },
            // Network-first for HTML (always try to get fresh)
            {
              urlPattern: /\.html$/,
              handler: 'NetworkFirst',
              options: {
                cacheName: 'html-cache',
                expiration: {
                  maxEntries: 10,
                  maxAgeSeconds: 60 * 60 * 24, // 1 day
                },
                networkTimeoutSeconds: 3,
              },
            },
            // Network-only for Hilla API endpoints (no offline caching)
            {
              urlPattern: /\/connect\/.*/,
              handler: 'NetworkOnly',
            },
            // Network-only for actuator/health endpoints
            {
              urlPattern: /\/actuator\/.*/,
              handler: 'NetworkOnly',
            },
            // Network-only for PostHog analytics proxy
            {
              urlPattern: /\/a\/.*/,
              handler: 'NetworkOnly',
            },
          ],
        },
        // Disable in development (PWA requires HTTPS)
        devOptions: {
          enabled: false,
        },
      }),
    ],
    define: {
      'import.meta.env.VITE_FARO_COLLECTOR_URL': JSON.stringify(envVars.VITE_FARO_COLLECTOR_URL || ''),
      'import.meta.env.VITE_TRACE_CORS_URLS': JSON.stringify(envVars.VITE_TRACE_CORS_URLS || ''),
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(envVars.VITE_APP_VERSION || ''),
      'import.meta.env.VITE_POSTHOG_KEY': JSON.stringify(envVars.VITE_POSTHOG_KEY || ''),
      'import.meta.env.VITE_POSTHOG_HOST': JSON.stringify(envVars.VITE_POSTHOG_HOST || ''),
    },
  };
};

export default overrideVaadinConfig(customConfig);
