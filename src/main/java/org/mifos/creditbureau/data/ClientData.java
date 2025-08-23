package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.util.List;

//TODO: what do do about dependents
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
