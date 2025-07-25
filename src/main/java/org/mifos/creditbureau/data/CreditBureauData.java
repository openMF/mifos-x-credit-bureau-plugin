package org.mifos.creditbureau.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreditBureauData {

    private final long id;

    private final String creditBureauName;

    private final boolean available;

    private final boolean active;

    private final String country;

    private final CBRegisterParamsData cbRegisterParamsData;

}