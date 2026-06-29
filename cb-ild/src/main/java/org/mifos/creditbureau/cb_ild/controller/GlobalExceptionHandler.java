package org.mifos.creditbureau.cb_ild.controller;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.exception.CdcBadRequestException;
import org.mifos.creditbureau.cb_ild.exception.CdcNotConfiguredException;
import org.mifos.creditbureau.cb_ild.exception.CdcServerException;
import org.mifos.creditbureau.cb_ild.exception.CdcTimeoutException;
import org.mifos.creditbureau.cb_ild.exception.FineractConnectionException;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.exception.FineractServerException;
import org.mifos.creditbureau.cb_ild.exception.KycPrerequisiteException;
import org.slf4j.MDC;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Global exception handler — catches all exceptions from all controllers.
 *
 * Returns structured ErrorResponse to Angular on every error.
 * No more silent 500s — every error has a code + message.
 *
 * Security:
 *   Never expose: clientId, RFC, raw exception messages, stack traces
 *   All messages are generic — safe for Angular to display
 *   requestId from MDC — set by CorrelationIdFilter (Day 14)
 *
 * Frontend:
 *   Angular Tab 1 — 422 KYC_PREREQUISITE_FAILED → show "add RFC" message
 *   All tabs       — 503 CDC errors → show "try again" message
 *   All tabs       — 500 INTERNAL_ERROR → show "contact support" message
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== KYC PREREQUISITE — 422 =====

    @ExceptionHandler(KycPrerequisiteException.class)
    public ResponseEntity<ErrorResponse> handleKycPrerequisite(
            KycPrerequisiteException ex) {
        log.warn("KYC prerequisite failed — clientId: {}, missing: {}",
                ex.getClientId(), ex.getMissingField());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(buildResponse(
                        "KYC_PREREQUISITE_FAILED",
                        ex.getMessage()));
    }

    // ===== CDC BAD REQUEST — 400 =====

    @ExceptionHandler(CdcBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleCdcBadRequest(
            CdcBadRequestException ex) {
        log.error("CDC bad request — clientId: {}", ex.getClientId());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(
                        "CDC_BAD_REQUEST",
                        ex.getMessage()));
    }

    // ===== CDC SERVER ERROR — 503 =====

    @ExceptionHandler(CdcServerException.class)
    public ResponseEntity<ErrorResponse> handleCdcServer(
            CdcServerException ex) {
        log.error("CDC server error — clientId: {}, status: {}",
                ex.getClientId(), ex.getHttpStatus());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildResponse(
                        "CDC_SERVER_ERROR",
                        ex.getMessage()));
    }

    // ===== CDC TIMEOUT — 503 =====

    @ExceptionHandler(CdcTimeoutException.class)
    public ResponseEntity<ErrorResponse> handleCdcTimeout(
            CdcTimeoutException ex) {
        log.error("CDC timeout — clientId: {}", ex.getClientId());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildResponse(
                        "CDC_TIMEOUT",
                        ex.getMessage()));
    }

    // ===== FINERACT NOT FOUND — 404 =====

    @ExceptionHandler(FineractNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFineractNotFound(
            FineractNotFoundException ex) {
        log.warn("Fineract client not found — clientId: {}",
                ex.getClientId());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(buildResponse(
                        "FINERACT_CLIENT_NOT_FOUND",
                        ex.getMessage()));
    }

    // ===== CDC NOT CONFIGURED — 503 =====

    @ExceptionHandler(CdcNotConfiguredException.class)
    public ResponseEntity<ErrorResponse> handleCdcNotConfigured(
            CdcNotConfiguredException ex) {
        log.error("CDC not configured — requestId: {}",
                MDC.get("requestId"));
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildResponse(
                        "CDC_NOT_CONFIGURED",
                        ex.getMessage()));
    }

    // ===== FINERACT CONNECTION ERROR — 504 =====

    @ExceptionHandler(FineractConnectionException.class)
    public ResponseEntity<ErrorResponse> handleFineractConnection(
            FineractConnectionException ex) {
        // Fineract unreachable — retry eligible
        // Angular: show "Core banking system unreachable — try again"
        log.error("Fineract connection error — requestId: {}",
                MDC.get("requestId"));
        return ResponseEntity
                .status(HttpStatus.GATEWAY_TIMEOUT)
                .body(buildResponse(
                        "FINERACT_UNREACHABLE",
                        ex.getMessage()));
    }

    // ===== FINERACT SERVER ERROR — 503 =====

    @ExceptionHandler(FineractServerException.class)
    public ResponseEntity<ErrorResponse> handleFineractServer(
            FineractServerException ex) {
        // Fineract returned 5xx — retry eligible
        // Angular: show "Core banking system error — try again"
        log.error("Fineract server error — requestId: {}",
                MDC.get("requestId"));
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(buildResponse(
                        "FINERACT_SERVER_ERROR",
                        ex.getMessage()));
    }


    // ===== ACCESS DENIED — 403 =====

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex) {
        log.warn("Access denied — requestId: {}", MDC.get("requestId"));
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildResponse(
                        "ACCESS_DENIED",
                        "You do not have permission to perform this action"));
    }

    // ===== ILLEGAL ARGUMENT -- 400 =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Invalid argument - requestId: {}, message: {}",
                MDC.get("requestId"), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(
                        "INVALID_ARGUMENT",
                        ex.getMessage()));
    }

    // ===== ILLEGAL STATE TRANSITION -- 400 =====

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex) {
        log.warn("Invalid state transition - requestId: {}, message: {}",
                MDC.get("requestId"), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildResponse(
                        "INVALID_STATE_TRANSITION",
                        ex.getMessage()));
    }

    // ===== GENERIC FALLBACK — 500 =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        // Never expose real exception message — may contain PII
        log.error("Unexpected error — requestId: {}",
                MDC.get("requestId"), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred"));
    }

    // ===== HELPER =====

    private ErrorResponse buildResponse(String code, String message) {
        return new ErrorResponse(
                code,
                message,
                Instant.now(),
                MDC.get("requestId") != null
                        ? MDC.get("requestId")
                        : "unknown"
        );
    }
}
