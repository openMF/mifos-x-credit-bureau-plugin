package org.mifos.creditbureau.service;

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


import java.util.List;
import java.util.Map;

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

    @Test
    void canCreateCreditBureau(){
        //Setup
        CreditBureauData creditBureauData = CreditBureauData.builder()
                .creditBureauName("Test Bureau")
                .country("United States")
                .available(true)
                .active(true)
                .build();

        //Test Create CreditBureau
        CreditBureau savedBureau = writeService.createCreditBureau(creditBureauData);
        Long bureauId = savedBureau.getId();

        //Test get all Credit Bureau
        List<CreditBureauData> creditBureauDataList = readService.getAllCreditBureaus();

        //Test if CB Registration Param, empty hashmap is created
        CBRegisterParamsData cbRegisterParamsData = readService.getCreditBureauParams(bureauId);


        //expected
        assertNotNull(creditBureauDataList);
        assertEquals(1, creditBureauDataList.size());
        assertEquals("Test Bureau", creditBureauDataList.getFirst().getCreditBureauName());
        assertEquals("United States", creditBureauDataList.getFirst().getCountry());

        //expected for CB Registration Param
        assertNotNull(cbRegisterParamsData);
        assertEquals(0, cbRegisterParamsData.getRegistrationParams().size());


    }

    @Test
    void shouldWriteAndReadCreditBureauParams() {
        // Given
        // Create the actual JPA entities that will be saved to the database.
        CreditBureau initialBureau = new CreditBureau();
        initialBureau.setCreditBureauName("Test Bureau");
        initialBureau.setCountry("United States");
        initialBureau.setActive(true);
        initialBureau.setAvailable(true);

        CBRegisterParams initialParams = new CBRegisterParams();

        initialBureau.setCreditBureauParameter(initialParams);
        initialParams.setCreditBureau(initialBureau);

        CreditBureau savedBureau = creditBureauRepository.saveAndFlush(initialBureau);
        Long bureauId = savedBureau.getId(); // Use the real, generated ID

        // action
        CBRegisterParamsData updateDto = CBRegisterParamsData.builder()
                .id(initialBureau.getId())
                .registrationParam("username", "testuser")
                .registrationParam("password", "testpass")
                .registrationParam("endpoint", "http://localhost:8080")
                .build();

        writeService.configureCreditBureauParams(updateDto);

        // expected
        // to Read the data back from the database to verify the changes.
        List<String> result = readService.getCreditBureauParamKeys(bureauId); //return a list of keys
        Map<String, String> resultMap = readService.getRegistrationParamMap(bureauId);

        // Assert that the data was correctly updated.
        assertNotNull(result);
        assertNotNull(resultMap);
        assertEquals(3, result.size());
        assertEquals("testuser", resultMap.get("username"));
        assertEquals("testpass", resultMap.get("password"));
        assertEquals("http://localhost:8080", resultMap.get("endpoint"));
    }


}
