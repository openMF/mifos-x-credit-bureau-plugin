package org.mifos.creditbureau.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.models.FineractClientAddressResponse;
import org.mifos.creditbureau.data.models.FineractClientResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//TODO: implement null checks and error handling so that code will not break when api response is null.
//TODO: implement a dynamic search, not just getting one client at a time? Do the search first, and then get the client if there is a match
@Service
public class ClientApiService{

    @Value("${mifos.fineract.api.base-url.client}")
    private String fineractApiClientBaseUrl;

    @Value("${mifos.fineract.api.base-url.address}")
    private String fineractApiClientAddressBaseUrl;

    @Value("${mifos.fineract.api.username}")
    private String username;

    @Value("${mifos.fineract.api.password}")
    private String password;

    static RestTemplate restTemplate = new RestTemplate() ;

    public ClientData getClientData(Long clientId){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",getBasicAuthenticationHeader(username, password) );
        headers.add("fineract-platform-tenantid", "default");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String clientUrl  = fineractApiClientBaseUrl + clientId;
        //FineractClientResponse apiResponse = restTemplate.getForObject(clientUrl, FineractClientResponse.class);
        ResponseEntity<FineractClientResponse> apiResponseEntity = restTemplate.exchange(clientUrl, HttpMethod.GET, entity, FineractClientResponse.class);
        FineractClientResponse apiResponse = apiResponseEntity.getBody();

        String addressUrl = fineractApiClientAddressBaseUrl + clientId + "/addresses";
        //FineractClientAddressResponse addressResponse = restTemplate.getForObject(addressUrl, FineractClientAddressResponse.class);
        ResponseEntity<FineractClientAddressResponse[]> addressResponseEntity = restTemplate.exchange(addressUrl, HttpMethod.GET, entity, FineractClientAddressResponse[].class);
        FineractClientAddressResponse[] addressResponse = addressResponseEntity.getBody();

        FineractClientAddressResponse firstAddressResponse = addressResponse != null && addressResponse.length > 0 //what if i need multiple addresses
                ? addressResponse[0]
                : null;

        List<String> streetAddress = Stream.of(
                        firstAddressResponse.getAddressLine1(),
                        firstAddressResponse.getAddressLine2(),
                        firstAddressResponse.getAddressLine3()
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());


        return ClientData.builder()
                .id(apiResponse.getId())
                .firstName(apiResponse.getFirstname())
                .lastName(apiResponse.getLastname())
                .externalId(apiResponse.getExternalId())
                .dateOfBirth(apiResponse.getDateOfBirth())
                .phoneNumber(apiResponse.getMobileNo())
                .emailAddress(apiResponse.getEmailAddress())
                .nationality(firstAddressResponse.getCountryName())

                .addressType(firstAddressResponse.getAddressType())
                .addressId(firstAddressResponse.getId())
                .streetAddress(streetAddress)
                .townVillage(firstAddressResponse.getTownVillage())
                .city(firstAddressResponse.getCity())
                .stateProvince(firstAddressResponse.getStateName())
                .country(firstAddressResponse.getCountryName())
                .postalCode(firstAddressResponse.getPostalCode())

                .build();

    }
    private String getBasicAuthenticationHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

}
