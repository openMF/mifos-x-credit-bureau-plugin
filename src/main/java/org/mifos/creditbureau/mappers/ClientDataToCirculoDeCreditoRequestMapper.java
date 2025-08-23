package org.mifos.creditbureau.mappers;

import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRequest;
import org.mifos.creditbureau.data.ClientData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ClientDataToCirculoDeCreditoRequestMapper {

    public CirculoDeCreditoRequest toCirculoDeCreditoRequest(ClientData clientData) {
        return CirculoDeCreditoRequest.builder()
                .folio(clientData.getId())
                .persona(
                        CirculoDeCreditoRequest.Persona.builder()
                                .nombres(clientData.getFirstName())
                                .apellidoPaterno(clientData.getLastName())
                                .fechaNacimiento(convertDateOfBirth(clientData.getDateOfBirth()))
                                .RFC(clientData.getExternalId())
                                .nacionalidad(clientData.getNationality())
                                .sexo(clientData.getGender())
                                .estadoCivil(clientData.getMaritalStatus())
                                .domicilio(
                                        CirculoDeCreditoRequest.Domicilio.builder()
                                                .direccion(getSafe(clientData.getStreetAddress(), 0))
                                                .coloniaPoblacion(getSafe(clientData.getStreetAddress(), 1))
                                                .ciudad(clientData.getCity())
                                                .estado(clientData.getStateProvince())
                                                .CP(clientData.getPostalCode())
                                                .numeroTelefono(clientData.getPhoneNumber())
                                                .build()
                                )
                                .build()
                )
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