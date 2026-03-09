package org.mifos.creditbureau.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.ConsolidatedCreditReportService;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.SecurityTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Path("/circulo-de-credito")
@Component
public class CirculoDeCreditoApiResource {

    @Autowired
    private SecurityTestService securityTestService;

    @Autowired
    private ConsolidatedCreditReportService consolidatedCreditReportService;

    @POST
    @Path("/security-test/{creditBureauId}")
    public Response callSecurityTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return Response.ok(securityTestService.testSecurityEndpoint(creditBureauId)).build();
    }

    @POST
    @Path("/rcc-test/{creditBureauId}")
    public Response callRCCTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return Response.ok(consolidatedCreditReportService.testRCCSandboxEndpoint(creditBureauId).getBody()).build();
    }


}
