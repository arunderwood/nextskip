package io.nextskip.common.client;

/**
 * Exception thrown when an external API call fails.
 *
 * <p>This can represent network errors, HTTP errors, or other API communication issues.
 * Used by all external data clients across NextSkip modules.
 */
public class ExternalApiException extends RuntimeException {

    private final String apiName;

    public ExternalApiException(String apiName, String message) {
        super(message);
        this.apiName = apiName;
    }

    public ExternalApiException(String apiName, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
    }

    public String getApiName() {
        return apiName;
    }
}
