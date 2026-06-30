package org.mifos.creditbureau.cb_ild.exception;


/**
 * Thrown when CDC returns 4xx — data quality problem.
 *
 * HTTP 400 to Angular.
 * NEVER retry — same bad data will always fail.
 *
 * Causes:
 *   - Wrong RFC format
 *   - Missing mandatory field in CDC request
 *   - Invalid date format
 *
 * Security:
 *   responseBody truncated to 200 chars — no raw PII stored
 *   clientId stored internally — never in Angular message
 *   responseBody never logged — may contain PII
 */
public class CdcBadRequestException extends RuntimeException {

    private final Long clientId;
    private final String truncatedBody;

    public CdcBadRequestException(Long clientId, String responseBody) {
        super("CDC rejected request — data quality issue");
        this.clientId = clientId;
        this.truncatedBody = responseBody != null
                ? responseBody.substring(0, Math.min(responseBody.length(), 200))
                : null;
    }

    public Long getClientId() {
        return clientId;
    }

    public String getTruncatedBody() {
        return truncatedBody;
    }
}
