package org.mifos.creditbureau.cb_ild.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.dto.DisputeResponse;
import org.mifos.creditbureau.cb_ild.entity.DisputeCase;
import org.mifos.creditbureau.cb_ild.repository.DisputeCaseRepository;
import org.mifos.creditbureau.cb_ild.service.dispute.IDisputeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/disputes")
@Tag(
    name = "3. Dispute Management",
    description = "Dispute workflow. OPEN -> UNDER_REVIEW: all roles. UNDER_REVIEW -> RESOLVED: COMPLIANCE only."
)
public class DisputeController {

    private final IDisputeService disputeService;
    private final DisputeCaseRepository disputeCaseRepository;

    public DisputeController(
            IDisputeService disputeService,
            DisputeCaseRepository disputeCaseRepository) {
        this.disputeService = disputeService;
        this.disputeCaseRepository = disputeCaseRepository;
    }

    @Operation(
        summary = "Raise a new dispute against CDC data",
        description = """
            Creates a dispute when CDC data does not match institution records.
            Initial status is always OPEN.
            expiryDate is automatically set to today + 72 months (LRSIC).

            **Save the id from the response** — needed for PUT status and GET dispute.

            **Example request:**
```json
            {
              "submissionRecordId": 1,
              "disputeDetails": "CDC balance shows 14714 but loan was fully repaid"
            }
```

            **Roles:** ALL roles can raise disputes (KYC_OFFICER, CREDIT_ANALYST, COMPLIANCE)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created — dispute raised, status = OPEN"),
        @ApiResponse(responseCode = "400", description = "Bad Request — submissionRecordId does not exist"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')")
    public ResponseEntity<DisputeResponse> createDispute(
            @RequestBody CreateDisputeRequest request) {
        String raisedBy = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        log.info("Create dispute - submissionRecordId: {}, raisedBy: {}",
                request.submissionRecordId(), raisedBy);
        DisputeCase dispute = disputeService.createDispute(
                request.submissionRecordId(),
                request.disputeDetails(),
                raisedBy);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DisputeResponse.from(dispute));
    }

    @Operation(
        summary = "Advance dispute through state machine",
        description = """
            Moves a dispute forward through the state machine.

            **Valid transitions only:**
            - OPEN -> UNDER_REVIEW
            - UNDER_REVIEW -> RESOLVED

            Cannot go backward. Cannot skip states.
            Invalid transition returns 400 Bad Request.

            **Move to UNDER_REVIEW:**
```json
            { "newStatus": "UNDER_REVIEW", "resolutionNotes": "Investigating with CDC" }
```

            **Resolve:**
```json
            { "newStatus": "RESOLVED", "resolutionNotes": "Confirmed — corrected per LRSIC Article 23" }
```

            **Roles:** OPEN -> UNDER_REVIEW: all roles. UNDER_REVIEW -> RESOLVED: COMPLIANCE only.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — dispute status updated"),
        @ApiResponse(responseCode = "400", description = "Bad Request — invalid state transition or dispute not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('COMPLIANCE') or " +
            "(hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST') and " +
            "(#request.newStatus == null or !#request.newStatus.toUpperCase().equals('RESOLVED')))")
    public ResponseEntity<DisputeResponse> updateStatus(
            @Parameter(description = "Dispute ID from POST /api/disputes", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        log.info("Update dispute status - id: {}, newStatus: {}", id, request.newStatus());
        DisputeCase updated = disputeService.updateStatus(
                id, request.newStatus(), request.resolutionNotes());
        return ResponseEntity.ok(DisputeResponse.from(updated));
    }

    @Operation(
        summary = "View a dispute by ID",
        description = """
            Returns the full dispute record.
            institutionDataSummary and cdcDataSummary are truncated to 200 chars.

            **Roles:** OPEN -> UNDER_REVIEW: all roles. UNDER_REVIEW -> RESOLVED: COMPLIANCE only.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — full dispute record returned"),
        @ApiResponse(responseCode = "400", description = "Bad Request — dispute not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('KYC_OFFICER', 'CREDIT_ANALYST', 'COMPLIANCE')")
    public ResponseEntity<DisputeResponse> getDispute(
            @Parameter(description = "Dispute ID from POST /api/disputes", example = "1", required = true)
            @PathVariable Long id) {
        log.info("Get dispute - id: {}", id);
        DisputeCase dispute = disputeCaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dispute not found: " + id));
        return ResponseEntity.ok(DisputeResponse.from(dispute));
    }

    public record CreateDisputeRequest(
            Long submissionRecordId,
            String disputeDetails) {}

    public record UpdateStatusRequest(
            String newStatus,
            String resolutionNotes) {}
}
