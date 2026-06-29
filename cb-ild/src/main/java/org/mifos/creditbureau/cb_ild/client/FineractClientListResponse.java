package org.mifos.creditbureau.cb_ild.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Maps the response from Fineract GET /clients?limit=...&offset=...&status=active.
 *
 * Confirmed live against mifos-bank-1 (June 2026):
 *   GET /clients?limit=5
 *     -> {"totalFilteredRecords":16,"pageItems":[{"id":5,...},{"id":4,...},...]}
 *   GET /clients?limit=20&status=active
 *     -> {"totalFilteredRecords":7,"pageItems":[{"id":1,...},...]}
 *
 * Only id and status are read here — used by getAllActiveClientIds() to
 * build a List<Long> for SubmissionServiceImpl.runBatch(). Everything else
 * in pageItems (name, address, timeline, etc.) is irrelevant for this call;
 * full client data is fetched per-clientId by getClientData() when
 * submitClient() actually processes each ID.
 *
 * @JsonIgnoreProperties(ignoreUnknown = true) — pageItems contains many
 * fields (accountNo, displayName, officeName, timeline, legalForm, ...)
 * that are intentionally not mapped here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FineractClientListResponse(
        int totalFilteredRecords,
        List<FineractClientListItem> pageItems
) {

    /**
     * A single client entry within pageItems.
     *
     * status.value is read to allow a defensive client-side filter even
     * when the request already specified status=active — Fineract's
     * status filter is trusted as the primary mechanism, this is a
     * belt-and-suspenders check in getAllActiveClientIds().
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FineractClientListItem(
            long id,
            FineractClientStatus status,
            boolean active
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FineractClientStatus(
            String code,
            String value
    ) {}
}
