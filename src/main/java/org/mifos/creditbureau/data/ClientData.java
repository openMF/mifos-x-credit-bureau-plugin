package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Aggregates client data from all Fineract endpoint calls.
 *
 * Phase 2 additions:
 *   rfc — added from GET /clients/{id}/identifiers where
 *         documentType=NATIONAL_ID. Falls back to externalId.
 *         Required for CDC RCC request — CDC-001 without it.
 */
@Getter
@Builder
@AllArgsConstructor
public class ClientData {
    private final long id;
    private final String firstName;
    private final String lastName;
    private final String externalId;      // fallback RFC source
    private final String rfc;             // PRIMARY: from /identifiers NATIONAL_ID
    private final List<Integer> dateOfBirth;
    private final String nationality;
    private final String gender;
    private final String maritalStatus;
    private final String addressType;
    private final Long addressId;
    private final List<String> streetAddress;
    private final String townVillage;
    private final String city;
    private final String country;
    private final String postalCode;
    private final String stateProvince;
    private final String phoneNumber;
    private final String emailAddress;
}
