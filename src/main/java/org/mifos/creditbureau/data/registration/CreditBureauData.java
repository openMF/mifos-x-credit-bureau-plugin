package org.mifos.creditbureau.data.registration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
public class CreditBureauData {

    @Builder.Default
    private final long id = 0;

    private final String creditBureauName;

    private final boolean available;

    private final boolean active;

    private final String country;

    private final Set<String> registrationParamKeys;


}
