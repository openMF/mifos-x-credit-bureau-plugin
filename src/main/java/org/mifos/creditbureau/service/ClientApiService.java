package org.mifos.creditbureau.service;

import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.models.FineractClientAddressResponse;
import org.mifos.creditbureau.data.models.FineractClientResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//call this service, and use the populated dtos to construct a dto for a credit bureau
@Service
public class ClientApiService{

    @Autowired
    private RestTemplate restTemplate;

    String url;

    public ClientApiService(){
        restTemplate = new RestTemplate();
        url = "https://sandbox.mifos.community/";
    }

    public ClientData getClientData(Long clientId){
        String clientUrl  = url + "fineract-provider/api/v1/clients/" + clientId;
        FineractClientResponse apiResponse = restTemplate.getForObject(clientUrl, FineractClientResponse.class);

        String addressUrl = url + "fineract-provider/api/v1/clients/" + clientId + "/address";
        FineractClientAddressResponse addressResponse = restTemplate.getForObject(addressUrl, FineractClientAddressResponse.class);

        List<String> streetAddress = Stream.of(
                        addressResponse.getAddressLine1(),
                        addressResponse.getAddressLine2(),
                        addressResponse.getAddressLine3()
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());


        return ClientData.builder()
                .id(apiResponse.getId())
                .firstName(apiResponse.getFirstName())
                .lastName(apiResponse.getLastName())
                .externalId(apiResponse.getExternalId())
                .dateOfBirth(apiResponse.getDateOfBirth())
                .phoneNumber(apiResponse.getMobileNo())
                .emailAddress(apiResponse.getEmailAddress())
                .nationality(addressResponse.getCountryName())
                .streetAddress(streetAddress)
                .neighborhood(addressResponse.getTownCity())
                .stateProvince(addressResponse.getStateProvince())
                .postalCode(addressResponse.getPostalCode())
                .build();

    }

}
