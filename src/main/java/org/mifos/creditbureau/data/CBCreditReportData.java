package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
public class CBCreditReportData {
    // ==== Report Metadata ====
    @Builder.Default private String reportId = "string";
    @Builder.Default private String inquiryId = "string";
    @Builder.Default private String bureauName = "string";
    @Builder.Default private String reportDate = "string";
    @Builder.Default private String country = "string";
    @Builder.Default private String currency = "string";

    @Builder.Default private Person person = Person.builder().build();
    @Builder.Default private List<Address> addresses = List.of();
    @Builder.Default private List<Employment> employments = List.of();
    @Builder.Default private List<CreditAccount> creditAccounts = List.of();
    @Builder.Default private List<Inquiry> inquiries = List.of();
    @Builder.Default private List<PublicRecord> publicRecords = List.of();
    @Builder.Default private List<Score> scores = List.of();

    // ==== Person ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Person {
        @Builder.Default private String firstName = "string";
        @Builder.Default private String middleName = "string";
        @Builder.Default private String lastName = "string";
        @Builder.Default private String additionalLastName = "string";
        @Builder.Default private String dateOfBirth = "string";
        @Builder.Default private String gender = "string";
        @Builder.Default private String maritalStatus = "string";
        @Builder.Default private String nationality = "string";
        @Builder.Default private String residencyStatus = "string";
        @Builder.Default private Integer dependents = 0;
        @Builder.Default private String deceasedDate = "string";

        // Identifiers
        @Builder.Default private String taxId = "string";       // RFC, SSN, etc.
        @Builder.Default private String nationalId = "string"; // CURP, Passport, etc.
        @Builder.Default private String socialSecurityNumber = "string";
        @Builder.Default private String voterId = "string";
        @Builder.Default private String otherIdType = "string";
        @Builder.Default private String otherIdValue = "string";
    }

    // ==== Address ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Address {
        @Builder.Default private String streetAddress = "string";
        @Builder.Default private String neighborhood = "string";
        @Builder.Default private String municipality = "string";
        @Builder.Default private String city = "string";
        @Builder.Default private String state = "string";
        @Builder.Default private String postalCode = "string";
        @Builder.Default private String country = "string";
        @Builder.Default private String addressType = "string"; // current, previous, work, etc.
        @Builder.Default private String phoneNumber = "string";
        @Builder.Default private String residenceStartDate = "string";
    }

    // ==== Employment ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Employment {
        @Builder.Default private String employerName = "string";
        @Builder.Default private String employerAddress = "string";
        @Builder.Default private String employerPhone = "string";
        @Builder.Default private String jobTitle = "string";
        @Builder.Default private String employmentStartDate = "string";
        @Builder.Default private String employmentEndDate = "string";
        @Builder.Default private Integer monthlyIncome = 0;
        @Builder.Default private String incomeCurrency = "string";
        @Builder.Default private String verificationDate = "string";
    }

    // ==== Credit Account (Trade Line) ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreditAccount {
        @Builder.Default private String accountNumber = "string";
        @Builder.Default private String creditorName = "string";
        @Builder.Default private String creditorId = "string";
        @Builder.Default private String accountType = "string";       // e.g., mortgage, credit card
        @Builder.Default private String responsibilityType = "string"; // individual, joint, guarantor
        @Builder.Default private String accountStatus = "string";     // open, closed, delinquent, etc.
        @Builder.Default private String openedDate = "string";
        @Builder.Default private String closedDate = "string";
        @Builder.Default private String lastPaymentDate = "string";
        @Builder.Default private String lastUpdatedDate = "string";
        @Builder.Default private Integer creditLimit = 0;
        @Builder.Default private Integer originalAmount = 0;
        @Builder.Default private Integer currentBalance = 0;
        @Builder.Default private Integer pastDueAmount = 0;
        @Builder.Default private Integer installmentAmount = 0;
        @Builder.Default private String paymentFrequency = "string";
        @Builder.Default private Integer numberOfPayments = 0;
        @Builder.Default private Integer numberOfPaymentsLate = 0;
        @Builder.Default private String worstDelinquency = "string";
        @Builder.Default private String worstDelinquencyDate = "string";
        @Builder.Default private String paymentHistory = "string"; // e.g., monthly codes
        @Builder.Default private String collateral = "string";
        @Builder.Default private String currency = "string";
    }

    // ==== Inquiry ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Inquiry {
        @Builder.Default private String inquiryDate = "string";
        @Builder.Default private String inquiredBy = "string";
        @Builder.Default private String inquiredById = "string";
        @Builder.Default private String inquiryPurpose = "string";
        @Builder.Default private String amountRequested = "string";
        @Builder.Default private String creditType = "string";
        @Builder.Default private String responsibilityType = "string";
        @Builder.Default private String currency = "string";
    }

    // ==== Public Records ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PublicRecord {
        @Builder.Default private String recordType = "string"; // bankruptcy, lien, judgment
        @Builder.Default private String fileDate = "string";
        @Builder.Default private String status = "string";
        @Builder.Default private Integer amount = 0;
        @Builder.Default private String courtName = "string";
        @Builder.Default private String country = "string";
    }

    // ==== Scores ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Score {
        @Builder.Default private String scoreType = "string";   // bureau score, FICO, etc.
        @Builder.Default private Integer scoreValue = 0;
        @Builder.Default private String scoreDate = "string";
        @Builder.Default private String riskLevel = "string";
    }
}
