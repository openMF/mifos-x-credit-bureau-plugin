package org.mifos.creditbureau.cb_ild.exception;


/**
 * Thrown when CDC connection or read times out.
 *
 * HTTP 503 to Angular.
 * CAN retry — network may recover.
 *
 * Angular shows: "Credit bureau not responding — try again later"
 *
 * Security:
 *   originalMessage stored as cause — never in Angular response
 *   clientId stored internally — never logged
 *   cause message never logged — may contain internal network details
 */
public class CdcTimeoutException extends RuntimeException {

    private final Long clientId;

    public CdcTimeoutException(Long clientId, String originalMessage) {
        super("CDC connection timed out — retry eligible",
                new RuntimeException(originalMessage));
        this.clientId = clientId;
    }

    public Long getClientId() {
        return clientId;
    }
}
