package org.mifos.creditbureau.service;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.mifos.creditbureau.domain.CreditBureau;


public interface CreditBureauRegistrationWriteService {

    //createCreditBureauConfiguration
    CreditBureau createCreditBureau();

    //createCreditBureauRegistrationParam
    void updateCreditBureau();

    //updateCreditBureauRegistrationParam
    void updateCreditBureauParams();


}
