package org.mifos.creditbureau.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.data.registration.CBRegisterParamsData;
import org.mifos.creditbureau.data.registration.CreditBureauData;
import org.mifos.creditbureau.data.registration.CreditBureauSummary;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


@Path("/credit-bureaus")
@Component
@Produces(MediaType.APPLICATION_JSON)
public class CreditBureauRegistrationApiResource {
    private final CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService;
    private final CreditBureauRegistrationReadService creditBureauRegistrationReadService;
    private final CreditBureauRepository creditBureauRepository;


    @Autowired
    public CreditBureauRegistrationApiResource(CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService, CreditBureauRegistrationReadService creditBureauRegistrationReadService, CreditBureauRepository creditBureauRepository, CBRegisterParamRepository cbRegisterParamRepository, CBRegisterParamRepository cbRegisterParamRepository1) {
        this.creditBureauRegistrationWriteService = creditBureauRegistrationWriteService;
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
        this.creditBureauRepository = creditBureauRepository;

    }

    @GET
    @Path("")
    //Retrieve all Credit Bureaus
    public List<CreditBureauSummary> getAllCreditBureaus() {
        // Fetch the list of CreditBureauData objects
        List<CreditBureauData> creditBureaus = Optional.ofNullable(
                this.creditBureauRegistrationReadService.getAllCreditBureaus())
                .orElse(Collections.emptyList());

        // Transform the list into a list of names using a Java Stream
        return creditBureaus.stream()
                .map(cb -> new CreditBureauSummary(cb.getId(), cb.getCreditBureauName()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //create a Credit Bureau and CBRegisterParams, will not be exposed
    public Response createCreditBureau(CreditBureauData creditBureauData) {
        CreditBureau createdCreditBureau = creditBureauRegistrationWriteService.createCreditBureau(creditBureauData);
        CreditBureauSummary summary = new CreditBureauSummary(
                createdCreditBureau.getId(),
                createdCreditBureau.getCreditBureauName()
        );
        return Response.status(Response.Status.CREATED).entity(summary).build();
    }

    @GET
    @Path("/{id}")
    //return value is CreditBureauSummary we do not want to expose the Entity itself
    public Response getCreditBureauById(@PathParam("id") Long id) {
        Optional<CreditBureau> creditBureauOpt = creditBureauRepository.findById(id);

        if (creditBureauOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CreditBureau cb = creditBureauOpt.get();

        CreditBureauSummary summary =
                new CreditBureauSummary(cb.getId(), cb.getCreditBureauName());

        return Response.ok(summary).build();
    }

    @GET
    @Path("/{id}/configuration-keys")
    //Get the titles of the secrets needed
    public List<String> getConfigParamKeys(@PathParam("id") Long organizationCreditBureauId) {        // Fetch the configuration parameters for the given ID
        // Call the service layer to get the parameters DTO
        List<String> creditBureauParamKeys = this.creditBureauRegistrationReadService.getCreditBureauParamKeys(organizationCreditBureauId);
        return Objects.requireNonNullElse(creditBureauParamKeys, Collections.emptyList());

    }

    @PUT
    @Path("/{id}/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //Enter the values of the API key into the dto
    public Response configureCreditBureauParams(@PathParam("id") Long id, CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
        return Response.status(Response.Status.CREATED).entity(createdCBParams).build();
    }

    @GET
    @Path("/{id}/configuration-map")
    //will not be exposed
    public Map<String, String> getCBRegisterParamsById(@PathParam("id") Long id) {
        Map<String, String> cbRegisterParams = creditBureauRegistrationReadService.getRegistrationParamMap(id);
        return Objects.requireNonNullElse(cbRegisterParams, Collections.emptyMap());
    }

}
