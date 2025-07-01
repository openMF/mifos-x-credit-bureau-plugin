package org.mifos.creditbureau.data;

/**
 * Example class demonstrating how to use the CreditBureauConfigurationParam parent class
 * and its child classes for different credit bureau configurations.
 */
public class CreditBureauConfigExample {

    public static void main(String[] args) {
        // Example 1: Using the parent class directly for a generic credit bureau
        CreditBureauConfigurationParam genericConfig = new CreditBureauConfigurationParam(1L, 100L);
        genericConfig.setParam("username", "user123")
                    .setParam("password", "pass456")
                    .setParam("apiEndpoint", "https://api.generic-bureau.com")
                    .setParam("timeout", "30000");
        
        System.out.println("Generic config username: " + genericConfig.getParam("username"));
        System.out.println("Generic config has " + genericConfig.getAllParams().size() + " parameters");
        
        // Example 2: Using a specific child class for a particular credit bureau
        CirculoDeCreditoCreditBureauConfig specificConfig = new CirculoDeCreditoCreditBureauConfig(2L, 200L);
        specificConfig.setApiKey("abc123xyz")
                     .setPrivateKey("private-key-content")
                     .setPublicKey("public-key-content")
                     .setEndpointUrl("https://api.example-bureau.com");
        
        // Using type-safe getters
        System.out.println("Specific config API key: " + specificConfig.getApiKey());
        
        // We can also add custom parameters not defined in the class
        specificConfig.setParam("customParam", "customValue");
        System.out.println("Custom parameter value: " + specificConfig.getParam("customParam"));
        
        // Example 3: Using the factory method
        CirculoDeCreditoCreditBureauConfig factoryConfig = CirculoDeCreditoCreditBureauConfig.instance(
                300L, 3L, "factory-api-key", "factory-private-key", 
                "factory-public-key", "https://api.factory-example.com");
        
        System.out.println("Factory config endpoint: " + factoryConfig.getEndpointUrl());
        
        // Example 4: Creating another type of credit bureau configuration
        // In a real application, you would create another child class for this
        CreditBureauConfigurationParam anotherConfig = new CreditBureauConfigurationParam(4L, 400L);
        anotherConfig.setParam("clientId", "client-123")
                    .setParam("clientSecret", "secret-456")
                    .setParam("grantType", "client_credentials")
                    .setParam("scope", "read write")
                    .setParam("tokenUrl", "https://auth.another-bureau.com/token");
        
        System.out.println("Another config client ID: " + anotherConfig.getParam("clientId"));
    }
}