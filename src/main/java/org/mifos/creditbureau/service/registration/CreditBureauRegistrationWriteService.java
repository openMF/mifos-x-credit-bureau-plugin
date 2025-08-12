package org.mifos.creditbureau.service.registration;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;


public interface CreditBureauRegistrationWriteService {

    //create a credit bureau
    CreditBureau createCreditBureau(CreditBureauData creditBureauData);

    //add param keys to Credit Bureau
    void configureCreditBureauParamsKeys(Long id, CBRegisterParamsData cbRegisterParamsData);

    //createCreditBureauConfiguration
    CBRegisterParams configureCreditBureauParamsValues(Long id,CBRegisterParamsData cbRegisterParamsData);

    //createCreditBureauRegistrationParam
    void updateCreditBureau();

    //updateCreditBureauRegistrationParam
    void updateCreditBureauParams();


}
