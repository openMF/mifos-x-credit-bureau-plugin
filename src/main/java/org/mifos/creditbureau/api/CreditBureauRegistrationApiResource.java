package org.mifos.creditbureau.api;

import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mifos.creditbureau.data.registration.CBRegisterParamsData;
import org.mifos.creditbureau.data.registration.CreditBureauData;
import org.mifos.creditbureau.data.registration.CreditBureauSummary;
import org.mifos.creditbureau.data.registration.CreditBureauConfigRequest;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/credit-bureaus")
@Component
@Produces(MediaType.APPLICATION_JSON)
public class CreditBureauRegistrationApiResource {

    private final CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService;
    private final CreditBureauRegistrationReadService creditBureauRegistrationReadService;
    private final CreditBureauRepository creditBureauRepository;

    @Autowired
    public CreditBureauRegistrationApiResource(
            CreditBureauRegistrationWriteServiceImpl creditBureauRegistrationWriteService,
            CreditBureauRegistrationReadService creditBureauRegistrationReadService,
            CreditBureauRepository creditBureauRepository) {
        this.creditBureauRegistrationWriteService = creditBureauRegistrationWriteService;
        this.creditBureauRegistrationReadService = creditBureauRegistrationReadService;
        this.creditBureauRepository = creditBureauRepository;
    }

    @GET
    @Path("")
    public List<CreditBureauSummary> getAllCreditBureaus() {
        List<CreditBureauData> creditBureaus = Optional.ofNullable(
                        this.creditBureauRegistrationReadService.getAllCreditBureaus())
                .orElse(Collections.emptyList());

        return creditBureaus.stream()
                .map(cb -> new CreditBureauSummary(cb.getId(), cb.getCreditBureauName()))
                .collect(Collectors.toList());
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
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
    public Response getCreditBureauById(@PathParam("id") Long id) {
        Optional<CreditBureau> creditBureauOpt = creditBureauRepository.findById(id);
        if (creditBureauOpt.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        CreditBureau cb = creditBureauOpt.get();
        CreditBureauSummary summary = new CreditBureauSummary(cb.getId(), cb.getCreditBureauName());
        return Response.ok(summary).build();
    }

    @GET
    @Path("/{id}/configuration-keys")
    public List<String> getConfigParamKeys(@PathParam("id") Long organizationCreditBureauId) {
        List<String> creditBureauParamKeys = this.creditBureauRegistrationReadService.getCreditBureauParamKeys(organizationCreditBureauId);
        return Optional.ofNullable(creditBureauParamKeys).orElse(Collections.emptyList());
    }

    @PUT
    @Path("/{id}/configuration")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response configureCreditBureauParams(@PathParam("id") Long id,
                                                @Valid CreditBureauConfigRequest configRequest) {
        if (configRequest == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Request body must not be null").build();
        }

        if (configRequest.getOrganisationCreditBureauId() != null && !id.equals(configRequest.getOrganisationCreditBureauId())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Path id does not match organisationCreditBureauId in payload").build();
        }

        Map<String, String> params = new HashMap<>();
        if (configRequest.getRegistrationParams() != null) {
            params.putAll(configRequest.getRegistrationParams());
        }

        if (configRequest.getUsername() != null) {
            params.put("username", configRequest.getUsername());
        }
        if (configRequest.getXApiKey() != null) {
            params.put("x-api-key", configRequest.getXApiKey());
        }
        if (configRequest.getCertificate() != null) {
            params.put("certificate", configRequest.getCertificate());
        }

        CBRegisterParamsData cbRegisterParamsData = CBRegisterParamsData.builder().registrationParams(params).build();
        CBRegisterParams createdCBParams = creditBureauRegistrationWriteService.configureCreditBureauParamsValues(id, cbRegisterParamsData);
        return Response.status(Response.Status.CREATED).entity(createdCBParams).build();
    }

    @GET
    @Path("/{id}/configuration-map")
    public Map<String, String> getCBRegisterParamsById(@PathParam("id") Long id) {
        Map<String, String> cbRegisterParams = creditBureauRegistrationReadService.getRegistrationParamMap(id);
        return Optional.ofNullable(cbRegisterParams).orElse(Collections.emptyMap());
    }

}
