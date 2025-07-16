package org.mifos.creditbureau.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mifos.creditbureau.data.CBRegisterParamsData;
import org.mifos.creditbureau.data.CreditBureauData;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.HashMap;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface CreditBureauMapper {

    CreditBureauMapper INSTANCE = Mappers.getMapper(CreditBureauMapper.class);

    //---Credit Bureau Mappings -----
    //Entity to dto
    @Mapping(source = "creditBureauParameter", target = "cbRegisterParamsData")
    CreditBureauData toCreditBureauData(CreditBureau creditBureau);
    //DTO to entity
    @Mapping(source = "cbRegisterParamsData", target = "creditBureauParameter")
    CreditBureau toCreditBureau(CreditBureauData creditBureauData);

    //---Credit Bureau Param Mappings ---
    //DTO to Entity
    CBRegisterParamsData toCBRegisterParamsData(CBRegisterParams cbRegisterParams);
    //Entity to DTO
    @Mapping(target = "creditBureau", ignore = true)
    CBRegisterParams toCBRegisterParams(CBRegisterParamsData cbRegisterParamsData);

    @Mapping(target = "id", ignore = true) // Never update the primary key
    @Mapping(target = "creditBureauParameter", ignore = true) // Nested objects should be updated manually for clarity
    void updateCreditBureauFromData(CreditBureauData dto, @MappingTarget CreditBureau entity);
}
