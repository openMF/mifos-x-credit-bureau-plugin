package org.mifos.creditbureau.cb_ild.service.submission;

/**
 * DTO returned by POST /api/submissions/run.
 *
 * Java 21 record (RULE 02).
 *
 * runBatch() is @Async — the full List<SubmissionRecord> result is not
 * available synchronously when the controller returns. This ack confirms
 * the batch was accepted and started, with a count of clients that will be
 * processed. The actual per-client results (ACCEPTED/REJECTED/PENDING_RETRY)
 * are visible afterward via GET /api/submissions/history.
 *
 * clientCount reflects the resolved target list size:
 *   - if the request specified clientIds, clientCount = clientIds.size()
 *   - if the request omitted clientIds (or sent empty), clientCount =
 *     fineractApiClient.getAllActiveClientIds().size() at request time
 *     (confirmed live: 7 active clients on mifos-bank-1 as of June 2026)
 */
public record BatchSubmissionAck(
        String message,
        int clientCount
) {}
