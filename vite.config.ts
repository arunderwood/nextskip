import { UserConfigFn, loadEnv } from 'vite';
import { overrideVaadinConfig } from './vite.generated';

const customConfig: UserConfigFn = (env) => {
  // Detect production mode via Render's RENDER env var or Vite's mode
  // https://render.com/docs/environment-variables#render
  const isProduction = process.env.RENDER === 'true' || env.mode !== 'development';
  const mode = isProduction ? 'production' : 'development';
  const envVars = loadEnv(mode, process.cwd(), 'VITE_');

  return {
    define: {
      'import.meta.env.VITE_FARO_COLLECTOR_URL': JSON.stringify(envVars.VITE_FARO_COLLECTOR_URL || ''),
      'import.meta.env.VITE_TRACE_CORS_URLS': JSON.stringify(envVars.VITE_TRACE_CORS_URLS || ''),
      'import.meta.env.VITE_APP_VERSION': JSON.stringify(envVars.VITE_APP_VERSION || ''),
    },
  };
};

export default overrideVaadinConfig(customConfig);
