package org.mifos.creditbureau.service.CirculoDeCredito;

import com.sun.research.ws.wadl.HTTPMethods;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class SecurityTestService {

    @Value("${mifos.circulodecredito.base.url}")
    private String CirculoDeCreditoBaseUrl;

    private final SignatureService signatureService;

    static RestTemplate restTemplate = new RestTemplate();

    public SecurityTestService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public ResponseEntity<String> testSecurityEndpoint(Long creditBureauId)throws Exception{
        String url = CirculoDeCreditoBaseUrl + "/v1/securitytest";
        String requestBody = "{\"attribute\":\"Hello World!\"}";

        Map<String, String> headersMap = signatureService.buildHeaders(creditBureauId, requestBody);

        HttpHeaders headers = new HttpHeaders();
        headersMap.forEach(headers::add);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        // Log it
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());

        return response;
    }
}
