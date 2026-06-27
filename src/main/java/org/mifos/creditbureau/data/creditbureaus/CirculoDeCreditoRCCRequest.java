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
    @Builder.Default String primerNombre = "";
    @Builder.Default String apellidoPaterno = "";
    @Builder.Default String apellidoMaterno = "NA";
    @Builder.Default String fechaNacimiento = "";
    @Builder.Default String rfc = "";
    @Builder.Default String nacionalidad = "MX";
    @Builder.Default Domicilio domicilio = Domicilio.builder().build();

    @Data
    @Builder
    @Getter
    @AllArgsConstructor
    public static class Domicilio {
        @Builder.Default String direccion = "";
        @Builder.Default String colonia = "";
        @Builder.Default String municipio = "";
        @Builder.Default String ciudad = "";
        @Builder.Default String estado = "";
        @Builder.Default String codigoPostal = "";
    }
}
