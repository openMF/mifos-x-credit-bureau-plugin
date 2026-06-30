package org.mifos.creditbureau.cb_ild.exception;


/**
 * Thrown when real CDC mode is requested but not yet configured.
 *
 * HTTP 503 to Angular.
 * Angular shows: "CDC integration not yet configured — contact admin"
 *
 * Thrown when mifos.cdc.mock.enabled=false but Phase 2 implementation
 * is not complete. Replaces UnsupportedOperationException which:
 *   1. Falls through to generic 500 INTERNAL_ERROR handler
 *   2. Exposes internal message "Real CDC mode not implemented yet"
 *   3. Gives Angular no actionable guidance
 *
 * Security:
 *   Message is generic — no internal implementation details exposed
 */
public class CdcNotConfiguredException extends RuntimeException {

    public CdcNotConfiguredException() {
        super("CDC integration not configured — contact administrator");
    }
}
