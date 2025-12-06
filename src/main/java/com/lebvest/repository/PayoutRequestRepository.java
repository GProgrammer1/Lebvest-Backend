package com.lebvest.repository;

import com.lebvest.model.entities.investment.PayoutRequest;
import com.lebvest.model.enums.PayoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutRequestRepository extends JpaRepository<PayoutRequest, Long> {
    List<PayoutRequest> findByInvestor_IdOrderByCreatedAtDesc(Long investorId);
    
    List<PayoutRequest> findByInvestor_IdAndStatusOrderByCreatedAtDesc(Long investorId, PayoutStatus status);
    
    List<PayoutRequest> findByCompany_IdOrderByCreatedAtDesc(Long companyId);
    
    List<PayoutRequest> findByCompany_IdAndStatusOrderByCreatedAtDesc(Long companyId, PayoutStatus status);
    
    List<PayoutRequest> findByStatusOrderByCreatedAtDesc(PayoutStatus status);
    
    Optional<PayoutRequest> findByInvestorInvestment_Id(Long investorInvestmentId);
    
    @Query("SELECT pr FROM PayoutRequest pr WHERE pr.investment.id = :investmentId AND pr.status = :status")
    List<PayoutRequest> findByInvestmentIdAndStatus(
            @Param("investmentId") Long investmentId,
            @Param("status") PayoutStatus status);
}
