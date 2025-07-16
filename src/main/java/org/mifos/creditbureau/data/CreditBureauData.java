package org.mifos.creditbureau.data;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
public class CreditBureauData {

    private final long id;

    private final String creditBureauName;

    private final boolean isAvailable;

    private final boolean isActive;

    private final String country;

    private final CBRegisterParamsData cbRegisterParamsData;

    public static CreditBureauData instance(
            final long cbParamsId,
            final long id,
            final String creditBureauName,
            final boolean isAvailable,
            final boolean isActive,
            final String country
            ){

        CBRegisterParamsData params = CBRegisterParamsData.builder()
                .id(cbParamsId)
                .build();

        return CreditBureauData.builder()
                .id(id)
                .creditBureauName(creditBureauName)
                .isAvailable(isAvailable)
                .isActive(isActive)
                .country(country)
                .cbRegisterParamsData(params)
                .build();
    }

}