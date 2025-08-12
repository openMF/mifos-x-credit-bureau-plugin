package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClientAddressData {

    private final String streetAddress;

    private final String neighborhood;

    private final String municipality;

    private final String city;

    private final String state;

    private final String postalCode;

    private final String residenceDate;

    private final String phoneNumber;

    private final String addressType;

    private final String settlementType;

    private final String country;
}
