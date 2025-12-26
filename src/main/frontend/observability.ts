import { createRoutesFromChildren, matchRoutes, Routes, useLocation, useNavigationType } from 'react-router-dom';
import {
  createReactRouterV6Options,
  getWebInstrumentations,
  initializeFaro,
  ReactIntegration,
} from '@grafana/faro-react';
import { TracingInstrumentation } from '@grafana/faro-web-tracing';

const collectorUrl = import.meta.env.VITE_FARO_COLLECTOR_URL;

// Parse comma-separated regex patterns from env var
// Example: "nextskip\\.io,staging\\.nextskip\\.com"
const traceCorsPatterns =
  import.meta.env.VITE_TRACE_CORS_URLS?.split(',')
    .filter(Boolean)
    .map((pattern) => new RegExp(pattern)) ?? [];

if (collectorUrl) {
  initializeFaro({
    url: collectorUrl,
    app: {
      name: 'nextskip',
      version: import.meta.env.VITE_APP_VERSION || '0.0.0',
    },
    instrumentations: [
      ...getWebInstrumentations(),
      new ReactIntegration({
        router: createReactRouterV6Options({
          createRoutesFromChildren,
          matchRoutes,
          Routes,
          useLocation,
          useNavigationType,
        }),
      }),
      new TracingInstrumentation({
        instrumentationOptions: {
          propagateTraceHeaderCorsUrls: traceCorsPatterns,
        },
      }),
    ],
  });
}
