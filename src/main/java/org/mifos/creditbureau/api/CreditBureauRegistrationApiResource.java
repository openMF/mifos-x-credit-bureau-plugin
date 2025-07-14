package org.mifos.creditbureau.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.service.CreditBureauRegistrationWriteImplServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@AllArgsConstructor
@Path("/CreditBureauRegistration")
public class CreditBureauRegistrationApiResource {
    private final CreditBureauRegistrationWriteImplServiceImpl creditBureauRegistrationWriteService;

    @GET
    @Path("/available")
    public List<String> getAvailableCreditBureaus() {
        return null;
    }

    @GET
    @Path("/{id}/configParams")
    public List<String> getConfigParams(@PathParam("id") Long organizationCreditBureauId) {
        return null;

    }

    @POST
    @Path("/{id}/configure")
    public ResponseEntity<CBRegisterParams> configureCreditBureauParams(@RequestBody CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParams(cbRegisterParamsData);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCBParams);
    }
}
