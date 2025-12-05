package com.lebvest.model.entities.company;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/**
 * Entity to store company verification documents (Step 2 of company registration)
 * These documents are uploaded once and reused for all projects.
 */
@Entity
@Table(name = "company_verification_documents")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyVerificationDocuments {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", unique = true, nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Company company;

    // A. Company Legal Documents
    @Column(length = 512)
    private String certificateOfIncorporation; // Certificate of Incorporation / Commercial Register Extract

    @Column(length = 512)
    private String articlesOfAssociation; // Articles of Association / Bylaws

    @Column(length = 512)
    private String taxRegistrationCertificate; // Tax Registration Certificate

    @Column(length = 512)
    private String proofOfRegisteredAddress; // Proof of Registered Address (lease or utility bill)

    // B. Ownership & Control
    @Column(length = 512)
    private String shareholderStructure; // Shareholder Structure (UBOs ≥ 25%)

    @ElementCollection
    @CollectionTable(name = "company_ubo_ids", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> uboIds = new java.util.ArrayList<>(); // IDs of All UBOs

    @ElementCollection
    @CollectionTable(name = "company_director_ids", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> directorIds = new java.util.ArrayList<>(); // IDs of Directors

    @ElementCollection
    @CollectionTable(name = "company_signatory_ids", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> authorizedSignatoryIds = new java.util.ArrayList<>(); // IDs of Authorized Signatory

    @Column(length = 512)
    private String boardResolution; // Board Resolution / Power of Attorney authorizing the account operator

    @Column(length = 512)
    private String pepSanctionsDeclaration; // PEP/Sanctions Declaration Form

    // C. Finance & Banking
    @Column(length = 512)
    private String bankAccountConfirmation; // Bank Account Confirmation (IBAN in the company name)

    @ElementCollection
    @CollectionTable(name = "company_financial_statements", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> financialStatements = new java.util.ArrayList<>(); // Latest 1–3 years Financial Statements

    @ElementCollection
    @CollectionTable(name = "company_management_accounts", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> managementAccounts = new java.util.ArrayList<>(); // Latest management accounts (if startup)

    @ElementCollection
    @CollectionTable(name = "company_bank_statements", joinColumns = @JoinColumn(name = "verification_docs_id"))
    @Column(name = "document_path")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private java.util.List<String> bankStatements = new java.util.ArrayList<>(); // 3–6 months bank statements

    @Column(length = 512)
    private String sourceOfFundsDeclaration; // Source-of-Funds / Source-of-Wealth Declaration

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;
}

