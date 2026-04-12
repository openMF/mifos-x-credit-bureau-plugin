package org.mifos.creditbureau.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.mifos.creditbureau.data.registration.CreditBureauConfigRequest;

import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CreditBureauConfigRequestValidationTest {

    @Autowired
    private Validator validator;

    private CreditBureauConfigRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = CreditBureauConfigRequest.builder()
                .organisationCreditBureauId(1L)
                .username("valid_user")
                .xApiKey("valid_api_key_12345")
                .certificate(null)
                .registrationParams(new HashMap<>())
                .build();
    }

    @Test
    void validRequestPassesValidation() {
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void validationFailsForNullOrganisationCreditBureauId() {
        validRequest.setOrganisationCreditBureauId(null);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("organisationCreditBureauId")));
    }

    @Test
    void validationFailsForBlankUsername() {
        validRequest.setUsername("");
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username")));
    }

    @Test
    void validationFailsForBlankXApiKey() {
        validRequest.setXApiKey("");
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("xApiKey")));
    }

    @Test
    void validationFailsForOversizeUsername() {
        String oversizeUsername = "a".repeat(256);
        validRequest.setUsername(oversizeUsername);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("username") && 
                        v.getMessage().contains("255")));
    }

    @Test
    void validationFailsForOversizeXApiKey() {
        String oversizeKey = "k".repeat(1025);
        validRequest.setXApiKey(oversizeKey);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("xApiKey") && 
                        v.getMessage().contains("1024")));
    }

    @Test
    void validationFailsForOversizeCertificate() {
        String oversizeCert = "c".repeat(4097);
        validRequest.setCertificate(oversizeCert);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("certificate") && 
                        v.getMessage().contains("4096")));
    }

    @Test
    void validationFailsForNullRegistrationParams() {
        validRequest.setRegistrationParams(null);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("registrationParams")));
    }

    @Test
    void certificateIsOptionalAndAcceptsNull() {
        validRequest.setCertificate(null);
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }

    @Test
    void certificateIsOptionalAndAcceptsValue() {
        validRequest.setCertificate("-----BEGIN CERTIFICATE-----\nMIID...");
        Set<ConstraintViolation<CreditBureauConfigRequest>> violations = validator.validate(validRequest);
        assertTrue(violations.isEmpty());
    }
}
