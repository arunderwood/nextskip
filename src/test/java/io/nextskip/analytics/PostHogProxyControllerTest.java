package io.nextskip.analytics;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for PostHogProxyController using WireMock.
 *
 * <p>Tests verify that the proxy correctly forwards requests to PostHog,
 * filters blocked headers, and forwards client IP addresses.</p>
 */
class PostHogProxyControllerTest {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String SUCCESS_RESPONSE = "{\"status\":1}";
    private static final String HTTP_METHOD_POST = "POST";
    private static final String HTTP_METHOD_GET = "GET";
    private static final String INGEST_PATH = "/a/e";
    private static final String INGEST_ENDPOINT = "/e";
    private static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /** RFC 5737 TEST-NET-1 IP address for documentation/testing purposes. */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String TEST_CLIENT_IP = "192.0.2.42";

    /** Localhost IP for default mock requests. */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private static final String LOCALHOST_IP = "127.0.0.1";

    private static final byte[] SAMPLE_BODY =
            "{\"event\":\"test\"}".getBytes(StandardCharsets.UTF_8);

    private WireMockServer ingestServer;
    private WireMockServer assetsServer;
    private PostHogProxyController controller;

    @BeforeEach
    void setUp() {
        // Start WireMock servers for ingest and assets endpoints
        ingestServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        ingestServer.start();

        assetsServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        assetsServer.start();

        // Configure properties to use WireMock servers
        String ingestUrl = "http://localhost:" + ingestServer.port();
        String assetsUrl = "http://localhost:" + assetsServer.port();
        PostHogProxyProperties properties = new PostHogProxyProperties(true, ingestUrl, assetsUrl);

        // Create controller with WebClient
        WebClient.Builder webClientBuilder = WebClient.builder();
        controller = new PostHogProxyController(webClientBuilder, properties);
    }

    @AfterEach
    void tearDown() {
        ingestServer.stop();
        assetsServer.stop();
    }

    @Test
    void testProxyIngest_ForwardsRequest_Success() {
        // Given: PostHog ingest endpoint returns success
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(SUCCESS_RESPONSE)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Response is forwarded correctly
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        String bodyStr = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue(bodyStr.contains("status"));

        // Verify WireMock received the request
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT)));
    }

    @Test
    void testProxyIngest_ForwardsQueryString() {
        // Given: Ingest endpoint expects query parameters
        ingestServer.stubFor(get(urlEqualTo("/decide?v=3&ip=1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"featureFlags\":{}}")));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_GET, "/a/decide");
        request.setQueryString("v=3&ip=1");

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, null).block();

        // Then: Query string is preserved
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        ingestServer.verify(getRequestedFor(urlEqualTo("/decide?v=3&ip=1")));
    }

    @Test
    void testProxyAssets_ForwardsStaticRequest() {
        // Given: Assets CDN returns JavaScript file
        String jsContent = "!function(){console.log('posthog')}()";
        assetsServer.stubFor(get(urlPathEqualTo("/static/array.js"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/javascript")
                        .withBody(jsContent)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_GET, "/a/static/array.js");

        // When: Proxy the assets request
        ResponseEntity<byte[]> response = controller.proxyAssets(request).block();

        // Then: Asset content is returned
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(jsContent.getBytes(StandardCharsets.UTF_8), response.getBody());
        assetsServer.verify(getRequestedFor(urlPathEqualTo("/static/array.js")));
    }

    @Test
    void testProxyIngest_ForwardsClientIp() {
        // Given: Request with client IP via RemoteAddr (Tomcat parsed)
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse().withStatus(200)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setRemoteAddr(TEST_CLIENT_IP);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Client IP is forwarded via X-Forwarded-For and X-Real-IP
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT))
                .withHeader("X-Forwarded-For", equalTo(TEST_CLIENT_IP))
                .withHeader("X-Real-IP", equalTo(TEST_CLIENT_IP)));
    }

    @Test
    void testProxyIngest_SetsHostHeader() {
        // Given: Ingest server
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse().withStatus(200)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Host header is set to target hostname (URI.getHost() returns hostname without port)
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT))
                .withHeader(HttpHeaders.HOST, equalTo("localhost")));
    }

    @Test
    void testProxyIngest_FiltersCookieHeader() {
        // Given: Request contains cookie header (should be blocked)
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse().withStatus(200)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.addHeader("Cookie", "session=abc123");
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Cookie header is not forwarded
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT))
                .withoutHeader("Cookie"));
    }

    @Test
    void testProxyIngest_FiltersHopByHopHeaders() {
        // Given: Request contains hop-by-hop headers
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse().withStatus(200)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.addHeader("Connection", "keep-alive");
        request.addHeader(TRANSFER_ENCODING, "chunked");
        request.addHeader("Upgrade", "websocket");
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Hop-by-hop headers are not forwarded
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT))
                .withoutHeader("Connection")
                .withoutHeader(TRANSFER_ENCODING)
                .withoutHeader("Upgrade"));
    }

    @Test
    void testProxyIngest_ForwardsSafeHeaders() {
        // Given: Request contains safe headers that should be forwarded
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse().withStatus(200)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.addHeader("User-Agent", "Mozilla/5.0");
        request.addHeader("Accept", "application/json");
        request.addHeader("Accept-Language", "en-US");
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Safe headers are forwarded
        ingestServer.verify(postRequestedFor(urlPathEqualTo(INGEST_ENDPOINT))
                .withHeader("User-Agent", equalTo("Mozilla/5.0"))
                .withHeader("Accept", equalTo("application/json"))
                .withHeader("Accept-Language", equalTo("en-US")));
    }

    @Test
    void testProxyIngest_FiltersSetCookieFromResponse() {
        // Given: PostHog response contains Set-Cookie header
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Set-Cookie", "ph_session=xyz")
                        .withHeader("Set-Cookie2", "legacy=cookie")
                        .withBody(SUCCESS_RESPONSE)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Set-Cookie headers are filtered from response
        assertNotNull(response);
        assertNull(response.getHeaders().get("Set-Cookie"));
        assertNull(response.getHeaders().get("Set-Cookie2"));
    }

    @Test
    void testProxyIngest_FiltersTransferEncodingFromResponse() {
        // Given: Response contains transfer-encoding header
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(TRANSFER_ENCODING, "chunked")
                        .withBody(SUCCESS_RESPONSE)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Transfer-Encoding is filtered from response
        assertNotNull(response);
        assertNull(response.getHeaders().get(TRANSFER_ENCODING));
    }

    @Test
    void testProxyIngest_PreservesContentTypeInResponse() {
        // Given: Response contains content-type (should be preserved)
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(SUCCESS_RESPONSE)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Content-Type is preserved
        assertNotNull(response);
        List<String> contentTypes = response.getHeaders().get(CONTENT_TYPE);
        assertNotNull(contentTypes);
        assertTrue(contentTypes.contains(CONTENT_TYPE_JSON));
    }

    @Test
    void testProxyIngest_Returns502OnUpstreamError() {
        // Given: Ingest server is unreachable (connection refused)
        ingestServer.stop();

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Returns 502 Bad Gateway
        assertNotNull(response);
        assertEquals(502, response.getStatusCode().value());
    }

    @Test
    void testProxyIngest_NoBody() {
        // Given: GET request with no body
        ingestServer.stubFor(get(urlPathEqualTo("/decide"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"featureFlags\":{}}")));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_GET, "/a/decide");

        // When: Proxy the request without body
        ResponseEntity<byte[]> response = controller.proxyIngest(request, null).block();

        // Then: Request succeeds
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testProxyIngest_PreservesResponseBody() {
        // Given: Upstream returns specific response body
        String expectedBody = "{\"status\":1,\"queued\":true}";
        ingestServer.stubFor(post(urlPathEqualTo(INGEST_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .withBody(expectedBody)));

        MockHttpServletRequest request = createMockRequest(HTTP_METHOD_POST, INGEST_PATH);
        request.setContent(SAMPLE_BODY);

        // When: Proxy the request
        ResponseEntity<byte[]> response = controller.proxyIngest(request, SAMPLE_BODY).block();

        // Then: Response body is preserved exactly
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(expectedBody, new String(response.getBody(), StandardCharsets.UTF_8));
    }

    private MockHttpServletRequest createMockRequest(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.setRemoteAddr(LOCALHOST_IP);
        return request;
    }
}
