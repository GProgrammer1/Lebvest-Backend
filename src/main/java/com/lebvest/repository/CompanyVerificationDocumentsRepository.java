package com.lebvest.repository;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyVerificationDocuments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyVerificationDocumentsRepository extends JpaRepository<CompanyVerificationDocuments, Long> {
    Optional<CompanyVerificationDocuments> findByCompany(Company company);
    Optional<CompanyVerificationDocuments> findByCompanyId(Long companyId);
    
    // Batch load verification documents by company IDs
    @Query("SELECT v FROM CompanyVerificationDocuments v WHERE v.company.id IN :companyIds")
    List<CompanyVerificationDocuments> findByCompanyIds(@Param("companyIds") List<Long> companyIds);
}

