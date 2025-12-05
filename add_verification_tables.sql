-- SQL Script to add company verification support
-- Run this to add the status column and verification documents table

USE lebvest;

-- Add status column to companies table if it doesn't exist
ALTER TABLE companies ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'PENDING';

-- Update existing APPROVED companies to APPROVED status
UPDATE companies SET status = 'APPROVED' WHERE status = 'PENDING';

-- Create company_verification_documents table
CREATE TABLE IF NOT EXISTS company_verification_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL UNIQUE,
    certificate_of_incorporation VARCHAR(512),
    articles_of_association VARCHAR(512),
    tax_registration_certificate VARCHAR(512),
    proof_of_registered_address VARCHAR(512),
    shareholder_structure VARCHAR(512),
    board_resolution VARCHAR(512),
    pep_sanctions_declaration VARCHAR(512),
    bank_account_confirmation VARCHAR(512),
    source_of_funds_declaration VARCHAR(512),
    is_approved BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Create collection tables for lists
CREATE TABLE IF NOT EXISTS company_ubo_ids (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_director_ids (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_signatory_ids (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_financial_statements (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_management_accounts (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS company_bank_statements (
    verification_docs_id BIGINT NOT NULL,
    document_path VARCHAR(512) NOT NULL,
    FOREIGN KEY (verification_docs_id) REFERENCES company_verification_documents(id) ON DELETE CASCADE
);

SELECT 'Verification tables created successfully!' as status;

