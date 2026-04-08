package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;
import org.mifos.creditbureau.service.ClientApiService;
import org.mifos.creditbureau.service.connectors.ConnectorRegistry;
import org.mifos.creditbureau.service.connectors.CreditBureauConnector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Bureau-agnostic credit report endpoint.
 * <p>
 * Uses the {@link ConnectorRegistry} to resolve the correct
 * {@link CreditBureauConnector} at runtime based on the credit bureau's
 * registered type. Supports any bureau with a registered connector —
 * no code changes needed to add new bureaus.
 * <p>
 * Security: Inherits Spring Security basic auth enforcement via
 * {@code SecurityConfig} (all {@code /api/**} paths require authentication).
 */
@Path("/credit-reports")
@Component
@Produces(MediaType.APPLICATION_JSON)
public class CreditReportApiResource {

    private final ConnectorRegistry connectorRegistry;
    private final ClientApiService clientApiService;

    @Autowired
    public CreditReportApiResource(ConnectorRegistry connectorRegistry,
                                   ClientApiService clientApiService) {
        this.connectorRegistry = connectorRegistry;
        this.clientApiService = clientApiService;
    }

    /**
     * Fetch a credit report for a client from a specific credit bureau.
     * The correct connector is resolved automatically based on the bureau type.
     *
     * @param creditBureauId the registered credit bureau ID
     * @param clientId       the Fineract client ID
     * @return the generalized credit report result
     */
    @POST
    @Path("/{creditBureauId}/client/{clientId}")
    public Response fetchCreditReport(
            @PathParam("creditBureauId") Long creditBureauId,
            @PathParam("clientId") Long clientId) {

        ClientData clientData = clientApiService.getClientData(clientId);
        CreditBureauConnector connector = connectorRegistry.getConnector(creditBureauId);
        CreditReportResult result = connector.fetchCreditReport(creditBureauId, clientData);

        return Response.ok(result).build();
    }

    /**
     * Test connectivity with a registered credit bureau.
     *
     * @param creditBureauId the registered credit bureau ID
     * @return the connection test result
     */
    @POST
    @Path("/{creditBureauId}/test-connection")
    public Response testConnection(
            @PathParam("creditBureauId") Long creditBureauId) {

        CreditBureauConnector connector = connectorRegistry.getConnector(creditBureauId);
        ConnectionTestResult result = connector.testConnection(creditBureauId);

        return Response.ok(result).build();
    }

    /**
     * List all bureau types that have a registered connector.
     *
     * @return set of supported bureau type keys
     */
    @GET
    @Path("/supported-bureaus")
    public Response getSupportedBureaus() {
        Set<String> types = connectorRegistry.getSupportedTypes();
        return Response.ok(types).build();
    }
}
