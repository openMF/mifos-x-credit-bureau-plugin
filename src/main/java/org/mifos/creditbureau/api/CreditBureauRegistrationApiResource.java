package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.service.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.CreditBureauRegistrationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public CreditBureauRegistrationApiResource(CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService, CreditBureauRegistrationReadService creditBureauRegistrationReadService) {
        this.creditBureauRegistrationWriteService = creditBureauRegistrationWriteService;
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
    }

    @GET
    @Path("/available")
    //Retrieve all Credit Bureaus
    public List<String> getAllCreditBureaus() {
        // Fetch the list of CreditBureauData objects
        List<CreditBureauData> creditBureaus = Optional.ofNullable(
                this.creditBureauRegistrationReadService.getAllCreditBureaus())
                .orElse(Collections.emptyList());

        // Transform the list into a list of names using a Java Stream
        return creditBureaus.stream()
                .map(CreditBureauData::getCreditBureauName)
                .collect(Collectors.toList());
    }

    @POST
    @Path("/creditbureaus")
    //create a Credit Bureau and CBRegisterParams
    public ResponseEntity<CreditBureau> createCreditBureau(@RequestBody CreditBureauData creditBureauData) {
        CreditBureau createdCreditBureau = creditBureauRegistrationWriteService.createCreditBureau(creditBureauData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCreditBureau);
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
    //Enter the values of the API key into the dto
    public ResponseEntity<CBRegisterParams> configureCreditBureauParams(@RequestBody CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParams(cbRegisterParamsData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCBParams);
    }
}
