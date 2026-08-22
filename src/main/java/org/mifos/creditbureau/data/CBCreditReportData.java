package org.mifos.creditbureau.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class CBCreditReportData {
    // ==== Report Metadata ====
    private String reportId;
    private String inquiryId;
    private String bureauName;
    private String reportDate;
    private String country;
    private String currency;

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
        private String firstName;
        private String middleName;
        private String lastName;
        private String additionalLastName;
        private String dateOfBirth;
        private String gender;
        private String maritalStatus;
        private String nationality;
        private String residencyStatus;
        @Builder.Default private Integer dependents = 0;
        private String deceasedDate;

        // Identifiers
        private String taxId;       // RFC, SSN, etc.
        private String nationalId; // CURP, Passport, etc.
        private String socialSecurityNumber;
        private String voterId;
        private String otherIdType;
        private String otherIdValue;
    }

    // ==== Address ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Address {
        private String streetAddress;
        private String neighborhood;
        private String municipality;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String addressType; // current, previous, work, etc.
        private String phoneNumber;
        private String residenceStartDate;
    }

    // ==== Employment ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Employment {
        private String employerName;
        private String employerAddress;
        private String employerPhone;
        private String jobTitle;
        private String employmentStartDate;
        private String employmentEndDate;
        @Builder.Default private Integer monthlyIncome = 0;
        private String incomeCurrency;
        private String verificationDate;
    }

    // ==== Credit Account (Trade Line) ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class CreditAccount {
        private String accountNumber;
        private String creditorName;
        private String creditorId;
        private String accountType;       // e.g., mortgage, credit card
        private String responsibilityType; // individual, joint, guarantor
        private String accountStatus;     // open, closed, delinquent, etc.
        private String openedDate;
        private String closedDate;
        private String lastPaymentDate;
        private String lastUpdatedDate;
        @Builder.Default private Integer creditLimit = 0;
        @Builder.Default private Integer originalAmount = 0;
        @Builder.Default private Integer currentBalance = 0;
        @Builder.Default private Integer pastDueAmount = 0;
        @Builder.Default private Integer installmentAmount = 0;
        private String paymentFrequency;
        @Builder.Default private Integer numberOfPayments = 0;
        @Builder.Default private Integer numberOfPaymentsLate = 0;
        private String worstDelinquency;
        private String worstDelinquencyDate;
        private String paymentHistory; // e.g., monthly codes
        private String collateral;
        private String currency;
    }

    // ==== Inquiry ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Inquiry {
        private String inquiryDate;
        private String inquiredBy;
        private String inquiredById;
        private String inquiryPurpose;
        private String amountRequested;
        private String creditType;
        private String responsibilityType;
        private String currency;
    }

    // ==== Public Records ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class PublicRecord {
        private String recordType; // bankruptcy, lien, judgment
        private String fileDate;
        private String status;
        @Builder.Default private Integer amount = 0;
        private String courtName;
        private String country;
    }

    // ==== Scores ====
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Score {
        private String scoreType;   // bureau score, FICO, etc.
        @Builder.Default private Integer scoreValue = 0;
        private String scoreDate;
        private String riskLevel;
    }
}
