package io.nextskip.contests.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.nextskip.common.client.ExternalApiException;
import io.nextskip.common.model.FrequencyBand;
import io.nextskip.contests.internal.dto.ContestSeriesDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Client for scraping WA7BNM Contest Calendar detail pages (contestdetails.php?ref=N). */
@Component
@SuppressWarnings({
        "PMD.AvoidCatchingGenericException", // Intentional: wrap parsing exceptions
        "PMD.CyclomaticComplexity" // HTML scraper requires many parsing branches
})
public class ContestSeriesClient {

    private static final Logger LOG = LoggerFactory.getLogger(ContestSeriesClient.class);
    private static final String CLIENT_NAME = "contest-series";
    private static final String BASE_URL = "https://contestcalendar.com";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    // Patterns for parsing page content
    private static final Pattern REVISION_DATE_PATTERN =
            Pattern.compile("Revision Date:\\s*([A-Za-z]+\\s+\\d{1,2},\\s*\\d{4})");
    private static final DateTimeFormatter REVISION_DATE_FORMAT =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    // WARC bands to exclude when "except WARC" is specified
    private static final Set<FrequencyBand> WARC_BANDS = Set.of(
            FrequencyBand.BAND_60M,
            FrequencyBand.BAND_30M,
            FrequencyBand.BAND_17M,
            FrequencyBand.BAND_12M
    );

    // All HF bands (excluding 6m and 2m)
    private static final Set<FrequencyBand> ALL_HF_BANDS = EnumSet.of(
            FrequencyBand.BAND_160M,
            FrequencyBand.BAND_80M,
            FrequencyBand.BAND_60M,
            FrequencyBand.BAND_40M,
            FrequencyBand.BAND_30M,
            FrequencyBand.BAND_20M,
            FrequencyBand.BAND_17M,
            FrequencyBand.BAND_15M,
            FrequencyBand.BAND_12M,
            FrequencyBand.BAND_10M
    );

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    @Autowired
    public ContestSeriesClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this(webClientBuilder, circuitBreakerRegistry, retryRegistry, BASE_URL);
    }

    protected ContestSeriesClient(
            WebClient.Builder webClientBuilder,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CLIENT_NAME);
        this.retry = retryRegistry.retry(CLIENT_NAME);
    }

    /** Fetches and parses a contest series detail page. */
    public ContestSeriesDto fetchSeriesDetails(String wa7bnmRef) {
        Supplier<ContestSeriesDto> decoratedFetch = () -> doFetchSeriesDetails(wa7bnmRef);

        Supplier<ContestSeriesDto> retryWrapped = Retry.decorateSupplier(retry, decoratedFetch);
        Supplier<ContestSeriesDto> cbWrapped = CircuitBreaker.decorateSupplier(circuitBreaker, retryWrapped);

        try {
            return cbWrapped.get();
        } catch (Exception e) {
            LOG.error("Failed to fetch contest series ref={}: {}", wa7bnmRef, e.getMessage());
            throw new ExternalApiException(CLIENT_NAME,
                    "Failed to fetch contest series ref=" + wa7bnmRef + ": " + e.getMessage(), e);
        }
    }

    /** Fetches only the revision date for change detection. */
    public Optional<LocalDate> fetchRevisionDate(String wa7bnmRef) {
        try {
            String html = fetchPageHtml(wa7bnmRef);
            return parseRevisionDate(html);
        } catch (Exception e) {
            LOG.warn("Failed to fetch revision date for ref={}: {}", wa7bnmRef, e.getMessage());
            return Optional.empty();
        }
    }

    private ContestSeriesDto doFetchSeriesDetails(String wa7bnmRef) {
        try {
            String html = fetchPageHtml(wa7bnmRef);
            return parseContestDetails(wa7bnmRef, html);

        } catch (WebClientResponseException e) {
            LOG.error("HTTP error fetching contest ref={}: {} {}",
                    wa7bnmRef, e.getStatusCode(), e.getStatusText());
            throw new ExternalApiException(CLIENT_NAME,
                    "HTTP " + e.getStatusCode() + " fetching ref=" + wa7bnmRef, e);

        } catch (WebClientRequestException e) {
            LOG.error("Network error fetching contest ref={}", wa7bnmRef, e);
            throw new ExternalApiException(CLIENT_NAME,
                    "Network error fetching ref=" + wa7bnmRef + ": " + e.getMessage(), e);

        } catch (Exception e) {
            LOG.error("Unexpected error fetching contest ref={}", wa7bnmRef, e);
            throw new ExternalApiException(CLIENT_NAME,
                    "Unexpected error fetching ref=" + wa7bnmRef + ": " + e.getMessage(), e);
        }
    }

    private String fetchPageHtml(String wa7bnmRef) {
        LOG.debug("Fetching contest details page for ref={}", wa7bnmRef);

        String html = webClient.get()
                .uri("/contestdetails.php?ref={ref}", wa7bnmRef)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(REQUEST_TIMEOUT)
                .block();

        if (html == null || html.isBlank()) {
            throw new ExternalApiException(CLIENT_NAME, "Empty response for ref=" + wa7bnmRef);
        }

        return html;
    }

    /** Parses contest details from HTML content. Package-private for unit testing. */
    ContestSeriesDto parseContestDetails(String wa7bnmRef, String html) {
        Document doc = Jsoup.parse(html);

        String name = parseContestName(doc);
        Set<FrequencyBand> bands = parseBands(doc);
        Set<String> modes = parseModes(doc);
        String sponsor = parseSponsor(doc);
        String rulesUrl = parseRulesUrl(doc);
        String exchange = parseExchange(doc);
        String cabrilloName = parseCabrilloName(doc);
        LocalDate revisionDate = parseRevisionDate(html).orElse(null);

        LOG.debug("Parsed contest ref={}: name={}, bands={}, modes={}",
                wa7bnmRef, name, bands.size(), modes.size());

        return new ContestSeriesDto(
                wa7bnmRef,
                name,
                bands,
                modes,
                sponsor,
                rulesUrl,
                exchange,
                cabrilloName,
                revisionDate
        );
    }

    private String parseContestName(Document doc) {
        Element h1 = doc.selectFirst("h1");
        if (h1 != null && !h1.text().isBlank()) {
            return h1.text().trim();
        }

        String title = doc.title();
        if (title != null && !title.isBlank()) {
            // Remove common suffixes like " - Contest Calendar"
            int dashIndex = title.indexOf(" - ");
            if (dashIndex > 0) {
                return title.substring(0, dashIndex).trim();
            }
            return title.trim();
        }

        return null;
    }

    Set<FrequencyBand> parseBands(Document doc) {
        String bandsText = findFieldValue(doc, "Bands:");
        if (bandsText == null || bandsText.isBlank()) {
            return Set.of();
        }

        String normalized = bandsText.toLowerCase(Locale.ROOT);

        // Check for "any" or "all"
        if (normalized.contains("any") || normalized.matches("^all\\b.*")) {
            Set<FrequencyBand> result = EnumSet.copyOf(ALL_HF_BANDS);
            // Check for WARC exclusion
            if (normalized.contains("warc")) {
                result.removeAll(WARC_BANDS);
            }
            return result;
        }

        // Parse individual band mentions
        Set<FrequencyBand> bands = EnumSet.noneOf(FrequencyBand.class);
        for (FrequencyBand band : FrequencyBand.values()) {
            String bandName = band.getName().toLowerCase(Locale.ROOT);
            // Match "20m" or "20 m" or just "20" followed by non-digit
            if (normalized.contains(bandName)
                    || normalized.matches(".*\\b" + bandName.replace("m", "") + "\\s*m?\\b.*")) {
                bands.add(band);
            }
        }

        return bands;
    }

    Set<String> parseModes(Document doc) {
        String modeText = findFieldValue(doc, "Mode:");
        if (modeText == null || modeText.isBlank()) {
            return Set.of();
        }

        String normalized = modeText.toLowerCase(Locale.ROOT);
        Set<String> modes = new HashSet<>();

        // Check for "any"
        if (normalized.contains("any")) {
            modes.add("CW");
            modes.add("SSB");
            modes.add("Digital");
            return modes;
        }

        // Check for specific modes
        if (normalized.contains("cw")) {
            modes.add("CW");
        }
        if (normalized.contains("ssb") || normalized.contains("phone")) {
            modes.add("SSB");
        }
        if (normalized.contains("digital") || normalized.contains("rtty")
                || normalized.contains("ft8") || normalized.contains("ft4")
                || normalized.contains("psk")) {
            modes.add("Digital");
        }
        if (normalized.contains("fm")) {
            modes.add("FM");
        }
        if (normalized.contains("am")) {
            modes.add("AM");
        }

        return modes;
    }

    private String parseSponsor(Document doc) {
        return findFieldValue(doc, "Sponsor:");
    }

    private String parseRulesUrl(Document doc) {
        String href = findRulesLinkInTableCells(doc.select("td, th"));
        if (href != null) return href;
        for (Element link : doc.select("a[href]")) {
            href = extractRulesHrefFromLink(link);
            if (href != null) return href;
        }
        return null;
    }

    private String findRulesLinkInTableCells(List<Element> cells) {
        for (int i = 0; i < cells.size() - 1; i++) {
            if (!cells.get(i).text().toLowerCase(Locale.ROOT).contains("find rules at")) continue;
            Element link = cells.get(i + 1).selectFirst("a[href]");
            if (link != null && !link.attr("href").isBlank()) return link.attr("href");
        }
        return null;
    }

    private String extractRulesHrefFromLink(Element link) {
        Element parent = link.parent();
        if (parent == null) return null;
        String parentText = parent.text().toLowerCase(Locale.ROOT);
        if (!parentText.contains("find rules at") && !parentText.contains("official rules")) return null;
        String href = link.attr("abs:href");
        return href.isBlank() ? null : href;
    }

    private String parseExchange(Document doc) {
        return findFieldValue(doc, "Exchange:");
    }

    private String parseCabrilloName(Document doc) {
        return findFieldValue(doc, "Cabrillo name:");
    }

    Optional<LocalDate> parseRevisionDate(String html) {
        Matcher matcher = REVISION_DATE_PATTERN.matcher(html);
        if (matcher.find()) {
            try {
                return Optional.of(LocalDate.parse(matcher.group(1), REVISION_DATE_FORMAT));
            } catch (DateTimeParseException e) {
                LOG.debug("Failed to parse revision date: {}", matcher.group(1));
            }
        }
        return Optional.empty();
    }

    private String findFieldValue(Document doc, String label) {
        String labelLower = label.toLowerCase(Locale.ROOT);
        String result = findValueInTableCells(doc.select("td, th"), labelLower);
        if (result != null) return result;
        result = findValueInDefinitionList(doc.select("dt"), labelLower);
        if (result != null) return result;
        return findValueInBoldText(doc.select("b, strong"), labelLower);
    }

    private String findValueInTableCells(List<Element> cells, String labelLower) {
        for (int i = 0; i < cells.size() - 1; i++) {
            if (!cells.get(i).text().toLowerCase(Locale.ROOT).contains(labelLower)) continue;
            String value = cells.get(i + 1).text().trim();
            if (!value.isBlank()) return value;
        }
        return null;
    }

    private String findValueInDefinitionList(List<Element> dts, String labelLower) {
        for (Element dt : dts) {
            if (!dt.text().toLowerCase(Locale.ROOT).contains(labelLower)) continue;
            Element dd = dt.nextElementSibling();
            if (dd != null && "dd".equals(dd.tagName())) {
                String value = dd.text().trim();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }

    private String findValueInBoldText(List<Element> bolds, String labelLower) {
        for (Element bold : bolds) {
            if (!bold.text().toLowerCase(Locale.ROOT).contains(labelLower)) continue;
            String value = extractValueAfterColon(bold);
            if (value != null) return value;
        }
        return null;
    }

    private String extractValueAfterColon(Element element) {
        Element parent = element.parent();
        if (parent == null) return null;
        String parentText = parent.text();
        int colonIndex = parentText.indexOf(':');
        if (colonIndex < 0 || colonIndex >= parentText.length() - 1) return null;
        String value = parentText.substring(colonIndex + 1).trim();
        return value.isBlank() ? null : value;
    }
}
