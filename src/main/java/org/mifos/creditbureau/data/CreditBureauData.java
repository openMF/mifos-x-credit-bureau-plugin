package org.mifos.creditbureau.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CreditBureauData {

    private long id;

    private String creditBureauName;

    private boolean isAvailable;

    private boolean isActive;

    private String country;

    private CBRegisterParamsData cbRegisterParamsData;

    public static CreditBureauData instance(
            final long cbParamsId,
            final long id,
            final String creditBureauName,
            final boolean isAvailable,
            final boolean isActive,
            final String country
            ){

        CBRegisterParamsData params = new CBRegisterParamsData()
                .setId(cbParamsId);

        return new CreditBureauData()
                .setId(id)
                .setCreditBureauName(creditBureauName)
                .setAvailable(isAvailable)
                .setActive(isActive)
                .setCountry(country)
                .setCbRegisterParamsData(params);
    }

}
