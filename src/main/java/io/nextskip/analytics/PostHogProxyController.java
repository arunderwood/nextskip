package io.nextskip.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * Reverse proxy controller for PostHog analytics.
 *
 * <p>This controller forwards requests from /a/* to PostHog's servers,
 * allowing analytics data to be sent through a first-party domain to
 * avoid ad blocker interference.</p>
 *
 * <p>Security features implemented per PostHog best practices:</p>
 * <ul>
 *   <li>Standard IP detection (X-Forwarded-For, X-Real-IP, Forwarded RFC 7239)</li>
 *   <li>Host header rewritten to PostHog's domain</li>
 *   <li>Cookie filtering in both directions</li>
 *   <li>Hop-by-hop header filtering</li>
 * </ul>
 *
 * @see <a href="https://posthog.com/docs/advanced/proxy">PostHog Proxy Documentation</a>
 */
@RestController
@RequestMapping("/a")
@ConditionalOnProperty(name = "posthog.proxy.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PostHogProxyProperties.class)
public class PostHogProxyController {

    /** Headers that should NOT be forwarded in requests (hop-by-hop + security). */
    private static final Set<String> BLOCKED_REQUEST_HEADERS = Set.of(
        "host", "cookie", "connection", "keep-alive",
        "transfer-encoding", "te", "upgrade", "proxy-authorization"
    );

    /** Headers that should NOT be forwarded in responses (hop-by-hop + security). */
    private static final Set<String> BLOCKED_RESPONSE_HEADERS = Set.of(
        "transfer-encoding", "connection", "keep-alive",
        "set-cookie", "set-cookie2"
    );

    private final WebClient webClient;
    private final PostHogProxyProperties properties;

    /**
     * Creates a new PostHogProxyController.
     *
     * @param webClientBuilder the WebClient builder for making HTTP requests
     * @param properties the PostHog proxy configuration properties
     */
    public PostHogProxyController(
            WebClient.Builder webClientBuilder,
            PostHogProxyProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    /**
     * Proxies requests to PostHog's static assets CDN.
     *
     * @param request the incoming HTTP request
     * @return the proxied response from PostHog
     */
    @RequestMapping("/static/**")
    public Mono<ResponseEntity<byte[]>> proxyAssets(HttpServletRequest request) {
        String path = request.getRequestURI().substring("/a".length());
        URI targetUri = URI.create(properties.assetsUrl() + path);
        return forwardRequest(targetUri, request, null);
    }

    /**
     * Proxies requests to PostHog's ingest endpoint.
     *
     * @param request the incoming HTTP request
     * @param body the request body (optional)
     * @return the proxied response from PostHog
     */
    @RequestMapping("/**")
    public Mono<ResponseEntity<byte[]>> proxyIngest(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().substring("/a".length());
        String query = request.getQueryString();
        String url = properties.ingestUrl() + path + (query != null ? "?" + query : "");
        return forwardRequest(URI.create(url), request, body);
    }

    private Mono<ResponseEntity<byte[]>> forwardRequest(
            URI targetUri, HttpServletRequest request, byte[] body) {

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        String clientIp = getClientIp(request);

        WebClient.RequestBodySpec spec = webClient.method(method)
                .uri(targetUri)
                // Set Host header to PostHog's host (required)
                .header(HttpHeaders.HOST, targetUri.getHost())
                // Forward client IP for geolocation (PostHog best practice)
                .header("X-Forwarded-For", clientIp)
                .header("X-Real-IP", clientIp);

        // Forward safe headers from original request
        request.getHeaderNames().asIterator().forEachRemaining(name -> {
            if (!BLOCKED_REQUEST_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                spec.header(name, request.getHeader(name));
            }
        });

        WebClient.RequestHeadersSpec<?> headersSpec =
            body != null ? spec.bodyValue(body) : spec;

        return headersSpec.retrieve()
                .toEntity(byte[].class)
                .map(response -> ResponseEntity
                        .status(response.getStatusCode())
                        .headers(filterResponseHeaders(response.getHeaders()))
                        .body(response.getBody()))
                .onErrorResume(e -> Mono.just(
                        ResponseEntity.status(502).build()));
    }

    /**
     * Gets the client IP address from the request.
     *
     * <p>This method relies on Tomcat's RemoteIpValve, which is enabled via
     * {@code server.forward-headers-strategy: framework} in application.yml.
     * The valve automatically parses X-Forwarded-For and RFC 7239 Forwarded
     * headers, making {@code request.getRemoteAddr()} return the correct
     * client IP address.</p>
     *
     * @param request the HTTP request
     * @return the client IP address as parsed by Tomcat's RemoteIpValve
     * @see <a href="https://tomcat.apache.org/tomcat-10.1-doc/api/org/apache/catalina/valves/RemoteIpValve.html">
     *      Tomcat RemoteIpValve</a>
     */
    private String getClientIp(HttpServletRequest request) {
        // Tomcat's RemoteIpValve (enabled via forward-headers-strategy: framework)
        // has already parsed X-Forwarded-For and Forwarded headers.
        return request.getRemoteAddr();
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders headers) {
        HttpHeaders filtered = new HttpHeaders();
        headers.forEach((name, values) -> {
            if (!BLOCKED_RESPONSE_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                filtered.addAll(name, values);
            }
        });
        return filtered;
    }
}
