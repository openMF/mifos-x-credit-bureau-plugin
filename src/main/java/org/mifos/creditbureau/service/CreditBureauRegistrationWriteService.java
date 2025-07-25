package org.mifos.creditbureau.service;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;


public interface CreditBureauRegistrationWriteService {

    //create a credit bureau
    CreditBureau createCreditBureau(CreditBureauData creditBureauData);

    //createCreditBureauConfiguration
    CBRegisterParams configureCreditBureauParams(CBRegisterParamsData cbRegisterParamsData);

    //createCreditBureauRegistrationParam
    void updateCreditBureau();

    //updateCreditBureauRegistrationParam
    void updateCreditBureauParams();


}
