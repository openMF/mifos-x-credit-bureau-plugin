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
        headers.add("Accept", "application/json");
        headers.add("Accept-Encoding", "gzip, deflate, br");
        headers.add("Connection", "keep-alive");
        headers.add("Cache-Control", "no-cache");
        headers.add("Cookie", "__cf_bm=a7WVNugXyQCLoNm7Nu5OO0wAmc2Mb43Dy4_5UcY50LQ-1755881022-1.0.1.1-n9EpXP87m5CytvJ2P2Um0aDIdBAthvYUkCXF34y9FK5QRASePmrQDkyxZqAa8L4g5KgYzWTc6o4zGKJ77EmX5kW9goafXJml1ABPAx9b3Mg; visid_incap_2077528=VsMEk2klQLWMD2dgJ6bkJ7a0WWgAAAAAQUIPAAAAAAAau0PlvkVXo550gjWeEL9");

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        // Log it
        System.out.println("Status: " + response.getStatusCode());
        System.out.println("Body: " + response.getBody());

        return response;
    }
}
