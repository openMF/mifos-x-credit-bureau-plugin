package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.service.ClientApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Path("/client")
@Component
public class ClientApiResource {

    @Autowired
    private ClientApiService clientApiService;

    @GET
    @Path("/{clientId}")
    public ResponseEntity<ClientData> getClientData(@PathParam("clientId") Long clientId){
        ClientData clientData = clientApiService.getClientData(clientId);
        return ResponseEntity.ok(clientData);
    }
}
