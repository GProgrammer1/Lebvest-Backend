package com.lebvest.repository;

import com.lebvest.model.entities.investment.InvestmentRequest;
import com.lebvest.model.enums.InvestmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRequestRepository extends JpaRepository<InvestmentRequest, Long> {
    
    List<InvestmentRequest> findByInvestment_Company_IdAndStatusOrderByCreatedAtDesc(
            Long companyId, InvestmentRequestStatus status);
    
    List<InvestmentRequest> findByInvestor_IdAndStatusOrderByCreatedAtDesc(
            Long investorId, InvestmentRequestStatus status);
    
    List<InvestmentRequest> findByInvestor_IdOrderByCreatedAtDesc(Long investorId);
    
    Optional<InvestmentRequest> findByInvestor_IdAndInvestment_IdAndStatus(
            Long investorId, Long investmentId, InvestmentRequestStatus status);
    
    @Query("SELECT ir FROM InvestmentRequest ir WHERE ir.investment.id = :investmentId AND ir.status = :status")
    List<InvestmentRequest> findByInvestmentIdAndStatus(
            @Param("investmentId") Long investmentId, 
            @Param("status") InvestmentRequestStatus status);
    
    Optional<InvestmentRequest> findByStripePaymentIntentId(String paymentIntentId);
}
