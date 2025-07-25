package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

//Reformat data from fineract-client api for internal
@Getter
@AllArgsConstructor
public class ClientData {

    private final long id;

    private final String clientFirstName;

    private final String clientLastName;

    private final String clientBirthDate;

    private final String RFC;

    private final String nationality;

    private final String address;
}
