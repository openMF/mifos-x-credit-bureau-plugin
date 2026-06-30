package org.mifos.creditbureau.cb_ild.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps one address entry from Fineract GET /clients/{id}/addresses.
 *
 * Field names match exactly what Fineract returns — confirmed from
 * plugin FineractClientAddressResponse.java.
 *
 * All fields are nullable because:
 * 1. GET /clients/{id}/addresses returns 404 on sandbox (Bug 3)
 * 2. Even when it returns data, individual fields may be null
 *
 * FineractApiClient handles 404 by returning null for the whole DTO.
 * KYC scorer deducts 15 address points when hasMinimumData() is false.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FineractAddressDTO(
        Long id,
        String addressType,
        String street,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String townVillage,
        String city,
        String countyDistrict,
        String countryName,
        String stateName,
        String postalCode
) {

    /**
     * Returns true if address has minimum data for CDC submission.
     * CDC Reportar Cartera requires at least addressLine1 and city.
     * KYC scorer uses this to award or deduct address points.
     */
    public boolean hasMinimumData() {
        return (addressLine1 != null && !addressLine1.isBlank())
                && (city != null && !city.isBlank());
    }
}
