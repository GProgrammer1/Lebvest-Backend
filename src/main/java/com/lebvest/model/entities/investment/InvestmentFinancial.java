package com.lebvest.model.entities.investment;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(
        name = "investment_financials",
        indexes = @Index(name = "idx_ifin_investment", columnList = "investment_id")
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class InvestmentFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many financial records belong to one investment.
     */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /**
     * The fiscal year for this record.
     */
    @NonNull
    @Column(nullable = false)
    private Integer year;

    /**
     * Revenue amount for the year.
     */
    @NonNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue;

    /**
     * Expenses amount for the year.
     */
    @NonNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal expenses;

    /**
     * Profit amount for the year.
     */
    @NonNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal profit;
}
