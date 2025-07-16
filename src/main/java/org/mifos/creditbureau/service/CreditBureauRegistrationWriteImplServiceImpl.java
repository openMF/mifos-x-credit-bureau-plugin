package org.mifos.creditbureau.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.mappers.CreditBureauMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class CreditBureauRegistrationWriteImplServiceImpl implements CreditBureauRegistrationWriteService {

    private final CreditBureauMapper creditBureauMapper;
    private final CBRegisterParamRepository CBRegisterParamRepository;


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
