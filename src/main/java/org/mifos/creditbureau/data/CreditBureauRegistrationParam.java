package org.mifos.creditbureau.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents configuration parameters for a Credit Bureau.
 * This is a parent class that uses the Strategy pattern combined with a Map-based approach
 * to allow child classes to define their own specific keys.
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class CreditBureauRegistrationParam {

    private long creditBureauOrganizationParamId;
    private long creditBureauOrganizationId;

    // Map to store dynamic configuration parameters
    protected Map<String, String> registrationParams = new HashMap<>();

    public CreditBureauRegistrationParam(long creditBureauOrganizationParamId, long creditBureauOrganizationId) {
        this.creditBureauOrganizationParamId = creditBureauOrganizationParamId;
        this.creditBureauOrganizationId = creditBureauOrganizationId;
    }

    public String getParam(String key) {
        return registrationParams.get(key);
    }

    public CreditBureauRegistrationParam setParam(String key, String value) {
        registrationParams.put(key, value);
        return this;
    }

    public Map<String, String> getAllParams() {
        return new HashMap<>(registrationParams);
    }

    public static CreditBureauRegistrationParam instance(final long creditBureauOrganizationId, final long creditBureauOrganizationParamId) {
        return new CreditBureauRegistrationParam(creditBureauOrganizationId, creditBureauOrganizationParamId);
    }
}
