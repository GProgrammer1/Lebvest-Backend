package com.lebvest.model.entities.investment;

import com.lebvest.model.entities.investor.Investor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Maps to the investor_investments table:
 *
 * CREATE TABLE investor_investments (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   investor_id      CHAR(36)       NOT NULL REFERENCES investors(id),
 *   investment_id    CHAR(36)       NOT NULL REFERENCES investments(id),
 *   amount           DECIMAL(15,2)  NOT NULL,
 *   invested_at      DATE           NOT NULL,
 *   current_value    DECIMAL(15,2)  NOT NULL
 * );
 * CREATE INDEX idx_ii_investor   ON investor_investments(investor_id);
 * CREATE INDEX idx_ii_investment ON investor_investments(investment_id);
 */
@Entity
@Table(
        name = "investor_investments",
        indexes = {
                @Index(name = "idx_ii_investor", columnList = "investor_id"),
                @Index(name = "idx_ii_investment", columnList = "investment_id")
        }
)
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@SuperBuilder
public class InvestorInvestment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Many investments belong to one investor. */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investor_id", nullable = false)
    private Investor investor;

    /** Many investments belong to one investment listing. */
    @NonNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "investment_id", nullable = false)
    private Investment investment;

    /** Amount the investor put in. */
    @NonNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Date when the investment was made. */
    @NonNull
    @Column(name = "invested_at", nullable = false)
    private LocalDate investedAt;

    /** Current value of that investment. */
    @NonNull
    @Column(name = "current_value", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentValue;
}
