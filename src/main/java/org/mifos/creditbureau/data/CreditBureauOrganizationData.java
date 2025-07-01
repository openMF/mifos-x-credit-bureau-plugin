package org.mifos.creditbureau.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CreditBureauOrganizationData {

    private long creditBureauOrganizationId;

    private long creditBureauOrganizationParamId;

    private String creditBureauName;

    private boolean creditBureauOrganizationIsAvailable;

    private boolean creditBureauOrganizationIsActive;

    private String country;

    public static CreditBureauOrganizationData instance(final long creditBureauOrganizationId, final long creditBureauOrganizationParamId, final String creditBureauName, final boolean creditBureauOrganizationIsAvailable, final boolean creditBureauOrganizationIsActive, final String country){

        return new CreditBureauOrganizationData()
                .setCreditBureauOrganizationId(creditBureauOrganizationId)
                .setCreditBureauOrganizationParamId(creditBureauOrganizationParamId)
                .setCreditBureauName(creditBureauName)
                .setCreditBureauOrganizationIsActive(creditBureauOrganizationIsActive)
                .setCreditBureauOrganizationIsAvailable(creditBureauOrganizationIsAvailable)
                .setCreditBureauOrganizationIsActive(creditBureauOrganizationIsActive)
                .setCountry(country);
    }
}
