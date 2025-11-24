package com.lebvest.repository;

import com.lebvest.model.entities.investment.Investment;
import com.lebvest.model.enums.InvestmentCategory;
import com.lebvest.model.enums.InvestmentType;
import com.lebvest.model.enums.Location;
import com.lebvest.model.enums.RiskLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    @Query("SELECT i FROM Investment i WHERE " +
            "(:category IS NULL OR i.category = :category) AND " +
            "(:riskLevel IS NULL OR i.riskLevel = :riskLevel) AND " +
            "(:minReturn IS NULL OR i.expectedReturn >= :minReturn) AND " +
            "(:location IS NULL OR i.location = :location) AND " +
            "(:sector IS NULL OR i.company.sector = :sector) AND " +
            "(:investmentType IS NULL OR i.investmentType = :investmentType) AND " +
            "(:minAmount IS NULL OR i.minInvestment >= :minAmount) AND " +
            "(:maxAmount IS NULL OR i.minInvestment <= :maxAmount)")
    Page<Investment> findInvestmentsWithFilters(
            @Param("category") InvestmentCategory category,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("minReturn") BigDecimal minReturn,
            @Param("location") Location location,
            @Param("sector") com.lebvest.model.enums.CompanySector sector,
            @Param("investmentType") InvestmentType investmentType,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable
    );

    @Query("SELECT i FROM Investment i WHERE i.deadline >= CURRENT_DATE ORDER BY i.raisedAmount DESC, i.createdAt DESC")
    List<Investment> findFeaturedInvestments(Pageable pageable);

    @Query("SELECT i FROM Investment i WHERE i.deadline >= CURRENT_DATE ORDER BY i.createdAt DESC")
    List<Investment> findRecentInvestments(Pageable pageable);

    List<Investment> findTop5ByOrderByCreatedAtDesc();
}
