package org.mifos.creditbureau.cb_ild.controller;

import java.time.Instant;

/**
 * Structured error response returned to Angular on all exceptions.
 *
 * Java 21 record — immutable, compact.
 *
 * Fields:
 *   code      — machine-readable error code for Angular switch/case
 *   message   — human-readable message shown to loan officer
 *   timestamp — when the error occurred (ISO-8601)
 *   requestId — correlation ID from MDC for support tracking
 *
 * Security:
 *   Never include: clientId, RFC, raw exception message
 *   message is always generic — never exposes internal details
 *
 * Frontend:
 *   Angular Tab 1 — handles 422 KYC_PREREQUISITE_FAILED specially
 *   All tabs       — show message to loan officer
 *   requestId      — shown in error UI for support reference
 *
 * TypeScript interface:
 *   interface ErrorResponse {
 *     code: string;
 *     message: string;
 *     timestamp: string;
 *     requestId: string;
 *   }
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String requestId
) {}
