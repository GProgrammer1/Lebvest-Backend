package com.lebvest.repository;

import com.lebvest.model.entities.investment.PayoutHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayoutHistoryRepository extends JpaRepository<PayoutHistory, Long> {
    List<PayoutHistory> findByInvestor_IdOrderByCompletedAtDesc(Long investorId);
    
    List<PayoutHistory> findByInvestment_IdOrderByCompletedAtDesc(Long investmentId);
}
