package com.lebvest.repository;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    boolean existsByUser(User user);
    Optional<Company> findByUser(User user);
}
