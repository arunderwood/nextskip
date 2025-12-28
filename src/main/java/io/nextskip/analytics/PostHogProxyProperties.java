package io.nextskip.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the PostHog analytics reverse proxy.
 *
 * <p>These properties control the behavior of the PostHog proxy controller,
 * allowing configuration of the upstream PostHog endpoints.</p>
 */
@ConfigurationProperties(prefix = "posthog.proxy")
public record PostHogProxyProperties(
    boolean enabled,
    String ingestUrl,
    String assetsUrl
) {
    /**
     * Creates a new PostHogProxyProperties with default values for null URLs.
     *
     * @param enabled whether the proxy is enabled
     * @param ingestUrl the PostHog ingest URL (defaults to US cloud)
     * @param assetsUrl the PostHog assets URL (defaults to US cloud)
     */
    public PostHogProxyProperties {
        if (ingestUrl == null) {
            ingestUrl = "https://us.i.posthog.com";
        }
        if (assetsUrl == null) {
            assetsUrl = "https://us-assets.i.posthog.com";
        }
    }
}
