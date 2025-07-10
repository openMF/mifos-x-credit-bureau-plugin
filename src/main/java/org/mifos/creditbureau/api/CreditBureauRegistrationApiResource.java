package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import java.util.List;


@Path("/CreditBureauRegistration")
public class CreditBureauRegistrationApiResource {

    @GET
    @Path("/available")
    public List<String> getAvailableCreditBureaus() {

    }

    @GET
    @Path("/{id}/configParams")
    public List<String> getConfigParams(@PathParam("id") Long organizationCreditBureauId) {

    }

    @POST
    @Path("/{id}/configure")
    public String configureCreditBureau(@PathParam("id") Long id) {

    }
}
