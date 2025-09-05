package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mifos.creditbureau.connector.circulodecredito.ApiClient;
import org.mifos.creditbureau.connector.circulodecredito.ApiException;
import org.mifos.creditbureau.connector.circulodecredito.api.ScoreDatosGeneralesApi;
import org.mifos.creditbureau.connector.circulodecredito.model.RequestScoreDG;
import org.mifos.creditbureau.connector.circulodecredito.model.ResponseScore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FicoScoreService {

    @Value("${mifos.circulodecredito.base.url}")
    private String circuloDeCreditoBaseUrl;

    private final SignatureService signatureService;

    public FicoScoreService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public ResponseScore getFicoScore(Long creditBureauId, RequestScoreDG request) throws JsonProcessingException, ApiException {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath(circuloDeCreditoBaseUrl);

        String requestBody = apiClient.getObjectMapper().writeValueAsString(request);
        Map<String, String> headers = signatureService.buildHeaders(creditBureauId, requestBody);

        ScoreDatosGeneralesApi api = new ScoreDatosGeneralesApi(apiClient);
        
        return api.getScoreDG(headers.get("x-signature"), headers.get("x-api-key"), headers.get("username"), headers.get("password"), request);
    }
}