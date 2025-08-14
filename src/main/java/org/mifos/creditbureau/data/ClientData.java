package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ClientData {
    private final long id;
    private final String firstName;
    private final String lastName;
    private final String externalId;
    private final List<Integer> dateOfBirth;
    private final String nationality;
    private final List<String> streetAddress;
    private final String neighborhood;
    private final String municipality;
    private final String postalCode;
    private final String stateProvince;
    private final String country;
    private final String phoneNumber;
    private final String emailAddress;

}
