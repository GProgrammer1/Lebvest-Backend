package com.lebvest.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for company verification documents submission (Step 2)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyVerificationRequest {
    // A. Company Legal Documents
    private String certificateOfIncorporation;
    private String articlesOfAssociation;
    private String taxRegistrationCertificate;
    private String proofOfRegisteredAddress;

    // B. Ownership & Control
    private String shareholderStructure;
    private List<String> uboIds; // IDs of All UBOs
    private List<String> directorIds; // IDs of Directors
    private List<String> authorizedSignatoryIds; // IDs of Authorized Signatory
    private String boardResolution;
    private String pepSanctionsDeclaration;

    // C. Finance & Banking
    private String bankAccountConfirmation;
    private List<String> financialStatements; // Latest 1–3 years Financial Statements
    private List<String> managementAccounts; // Latest management accounts (if startup)
    private List<String> bankStatements; // 3–6 months bank statements
    private String sourceOfFundsDeclaration;
}

