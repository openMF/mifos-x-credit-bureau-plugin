package org.mifos.creditbureau.service;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.mappers.CreditBureauMapper;


import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
//Tests look super messy
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
                // Add individual registration parameters using the singular form method
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
        // Create and save test credit bureaus
        savedBureau = writeService.createCreditBureau(creditBureauData);
        bureauId = savedBureau.getId();

        savedBureau1 = writeService.createCreditBureau(creditBureauData1);
        bureauId1 = savedBureau1.getId();
    }

    @Disabled
    @Test
    @DisplayName("")
    void canThrowExceptionIfCreditBureauNotFound(){}

    @Test
    @DisplayName( "Can create and save a credit bureau, created credit bureau should have also have created ")
    void canCreateAndSaveCreditBureau(){
        //action
        //reads Test Bureau's registration parameter keys
        List<String> keys = readService.getCreditBureauParamKeys(bureauId);

        //reads Test Bureau 1's registration parameter keys
        List<String> keys1 = readService.getCreditBureauParamKeys(bureauId1);

        // assertions
        assertNotNull(savedBureau);
        assertNotNull(savedBureau1);

        // Verify IDs are generated
        assertNotNull(bureauId);
        assertNotNull(bureauId1);

        // Verify properties of the first credit bureau
        assertEquals("Test Bureau", savedBureau.getCreditBureauName());
        assertEquals("United States", savedBureau.getCountry());
        assertFalse(savedBureau.isActive());

        //Verify if the right keys are there for the first credit bureau
        assertEquals(3, keys.size());
        assertTrue(keys.contains("username"));
        assertTrue(keys.contains("password"));
        assertTrue(keys.contains("apiKey"));

        // Verify properties of the second credit bureau
        assertEquals("Test Bureau 1", savedBureau1.getCreditBureauName());
        assertEquals("Mexico", savedBureau1.getCountry());
        assertFalse(savedBureau1.isActive());

        //Verify if the right keys are there for the second credit bureau
        assertEquals(2, keys1.size());
        assertTrue(keys1.contains("publicKey"));
        assertTrue(keys1.contains("privateKey"));

    }



    @Test
    void canRetrieveAllCreditBureaus(){
        // Verify credit bureaus are in the database
        List<CreditBureauData> allBureaus = readService.getAllCreditBureaus();
        assertNotNull(allBureaus);
        assertTrue(allBureaus.size() == 2);
    }

    @Test
    void canRetrieveCreditBureauById(){
        // Verify the credit bureaus can be found in the repository
        assertTrue(creditBureauRepository.findById(bureauId).isPresent());
        assertTrue(creditBureauRepository.findById(bureauId1).isPresent());
    }

    @Disabled
    @Test
    @DisplayName( "User can only view Credit Bureaus with configured param keys")
    void canThrowExceptionIfCreditBureauNotFoundById(){}

    @Disabled
    @Test
    @DisplayName( "User should not be able view Credit Bureau if Param Keys have not been configured")
    void canThrowExceptionIfCreditBureauParamKeysAreEmpty(){}

    @Disabled
    @Test
    @DisplayName("")
    void canThrowExceptionIfCreditBureauParamKeysNotFound(){}

    @Test
    @DisplayName("")
    void canConfigureAndRetrieveCreditBureauConfigurationParamValues(){
        //configures param values
        writeService.configureCreditBureauParamsValues(bureauId,creditBureauDataParams);
        writeService.configureCreditBureauParamsValues(bureauId1,creditBureauData1Params);

        //retrieves the param values
        Map<String, String> values = readService.getRegistrationParamMap(bureauId);
        Map<String, String> values1 = readService.getRegistrationParamMap(bureauId1);

        //tests
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

    @Disabled
    @Test
    @DisplayName("")
    void canThrowExceptionIfConfiguringValuesForNonEmptyValues(){}

    @Disabled
    @Test
    @DisplayName("")
    void canThrowExceptionIfCreditBureauParamValuesNotFound(){}


}
