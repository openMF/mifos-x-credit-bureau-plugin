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

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Service
public class CreditBureauRegistrationWriteServiceImpl implements CreditBureauRegistrationWriteService {

    private final CreditBureauMapper creditBureauMapper;
    private final CBRegisterParamRepository CBRegisterParamRepository;
    private final CreditBureauRepository creditBureauRepository;

    @Override
    @Transactional
    public CreditBureau createCreditBureau(CreditBureauData creditBureauData) {
        //Create a Credit Bureau with basic info
        CreditBureau creditBureau = creditBureauMapper.toCreditBureau(creditBureauData);
        creditBureau = creditBureauRepository.saveAndFlush(creditBureau);

        //Create CBRegisterParams with empty values for all keys
        CBRegisterParams cbRegisterParams = new CBRegisterParams();
        creditBureau.setCreditBureauParameter(cbRegisterParams);
        cbRegisterParams.setCreditBureau(creditBureau);

        //Initialize the registration params with keys from creditBureauData
        Map<String, String> registrationParams = new HashMap<>();
        for(String key : creditBureauData.getRegistrationParamKeys()){
            registrationParams.put(key, null);
        }
        cbRegisterParams.setRegistrationParams(registrationParams);

        return CBRegisterParamRepository.save(cbRegisterParams).getCreditBureau();
    }

    @Override
    @Transactional
    /*
    - Configures the values in the hashmap registerparams
    - can only be done if the keys already exist. so if the keys in the data match the keys in the database/entity
    * */
    public CBRegisterParams configureCreditBureauParamsValues(Long bureauId, CBRegisterParamsData cbRegisterParamsData) { //takes a dto
        CBRegisterParams existingParams = CBRegisterParamRepository.findById(bureauId)
                .orElseThrow(() -> new EntityNotFoundException("CBRegisterParams not found with id: " + bureauId));

        Map<String, String> existingMap = existingParams.getRegistrationParams();
        Map<String, String> valueMap = cbRegisterParamsData.getRegistrationParams();

        for(Map.Entry<String, String> entry : valueMap.entrySet()){
            String key = entry.getKey();
            if(existingMap.containsKey(key)){
                existingMap.put(key, entry.getValue());
            }
        }

        return CBRegisterParamRepository.save(existingParams);
    }

    @Override
    //should not be exposed to the controller only internally
    public void configureCreditBureauParamsKeys(Long bureauId, CBRegisterParamsData cbRegisterParamsData) {

        CBRegisterParams existingParams = CBRegisterParamRepository.findById(bureauId)
                .orElseThrow(() -> new EntityNotFoundException("CBRegisterParams not found with id: " + bureauId));

        Map<String, String> existingMap = existingParams.getRegistrationParams();
        Map<String, String> dtoMap = cbRegisterParamsData.getRegistrationParams();

        // Add keys from the DTO to the existing map with empty values
        // Only add keys that don't already exist in the map
        for (String key : dtoMap.keySet()) {
            if (!existingMap.containsKey(key)) {
                existingMap.put(key, "");
            }
        }

        CBRegisterParamRepository.save(existingParams);
    }

    @Override
    public void updateCreditBureau() {

    }

    @Override
    public void updateCreditBureauParams() {

    }
}
