package com.lebvest.repository;

import com.lebvest.model.entities.company.VerificationDocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VerificationDocumentHistoryRepository extends JpaRepository<VerificationDocumentHistory, Long> {
    List<VerificationDocumentHistory> findByCompany_IdOrderByCreatedAtDesc(Long companyId);
    
    List<VerificationDocumentHistory> findByCompany_IdAndStatusOrderByCreatedAtDesc(
            Long companyId, com.lebvest.model.enums.VerificationStatus status);
}
