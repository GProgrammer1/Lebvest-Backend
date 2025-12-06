package com.lebvest.repository;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.company.CompanyNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyNotificationRepository extends JpaRepository<CompanyNotification, String> {
    List<CompanyNotification> findByCompanyOrderByNotifiedAtDesc(Company company);
    List<CompanyNotification> findByCompanyAndIsReadFalseOrderByNotifiedAtDesc(Company company);
}
