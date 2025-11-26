package com.lebvest.repository;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.InvestorNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestorNotificationRepository extends JpaRepository<InvestorNotification, Long> {
    List<InvestorNotification> findByInvestorOrderByNotifiedAtDesc(Investor investor);
    Optional<InvestorNotification> findByIdAndInvestor(Long id, Investor investor);
}


