package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    static RestTemplate restTemplate = new RestTemplate();

    public ConsolidatedCreditReportService() {

    }

    public ResponseEntity<String> testRCCSandboxEndpoint()throws Exception{
        //url
        String url = baseUrl + "/v1/rcc";

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
        HttpHeaders headers = new HttpHeaders();
        //get x-api-key from database
        Map<String, String> keys = creditBureauRegistrationReadService.getRegistrationParamMap(creditBureauId); //need to know th id
        headers.set("x-api-key", "your_api_key_here");

        ResponseEntity<String> response = restTemplate.postForEntity(url, circuloDeCreditoRCCRequest, String.class);

        // Log it
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());

        return response;
    }


}