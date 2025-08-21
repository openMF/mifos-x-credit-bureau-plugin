package org.mifos.creditbureau.data.creditbureaus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

//Reformat clientData for the destination credit bureau.
@Getter
@Builder
@AllArgsConstructor
public class CirculoDeCreditoRequest {
    private Long folio;
    @Builder.Default
    private Persona persona = Persona.builder().build();

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Persona {
        @Builder.Default private String nombres = "string";
        @Builder.Default private String segundoNombre = "string";
        @Builder.Default private String apellidoPaterno = "string";
        @Builder.Default private String apellidoMaterno = "string";
        @Builder.Default private String apellidoAdicional = "string";
        @Builder.Default private String fechaNacimiento = "string";
        @Builder.Default private String RFC = "string";
        @Builder.Default private String CURP = "string";
        @Builder.Default private String nacionalidad = "string";
        @Builder.Default private String residencia = "string";
        @Builder.Default private String estadoCivil = "string";
        @Builder.Default private String sexo = "string";
        @Builder.Default private String claveElectorIFE = "string";
        @Builder.Default private String numeroDependientes = "string";
        @Builder.Default private String fechaDefuncion = "string";
        @Builder.Default private Domicilio domicilio = Domicilio.builder().build();

    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Domicilio {
        @Builder.Default private String direccion = "string";
        @Builder.Default private String coloniaPoblacion = "string";
        @Builder.Default private String delegacionMunicipio = "string";
        @Builder.Default private String ciudad = "string";
        @Builder.Default private String estado = "string";
        @Builder.Default private String CP = "string";
        @Builder.Default private String fechaResidencia = "string";
        @Builder.Default private String numeroTelefono = "string";
        @Builder.Default private String tipoDomicilio = "string";
        @Builder.Default private Integer tipoAsentamiento = 0;
        @Builder.Default private String fechaRegistroDomicilio = "string";
        @Builder.Default private Integer tipoAltaDomicilio = 0;
        @Builder.Default private String idDomicilio = "string";

    }
}
