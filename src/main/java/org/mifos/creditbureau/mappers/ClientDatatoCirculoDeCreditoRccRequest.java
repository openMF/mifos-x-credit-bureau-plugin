package org.mifos.creditbureau.mappers;

import org.mifos.creditbureau.data.ClientData;
import org.springframework.stereotype.Component;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRCCRequest;

import java.util.List;

//TODO: For Production
@Component
public class ClientDatatoCirculoDeCreditoRccRequest {

    public CirculoDeCreditoRCCRequest toCirculoDeCreditoRccRequest(ClientData clientData) {

        return CirculoDeCreditoRCCRequest.builder()
                .primerNombre(clientData.getFirstName())
                .apellidoPaterno(clientData.getLastName())
                .fechaNacimiento(convertDateOfBirth(clientData.getDateOfBirth()))
                .rfc(clientData.getExternalId())
                .domicilio(CirculoDeCreditoRCCRequest.Domicilio.builder()
                        .direccion(getSafe(clientData.getStreetAddress(), 0)) // always going to have three indexes?
                        .colonia(getSafe(clientData.getStreetAddress(), 1))
                        .municipio(getSafe(clientData.getStreetAddress(), 2))
                        .ciudad(clientData.getCity())
                        .estado(clientData.getStateProvince())
                        .codigoPostal(clientData.getPostalCode())
                        .build())
                .build();

    }

    private String convertDateOfBirth(List<Integer> dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.size() < 3) return "";
        return String.format("%04d-%02d-%02d", dateOfBirth.get(0), dateOfBirth.get(1), dateOfBirth.get(2));
    }

    private String getSafe(List<String> list, int index) {
        return (list != null && list.size() > index) ? list.get(index) : null;
    }
}
