package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CreditBureauData {

    private final long id;

    private final String creditBureauName;

    private final boolean available;

    private final boolean active;

    private final String country;

    private final CBRegisterParamsData creditBureauParameter;

}
