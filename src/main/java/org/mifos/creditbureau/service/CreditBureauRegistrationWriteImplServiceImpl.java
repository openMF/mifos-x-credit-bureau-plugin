package org.mifos.creditbureau.service;

import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.mappers.CreditBureauMapper;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class CreditBureauRegistrationWriteImplServiceImpl implements CreditBureauRegistrationWriteService {

    private final CreditBureauMapper creditBureauMapper;
    private final CBRegisterParamRepository CBRegisterParamRepository;


    @Override
    public CBRegisterParams configureCreditBureauParams(CBRegisterParamsData cbRegisterParamsData) {
        CBRegisterParams creditBureauParam = creditBureauMapper.toCBRegisterParams(cbRegisterParamsData);
        return CBRegisterParamRepository.save(creditBureauParam);
    }

    @Override
    public void updateCreditBureau() {

    }

    @Override
    public void updateCreditBureauParams() {

    }
}
