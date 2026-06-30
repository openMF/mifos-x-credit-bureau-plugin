package org.mifos.creditbureau.cb_ild.exception;

/**
 * Thrown when Fineract returns HTTP 5xx response.
 *
 * Means Fineract has a server-side problem.
 * GlobalExceptionHandler maps this to HTTP 503 response.
 * Retry eligible — Fineract may recover on next attempt.
 *
 * Stores the HTTP status code for logging purposes.
 * Status code is logged internally only — never exposed
 * in the response body sent to Angular.
 */
public class FineractServerException extends RuntimeException {

    private final int httpStatus;

    public FineractServerException(int httpStatus) {
        super("Fineract server error — retry eligible");
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
