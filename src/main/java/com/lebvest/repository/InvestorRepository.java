package com.lebvest.repository;

import com.lebvest.model.entities.investor.Investor;
import com.lebvest.model.entities.investor.User;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {

    boolean existsByUser(User user);
    
    Optional<Investor> findByUser(User user);

    @EntityGraph(attributePaths = {
            "user",
            "preferences",
            "watchlist",
            "watchlist.company",
            "notifications",
            "notifications.relatedInvestment",
            "notifications.relatedInvestment.company"
            // Note: investments and goals are fetched lazily to avoid MultipleBagFetchException
            // They will be loaded when accessed within the @Transactional method
    })
    @Query("SELECT i FROM Investor i WHERE i.user.email = :email")
    Optional<Investor> findOneWithDetailsByUserEmail(@Param("email") String email);
    
    // Simpler method for profile - only loads user, no collections
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Investor i WHERE i.user.email = :email")
    Optional<Investor> findByUserEmailForProfile(@Param("email") String email);

    @EntityGraph(attributePaths = {"user", "preferences"})
    @Query("SELECT DISTINCT i FROM Investor i LEFT JOIN i.preferences p " +
           "WHERE (:query IS NULL OR :query = '' OR LOWER(i.user.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.user.email) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:minPortfolio IS NULL OR i.portfolio_value >= :minPortfolio) " +
           "AND (:riskLevel IS NULL OR EXISTS (SELECT 1 FROM p.riskLevels rl WHERE rl = :riskLevel)) " +
           "AND (:category IS NULL OR EXISTS (SELECT 1 FROM p.categories c WHERE c = :category))")
    Page<Investor> searchInvestors(
            @Param("query") String query,
            @Param("minPortfolio") BigDecimal minPortfolio,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("category") InvestmentCategory category,
            Pageable pageable
    );
}
