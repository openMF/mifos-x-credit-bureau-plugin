package org.mifos.creditbureau.mappers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;

@Component
public class CreditBureauMapper {

    public CreditBureauData toCreditBureauData(CreditBureau creditBureau) {
        if (creditBureau == null) {
            return null;
        }

        CBRegisterParamsData creditBureauParameter = null;
        if (creditBureau.getCreditBureauParameter() != null) {
            creditBureauParameter = toCBRegisterParamsData(creditBureau.getCreditBureauParameter());
        }

        return CreditBureauData.builder()
                .id(creditBureau.getId())
                .creditBureauName(creditBureau.getCreditBureauName())
                .available(creditBureau.isAvailable())
                .active(creditBureau.isActive())
                .country(creditBureau.getCountry())
                .creditBureauParameter(creditBureauParameter)
                .build();
    }

    public CreditBureau toCreditBureau(CreditBureauData creditBureauData) {
        if (creditBureauData == null) {
            return null;
        }

        CreditBureau creditBureau = new CreditBureau();
        // id is ignored as per the original MapStruct configuration
        creditBureau.setCreditBureauName(creditBureauData.getCreditBureauName());
        creditBureau.setAvailable(creditBureauData.isAvailable());
        creditBureau.setActive(creditBureauData.isActive());
        creditBureau.setCountry(creditBureauData.getCountry());

        if (creditBureauData.getCreditBureauParameter() != null) {
            CBRegisterParams params = toCBRegisterParams(creditBureauData.getCreditBureauParameter());
            params.setCreditBureau(creditBureau);
            creditBureau.setCreditBureauParameter(params);
        }

        return creditBureau;
    }

    public CBRegisterParamsData toCBRegisterParamsData(CBRegisterParams cbRegisterParams) {
        if (cbRegisterParams == null) {
            return null;
        }

        CBRegisterParamsData.CBRegisterParamsDataBuilder builder = CBRegisterParamsData.builder()
                .id(cbRegisterParams.getId());

        if (cbRegisterParams.getRegistrationParams() != null) {
            for (Map.Entry<String, String> entry : cbRegisterParams.getRegistrationParams().entrySet()) {
                builder.registrationParam(entry.getKey(), entry.getValue());
            }
        }

        return builder.build();
    }

    public CBRegisterParams toCBRegisterParams(CBRegisterParamsData cbRegisterParamsData) {
        if (cbRegisterParamsData == null) {
            return null;
        }

        CBRegisterParams cbRegisterParams = new CBRegisterParams();
        // id is ignored as per the original MapStruct configuration

        if (cbRegisterParamsData.getRegistrationParams() != null) {
            Map<String, String> registrationParams = new HashMap<>(cbRegisterParamsData.getRegistrationParams());
            cbRegisterParams.setRegistrationParams(registrationParams);
        }

        return cbRegisterParams;
    }
}
