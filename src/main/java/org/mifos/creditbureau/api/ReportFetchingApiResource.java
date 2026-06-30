package org.mifos.creditbureau.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.fetching.AutoReportFetchResult;
import org.mifos.creditbureau.data.fetching.FetchedReportData;
import org.mifos.creditbureau.service.fetching.AutoReportFetchingService;
import org.springframework.stereotype.Component;

/**
 * REST API Resource for auto-fetching credit bureau reports.
 * Provides endpoints for triggering automatic report fetches and retrieving cached reports.
 */
@Path("/client")
@Component
@AllArgsConstructor
@Slf4j
public class ReportFetchingApiResource {

    private final AutoReportFetchingService autoReportFetchingService;

    /**
     * Auto-fetch a credit report for a specific client.
     * Automatically detects credit bureau, validates configuration,
     * and fetches the report without manual intervention.
     *
     * Example: POST /client/123/auto-fetch-report
     *
     * @param clientId the ID of the client
     * @return Response with AutoReportFetchResult containing fetch status and report data
     */
    @POST
    @Path("/{clientId}/auto-fetch-report")
    public Response autoFetchReportForClient(@PathParam("clientId") Long clientId) {
        log.info("API Request: Auto-fetch report for client: {}", clientId);

        try {
            // Check if fetch is already in progress
            if (autoReportFetchingService.isFetchInProgress(clientId)) {
                log.warn("Fetch operation already in progress for client: {}", clientId);
                return Response.status(Response.Status.CONFLICT)
                        .entity("Fetch operation already in progress for this client")
                        .build();
            }

            AutoReportFetchResult result = autoReportFetchingService.autoFetchReportForClient(clientId);

            if (result.isSuccess()) {
                log.info("Report fetch completed successfully for client: {}", clientId);
                return Response.ok()
                        .entity(result)
                        .build();
            } else {
                // Return 4xx or 5xx based on error type
                Response.Status status = determineStatusFromFetchFailure(result);
                log.warn("Report fetch failed for client: {} with status: {}", clientId, status);
                return Response.status(status)
                        .entity(result)
                        .build();
            }

        } catch (Exception e) {
            log.error("Unexpected error during auto-fetch for client: {}", clientId, e);
            return Response.serverError()
                    .entity("Error during report fetch: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Auto-fetch a credit report from a specific credit bureau.
     * Includes retry logic with exponential backoff.
     *
     * Example: POST /client/123/auto-fetch-report/bureau/456
     *
     * @param clientId the ID of the client
     * @param creditBureauId the ID of the credit bureau to fetch from
     * @return Response with AutoReportFetchResult containing fetch status and report data
     */
    @POST
    @Path("/{clientId}/auto-fetch-report/bureau/{creditBureauId}")
    public Response autoFetchReportFromBureau(@PathParam("clientId") Long clientId,
                                             @PathParam("creditBureauId") Long creditBureauId) {
        log.info("API Request: Auto-fetch report for client: {} from bureau: {}", clientId, creditBureauId);

        try {
            if (autoReportFetchingService.isFetchInProgress(clientId)) {
                log.warn("Fetch operation already in progress for client: {}", clientId);
                return Response.status(Response.Status.CONFLICT)
                        .entity("Fetch operation already in progress for this client")
                        .build();
            }

            AutoReportFetchResult result = autoReportFetchingService
                    .autoFetchReportForClientFromBureau(clientId, creditBureauId);

            if (result.isSuccess()) {
                log.info("Report fetch completed successfully for client: {} from bureau: {}",
                        clientId, creditBureauId);
                return Response.ok()
                        .entity(result)
                        .build();
            } else {
                Response.Status status = determineStatusFromFetchFailure(result);
                log.warn("Report fetch failed for client: {} from bureau: {} with status: {}",
                        clientId, creditBureauId, status);
                return Response.status(status)
                        .entity(result)
                        .build();
            }

        } catch (Exception e) {
            log.error("Unexpected error during auto-fetch for client: {} from bureau: {}",
                    clientId, creditBureauId, e);
            return Response.serverError()
                    .entity("Error during report fetch: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieve the last fetched report for a client.
     * Returns cached report data without making a new API call.
     *
     * Example: GET /client/123/latest-report
     *
     * @param clientId the ID of the client
     * @return Response with FetchedReportData if found, or 404 if not found
     */
    @GET
    @Path("/{clientId}/latest-report")
    public Response getLatestFetchedReport(@PathParam("clientId") Long clientId) {
        log.info("API Request: Get latest fetched report for client: {}", clientId);

        try {
            FetchedReportData reportData = autoReportFetchingService.getLastFetchedReport(clientId);

            if (reportData == null) {
                log.info("No fetched report found for client: {}", clientId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No fetched report found for client: " + clientId)
                        .build();
            }

            log.info("Retrieved latest report for client: {} fetched at: {}",
                    clientId, reportData.getFetchedAt());
            return Response.ok()
                    .entity(reportData)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving latest report for client: {}", clientId, e);
            return Response.serverError()
                    .entity("Error retrieving report: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Retrieve the last fetched report from a specific credit bureau.
     * Returns cached report data without making a new API call.
     *
     * Example: GET /client/123/latest-report/bureau/456
     *
     * @param clientId the ID of the client
     * @param creditBureauId the ID of the credit bureau
     * @return Response with FetchedReportData if found, or 404 if not found
     */
    @GET
    @Path("/{clientId}/latest-report/bureau/{creditBureauId}")
    public Response getLatestReportFromBureau(@PathParam("clientId") Long clientId,
                                            @PathParam("creditBureauId") Long creditBureauId) {
        log.info("API Request: Get latest report for client: {} from bureau: {}", clientId, creditBureauId);

        try {
            FetchedReportData reportData = autoReportFetchingService
                    .getLastFetchedReportForBureau(clientId, creditBureauId);

            if (reportData == null) {
                log.info("No fetched report found for client: {} from bureau: {}", clientId, creditBureauId);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("No fetched report found for client: " + clientId + " from bureau: " + creditBureauId)
                        .build();
            }

            log.info("Retrieved latest report for client: {} from bureau: {} fetched at: {}",
                    clientId, creditBureauId, reportData.getFetchedAt());
            return Response.ok()
                    .entity(reportData)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving report for client: {} from bureau: {}", clientId, creditBureauId, e);
            return Response.serverError()
                    .entity("Error retrieving report: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Clear cached reports for a specific client.
     * Useful for forcing a fresh fetch on next request.
     *
     * Example: POST /client/123/clear-cache
     *
     * @param clientId the ID of the client
     * @return Response indicating success
     */
    @POST
    @Path("/{clientId}/clear-cache")
    public Response clearClientCache(@PathParam("clientId") Long clientId) {
        log.info("API Request: Clear cache for client: {}", clientId);

        try {
            autoReportFetchingService.clearCachedReports(clientId);
            log.info("Cache cleared successfully for client: {}", clientId);
            return Response.ok()
                    .entity("Cache cleared successfully for client: " + clientId)
                    .build();
        } catch (Exception e) {
            log.error("Error clearing cache for client: {}", clientId, e);
            return Response.serverError()
                    .entity("Error clearing cache: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Check if a report fetch is currently in progress for a client.
     *
     * Example: GET /client/123/fetch-status
     *
     * @param clientId the ID of the client
     * @return Response with boolean indicating if fetch is in progress
     */
    @GET
    @Path("/{clientId}/fetch-status")
    public Response getFetchStatus(@PathParam("clientId") Long clientId) {
        log.info("API Request: Get fetch status for client: {}", clientId);

        try {
            boolean inProgress = autoReportFetchingService.isFetchInProgress(clientId);
            log.info("Fetch status for client: {}: {}", clientId, inProgress ? "IN_PROGRESS" : "IDLE");

            return Response.ok()
                    .entity("{\"clientId\": " + clientId + ", \"fetchInProgress\": " + inProgress + "}")
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving fetch status for client: {}", clientId, e);
            return Response.serverError()
                    .entity("Error retrieving status: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Determine HTTP Response Status based on fetch failure type.
     */
    private Response.Status determineStatusFromFetchFailure(AutoReportFetchResult result) {
        if (result.getStatus() == null) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }

        switch (result.getStatus()) {
            case SELECTION_FAILED:
            case VALIDATION_FAILED:
                return Response.Status.BAD_REQUEST;

            case RETRY_EXCEEDED:
            case TEMPORARY_FAILURE:
                return Response.Status.SERVICE_UNAVAILABLE;

            case INTERRUPTED:
                return Response.Status.INTERNAL_SERVER_ERROR;

            case PERMANENT_FAILURE:
            default:
                return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
