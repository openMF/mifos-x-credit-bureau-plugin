package org.mifos.creditbureau.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.ConsolidatedCreditReportService;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.SecurityTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * REST resource for Circulo de Credito credit bureau operations.
 *
 * Endpoints:
 *   POST /circulo-de-credito/rcc/{clientId}       — real CDC pull (CB-ILD Phase 2)
 *   POST /circulo-de-credito/security-test/{id}   — ECDSA security test
 *   POST /circulo-de-credito/rcc-test/{id}        — CDC sandbox test
 */
@Path("/circulo-de-credito")
@Component
public class CirculoDeCreditoApiResource {

    @Autowired
    private SecurityTestService securityTestService;

    @Autowired
    private ConsolidatedCreditReportService consolidatedCreditReportService;

    /**
     * POST /circulo-de-credito/rcc/{clientId}?creditBureauId={id}
     *
     * CB-ILD calls this endpoint for real CDC credit report pulls.
     * Fetches Fineract client data, builds CDC request with ECDSA signing,
     * calls CDC production endpoint, maps and returns response.
     *
     * Phase 2 — replaces mock mode in CB-ILD CdcScorePullServiceImpl.
     *
     * @param clientId       Fineract client ID — must not be null
     * @param creditBureauId credit bureau registration ID — must not be null
     * @return HTTP 200 with CBCreditReportData, or error response
     */
    @POST
    @Path("/rcc/{clientId}")
    public Response callRCC(
            @PathParam("clientId") Long clientId,
            @jakarta.ws.rs.QueryParam("creditBureauId") Long creditBureauId) {

        if (clientId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("clientId must not be null")
                    .build();
        }
        if (creditBureauId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("creditBureauId must not be null")
                    .build();
        }

        try {
            return Response.ok(
                    consolidatedCreditReportService
                            .getConsolidatedCreditReport(
                                    creditBureauId, clientId))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        } catch (RuntimeException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Failed to retrieve credit report: "
                            + e.getMessage())
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Unexpected error retrieving credit report")
                    .build();
        }
    }

    @POST
    @Path("/security-test/{creditBureauId}")
    public Response callSecurityTest(
            @PathParam("creditBureauId") Long creditBureauId) {
        try {
            return Response.ok(
                    securityTestService.testSecurityEndpoint(creditBureauId))
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Security test failed").build();
        }
    }

    /*Sandbox connection*/
    @POST
    @Path("/rcc-test/{creditBureauId}")
    public Response callRCCTest(
            @PathParam("creditBureauId") Long creditBureauId) {
        try {
            return Response.ok(
                    consolidatedCreditReportService
                            .testRCCSandboxEndpoint(creditBureauId).getBody())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("RCC test failed").build();
        }
    }
}
