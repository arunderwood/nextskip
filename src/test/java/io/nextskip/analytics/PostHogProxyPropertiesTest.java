package io.nextskip.analytics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PostHogProxyProperties configuration record.
 */
class PostHogProxyPropertiesTest {

    private static final String DEFAULT_INGEST_URL = "https://us.i.posthog.com";
    private static final String DEFAULT_ASSETS_URL = "https://us-assets.i.posthog.com";
    private static final String CUSTOM_INGEST_URL = "https://eu.i.posthog.com";
    private static final String CUSTOM_ASSETS_URL = "https://eu-assets.i.posthog.com";

    @Test
    void testDefaults_WhenNullUrls_UsesDefaultValues() {
        // When: Create with null URLs
        PostHogProxyProperties properties = new PostHogProxyProperties(true, null, null);

        // Then: Defaults are applied
        assertEquals(DEFAULT_INGEST_URL, properties.ingestUrl());
        assertEquals(DEFAULT_ASSETS_URL, properties.assetsUrl());
        assertTrue(properties.enabled());
    }

    @Test
    void testCustomUrls_WhenProvided_UsesCustomValues() {
        // When: Create with custom URLs
        PostHogProxyProperties properties = new PostHogProxyProperties(
                true, CUSTOM_INGEST_URL, CUSTOM_ASSETS_URL);

        // Then: Custom values are used
        assertEquals(CUSTOM_INGEST_URL, properties.ingestUrl());
        assertEquals(CUSTOM_ASSETS_URL, properties.assetsUrl());
    }

    @Test
    void testEnabled_WhenFalse_ReturnsFalse() {
        // When: Create with enabled=false
        PostHogProxyProperties properties = new PostHogProxyProperties(false, null, null);

        // Then: Enabled is false
        assertFalse(properties.enabled());
    }

    @Test
    void testMixedNulls_OnlyNullIngestUrl_UsesDefaultForIngest() {
        // When: Only ingestUrl is null
        PostHogProxyProperties properties = new PostHogProxyProperties(
                true, null, CUSTOM_ASSETS_URL);

        // Then: Default used for ingest, custom for assets
        assertEquals(DEFAULT_INGEST_URL, properties.ingestUrl());
        assertEquals(CUSTOM_ASSETS_URL, properties.assetsUrl());
    }

    @Test
    void testMixedNulls_OnlyNullAssetsUrl_UsesDefaultForAssets() {
        // When: Only assetsUrl is null
        PostHogProxyProperties properties = new PostHogProxyProperties(
                true, CUSTOM_INGEST_URL, null);

        // Then: Custom used for ingest, default for assets
        assertEquals(CUSTOM_INGEST_URL, properties.ingestUrl());
        assertEquals(DEFAULT_ASSETS_URL, properties.assetsUrl());
    }
}
