package org.mifos.creditbureau.mappers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.mifos.creditbureau.data.registration.CBRegisterParamsData;
import org.mifos.creditbureau.data.registration.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;

@Component
public class CreditBureauMapper {

    public CreditBureauData toCreditBureauData(CreditBureau creditBureau) {
        if (creditBureau == null) {
            return null;
        }

        // Extract registration parameter keys from the entity
        Set<String> registrationParamKeys = new HashSet<>();
        if (creditBureau.getCreditBureauParameter() != null &&
            creditBureau.getCreditBureauParameter().getRegistrationParams() != null) {
            registrationParamKeys =
                    new HashSet<>(creditBureau.getCreditBureauParameter().getRegistrationParams().keySet());
        }

        return CreditBureauData.builder()
                .id(creditBureau.getId())
                .creditBureauName(creditBureau.getCreditBureauName())
                .active(creditBureau.isActive())
                .country(creditBureau.getCountry())
                .registrationParamKeys(registrationParamKeys)
                .build();
    }

    public CreditBureau toCreditBureau(CreditBureauData creditBureauData) {
        if (creditBureauData == null) {
            return null;
        }

        CreditBureau creditBureau = new CreditBureau();
        // id is ignored as per the original MapStruct configuration
        creditBureau.setCreditBureauName(creditBureauData.getCreditBureauName());
        creditBureau.setActive(creditBureauData.isActive());
        creditBureau.setCountry(creditBureauData.getCountry());

        // Registration Params handled in the service layer

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

    public Set<String> extractRegistrationParamKeys(Map<String, String> map) {
        return map!=null ? new HashSet<>(map.keySet()) : new HashSet<>();
    }
}
