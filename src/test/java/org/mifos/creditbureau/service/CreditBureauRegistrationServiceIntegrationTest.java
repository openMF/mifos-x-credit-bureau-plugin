package org.mifos.creditbureau.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.data.registration.CreditBureauData;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadImplService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationWriteService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationWriteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.mifos.creditbureau.data.registration.CBRegisterParamsData;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.mappers.CreditBureauMapper;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({CreditBureauRegistrationWriteServiceImpl.class, CreditBureauRegistrationReadImplService.class,
        EncryptionService.class, org.mifos.creditbureau.config.BouncyCastleConfig.class, CreditBureauMapper.class})
class CreditBureauRegistrationServiceIntegrationTest {

    @Autowired
    private CreditBureauRegistrationWriteService writeService;

    @Autowired
    private CreditBureauRegistrationReadService readService;

    @Autowired
    private CreditBureauRepository creditBureauRepository;

    @Autowired
    private CreditBureauMapper mapper;

    @Autowired
    private EncryptionService encryptionService;

    private CreditBureauData creditBureauData;

    private CreditBureauData creditBureauData1;

    private CBRegisterParamsData creditBureauDataParams;

    private CBRegisterParamsData creditBureauData1Params;

    private CreditBureau savedBureau;

    private CreditBureau savedBureau1;

    Long bureauId;

    Long bureauId1;

    @PostConstruct
    public void init() {
        creditBureauData = CreditBureauData.builder()
                .creditBureauName("Test Bureau")
                .country("United States")
                .active(false)
                .registrationParamKeys(new HashSet<>(Arrays.asList("username", "password", "apiKey")))
                .build();

        creditBureauDataParams = CBRegisterParamsData.builder()
                .registrationParam("username", "testUser")
                .registrationParam("password", "testPassword")
                .registrationParam("apiKey", "1234567890123456")
                .build();


        creditBureauData1 = CreditBureauData.builder()
                .creditBureauName("Test Bureau 1")
                .country("Mexico")
                .active(false)
                .registrationParamKeys(new HashSet<>(Arrays.asList("publicKey", "privateKey")))
                .build();

        creditBureauData1Params = CBRegisterParamsData.builder()
                .registrationParam("publicKey", "09876543210987")
                .registrationParam("privateKey", "abcdefghijk23")
                .build();
    }

    @BeforeEach
    void setUp() {
        savedBureau = writeService.createCreditBureau(creditBureauData);
        bureauId = savedBureau.getId();

        savedBureau1 = writeService.createCreditBureau(creditBureauData1);
        bureauId1 = savedBureau1.getId();
    }

    @Test
    @DisplayName("Retrieving params for non-existent bureau throws EntityNotFoundException")
    void canThrowExceptionIfCreditBureauNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> readService.getCreditBureauParams(999L));
    }

    @Test
    @DisplayName("Can create and save a credit bureau, created credit bureau should have also have created ")
    void canCreateAndSaveCreditBureau() {
        List<String> keys = readService.getCreditBureauParamKeys(bureauId);
        List<String> keys1 = readService.getCreditBureauParamKeys(bureauId1);

        assertNotNull(savedBureau);
        assertNotNull(savedBureau1);

        assertNotNull(bureauId);
        assertNotNull(bureauId1);

        assertEquals("Test Bureau", savedBureau.getCreditBureauName());
        assertEquals("United States", savedBureau.getCountry());
        assertFalse(savedBureau.isActive());

        assertEquals(3, keys.size());
        assertTrue(keys.contains("username"));
        assertTrue(keys.contains("password"));
        assertTrue(keys.contains("apiKey"));

        assertEquals("Test Bureau 1", savedBureau1.getCreditBureauName());
        assertEquals("Mexico", savedBureau1.getCountry());
        assertFalse(savedBureau1.isActive());

        assertEquals(2, keys1.size());
        assertTrue(keys1.contains("publicKey"));
        assertTrue(keys1.contains("privateKey"));
    }

    @Test
    void canRetrieveAllCreditBureaus() {
        List<CreditBureauData> allBureaus = readService.getAllCreditBureaus();
        assertNotNull(allBureaus);
        assertTrue(allBureaus.size() == 2);
    }

    @Test
    void canRetrieveCreditBureauById() {
        assertTrue(creditBureauRepository.findById(bureauId).isPresent());
        assertTrue(creditBureauRepository.findById(bureauId1).isPresent());
    }

    @Test
    @DisplayName("Retrieving param keys for non-existent bureau throws EntityNotFoundException")
    void canThrowExceptionIfCreditBureauNotFoundById() {
        assertThrows(EntityNotFoundException.class,
                () -> readService.getCreditBureauParamKeys(999L));
    }

    @Test
    @DisplayName("Retrieving param values for non-existent bureau throws EntityNotFoundException")
    void canThrowExceptionIfCreditBureauParamKeysAreEmpty() {
        assertThrows(EntityNotFoundException.class,
                () -> readService.getRegistrationParamMap(999L));
    }

    @Test
    @DisplayName("getCreditBureauParams for non-existent ID throws EntityNotFoundException")
    void canThrowExceptionIfCreditBureauParamKeysNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> readService.getCreditBureauParams(999L));
    }

    @Test
    @DisplayName("Can configure and then retrieve decrypted credit bureau param values")
    void canConfigureAndRetrieveCreditBureauConfigurationParamValues() {
        writeService.configureCreditBureauParamsValues(bureauId, creditBureauDataParams);
        writeService.configureCreditBureauParamsValues(bureauId1, creditBureauData1Params);

        Map<String, String> values = readService.getRegistrationParamMap(bureauId);
        Map<String, String> values1 = readService.getRegistrationParamMap(bureauId1);

        assertNotNull(values);
        assertNotNull(values1);
        assertEquals(3, values.size());
        assertEquals(2, values1.size());
        assertEquals("testUser", values.get("username"));
        assertEquals("testPassword", values.get("password"));
        assertEquals("1234567890123456", values.get("apiKey"));
        assertEquals("09876543210987", values1.get("publicKey"));
        assertEquals("abcdefghijk23", values1.get("privateKey"));
    }

    @Test
    @DisplayName("Configuring values for already-configured bureau throws IllegalStateException")
    void canThrowExceptionIfConfiguringValuesForNonEmptyValues() {
        writeService.configureCreditBureauParamsValues(bureauId, creditBureauDataParams);

        assertThrows(IllegalStateException.class,
                () -> writeService.configureCreditBureauParamsValues(bureauId, creditBureauDataParams));
    }

    @Test
    @DisplayName("Configuring values for non-existent bureau throws EntityNotFoundException")
    void canThrowExceptionIfCreditBureauParamValuesNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> writeService.configureCreditBureauParamsValues(999L, creditBureauDataParams));
    }
}
