package org.mifos.creditbureau.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.mappers.CreditBureauMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class CreditBureauRegistrationWriteServiceImpl implements CreditBureauRegistrationWriteService {

    private final CreditBureauMapper creditBureauMapper;
    private final CBRegisterParamRepository CBRegisterParamRepository;
    private final CreditBureauRepository creditBureauRepository;

    @Override
    @Transactional
    public CreditBureau createCreditBureau(CreditBureauData creditBureauData) {
        CreditBureau creditBureau = creditBureauMapper.toCreditBureau(creditBureauData);
        creditBureau = creditBureauRepository.save(creditBureau);
        CBRegisterParams cbRegisterParams = new CBRegisterParams();
        creditBureau.setCreditBureauParameter(cbRegisterParams);
        cbRegisterParams.setCreditBureau(creditBureau);
        return CBRegisterParamRepository.save(cbRegisterParams).getCreditBureau();
    }

    @Override
    @Transactional
    public CBRegisterParams configureCreditBureauParams(CBRegisterParamsData cbRegisterParamsData) {
        Long bureauId = cbRegisterParamsData.getId();

        CBRegisterParams existingParams = CBRegisterParamRepository.findById(bureauId)
                .orElseThrow(() -> new EntityNotFoundException("CBRegisterParams not found with id: " + bureauId));

        existingParams.getRegistrationParams().clear();
        existingParams.getRegistrationParams().putAll(cbRegisterParamsData.getRegistrationParams());
        return CBRegisterParamRepository.save(existingParams);
    }

    @Override
    public void updateCreditBureau() {

    }

    @Override
    public void updateCreditBureauParams() {

    }
}
