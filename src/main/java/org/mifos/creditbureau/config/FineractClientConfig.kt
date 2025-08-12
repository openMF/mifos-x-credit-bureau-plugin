import org.mifos.fineract.client.apis.ClientApi
import org.mifos.fineract.client.apis.ClientsAddressApi
import org.mifos.fineract.client.infrastructure.FineractClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FineractClientConfig {

    private val baseUrl = "https://sandbox.mifos.community/fineract-provider/api/v1"
    private val tenant = "default"
    private val username = "mifos"
    private val password = "password"

    @Bean
    fun fineractClient(): FineractClient {
        return FineractClient.builder()
            .baseURL(baseUrl)
            .basicAuth(username, password)
            .tenant(tenant)
            .build()
    }

    @Bean
    fun clientApi(fineractClient: FineractClient): ClientApi {
        return fineractClient.clientApi()
    }

    @Bean
    fun clientsAddressApi(fineractClient: FineractClient): ClientsAddressApi {
        return fineractClient.clientsAddressApi()
    }
}
