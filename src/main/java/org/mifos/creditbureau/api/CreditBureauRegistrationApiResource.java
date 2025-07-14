package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.service.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.CreditBureauRegistrationWriteImplServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Path("/CreditBureauRegistration")
public class CreditBureauRegistrationApiResource {
    private final CreditBureauRegistrationWriteImplServiceImpl creditBureauRegistrationWriteService;
    private final CreditBureauRegistrationReadService creditBureauRegistrationReadService;

    @GET
    @Path("/available")
    public List<String> getAllCreditBureaus() {
        // Fetch the list of CreditBureauData objects
        List<CreditBureauData> creditBureaus = this.creditBureauRegistrationReadService.getAllCreditBureaus();

        // Transform the list into a list of names using a Java Stream
        return creditBureaus.stream()
                .map(CreditBureauData::getCreditBureauName)
                .collect(Collectors.toList());
    }

    @GET
    @Path("/{id}/configParams")
    //TODO: Complete this for later
    public List<String> getConfigParams(Long organizationCreditBureauId) {        // Fetch the configuration parameters for the given ID
        return new ArrayList<>();
    }

    @POST
    @Path("/{id}/configure")
    public ResponseEntity<CBRegisterParams> configureCreditBureauParams(@RequestBody CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParams(cbRegisterParamsData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCBParams);
    }
}
