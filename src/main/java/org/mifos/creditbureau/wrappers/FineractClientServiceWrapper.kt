package org.mifos.creditbureau.wrappers

import kotlinx.coroutines.runBlocking
import org.mifos.fineract.client.infrastructure.FineractClient
import org.mifos.fineract.client.apis.ClientApi
import org.mifos.fineract.client.apis.ClientsAddressApi
import org.mifos.fineract.client.models.GetClientsClientIdResponse
import org.mifos.fineract.client.models.GetClientClientIdAddressesResponse
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service

@Service
class FineractClientServiceWrapper(
    private val fineractClient: FineractClient,
    private val clientApi: ClientApi,
    private val clientsAddressApi: ClientsAddressApi
){
    @Bean
    fun getClient(clientId: Long) : GetClientsClientIdResponse {
        return runBlocking{
            clientApi.retrieveOne11(clientId)
        }
    }

    fun getClientAddress(clientId: Long) : List<GetClientClientIdAddressesResponse>{
        return runBlocking{
            clientsAddressApi.getAddresses1(clientId)
        }
    }

}