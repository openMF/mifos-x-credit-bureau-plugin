package org.mifos.creditbureau.service;

import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.models.FineractClientAddressResponse;
import org.mifos.creditbureau.data.models.FineractClientResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//call this service, and use the populated dtos to construct a dto for a credit bureau
@Service
public class ClientApiService{

    @Autowired
    private RestTemplate restTemplate;

    String url;

    public ClientApiService(){
        url = "https://sandbox.mifos.community/";
        this.restTemplate = new RestTemplate();

    }

    public ClientData getClientData(Long clientId){
        //https://sandbox.mifos.community/fineract-provider/api/v1/clients/1
        String clientUrl  = url + "fineract-provider/api/v1/clients/" + clientId;
        FineractClientResponse apiResponse = restTemplate.getForObject(clientUrl, FineractClientResponse.class);

        String addressUrl = url + "fineract-provider/api/v1/clients/" + clientId + "/address";
        FineractClientAddressResponse addressResponse = restTemplate.getForObject(addressUrl, FineractClientAddressResponse.class);

        List<String> streetAddress = Stream.of(
                        Objects.requireNonNull(addressResponse).getAddressLine1(),
                        Objects.requireNonNull(addressResponse).getAddressLine2(),
                        Objects.requireNonNull(addressResponse).getAddressLine3()
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());


        return ClientData.builder()
                .id(Objects.requireNonNull(apiResponse).getId())
                .firstName(Objects.requireNonNull(apiResponse).getFirstName())
                .lastName(Objects.requireNonNull(apiResponse).getLastName())
                .externalId(Objects.requireNonNull(apiResponse).getExternalId())
                .dateOfBirth(Objects.requireNonNull(apiResponse).getDateOfBirth())
                .phoneNumber(Objects.requireNonNull(apiResponse).getMobileNo())
                .emailAddress(Objects.requireNonNull(apiResponse).getEmailAddress())
                .nationality(addressResponse.getCountryName())
                .streetAddress(streetAddress)
                .neighborhood(Objects.requireNonNull(addressResponse).getTownCity())
                .stateProvince(Objects.requireNonNull(addressResponse).getStateProvince())
                .postalCode(Objects.requireNonNull(addressResponse).getPostalCode())
                .build();

    }

}
