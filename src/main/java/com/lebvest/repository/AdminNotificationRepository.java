package com.lebvest.repository;

import com.lebvest.model.entities.admin.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lebvest.model.entities.investor.Investor;
import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findByInvestorAndReadFalse(Investor investor);
}
