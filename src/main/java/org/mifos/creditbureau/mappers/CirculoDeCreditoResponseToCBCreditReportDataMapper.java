package org.mifos.creditbureau.mappers;

import org.mifos.creditbureau.data.CBCreditReportData;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoResponse;
import org.mapstruct.Mapper;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class CirculoDeCreditoResponseToCBCreditReportDataMapper {

    public CBCreditReportData toCBCreditReportData(CirculoDeCreditoResponse src) {
        if (src == null) {
            return CBCreditReportData.builder().build();
        }

        CBCreditReportData.CreditAccount defaultAccount = CBCreditReportData.CreditAccount.builder().build();

        // Person
        CBCreditReportData.Person person = mapPerson(src.getPersona());

        // Addresses
        List<CBCreditReportData.Address> addresses = safeList(src.getDomicilios()).stream()
                .map(this::mapAddress)
                .collect(Collectors.toList());

        // Employments
        List<CBCreditReportData.Employment> employments = safeList(src.getEmpleos()).stream()
                .map(this::mapEmployment)
                .collect(Collectors.toList());

        // Credit Accounts
        List<CBCreditReportData.CreditAccount> accounts = safeList(src.getCreditos()).stream()
                .map(this::mapCreditAccount)
                .collect(Collectors.toList());

        // Inquiries
        List<CBCreditReportData.Inquiry> inquiries = safeList(src.getConsultas()).stream()
                .map(this::mapInquiry)
                .collect(Collectors.toList());

        return CBCreditReportData.builder()
                .reportId(nvl(src.getFolioConsulta(), ""))
                .inquiryId(nvl(src.getFolioConsulta(), ""))
                .bureauName("Circulo de Credito")
                .reportDate(findReportDate(src))
                .country("MX")
                .currency(guessCurrency(src))
                .person(person)
                .addresses(addresses)
                .employments(employments)
                .creditAccounts(accounts.isEmpty() ? List.of(defaultAccount) : accounts)
                .inquiries(inquiries)
                .publicRecords(List.of())
                .scores(List.of())
                .build();
    }

    private CBCreditReportData.Person mapPerson(CirculoDeCreditoResponse.Persona p) {
        if (p == null) {
            return CBCreditReportData.Person.builder().build();
        }
        return CBCreditReportData.Person.builder()
                .firstName(nvl(p.getNombres(), ""))
                .lastName(nvl(p.getApellidoPaterno(), ""))
                .additionalLastName(nvl(p.getApellidoMaterno(), ""))
                .middleName(nvl(p.getApellidoAdicional(), ""))
                .dateOfBirth(nvl(p.getFechaNacimiento(), ""))
                .gender(nvl(p.getSexo(), ""))
                .maritalStatus(nvl(p.getEstadoCivil(), ""))
                .nationality(nvl(p.getNacionalidad(), ""))
                .residencyStatus(p.getResidencia() == null ? "" : String.valueOf(p.getResidencia()))
                .dependents(nvl(p.getNumeroDependientes(), 0))
                .deceasedDate(nvl(p.getFechaDefuncion(), ""))
                .taxId(nvl(p.getRfc(), ""))
                .nationalId(nvl(p.getCurp(), ""))
                .socialSecurityNumber(nvl(p.getNumeroSeguridadSocial(), ""))
                .voterId(nvl(p.getClaveElector(), ""))
                .otherIdType("")
                .otherIdValue("")
                .build();
    }

    private CBCreditReportData.Address mapAddress(CirculoDeCreditoResponse.Domicilio d) {
        if (d == null) {
            return CBCreditReportData.Address.builder().build();
        }
        return CBCreditReportData.Address.builder()
                .streetAddress(nvl(d.getDireccion(), ""))
                .neighborhood(nvl(d.getColonia(), ""))
                .municipality(nvl(d.getMunicipio(), ""))
                .city(nvl(d.getCiudad(), ""))
                .state(nvl(d.getEstado(), ""))
                .postalCode(nvl(d.getCodigoPostal(), ""))
                .country("MX")
                .addressType(nvl(d.getTipoDomicilio(), ""))
                .phoneNumber(nvl(d.getNumeroTelefono(), ""))
                .residenceStartDate(nvl(d.getFechaResidencia(), ""))
                .build();
    }

    private CBCreditReportData.Employment mapEmployment(CirculoDeCreditoResponse.Empleo e) {
        if (e == null) {
            return CBCreditReportData.Employment.builder().build();
        }
        String employerAddress = joinNonEmpty(
                e.getDireccion(), e.getColonia(), e.getMunicipio(), e.getCiudad(), e.getEstado(), e.getCodigoPostal()
        );
        return CBCreditReportData.Employment.builder()
                .employerName(nvl(e.getNombreEmpresa(), ""))
                .employerAddress(employerAddress)
                .employerPhone(nvl(e.getNumeroTelefono(), ""))
                .jobTitle(nvl(e.getPuesto(), ""))
                .employmentStartDate(nvl(e.getFechaContratacion(), ""))
                .employmentEndDate(nvl(e.getFechaUltimoDiaEmpleo(), ""))
                .monthlyIncome(nvl(e.getSalarioMensual(), 0))
                .incomeCurrency(nvl(e.getClaveMoneda(), guessCurrencyByCode(e.getClaveMoneda())))
                .verificationDate(nvl(e.getFechaVerificacionEmpleo(), ""))
                .build();
    }

    private CBCreditReportData.CreditAccount mapCreditAccount(CirculoDeCreditoResponse.Credito c) {
        if (c == null) {
            return CBCreditReportData.CreditAccount.builder().build();
        }
        return CBCreditReportData.CreditAccount.builder()
                .accountNumber(nvl(c.getCuentaActual(), ""))
                .creditorName(nvl(c.getNombreOtorgante(), ""))
                .creditorId(nvl(c.getClaveOtorgante(), ""))
                .accountType(nvl(c.getTipoCuenta(), ""))
                .responsibilityType(nvl(c.getTipoResponsabilidad(), ""))
                .accountStatus(nvl(c.getPagoActual(), ""))
                .openedDate(nvl(c.getFechaAperturaCuenta(), ""))
                .closedDate(nvl(c.getFechaCierreCuenta(), ""))
                .lastPaymentDate(nvl(c.getFechaUltimoPago(), ""))
                .lastUpdatedDate(nvl(c.getFechaActualizacion(), ""))
                .creditLimit(nvl(firstNonNull(c.getLimiteCredito(), c.getCreditoMaximo()), 0))
                .originalAmount(nvl(c.getValorActivoValuacion(), 0))
                .currentBalance(nvl(c.getSaldoActual(), 0))
                .pastDueAmount(nvl(c.getSaldoVencido(), 0))
                .installmentAmount(nvl(c.getMontoPagar(), 0))
                .paymentFrequency(nvl(c.getFrecuenciaPagos(), ""))
                .numberOfPayments(nvl(c.getNumeroPagos(), 0))
                .numberOfPaymentsLate(nvl(c.getNumeroPagosVencidos(), 0))
                .worstDelinquency(c.getPeorAtraso() == null ? "" : String.valueOf(c.getPeorAtraso()))
                .worstDelinquencyDate(nvl(c.getFechaPeorAtraso(), ""))
                .paymentHistory(nvl(c.getHistoricoPagos(), ""))
                .collateral(nvl(c.getGarantia(), ""))
                .currency(nvl(c.getClaveUnidadMonetaria(), guessCurrencyByCode(c.getClaveUnidadMonetaria())))
                .build();
    }

    private CBCreditReportData.Inquiry mapInquiry(CirculoDeCreditoResponse.Consulta q) {
        if (q == null) {
            return CBCreditReportData.Inquiry.builder().build();
        }
        return CBCreditReportData.Inquiry.builder()
                .inquiryDate(nvl(q.getFechaConsulta(), ""))
                .inquiredBy(nvl(q.getNombreOtorgante(), ""))
                .inquiredById(nvl(q.getClaveOtorgante(), ""))
                .inquiryPurpose(nvl(q.getServicios(), ""))
                .amountRequested(nvl(q.getImporteCredito(), ""))
                .creditType(nvl(q.getTipoCredito(), ""))
                .responsibilityType(nvl(q.getTipoResponsabilidad(), ""))
                .currency(nvl(q.getClaveUnidadMonetaria(), guessCurrencyByCode(q.getClaveUnidadMonetaria())))
                .build();
    }

    // ===== Helpers =====
    private <T> List<T> safeList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private String nvl(String value, String def) {
        return value == null ? def : value;
    }

    private Integer nvl(Integer value, Integer def) {
        return value == null ? def : value;
    }

    private <T> T firstNonNull(T a, T b) {
        return a != null ? a : b;
    }

    private String joinNonEmpty(String... parts) {
        return java.util.Arrays.stream(parts)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String guessCurrency(CirculoDeCreditoResponse src) {
        // Try to infer currency from any available sections; default to MXN
        String code = null;
        if (src.getCreditos() != null && !src.getCreditos().isEmpty()) {
            code = src.getCreditos().stream().map(CirculoDeCreditoResponse.Credito::getClaveUnidadMonetaria)
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }
        if (code == null && src.getConsultas() != null && !src.getConsultas().isEmpty()) {
            code = src.getConsultas().stream().map(CirculoDeCreditoResponse.Consulta::getClaveUnidadMonetaria)
                    .filter(Objects::nonNull).findFirst().orElse(null);
        }
        return guessCurrencyByCode(code);
    }

    private String guessCurrencyByCode(String code) {
        if (code == null) return "MXN";
        // Basic mapping; adjust if catalog differs
        return switch (code.trim().toUpperCase()) {
            case "MXN", "MX$", "$" -> "MXN";
            case "USD", "US$" -> "USD";
            default -> code;
        };
    }

    private String findReportDate(CirculoDeCreditoResponse src) {
        // Choose a reasonable date representative for the report
        // Prefer the latest fechaReporte from creditos, otherwise any fechaConsulta from consultas, else empty
        String date = null;
        if (src.getCreditos() != null) {
            date = src.getCreditos().stream()
                    .map(CirculoDeCreditoResponse.Credito::getFechaReporte)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }
        if (date == null && src.getConsultas() != null) {
            date = src.getConsultas().stream()
                    .map(CirculoDeCreditoResponse.Consulta::getFechaConsulta)
                    .filter(Objects::nonNull)
                    .findFirst().orElse(null);
        }
        return date == null ? "" : date;
    }
}
