package org.mifos.creditbureau.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing registration parameters for a Credit Bureau.
 * This entity uses a Map-based approach to store dynamic configuration parameters.
 */
@Entity
@Table(name = "credit_bureau_registration_param")
@Getter
@Setter
@NoArgsConstructor
public class CreditBureauRegistrationParam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long creditBureauOrganizationParamId;

    @Column(name = "organization_id")
    private Long creditBureauOrganizationId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "credit_bureau_registration_param_values", joinColumns = @JoinColumn(name = "param_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, String> registrationParams = new HashMap<>();

    public CreditBureauRegistrationParam(Long creditBureauOrganizationParamId, Long creditBureauOrganizationId) {
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

    /**
     * Creates a new instance of CreditBureauRegistrationParam.
     */
    public static CreditBureauRegistrationParam instance(final Long creditBureauOrganizationId) {
        CreditBureauRegistrationParam param = new CreditBureauRegistrationParam();
        param.setCreditBureauOrganizationId(creditBureauOrganizationId);
        return param;
    }
}