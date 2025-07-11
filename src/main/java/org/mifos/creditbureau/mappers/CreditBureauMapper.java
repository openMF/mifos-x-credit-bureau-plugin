package org.mifos.creditbureau.mappers;

import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;

import java.util.HashMap;
import java.util.Map;

public class CreditBureauMapper {

    public static CreditBureauData toData(CreditBureau entity) {
        CBRegisterParams params = entity.getCreditBureauParameter();
        CBRegisterParamsData paramsData = new CBRegisterParamsData()
                .setId(entity.getId());

        if (params != null) {
            paramsData.setRegistrationParams(params.getAllParams());
        }

        return new CreditBureauData()
                .setId(entity.getId())
                .setCreditBureauName(entity.getCreditBureauName())
                .setAvailable(entity.isAvailable())
                .setActive(entity.isActive())
                .setCountry(entity.getCountry())
                .setCbRegisterParamsData(paramsData);
    }

    public static CreditBureau fromData(CreditBureauData dto) {
        CreditBureau bureau = new CreditBureau();
        bureau.setId(dto.getId());
        bureau.setCreditBureauName(dto.getCreditBureauName());
        bureau.setAvailable(dto.isAvailable());
        bureau.setActive(dto.isActive());
        bureau.setCountry(dto.getCountry());

        CBRegisterParamsData paramsDto = dto.getCbRegisterParamsData();
        if (paramsDto != null) {
            CBRegisterParams params = new CBRegisterParams();
            params.setRegistrationParams(new HashMap<>(paramsDto.getRegistrationParams()));
            bureau.setCreditBureauParameter(params); // sets both sides
        }

        return bureau;
    }
}
