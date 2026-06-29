package org.mifos.creditbureau.cb_ild.client;

import java.util.List;

/**
 * Aggregates data from all 3 Fineract endpoint calls into one object.
 *
 * This is the single object every CB-ILD service uses.
 * Nothing in CB-ILD uses the 3 individual DTOs directly.
 *
 * Built by FineractApiClient.getClientData(clientId) which calls:
 *   1. GET /clients/{id}             → basic client data
 *   2. GET /clients/{id}/identifiers → nationalId (RFC)
 *   3. GET /clients/{id}/addresses   → address data
 *
 * RFC strategy (nationalId field):
 *   Primary:  GET /clients/{id}/identifiers where documentType=NATIONAL_ID
 *   Fallback: externalId from GET /clients/{id} basic call
 *   If both null: nationalId=null → KYC scorer hard gate → score=0
 *
 * SECURITY: This object is never logged in full.
 * nationalId (RFC) is PII — never appears in any log line.
 * dateOfBirth is PII — never appears in any log line.
 */
public record FineractClientData(
        long clientId,
        String firstName,
        String lastName,
        String nationalId,
        List<Integer> dateOfBirth,
        String phoneNumber,
        String emailAddress,
        String addressLine1,
        String addressLine2,
        String addressLine3,
        String city,
        String state,
        String postalCode,
        String country
) {

    /**
     * Hard gate check for KYC scorer.
     *
     * RFC (nationalId) is CDC's primary client matching key.
     * If RFC is missing, CDC rejects with CDC-001 error.
     * KYC scorer returns score=0 immediately without calling CDC.
     * This saves CDC API credits on every unqualified client.
     *
     * Checks both null and blank — an empty string RFC causes
     * CDC-001 exactly like a null RFC.
     */
    public boolean hasNationalId() {
        return nationalId != null && !nationalId.isBlank();
    }

    /**
     * Returns true if this client has minimum data for KYC scoring.
     * Used by KYC scorer before calculating weighted score.
     * A client with no name and no RFC has nothing useful to score.
     */
    public boolean hasMinimumData() {
        return hasNationalId()
                || (firstName != null && !firstName.isBlank())
                || (lastName != null && !lastName.isBlank());
    }

    /**
     * Returns true if date of birth is present and valid.
     * Fineract returns DOB as [year, month, day] — 3 elements minimum.
     * KYC scorer awards 20 points only when this returns true.
     */
    public boolean hasDob() {
        return dateOfBirth != null && dateOfBirth.size() >= 3;
    }

    /**
     * Returns true if address has minimum data for CDC submission.
     * Delegates to address fields directly — no FineractAddressDTO needed.
     * KYC scorer awards 15 points only when this returns true.
     */
    public boolean hasAddress() {
        return (addressLine1 != null && !addressLine1.isBlank())
                && (city != null && !city.isBlank());
    }

    /**
     * Returns true if phone number is present.
     * KYC scorer awards 5 points only when this returns true.
     */
    public boolean hasPhone() {
        return phoneNumber != null && !phoneNumber.isBlank();
    }
}
