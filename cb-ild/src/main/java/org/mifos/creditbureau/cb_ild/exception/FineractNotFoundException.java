package org.mifos.creditbureau.cb_ild.exception;

/**
 * Thrown when Fineract returns HTTP 404 for a client.
 *
 * Means the client does not exist in Fineract.
 * GlobalExceptionHandler maps this to HTTP 404 response.
 * Never retried — if client does not exist, retrying will
 * always return 404.
 *
 * SECURITY: Message never contains clientId in plain text
 * when clientId could be considered sensitive. Use generic
 * message for external responses, log details internally.
 */
public class FineractNotFoundException extends RuntimeException {

    private final Long clientId;

    public FineractNotFoundException(Long clientId) {
        super("Client not found in Fineract");
        this.clientId = clientId;
    }

    public Long getClientId() {
        return clientId;
    }
}
