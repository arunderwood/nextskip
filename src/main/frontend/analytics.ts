import posthog from 'posthog-js';

const posthogKey = import.meta.env.VITE_POSTHOG_KEY;
const posthogHost = import.meta.env.VITE_POSTHOG_HOST;

if (posthogKey && posthogHost) {
  posthog.init(posthogKey, {
    api_host: posthogHost,
    defaults: '2025-05-24',
    capture_pageview: true,
    capture_pageleave: true,
    disable_session_recording: true,
    respect_dnt: true,
  });
}

export { posthog };
