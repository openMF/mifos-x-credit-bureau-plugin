package org.mifos.creditbureau.cb_ild.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Maps the response from Fineract GET /clients/{id}.
 *
 * CRITICAL: Field names must match Fineract JSON exactly.
 * Fineract returns "firstname" and "lastname" — lowercase 'n'.
 * If we use "firstName" Jackson cannot deserialize and returns null.
 * Confirmed from plugin FineractClientResponse.java.
 *
 * dateOfBirth comes as a JSON array [year, month, day]:
 * Example: [2005, 5, 23] = May 23, 2005
 * Stored as List<Integer> — matches plugin exactly.
 * Can be null if client has no DOB set in Fineract.
 *
 * id is primitive long — client ID is never null in Fineract.
 * Using primitive matches plugin FineractClientResponse.java.
 *
 * externalId contains RFC in some Fineract configurations.
 * Used as fallback ONLY if GET /clients/{id}/identifiers returns [].
 * Primary RFC source is always GET /clients/{id}/identifiers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FineractClientResponse(
        long id,
        String firstname,
        String lastname,
        String externalId,
        String mobileNo,
        String emailAddress,
        List<Integer> dateOfBirth
) {}
