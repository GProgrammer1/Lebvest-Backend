package com.lebvest.repository;

import com.lebvest.model.entities.investment.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    List<Investment> findTop5ByOrderByCreatedAtDesc();

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT i.* FROM investments i " +
                    "JOIN companies c ON i.company_id = c.id " +
                    "WHERE LOWER(i.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(i.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
                    "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))",
            nativeQuery = true)
    List<Investment> searchInvestments(@org.springframework.data.repository.query.Param("query") String query);
}

