package org.mifos.creditbureau.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity representing a Credit Bureau Organization.
 */
@Entity
@Table(name = "credit_bureau_organization")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauOrganization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long creditBureauOrganizationId;

    @Column(name = "param_id")
    private Long creditBureauOrganizationParamId;

    @Column(name = "name", nullable = false)
    private String creditBureauName;

    @Column(name = "is_available")
    private boolean creditBureauOrganizationIsAvailable;

    @Column(name = "is_active")
    private boolean creditBureauOrganizationIsActive;

    @Column(name = "country")
    private String country;

    /**
     * Creates a new instance of CreditBureauOrganization.
     */
    public static CreditBureauOrganization instance(
            final String creditBureauName,
            final boolean creditBureauOrganizationIsAvailable,
            final boolean creditBureauOrganizationIsActive,
            final String country) {
        
        CreditBureauOrganization organization = new CreditBureauOrganization();
        organization.setCreditBureauName(creditBureauName);
        organization.setCreditBureauOrganizationIsAvailable(creditBureauOrganizationIsAvailable);
        organization.setCreditBureauOrganizationIsActive(creditBureauOrganizationIsActive);
        organization.setCountry(country);
        
        return organization;
    }
}