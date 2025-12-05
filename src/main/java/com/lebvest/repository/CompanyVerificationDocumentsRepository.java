package com.lebvest.repository;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyVerificationDocumentsRepository extends JpaRepository<CompanyVerificationDocuments, Long> {
    Optional<CompanyVerificationDocuments> findByCompany(Company company);
    Optional<CompanyVerificationDocuments> findByCompanyId(Long companyId);
}

