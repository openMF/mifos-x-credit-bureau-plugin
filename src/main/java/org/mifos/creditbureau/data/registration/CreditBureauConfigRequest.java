package org.mifos.creditbureau.data.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class CreditBureauConfigRequest {

    @NotNull
    private final Long organisationCreditBureauId;

    @NotBlank
    @Size(max = 255)
    private final String username;

    @NotBlank
    @Size(max = 1024)
    private final String xApiKey;

    @Size(max = 4096)
    private final String certificate;

    // allow additional arbitrary registration params
    @NotNull
    private final Map<String, String> registrationParams;

}
