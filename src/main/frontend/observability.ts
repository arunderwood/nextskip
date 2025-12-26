import { getWebInstrumentations, initializeFaro } from '@grafana/faro-web-sdk';
import { TracingInstrumentation } from '@grafana/faro-web-tracing';

// Only initialize if collector URL is configured
const collectorUrl = import.meta.env.VITE_FARO_COLLECTOR_URL;

if (collectorUrl) {
  initializeFaro({
    url: collectorUrl,
    app: {
      name: 'nextskip',
      version: import.meta.env.VITE_APP_VERSION || '0.0.0',
    },
    instrumentations: [...getWebInstrumentations(), new TracingInstrumentation()],
  });
}
