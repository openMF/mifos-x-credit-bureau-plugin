package org.mifos.creditbureau.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.data.CreditBureauSummary;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.service.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.CreditBureauRegistrationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Path("/CreditBureauRegistration")
@Component
public class CreditBureauRegistrationApiResource {
    private final CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService;
    private final CreditBureauRegistrationReadService creditBureauRegistrationReadService;
    private final CreditBureauRepository creditBureauRepository;
    private final CBRegisterParamRepository cbRegisterParamRepository;

    @Autowired
    public CreditBureauRegistrationApiResource(CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService, CreditBureauRegistrationReadService creditBureauRegistrationReadService, CreditBureauRepository creditBureauRepository, CBRegisterParamRepository cbRegisterParamRepository, CBRegisterParamRepository cbRegisterParamRepository1) {
        this.creditBureauRegistrationWriteService = creditBureauRegistrationWriteService;
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
        this.creditBureauRepository = creditBureauRepository;
        this.cbRegisterParamRepository = cbRegisterParamRepository1;
    }

    @GET
    @Path("/available")
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
    @Path("/creditbureaus")
    //create a Credit Bureau and CBRegisterParams
    public ResponseEntity<CreditBureauSummary> createCreditBureau(@RequestBody CreditBureauData creditBureauData) {
        CreditBureau createdCreditBureau = creditBureauRegistrationWriteService.createCreditBureau(creditBureauData);
        CreditBureauSummary summary = new CreditBureauSummary(
                createdCreditBureau.getId(),
                createdCreditBureau.getCreditBureauName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(summary);
    }

    @GET
    @Path("/{id}/configParams")
    //Get the titles of the secrets needed
    public List<String> getConfigParams(@PathParam("id") Long organizationCreditBureauId) {        // Fetch the configuration parameters for the given ID
        // Call the service layer to get the parameters DTO
        List<String> creditBureauParamKeys = this.creditBureauRegistrationReadService.getCreditBureauParamKeys(organizationCreditBureauId);
        return Objects.requireNonNullElse(creditBureauParamKeys, Collections.emptyList());

    }

    @POST
    @Path("/{id}/configure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    //Enter the values of the API key into the dto
    public ResponseEntity<CBRegisterParams> configureCreditBureauParams(@PathParam("id") Long id, @RequestBody CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCBParams);
    }

    @GET
    @Path("/creditbureaus/{id}")
    public ResponseEntity<CreditBureau> getCreditBureauById(@PathParam("id") Long id) {
        Optional<CreditBureau> creditBureauOpt = creditBureauRepository.findById(id);
        return creditBureauOpt.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GET
    @Path("/cbregisterparams/{id}")
    public ResponseEntity<CBRegisterParams> getCBRegisterParamsById(@PathParam("id") Long id) {
        Optional<CBRegisterParams> paramsOpt = cbRegisterParamRepository.findById(id);
        return paramsOpt
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
