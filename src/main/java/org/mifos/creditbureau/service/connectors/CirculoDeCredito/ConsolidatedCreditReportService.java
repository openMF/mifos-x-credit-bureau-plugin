package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import org.springframework.http.MediaType;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadImplService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRCCRequest;

import java.util.Map;

@Service
public class ConsolidatedCreditReportService {
//https://services.circulodecredito.com.mx/sandbox/v1/rcc
    @Value("${mifos.circulodecredito.base.url}")
    private String baseUrl;
    private final CreditBureauRegistrationReadImplService creditBureauRegistrationReadService;
    static RestTemplate restTemplate = new RestTemplate();

    public ConsolidatedCreditReportService(CreditBureauRegistrationReadImplService creditBureauRegistrationReadService) {
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
    }
    //TODO: production implementation
    //1. fetch ClientApiService
    //2. construct request body
    //3. fetch signage service
    //4. construct request header and signage
    //5. send request to cdc

    //TODO: what happens when any of the strings are empty
    public ResponseEntity<String> testRCCSandboxEndpoint(Long creditBureauId)throws Exception{
        //url
        String url = baseUrl + "sandbox/v1/rcc";

        //request body
        CirculoDeCreditoRCCRequest request = CirculoDeCreditoRCCRequest.builder()
                .primerNombre("JUAN")
                .apellidoPaterno("PRUEBA")
                .apellidoMaterno("CUATRO")
                .fechaNacimiento("1980-01-04")
                .rfc("PUAC800107")
                .domicilio(CirculoDeCreditoRCCRequest.Domicilio.builder()
                        .direccion("INSURGENTES SUR 1007")
                        .colonia("INSURGENTES SUR")
                        .municipio("MEXICO CITY")
                        .ciudad("MEXICO CITY")
                        .estado("DF")
                        .codigoPostal("11230")
                        .build())
                .build();

        //headers

        Map<String, String> keys = creditBureauRegistrationReadService.getRegistrationParamMap(creditBureauId); //need to know th id
        String apiKey = keys.get("x-api-key"); //assumes read service already decrypts
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        //entity
        HttpEntity<CirculoDeCreditoRCCRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        // Log it
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());

        return response;
    }


}