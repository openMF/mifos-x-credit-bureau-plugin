package org.mifos.creditbureau.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.ConsolidatedCreditReportService;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.SecurityTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
     * calls CDC production endpoint, maps response.
     *
     * Phase 2 — replaces mock mode in CB-ILD CdcScorePullServiceImpl.
     */
    @POST
    @Path("/rcc/{clientId}")
    public Response callRCC(
            @PathParam("clientId") Long clientId,
            @jakarta.ws.rs.QueryParam("creditBureauId") Long creditBureauId)
            throws Exception {
        return Response.ok(
                consolidatedCreditReportService
                        .getConsolidatedCreditReport(
                                creditBureauId, clientId))
                .build();
    }

    @POST
    @Path("/security-test/{creditBureauId}")
    public Response callSecurityTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return Response.ok(securityTestService.testSecurityEndpoint(creditBureauId)).build();
    }
    /*Sandbox connection*/
    @POST
    @Path("/rcc-test/{creditBureauId}")
    public Response callRCCTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return Response.ok(consolidatedCreditReportService.testRCCSandboxEndpoint(creditBureauId).getBody()).build();
    }


}
