package org.mifos.creditbureau.config;

import org.glassfish.jersey.server.ResourceConfig;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import org.springframework.context.annotation.Configuration;

//TODO swagger is returning server errors
@Configuration
public class JerseyConfig extends ResourceConfig {
    
    public JerseyConfig() {
        packages("org.mifos.creditbureau.api");
        //register(OpenApiResource.class);
    }
}