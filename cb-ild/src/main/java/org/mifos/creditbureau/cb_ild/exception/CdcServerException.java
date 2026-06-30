package org.mifos.creditbureau.cb_ild.exception;


/**
 * Thrown when CDC returns 5xx — CDC is down or overloaded.
 *
 * HTTP 503 to Angular.
 * CAN retry — CDC may recover.
 *
 * Angular shows: "Credit bureau temporarily unavailable — try again later"
 *
 * Security:
 *   clientId stored internally — never in Angular message
 *   httpStatus stored for retry logic — never in Angular message
 */
public class CdcServerException extends RuntimeException {

    private final Long clientId;
    private final int httpStatus;

    public CdcServerException(Long clientId, int httpStatus) {
        super("CDC server error — retry eligible");
        this.clientId = clientId;
        this.httpStatus = httpStatus;
    }

    public Long getClientId() {
        return clientId;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
