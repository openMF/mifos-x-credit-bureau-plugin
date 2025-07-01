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
public class CreditBureauConfigurationParam {

    private long creditBureauOrganizationParamId;
    private long creditBureauOrganizationId;

    // Map to store dynamic configuration parameters
    protected Map<String, String> configParams = new HashMap<>();

    public CreditBureauConfigurationParam(long creditBureauOrganizationParamId, long creditBureauOrganizationId) {
        this.creditBureauOrganizationParamId = creditBureauOrganizationParamId;
        this.creditBureauOrganizationId = creditBureauOrganizationId;
    }

    public String getParam(String key) {
        return configParams.get(key);
    }

    public CreditBureauConfigurationParam setParam(String key, String value) {
        configParams.put(key, value);
        return this;
    }

    public Map<String, String> getAllParams() {
        return new HashMap<>(configParams);
    }

    public static CreditBureauConfigurationParam instance(final long creditBureauOrganizationId, final long creditBureauOrganizationParamId) {
        return new CreditBureauConfigurationParam(creditBureauOrganizationId, creditBureauOrganizationParamId);
    }
}
