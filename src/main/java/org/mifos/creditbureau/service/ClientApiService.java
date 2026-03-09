package org.mifos.creditbureau.service;

import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.models.FineractClientAddressResponse;
import org.mifos.creditbureau.data.models.FineractClientResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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
        ResponseEntity<FineractClientResponse> apiResponseEntity;
        try {
            apiResponseEntity = restTemplate.exchange(clientUrl, HttpMethod.GET, entity, FineractClientResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch client from Fineract for clientId=" + clientId, e);
        }
        FineractClientResponse apiResponse = apiResponseEntity.getBody();
        if (apiResponse == null) {
            throw new IllegalStateException("Fineract returned an empty client payload for clientId=" + clientId);
        }

        String addressUrl = fineractApiClientAddressBaseUrl + clientId + "/addresses";
        ResponseEntity<FineractClientAddressResponse[]> addressResponseEntity;
        try {
            addressResponseEntity = restTemplate.exchange(addressUrl, HttpMethod.GET, entity, FineractClientAddressResponse[].class);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to fetch client addresses from Fineract for clientId=" + clientId, e);
        }
        FineractClientAddressResponse[] addressResponse = addressResponseEntity.getBody();

        FineractClientAddressResponse firstAddressResponse = addressResponse != null && addressResponse.length > 0 //what if i need multiple addresses
                ? addressResponse[0]
                : null;

        List<String> streetAddress = Stream.of(
                        firstAddressResponse != null ? firstAddressResponse.getAddressLine1() : null,
                        firstAddressResponse != null ? firstAddressResponse.getAddressLine2() : null,
                        firstAddressResponse != null ? firstAddressResponse.getAddressLine3() : null
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
                .nationality(firstAddressResponse != null ? firstAddressResponse.getCountryName() : null)

                .addressType(firstAddressResponse != null ? firstAddressResponse.getAddressType() : null)
                .addressId(firstAddressResponse != null ? firstAddressResponse.getId() : null)
                .streetAddress(streetAddress)
                .townVillage(firstAddressResponse != null ? firstAddressResponse.getTownVillage() : null)
                .city(firstAddressResponse != null ? firstAddressResponse.getCity() : null)
                .stateProvince(firstAddressResponse != null ? firstAddressResponse.getStateName() : null)
                .country(firstAddressResponse != null ? firstAddressResponse.getCountryName() : null)
                .postalCode(firstAddressResponse != null ? firstAddressResponse.getPostalCode() : null)

                .build();

    }
    private String getBasicAuthenticationHeader(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }

}
