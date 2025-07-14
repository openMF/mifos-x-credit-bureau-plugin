package org.mifos.creditbureau.service;

import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;

import java.util.List;

public interface CreditBureauRegistrationReadService {

    CBRegisterParamsData getCreditBureauParams(Long creditBureauId);

    List<CreditBureauData> getAllCreditBureaus();

}
