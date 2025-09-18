package org.mifos.creditbureau.api;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
    public ResponseEntity<String> callSecurityTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return ResponseEntity.ok(securityTestService.testSecurityEndpoint(creditBureauId).getBody());
    }

    @POST
    @Path("/rcc-test/{creditBureauId}")
    public ResponseEntity<String> callRCCTest(@PathParam("creditBureauId") Long creditBureauId) throws Exception{
        return ResponseEntity.ok(consolidatedCreditReportService.testRCCSandboxEndpoint(creditBureauId).getBody());
    }


}
