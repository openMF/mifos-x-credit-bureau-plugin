package org.mifos.creditbureau.service;

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

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CreditBureauRegistrationReadImplService implements CreditBureauRegistrationReadService {

    // Inject the JPA repository for CreditBureau entities
    private final CreditBureauRepository creditBureauRepository;
    private final CBRegisterParamRepository cbRegisterParamRepository;
    // Inject the MapStruct mapper for converting between entities and DTOs
    private final CreditBureauMapper creditBureauMapper;


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
                .map(CBRegisterParams::getRegistrationParams)
                .orElse(Collections.emptyMap());

    }

    @Override
    @Transactional(readOnly = true) // Use a read-only transaction for performance
    public List<CreditBureauData> getAllCreditBureaus() {
        // Fetch all CreditBureau entities from the database.
        List<CreditBureau> creditBureaus = creditBureauRepository.findAll();

        // Use Java Streams to map each CreditBureau entity to its corresponding DTO.
        return creditBureaus.stream()
                .map(creditBureauMapper::toCreditBureauData)
                .collect(Collectors.toList());
    }




}