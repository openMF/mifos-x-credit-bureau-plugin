package org.mifos.creditbureau.data.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public class FineractClientAddressResponse implements Serializable {

    private final Long id;

    private final String addressType;

    private final Long addressId;

    private final Long addressTypeId;

    private final String street;

    private final String addressLine1;

    private final String addressLine2;

    private final String addressLine3;

    private final String townVillage;

    private final String city;

    private final String countyDistrict;

    private final String countryName;

    private final String stateName;

    private final String postalCode;
}
