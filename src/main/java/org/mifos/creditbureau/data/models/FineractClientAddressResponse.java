package org.mifos.creditbureau.data.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class FineractClientAddressResponse implements Serializable {

    private final Long id;

    private final String addressLine1;

    private final String addressLine2;

    private final String addressLine3;

    private final String townCity;

    private final String city;

    private final String countyDistrict;

    private final String stateProvince;

    private final String countryName;

    private final String postalCode;
}
