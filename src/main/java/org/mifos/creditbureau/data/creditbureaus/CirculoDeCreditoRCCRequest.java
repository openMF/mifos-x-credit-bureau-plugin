package org.mifos.creditbureau.data.creditbureaus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@Builder
@Getter
@AllArgsConstructor
public class CirculoDeCreditoRCCRequest {
    @Builder.Default String primerNombre = "string";
    @Builder.Default String apellidoPaterno = "string";
    @Builder.Default String apellidoMaterno = "string";
    @Builder.Default String fechaNacimiento = "string";
    @Builder.Default String rfc = "string";
    @Builder.Default Domicilio domicilio = Domicilio.builder().build();

    @Data
    @Builder
    @Getter
    @AllArgsConstructor
    public static class Domicilio {
        @Builder.Default String direccion = "string";
        @Builder.Default String colonia = "string";
        @Builder.Default String municipio = "string";
        @Builder.Default String ciudad = "string";
        @Builder.Default String estado = "string";
        @Builder.Default String codigoPostal = "string";
    }
}
