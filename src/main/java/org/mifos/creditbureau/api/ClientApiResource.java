package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRequest;
import org.mifos.creditbureau.mappers.ClientDataToCirculoDeCreditoRequestMapper;
import org.mifos.creditbureau.service.ClientApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Path("/client")
@Component
@Produces(MediaType.APPLICATION_JSON)
public class ClientApiResource {

    @Autowired
    private ClientApiService clientApiService;

    @Autowired
    private ClientDataToCirculoDeCreditoRequestMapper mapper;

    @GET
    @Path("/{clientId}")
    public Response getClientData(@PathParam("clientId") Long clientId){
        ClientData clientData = clientApiService.getClientData(clientId);
        return Response.ok(clientData).build();
    }

    @GET
    @Path("/{clientId}/cdc-request")
    public Response getCDCRequest(@PathParam("clientId") Long clientId){
        ClientData clientData = clientApiService.getClientData(clientId);
        CirculoDeCreditoRequest circuloDeCreditoRequest = mapper.toCirculoDeCreditoRequest(clientData);
        return Response.ok(circuloDeCreditoRequest).build();
    }


}
