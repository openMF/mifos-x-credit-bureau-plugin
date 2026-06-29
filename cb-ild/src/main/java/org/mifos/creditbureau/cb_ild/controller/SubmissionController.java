package org.mifos.creditbureau.cb_ild.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.aop.Auditable;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.mifos.creditbureau.cb_ild.service.submission.BatchSubmissionAck;
import org.mifos.creditbureau.cb_ild.service.submission.ISubmissionService;
import org.mifos.creditbureau.cb_ild.service.submission.SubmissionRecordResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@Tag(
    name = "2. Submission Pipeline",
    description = "CDC submission pipeline — Triggers 2 and 3, batch runs, history, and retry queue."
)
public class SubmissionController {

    private final ISubmissionService submissionService;
    private final SubmissionRecordRepository submissionRecordRepository;

    public SubmissionController(
            ISubmissionService submissionService,
            SubmissionRecordRepository submissionRecordRepository) {
        this.submissionService = submissionService;
        this.submissionRecordRepository = submissionRecordRepository;
    }

    @Operation(
        summary = "Start a batch CDC submission run",
        description = """
            Starts a batch submission. Returns 202 immediately — runs in background via @Async.
            Check GET /api/submissions/history afterward for per-client results.

            **Request body:**
            - `{"clientIds": [5]}` — submit specific client(s)
            - `{"clientIds": []}` or omit body — submit ALL active Fineract clients

            **triggerType saved:** MANUAL_BATCH

            **Roles:** CREDIT_ANALYST, COMPLIANCE (KYC_OFFICER gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Accepted — batch started in background. Check /history for results."),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — KYC_OFFICER cannot call this")
    })
    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')")
    @Auditable(action = "SUBMISSION_BATCH_START", entityType = "SubmissionRecord")
    public ResponseEntity<BatchSubmissionAck> runSubmissions(
            @RequestBody(required = false) RunSubmissionsRequest request) {
        List<Long> clientIds = (request == null) ? null : request.clientIds();
        log.info("Batch submission run requested — explicit clientIds: {}",
                (clientIds == null) ? 0 : clientIds.size());
        submissionService.runBatch(clientIds);
        BatchSubmissionAck ack = (clientIds == null || clientIds.isEmpty())
                ? new BatchSubmissionAck("Batch submission started for all active clients", 0)
                : new BatchSubmissionAck("Batch submission started", clientIds.size());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ack);
    }

    @Operation(
        summary = "Paginated submission history for a client",
        description = """
            All CDC submissions for a client, newest first.

            **Key response fields:**
            - `status` — ACCEPTED / REJECTED / PENDING_RETRY / PERMANENTLY_FAILED
            - `triggerType` — LOAN_APPROVAL / SCREENING_EVENT / MANUAL_BATCH
            - `cdcReferenceId` — CDC's reference ID (e.g. 386636538)
            - `inquiryType` — HARD or SOFT (only for SCREENING_EVENT)
            - `expiryDate` — LRSIC 72-month retention date

            **Roles:** CREDIT_ANALYST, COMPLIANCE (KYC_OFFICER gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — HATEOAS paginated submission records"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — KYC_OFFICER cannot call this")
    })
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')")
    public ResponseEntity<PagedModel<SubmissionRecordResponse>> getSubmissionHistory(
            @Parameter(description = "Fineract client ID", example = "5", required = true)
            @RequestParam Long clientId,
            @Parameter(description = "Page number (zero-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") int size,
            PagedResourcesAssembler<SubmissionRecordResponse> assembler) {
        log.info("Submission history request — clientId: {}, page: {}, size: {}",
                clientId, page, size);
        Page<SubmissionRecord> records = submissionRecordRepository
                .findByClientIdOrderBySubmittedAtDesc(clientId, PageRequest.of(page, size));
        Page<SubmissionRecordResponse> response = records.map(SubmissionRecordResponse::from);
        @SuppressWarnings("unchecked")
        var pagedModel = (PagedModel<SubmissionRecordResponse>)(Object) assembler.toModel(response);
        return ResponseEntity.ok(pagedModel);
    }

    @Operation(
        summary = "View retry queue — all PENDING_RETRY submissions",
        description = """
            All submissions currently waiting to be retried.
            The SubmissionRetryScheduler retries these automatically every 6 hours
            with exponential backoff (max 3 attempts then PERMANENTLY_FAILED).

            **Empty list = healthy system.**
            Non-empty = CDC failures waiting for retry — Compliance should monitor this.

            **Roles:** CREDIT_ANALYST, COMPLIANCE (KYC_OFFICER gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK — list of PENDING_RETRY records (empty if system healthy)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — KYC_OFFICER cannot call this")
    })
    @GetMapping("/schedule")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')")
    public ResponseEntity<List<SubmissionRecordResponse>> getRetrySchedule() {
        log.info("Retry schedule requested");
        List<SubmissionRecordResponse> schedule = submissionRecordRepository
                .findByStatus(SubmissionStatus.PENDING_RETRY)
                .stream()
                .map(SubmissionRecordResponse::from)
                .toList();
        return ResponseEntity.ok(schedule);
    }

    @Operation(
        summary = "Trigger 3 — Log a CDC screening event (LRSIC inquiry logging)",
        description = """
            Logs a credit inquiry event to CDC as required by LRSIC.
            Every credit check must be reported with inquiry type.

            **inquiryType must be exactly HARD or SOFT.**
            Any other value returns 400 Bad Request.

            **HARD** — formal credit check before loan approval.
            **SOFT** — informal pre-qualification check.

            **triggerType saved:** SCREENING_EVENT

            **Roles:** KYC_OFFICER only.
            CREDIT_ANALYST and COMPLIANCE get 403 — separation of duties.
            They can AUDIT screening events via audit-trail but cannot CREATE them.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created — screening event logged to CDC"),
        @ApiResponse(responseCode = "400", description = "Bad Request — inquiryType must be HARD or SOFT"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — only KYC_OFFICER can call this")
    })
    @PostMapping("/report-screening")
    @PreAuthorize("hasRole('KYC_OFFICER')")
    @Auditable(action = "SUBMISSION_SCREENING", entityType = "SubmissionRecord")
    public ResponseEntity<SubmissionRecordResponse> reportScreening(
            @RequestBody ReportScreeningRequest request) {
        log.info("Report screening — clientId: {}, inquiryType: {}",
                request.clientId(), request.inquiryType());
        if (!"HARD".equals(request.inquiryType()) && !"SOFT".equals(request.inquiryType())) {
            throw new IllegalArgumentException(
                    "inquiryType must be HARD or SOFT, got: " + request.inquiryType());
        }
        var record = submissionService.submitClient(
                request.clientId(),
                TriggerType.SCREENING_EVENT,
                request.loanId(),
                request.inquiryType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SubmissionRecordResponse.from(record));
    }

    @Operation(
        summary = "Trigger 2 — Report loan approval to CDC",
        description = """
            Fires when a loan officer approves a loan in Mifos.
            Reports the approval to CDC in the background.

            **CRITICAL:** This endpoint NEVER blocks loan approval.
            If CDC is down, returns 200 so the loan officer can proceed.
            The failed submission is saved as PENDING_RETRY and retried automatically.

            **201** = CDC accepted.
            **200** = CDC failed but loan approval NOT blocked.

            **triggerType saved:** LOAN_APPROVAL

            **Roles:** CREDIT_ANALYST, COMPLIANCE (KYC_OFFICER gets 403)
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created — CDC accepted the loan approval report"),
        @ApiResponse(responseCode = "200", description = "OK — CDC failed but loan approval is NOT blocked"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden — KYC_OFFICER cannot call this")
    })
    @PostMapping("/report-approval")
    @PreAuthorize("hasAnyRole('CREDIT_ANALYST', 'COMPLIANCE')")
    @Auditable(action = "SUBMISSION_LOAN_APPROVAL", entityType = "SubmissionRecord")
    public ResponseEntity<SubmissionRecordResponse> reportApproval(
            @RequestBody ReportApprovalRequest request) {
        log.info("Report approval — clientId: {}, loanId: {}",
                request.clientId(), request.loanId());
        try {
            var record = submissionService.submitClient(
                    request.clientId(),
                    TriggerType.LOAN_APPROVAL,
                    request.loanId(),
                    null);
            int status = "ACCEPTED".equals(record.getStatus().name())
                    ? HttpStatus.CREATED.value()
                    : HttpStatus.OK.value();
            return ResponseEntity.status(status)
                    .body(SubmissionRecordResponse.from(record));
        } catch (Exception e) {
            log.error("CDC submission failed for loan approval — clientId: {}, loanId: {}. " +
                    "Loan approval NOT blocked. Error: {}",
                    request.clientId(), request.loanId(), e.getMessage());
            return ResponseEntity.ok().build();
        }
    }

    public record RunSubmissionsRequest(List<Long> clientIds) {}
    public record ReportScreeningRequest(Long clientId, Long loanId, String inquiryType) {}
    public record ReportApprovalRequest(Long clientId, Long loanId) {}
}
