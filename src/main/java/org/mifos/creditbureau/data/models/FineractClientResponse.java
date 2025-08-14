package org.mifos.creditbureau.data.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

//Reformat data from fineract-client api for internal
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class FineractClientResponse implements Serializable {

    private final long id;

    private final String firstName;

    private final String lastName;

    private final String externalId; //RFC

    private final String mobileNo;

    private final String emailAddress;

    private final List<Integer> dateOfBirth;

}
