package io.nextskip.common.client;

/**
 * Exception thrown when an API response has invalid or unexpected data.
 *
 * <p>This indicates a parsing error, validation failure, or API contract violation.
 * Used by all external data clients across NextSkip modules.
 */
public class InvalidApiResponseException extends ExternalApiException {

    public InvalidApiResponseException(String apiName, String message) {
        super(apiName, message);
    }

    public InvalidApiResponseException(String apiName, String message, Throwable cause) {
        super(apiName, message, cause);
    }
}
