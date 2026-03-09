package org.mifos.creditbureau.service.registration;

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

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CreditBureauRegistrationReadImplService implements CreditBureauRegistrationReadService {

    private final CreditBureauRepository creditBureauRepository;
    private final CBRegisterParamRepository cbRegisterParamRepository;
    private final CreditBureauMapper creditBureauMapper;
    private final EncryptionService encryptionService;


    @Override
    @Transactional(readOnly = true) // Use a read-only transaction for performance
    public CBRegisterParamsData getCreditBureauParams(Long creditBureauId) {
        return cbRegisterParamRepository.findById(creditBureauId)
                .map(creditBureauMapper::toCBRegisterParamsData)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCreditBureauParamKeys(Long creditBureauId) {
        return cbRegisterParamRepository.findById(creditBureauId)
                .map(params -> new ArrayList<>(params.getRegistrationParams().keySet()))
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getRegistrationParamMap(Long creditBureauId) {
        Optional<CBRegisterParams> cbParamOptional = cbRegisterParamRepository.findById(creditBureauId);

        return cbParamOptional
                .map(cbParams ->{
                    Map<String, String> encryptedMap = cbParams.getRegistrationParams();
                    Map<String, String> decryptedMap = new HashMap<>();

                    encryptedMap.forEach((key, value) -> {
                        try{
                            String decryptedValue = encryptionService.decrypt(value);
                            decryptedMap.put(key, decryptedValue);
                        }catch (Exception e){
                            throw new RuntimeException("Error decrypting parameter: " + e);
                        }
                    });
                    return decryptedMap;
                })
                .orElse(Collections.emptyMap());


    }

    @Override
    @Transactional(readOnly = true) // Use a read-only transaction for performance
    public List<CreditBureauData> getAllCreditBureaus() {
        List<CreditBureau> creditBureaus = creditBureauRepository.findAll();

        return creditBureaus.stream()
                .map(creditBureauMapper::toCreditBureauData)
                .collect(Collectors.toList());
    }




}