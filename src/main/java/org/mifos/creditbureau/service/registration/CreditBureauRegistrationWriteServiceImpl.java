package org.mifos.creditbureau.service.registration;

import jakarta.persistence.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.registration.CBRegisterParamsData;
import org.mifos.creditbureau.data.registration.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.mappers.CreditBureauMapper;
import org.mifos.creditbureau.service.EncryptionService;
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
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public CreditBureau createCreditBureau(CreditBureauData creditBureauData) {
        //Create a Credit Bureau with basic info
        CreditBureau creditBureau = creditBureauMapper.toCreditBureau(creditBureauData);

        //Create CBRegisterParams with empty values for all keys
        CBRegisterParams cbRegisterParams = new CBRegisterParams();

        //Initialize the registration params with keys from creditBureauData
        Map<String, String> registrationParams = new HashMap<>();
        for(String key : creditBureauData.getRegistrationParamKeys()){
            registrationParams.put(key, "");
        }
        cbRegisterParams.setRegistrationParams(new HashMap<>(registrationParams)); // Create a new HashMap to ensure JPA detects the change

        // Save CreditBureau first to generate its ID
        creditBureau = creditBureauRepository.saveAndFlush(creditBureau);

        // Now that creditBureau has an ID, set it on cbRegisterParams
        cbRegisterParams.setCreditBureau(creditBureau);
        // Also set the parameter on the creditBureau for consistency and cascade if needed
        creditBureau.setCreditBureauParameter(cbRegisterParams);

        // Save CBRegisterParams, which is the owning side and uses @MapsId
        CBRegisterParamRepository.save(cbRegisterParams);

        return creditBureau;
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

        valueMap.forEach((key, value) ->{
            if(existingMap.containsKey(key)) {
                try {
                    existingMap.put(key, encryptionService.encrypt(value));
                } catch (Exception e) {
                    throw new RuntimeException("Error encrypting parameter: " + e);
                }
            }
        });
//        for(Map.Entry<String, String> entry : valueMap.entrySet()){
//            String key = entry.getKey();
//            if(existingMap.containsKey(key)){
//                existingMap.put(key, entry.getValue());
//            }
//        }

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
