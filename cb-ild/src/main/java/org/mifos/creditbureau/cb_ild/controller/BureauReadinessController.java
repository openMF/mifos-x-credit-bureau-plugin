package org.mifos.creditbureau.cb_ild.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.aop.Auditable;
import org.mifos.creditbureau.cb_ild.dto.AuditEntryDTO;
import org.mifos.creditbureau.cb_ild.dto.BureauResponseDTO;
import org.mifos.creditbureau.cb_ild.entity.AuditEntry;
import org.mifos.creditbureau.cb_ild.repository.AuditEntryRepository;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.mifos.creditbureau.cb_ild.service.bureau.IBureauReadinessService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/clients")
@Tag(
    name = "1. Bureau Readiness",
    description = "Client-level CDC operations — KYC check (Trigger 1), stored credit report, and compliance audit trail."
)
public class BureauReadinessController {

    private final IBureauReadinessService bureauReadinessService;
    private final BureauResponseRepository bureauResponseRepository;
    private final AuditEntryRepository auditEntryRepository;

    public BureauReadinessController(
            IBureauReadinessService bureauReadinessService,
            BureauResponseRepository bureauResponseRepository,
            AuditEntryRepository auditEntryRepository) {
        this.bureauReadinessService = bureauReadinessService;
        this.bureauResponseRepository = bureauResponseRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Operation(
        summary = "Trigger 1 — KYC readiness check + CDC credit report pull",
        description = """
            **What this does:**
            1. Fetches client data from Fineract (name, DOB, RFC, address)
            2. Runs KycCompletenessScorer — weighted 0-100 score
            3. RFC missing = score 0, CDC never called (hard veto)
            4. Score >= 70 = calls CDC via plugin, saves bureau_response
            5. Returns KycReadinessResult

            **Response fields:**
            - `score` — KYC completeness score (0-100, threshold 70)
            - `ready` — true if score >= 70
            - `missingFields` — list of missing data fields
            - `riskBand` — LOW / MEDIUM / HIGH / VERY_HIGH from CDC
            - `ficoScore` — credit score from CDC (null if not available)
            - `scoreDropAlert` — true if score dropped since last pull
            - `pulledAt` — timestamp of this CDC pull

            **Requires:** VPN connected + plugin running for live CDC call

            **Roles:** KYC_OFFICER, COMPLIANCE (CREDIT_ANALYST gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — KYC score and CDC result returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — wrong or missing credentials"),
        @ApiResponse(responseCode = "403", description = "Forbidden — CREDIT_ANALYST cannot call this endpoint"),
        @ApiResponse(responseCode = "404", description = "Client not found in Fineract"),
        @ApiResponse(responseCode = "503", description = "Fineract or CDC unreachable — check VPN and plugin")
    })
    @GetMapping("/{id}/bureau-readiness")
    @PreAuthorize("hasAnyRole('KYC_OFFICER', 'COMPLIANCE')")
    @Auditable(action = "BUREAU_READINESS_CHECK", entityType = "BureauResponse")
    public ResponseEntity<KycReadinessResult> getBureauReadiness(
            @Parameter(description = "Fineract client ID", example = "5", required = true)
            @PathVariable Long id) {
        log.info("Bureau readiness request — clientId: {}", id);
        KycReadinessResult result = bureauReadinessService.checkReadiness(id);
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Get stored CDC credit report for a client",
        description = """
            Returns the latest CDC credit report stored in cbild_db.
            Does NOT make a new CDC call — reads from database only.
            Safe to call anytime without VPN or plugin.

            **Key fields:**
            - `riskBand` — LOW / MEDIUM / HIGH / VERY_HIGH
            - `expiryDate` — LRSIC 72-month retention date (e.g. 2032-06-28)
            - `tradelines` — JSON array of existing loans and payment history
            - `hasDelinquencies` — true if client has missed payments
            - `scoreDropAlert` — true if FICO score dropped since last pull

            **Never exposed:** fullResponse (raw CDC JSON), rawResponseHash, softDeleted

            **Roles:** CREDIT_ANALYST, COMPLIANCE (KYC_OFFICER gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — BureauResponseDTO returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — wrong or missing credentials"),
        @ApiResponse(responseCode = "403", description = "Forbidden — KYC_OFFICER cannot call this endpoint"),
        @ApiResponse(responseCode = "404", description = "No CDC report found for this client — call bureau-readiness first")
    })
    @GetMapping("/{id}/bureau-response")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')")
    @Auditable(action = "BUREAU_RESPONSE_READ", entityType = "BureauResponse")
    public ResponseEntity<BureauResponseDTO> getBureauResponse(
            @Parameter(description = "Fineract client ID", example = "5", required = true)
            @PathVariable Long id) {
        log.info("Bureau response request — clientId: {}", id);
        return bureauResponseRepository
                .findTopByClientIdOrderByPulledAtDesc(id)
                .map(BureauResponseDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "COMPLIANCE ONLY — Paginated audit trail for a client",
        description = """
            Every action ever taken on this client — who did it, when, result, duration.
            This is the ONLY endpoint exclusive to COMPLIANCE role.
            KYC_OFFICER and CREDIT_ANALYST always get 403 here (separation of duties).

            **Key fields:**
            - `action` — BUREAU_READINESS_CHECK / SUBMISSION_BATCH_START / SUBMISSION_SCREENING / SUBMISSION_LOAN_APPROVAL
            - `performedBy` — which user triggered the action (kyc_officer / credit_analyst / compliance)
            - `result` — SUCCESS or FAILURE
            - `durationMs` — how long the operation took in milliseconds
            - `requestId` — UUID linking this entry to the server log line

            **Never exposed:** oldValue, newValue, errorMessage (may contain internal details)

            **Date filter:** provide both startDate AND endDate to filter by range.

            **Roles:** COMPLIANCE only
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — paginated audit entries returned"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — wrong or missing credentials"),
        @ApiResponse(responseCode = "403", description = "Forbidden — ONLY COMPLIANCE can call this. KYC_OFFICER and CREDIT_ANALYST always get 403.")
    })
    @GetMapping("/{id}/audit-trail")
    @PreAuthorize("hasRole('COMPLIANCE')")
    public ResponseEntity<PagedModel<AuditEntryDTO>> getAuditTrail(
            @Parameter(description = "Fineract client ID", example = "5", required = true)
            @PathVariable Long id,
            @Parameter(description = "Filter start date (ISO format)", example = "2026-06-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Filter end date (ISO format)", example = "2026-06-30T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Page number (zero-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            PagedResourcesAssembler<AuditEntryDTO> assembler) {
        log.info("Audit trail request — clientId: {}, startDate: {}, endDate: {}",
                id, startDate, endDate);
        Page<AuditEntry> entries;
        PageRequest pageable = PageRequest.of(page, size);
        if (startDate != null && endDate != null) {
            entries = auditEntryRepository
                    .findByClientIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                            id, startDate, endDate, pageable);
        } else {
            entries = auditEntryRepository
                    .findByClientIdOrderByCreatedAtDesc(id, pageable);
        }
        Page<AuditEntryDTO> dtoPage = entries.map(AuditEntryDTO::from);
        @SuppressWarnings("unchecked")
        var pagedModel = (PagedModel<AuditEntryDTO>)(Object) assembler.toModel(dtoPage);
        return ResponseEntity.ok(pagedModel);
    }
}
