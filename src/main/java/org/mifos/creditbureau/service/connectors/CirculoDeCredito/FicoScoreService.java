package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.SignatureService;
import org.springframework.web.client.RestTemplate;

@Service
public class FicoScoreService {

    private String CirculoDeCreditoBaseUrl;

    private final SignatureService signatureService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FicoScoreService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }



}
