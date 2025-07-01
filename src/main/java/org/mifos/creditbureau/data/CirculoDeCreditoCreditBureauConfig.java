package org.mifos.creditbureau.data;

/**
 * Example child class that extends CreditBureauConfigurationParam
 * to demonstrate how to implement specific credit bureau configurations.
 */
public class CirculoDeCreditoCreditBureauConfig extends CreditBureauConfigurationParam {
    
    // Constants for parameter keys
    public static final String API_KEY = "apiKey";
    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";
    public static final String ENDPOINT_URL = "endpointUrl";

    public CirculoDeCreditoCreditBureauConfig() {
        super();
    }

    public CirculoDeCreditoCreditBureauConfig(long creditBureauOrganizationParamId, long creditBureauOrganizationId) {
        super(creditBureauOrganizationParamId, creditBureauOrganizationId);
    }

    public String getApiKey() {
        return getParam(API_KEY);
    }

    public CirculoDeCreditoCreditBureauConfig setApiKey(String apiKey) {
        setParam(API_KEY, apiKey);
        return this;
    }

    public String getPrivateKey() {
        return getParam(PRIVATE_KEY);
    }

    public CirculoDeCreditoCreditBureauConfig setPrivateKey(String privateKey) {
        setParam(PRIVATE_KEY, privateKey);
        return this;
    }

    public String getPublicKey() {
        return getParam(PUBLIC_KEY);
    }

    public CirculoDeCreditoCreditBureauConfig setPublicKey(String publicKey) {
        setParam(PUBLIC_KEY, publicKey);
        return this;
    }

    public String getEndpointUrl() {
        return getParam(ENDPOINT_URL);
    }

    public CirculoDeCreditoCreditBureauConfig setEndpointUrl(String endpointUrl) {
        setParam(ENDPOINT_URL, endpointUrl);
        return this;
    }

    public static CirculoDeCreditoCreditBureauConfig instance(
            final long creditBureauOrganizationId, 
            final long creditBureauOrganizationParamId,
            final String apiKey,
            final String privateKey,
            final String publicKey,
            final String endpointUrl) {
        
        return new CirculoDeCreditoCreditBureauConfig(creditBureauOrganizationParamId, creditBureauOrganizationId)
                .setApiKey(apiKey)
                .setPrivateKey(privateKey)
                .setPublicKey(publicKey)
                .setEndpointUrl(endpointUrl);
    }
}