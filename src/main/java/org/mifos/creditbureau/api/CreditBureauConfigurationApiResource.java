package org.mifos.creditbureau.api;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;


@Path("/CreditBureauRegistration")
public class CreditBureauConfigurationApiResource {

    @GET
    @Path("/available")
    public String getAvailableCreditBureaus() {
        // Implementation to be added
        return "{\"message\": \"Available credit bureaus\"}";
    }

    @GET
    @Path("/{id}/configParams")
    public String getConfigParams(@PathParam("organizationCreditBureauId") Long organizationCreditBureauId) {
        // Implementation to be added
        return "{\"message\": \"Config params for credit bureau " + organizationCreditBureauId + "\"}";
    }

    @POST
    @Path("/{id}/configure")
    public String configureCreditBureau(@PathParam("id") Long id) {
        // Implementation to be added
        return "{\"message\": \"Credit bureau " + id + " configured\"}";
    }
}
