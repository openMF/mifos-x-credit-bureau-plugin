package org.mifos.creditbureau.service;

import org.mifos.creditbureau.data.ClientData;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.mifos.fineract.client.infrastructure.FineractClient;

@Service
public class ClientApiService {

    private final FineractClient fineractClient;

    public ClientApiService() {
        this.fineractClient = new FineractClient.Builder()
                .baseURL("https://sandbox.mifos.community/fineract-provider/api/v1")
                .basicAuth("your-username", "your-password")
                .tenant("your-tenant")
                .build();

    }

    public ClientData buildClientData(Long clientId){
        //TODO: figure out how to use retrieveOne11
        //var clientData = fineractClient.getClients().retrieveOne11(clientId, null );
        return null;
    }


}
