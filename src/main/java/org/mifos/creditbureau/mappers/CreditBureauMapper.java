package org.mifos.creditbureau.mappers;

import org.mapstruct.Mapper;
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
    CreditBureauData toCreditBureauData(CreditBureau creditBureau);
    //DTO to entity
    CreditBureau toCreditBureau(CreditBureauData creditBureauData);

    //---Credit Bureau Param Mappings ---
    //DTO to Entity
    CBRegisterParamsData toCBRegisterParamsData(CBRegisterParams cbRegisterParams);
    //Entity to DTO
    CBRegisterParams toCBRegisterParams(CBRegisterParamsData cbRegisterParamsData);
}
