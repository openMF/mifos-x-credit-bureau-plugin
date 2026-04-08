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
public class CreditBureauRegistrationWriteServiceImpl
        implements CreditBureauRegistrationWriteService {

    private final CreditBureauMapper creditBureauMapper;
    private final CBRegisterParamRepository CBRegisterParamRepository;
    private final CreditBureauRepository creditBureauRepository;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public CreditBureau createCreditBureau(CreditBureauData creditBureauData) {

        // Validate input
        if (creditBureauData == null) {
            throw new IllegalArgumentException(
                    "CreditBureauData must not be null"
            );
        }

        CreditBureau creditBureau =
                creditBureauMapper.toCreditBureau(creditBureauData);
        CBRegisterParams cbRegisterParams = new CBRegisterParams();

        // Initialize the registration params with keys from creditBureauData
        Map<String, String> registrationParams = new HashMap<>();

        // FIX: Added null check before iterating registrationParamKeys
        // Previously caused NullPointerException when keys not provided
        if (creditBureauData.getRegistrationParamKeys() != null) {
            for (String key : creditBureauData.getRegistrationParamKeys()) {
                if (key != null && !key.isBlank()) {
                    registrationParams.put(key, "");
                }
            }
        }

        // Create a new HashMap to ensure JPA detects the change
        cbRegisterParams.setRegistrationParams(
                new HashMap<>(registrationParams)
        );

        // Save CreditBureau first to generate its ID
        creditBureau = creditBureauRepository.saveAndFlush(creditBureau);

        cbRegisterParams.setCreditBureau(creditBureau);
        creditBureau.setCreditBureauParameter(cbRegisterParams);

        CBRegisterParamRepository.save(cbRegisterParams);

        return creditBureau;
    }

    @Override
    @Transactional
    public CBRegisterParams configureCreditBureauParamsValues(
            Long bureauId,
            CBRegisterParamsData cbRegisterParamsData) {

        // Validate input
        if (bureauId == null) {
            throw new IllegalArgumentException(
                    "Bureau ID must not be null"
            );
        }
        if (cbRegisterParamsData == null) {
            throw new IllegalArgumentException(
                    "CBRegisterParamsData must not be null"
            );
        }

        CBRegisterParams existingParams = CBRegisterParamRepository
                .findById(bureauId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "CBRegisterParams not found with id: " + bureauId
                ));

        Map<String, String> existingMap =
                existingParams.getRegistrationParams();
        Map<String, String> valueMap =
                cbRegisterParamsData.getRegistrationParams();

        // FIX: Added null check on valueMap before iterating
        if (valueMap != null && existingMap != null) {
            boolean hasConfiguredValues = existingMap.values().stream()
                    .anyMatch(v -> v != null && !v.isEmpty());
            if (hasConfiguredValues) {
                throw new IllegalStateException(
                        "Credit bureau params already configured for bureau: " + bureauId
                                + ". Use update endpoint to modify existing values.");
            }

            valueMap.forEach((key, value) -> {
                if (key != null && existingMap.containsKey(key)) {
                    try {
                        existingMap.put(key, encryptionService.encrypt(value));
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Error encrypting parameter for key: "
                                        + key + " — " + e.getMessage()
                        );
                    }
                }
            });
        }

        return CBRegisterParamRepository.save(existingParams);
    }

    @Override
    public void configureCreditBureauParamsKeys(
            Long bureauId,
            CBRegisterParamsData cbRegisterParamsData) {

        // Validate input
        if (bureauId == null) {
            throw new IllegalArgumentException(
                    "Bureau ID must not be null"
            );
        }
        if (cbRegisterParamsData == null) {
            throw new IllegalArgumentException(
                    "CBRegisterParamsData must not be null"
            );
        }

        CBRegisterParams existingParams = CBRegisterParamRepository
                .findById(bureauId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "CBRegisterParams not found with id: " + bureauId
                ));

        Map<String, String> existingMap =
                existingParams.getRegistrationParams();
        Map<String, String> dtoMap =
                cbRegisterParamsData.getRegistrationParams();

        // FIX: Added null checks on both maps before iterating
        if (dtoMap != null && existingMap != null) {
            for (String key : dtoMap.keySet()) {
                if (key != null && !existingMap.containsKey(key)) {
                    existingMap.put(key, "");
                }
            }
        }

        CBRegisterParamRepository.save(existingParams);
    }

    @Override
    public void updateCreditBureau() {
        // TODO: Implement credit bureau update logic
        throw new UnsupportedOperationException(
                "updateCreditBureau not yet implemented"
        );
    }

    @Override
    public void updateCreditBureauParams() {
        // TODO: Implement credit bureau params update logic
        throw new UnsupportedOperationException(
                "updateCreditBureauParams not yet implemented"
        );
    }
}