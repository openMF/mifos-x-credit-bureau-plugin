package org.mifos.creditbureau.api;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.MediaType;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.*;
import java.util.stream.Collectors;


@Path("/credit-bureaus")
@Component
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
    //create a Credit Bureau and CBRegisterParams, will not be exposed
    public ResponseEntity<CreditBureauSummary> createCreditBureau(@RequestBody CreditBureauData creditBureauData) {
        CreditBureau createdCreditBureau = creditBureauRegistrationWriteService.createCreditBureau(creditBureauData);
        CreditBureauSummary summary = new CreditBureauSummary(
                createdCreditBureau.getId(),
                createdCreditBureau.getCreditBureauName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GET
    @Path("/{id}")
    //return value is CreditBureauSummary we do not want to expose the Entity itself
    public ResponseEntity<CreditBureauSummary> getCreditBureauById(@PathParam("id") Long id) {
        Optional<CreditBureau> creditBureauOpt = creditBureauRepository.findById(id);

        return creditBureauOpt
                .map(cb -> {
                    CreditBureauSummary summary = new CreditBureauSummary(
                            cb.getId(),
                            cb.getCreditBureauName()
                    );
                    return ResponseEntity.ok(summary);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
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
    public ResponseEntity<CBRegisterParams> configureCreditBureauParams(@PathParam("id") Long id, @RequestBody CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCBParams);
    }

    @GET
    @Path("/{id}/configuration-map")
    //will not be exposed
    public Map<String, String> getCBRegisterParamsById(@PathParam("id") Long id) {
        Map<String, String> cbRegisterParams = creditBureauRegistrationReadService.getRegistrationParamMap(id);
        return Objects.requireNonNullElse(cbRegisterParams, Collections.emptyMap());
    }

}
