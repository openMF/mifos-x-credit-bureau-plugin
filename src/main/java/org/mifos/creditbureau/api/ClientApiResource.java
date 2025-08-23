package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRequest;
import org.mifos.creditbureau.mappers.ClientDataToCirculoDeCreditoRequestMapper;
import org.mifos.creditbureau.service.ClientApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Path("/client")
@Component
public class ClientApiResource {

    @Autowired
    private ClientApiService clientApiService;

    @Autowired
    private ClientDataToCirculoDeCreditoRequestMapper mapper;

    @GET
    @Path("/{clientId}")
    public ResponseEntity<ClientData> getClientData(@PathParam("clientId") Long clientId){
        ClientData clientData = clientApiService.getClientData(clientId);
        return ResponseEntity.ok(clientData);
    }

    @GET
    @Path("/{clientId}/cdc-request")
    public ResponseEntity<CirculoDeCreditoRequest> getCDCRequest(@PathParam("clientId") Long clientId){
        ClientData clientData = clientApiService.getClientData(clientId);
        CirculoDeCreditoRequest circuloDeCreditoRequest = mapper.toCirculoDeCreditoRequest(clientData);
        return ResponseEntity.ok(circuloDeCreditoRequest);
    }


}
