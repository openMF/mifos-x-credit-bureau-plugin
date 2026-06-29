package org.mifos.creditbureau.cb_ild.exception;

/**
 * Thrown when connection to Fineract times out or is refused.
 *
 * Means network problem — Fineract unreachable.
 * GlobalExceptionHandler maps this to HTTP 504 response.
 * Retry eligible — network may recover on next attempt.
 *
 * originalMessage is stored internally for logging only.
 * Never exposed in response body sent to Angular.
 * Generic message returned to caller — no internal details leaked.
 */
public class FineractConnectionException extends RuntimeException {

    public FineractConnectionException(String originalMessage) {
        super("Could not connect to Fineract — retry eligible",
                new RuntimeException(originalMessage));
    }
}
