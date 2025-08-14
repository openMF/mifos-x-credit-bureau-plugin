package org.mifos.creditbureau.data.creditbureaus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

//Reformat clientData for the destination credit bureau.
@Getter
@Builder
@AllArgsConstructor
public class CirculoDeCreditoRequest {
    private String primerNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String fechaNacimiento;
    private String RFC;
    private String nacionalidad;
    private Address domicilio;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Address {
        private String direccion;
        private String coloniaPoblacion;
        private String delegacionMunicipio;
        private String ciudad;
        private String estado;
        private String postalCode;
        private String residenceDate;
        private String phoneNumber;
        private String addressType;
        private String settlementType;
    }
}
