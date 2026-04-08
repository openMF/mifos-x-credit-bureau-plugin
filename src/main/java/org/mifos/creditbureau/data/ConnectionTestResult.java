package org.mifos.creditbureau.data;

import lombok.Builder;
import lombok.Getter;

/**
 * Result of a connectivity test with an external credit bureau.
 */
@Builder
@Getter
public class ConnectionTestResult {

    /** Whether the connection test succeeded. */
    private final boolean success;

    /** The bureau type tested (e.g., "CIRCULO_DE_CREDITO"). */
    private final String bureauType;

    /** Human-readable message or response body from the bureau. */
    private final String message;

    /** HTTP status code returned by the bureau (0 if connection failed). */
    private final int httpStatusCode;
}
