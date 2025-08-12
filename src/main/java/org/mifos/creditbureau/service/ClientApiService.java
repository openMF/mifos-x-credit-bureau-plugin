package org.mifos.creditbureau.service;

import lombok.AllArgsConstructor;
import org.mifos.creditbureau.data.ClientData;
import org.springframework.stereotype.Service;
import org.mifos.creditbureau.wrappers.FineractClientServiceWrapper;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ClientAddressData;

//TODO: use a builder to populate the dtos
//call this service, and use the populated dtos to construct a dto for a credit bureau
@AllArgsConstructor
@Service
public class ClientApiService {

    private final FineractClientServiceWrapper fineractClientServiceWrapper;


}
