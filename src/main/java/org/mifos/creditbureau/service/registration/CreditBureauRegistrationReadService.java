package org.mifos.creditbureau.service.registration;

import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;

import java.util.List;
import java.util.Map;

public interface CreditBureauRegistrationReadService {

    CBRegisterParamsData getCreditBureauParams(Long creditBureauId);

    List<CreditBureauData> getAllCreditBureaus();

    List<String> getCreditBureauParamKeys(Long creditBureauId);

    Map<String, String> getRegistrationParamMap(Long creditBureauId);


}
