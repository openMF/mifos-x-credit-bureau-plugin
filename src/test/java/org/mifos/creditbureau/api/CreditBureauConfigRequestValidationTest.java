package org.mifos.creditbureau.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.data.registration.CreditBureauConfigRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreditBureauConfigRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validationFailsForBlankFields() {
        CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
                .organisationCreditBureauId(1L)
                .username("")
                .xApiKey("   ")
                .registrationParams(Map.of())
                .build();

        var violations = validator.validate(req);
        assertFalse(violations.isEmpty());
    }

    @Test
    void validationSucceedsForGoodRequest() {
        CreditBureauConfigRequest req = CreditBureauConfigRequest.builder()
                .organisationCreditBureauId(1L)
                .username("user1")
                .xApiKey("apikey123")
                .registrationParams(Map.of("username","user1","x-api-key","apikey123"))
                .build();

        var violations = validator.validate(req);
        assertTrue(violations.isEmpty());
    }
}
