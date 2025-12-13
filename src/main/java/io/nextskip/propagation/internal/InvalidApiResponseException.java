package io.nextskip.propagation.internal;

/**
 * Exception thrown when an API response has invalid or unexpected data.
 *
 * This indicates a parsing error, validation failure, or API contract violation.
 */
public class InvalidApiResponseException extends ExternalApiException {

    public InvalidApiResponseException(String apiName, String message) {
        super(apiName, message);
    }

    public InvalidApiResponseException(String apiName, String message, Throwable cause) {
        super(apiName, message, cause);
    }
}
