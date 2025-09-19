package org.mifos.creditbureau.data.creditbureaus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CirculoDeCreditoResponse {
    // folioConsulta -> inquiry/reference ID
    private String folioConsulta;
    // consultas -> inquiries made to the bureau
    private List<Consulta> consultas;
    // creditos -> credit accounts/trade lines
    private List<Credito> creditos;
    // domicilios -> addresses
    private List<Domicilio> domicilios;
    // empleos -> employments
    private List<Empleo> empleos;
    // persona -> person/subject
    private Persona persona;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Consulta {
        // fechaConsulta -> inquiry date (YYYY-MM-DD)
        private String fechaConsulta;
        // claveOtorgante -> grantor/creditor key (code)
        private String claveOtorgante;
        // nombreOtorgante -> grantor/creditor name
        private String nombreOtorgante;
        // direccionOtorgante -> grantor address
        private String direccionOtorgante;
        // telefonoOtorgante -> grantor phone number
        private String telefonoOtorgante;
        // tipoCredito -> credit type
        private String tipoCredito;
        // importeCredito -> credit amount (string as per sample JSON)
        private String importeCredito; // appears as string in sample JSON
        // tipoResponsabilidad -> responsibility type (e.g., I=Individual)
        private String tipoResponsabilidad;
        // claveUnidadMonetaria -> currency code
        private String claveUnidadMonetaria;
        // idDomicilio -> address ID
        private String idDomicilio;
        // servicios -> services/flags
        private String servicios;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Credito {
        // fechaActualizacion -> last update date
        private String fechaActualizacion;
        // registroImpugnado -> disputed record (0/1)
        private Integer registroImpugnado;
        // claveOtorgante -> grantor/creditor key (code)
        private String claveOtorgante;
        // nombreOtorgante -> grantor/creditor name
        private String nombreOtorgante;
        // cuentaActual -> current account number
        private String cuentaActual;
        // tipoResponsabilidad -> responsibility type (e.g., I=Individual)
        private String tipoResponsabilidad;
        // tipoCuenta -> account type
        private String tipoCuenta;
        // tipoCredito -> credit type
        private String tipoCredito;
        // claveUnidadMonetaria -> currency code
        private String claveUnidadMonetaria;
        // valorActivoValuacion -> appraised asset value
        private Integer valorActivoValuacion;
        // numeroPagos -> number of payments
        private Integer numeroPagos;
        // frecuenciaPagos -> payment frequency (e.g., S=Weekly/biweekly per bureau spec)
        private String frecuenciaPagos;
        // montoPagar -> installment amount to pay
        private Integer montoPagar;
        // fechaAperturaCuenta -> account opening date
        private String fechaAperturaCuenta;
        // fechaUltimoPago -> last payment date
        private String fechaUltimoPago;
        // fechaUltimaCompra -> last purchase date
        private String fechaUltimaCompra;
        // fechaCierreCuenta -> account closing date
        private String fechaCierreCuenta;
        // fechaReporte -> report date
        private String fechaReporte;
        // ultimaFechaSaldoCero -> last zero balance date
        private String ultimaFechaSaldoCero;
        // garantia -> collateral/guarantee
        private String garantia;
        // creditoMaximo -> maximum credit/credit limit granted
        private Integer creditoMaximo;
        // saldoActual -> current balance
        private Integer saldoActual;
        // limiteCredito -> credit limit
        private Integer limiteCredito;
        // saldoVencido -> past-due balance
        private Integer saldoVencido;
        // numeroPagosVencidos -> number of past-due payments
        private Integer numeroPagosVencidos;
        // pagoActual -> current payment status/rating
        private String pagoActual;
        // historicoPagos -> payment history
        private String historicoPagos;
        // fechaRecienteHistoricoPagos -> most recent payment history date
        private String fechaRecienteHistoricoPagos;
        // fechaAntiguaHistoricoPagos -> oldest payment history date
        private String fechaAntiguaHistoricoPagos;
        // clavePrevencion -> alert/prevention code
        private String clavePrevencion;
        // totalPagosReportados -> total payments reported
        private Integer totalPagosReportados;
        // peorAtraso -> worst delinquency (months/level)
        private Integer peorAtraso;
        // fechaPeorAtraso -> worst delinquency date (may be empty)
        private String fechaPeorAtraso; // may be empty string
        // saldoVencidoPeorAtraso -> past-due balance at worst delinquency
        private Integer saldoVencidoPeorAtraso;
        // montoUltimoPago -> last payment amount
        private Integer montoUltimoPago;
        // idDomicilio -> address ID linked to this trade line
        private String idDomicilio;
        // servicios -> services/flags
        private String servicios;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Domicilio {
        // direccion -> address/street
        private String direccion;
        // colonia -> neighborhood/colony
        private String colonia;
        // municipio -> municipality
        private String municipio;
        // ciudad -> city
        private String ciudad;
        // estado -> state
        private String estado;
        // codigoPostal -> postal code (ZIP)
        private String codigoPostal;
        // fechaResidencia -> residence start date
        private String fechaResidencia;
        // numeroTelefono -> phone number
        private String numeroTelefono;
        // tipoDomicilio -> address type (e.g., C=Home)
        private String tipoDomicilio;
        // tipoAsentamiento -> settlement type (catalog code)
        private Integer tipoAsentamiento;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Empleo {
        // nombreEmpresa -> employer name
        private String nombreEmpresa;
        // direccion -> address/street
        private String direccion;
        // colonia -> neighborhood/colony
        private String colonia;
        // municipio -> municipality
        private String municipio;
        // ciudad -> city
        private String ciudad;
        // estado -> state
        private String estado;
        // codigoPostal -> postal code (ZIP)
        private String codigoPostal;
        // numeroTelefono -> phone number
        private String numeroTelefono;
        // extension -> phone extension
        private String extension;
        // fax -> fax number
        private String fax;
        // puesto -> position/title
        private String puesto;
        // fechaContratacion -> hiring date
        private String fechaContratacion;
        // claveMoneda -> currency code
        private String claveMoneda;
        // salarioMensual -> monthly salary
        private Integer salarioMensual;
        // fechaUltimoDiaEmpleo -> last day of employment
        private String fechaUltimoDiaEmpleo;
        // fechaVerificacionEmpleo -> employment verification date
        private String fechaVerificacionEmpleo;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Persona {
        // nombres -> first name(s)
        private String nombres;
        // apellidoPaterno -> paternal last name
        private String apellidoPaterno;
        // apellidoMaterno -> maternal last name
        private String apellidoMaterno;
        // apellidoAdicional -> additional surname (can be null)
        private String apellidoAdicional; // can be null
        // fechaNacimiento -> date of birth
        private String fechaNacimiento;
        // rfc -> RFC tax ID (Mexico)
        private String rfc;
        // curp -> CURP national ID (Mexico)
        private String curp;
        // numeroSeguridadSocial -> social security number
        private String numeroSeguridadSocial;
        // nacionalidad -> nationality
        private String nacionalidad;
        // residencia -> residency status (catalog code)
        private Integer residencia;
        // estadoCivil -> marital status
        private String estadoCivil;
        // sexo -> sex/gender
        private String sexo;
        // claveElector -> voter ID
        private String claveElector;
        // numeroDependientes -> number of dependents
        private Integer numeroDependientes;
        // fechaDefuncion -> date of death
        private String fechaDefuncion;
    }
}
