package com.lebvest.model.entities.investment;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.entities.investor.Investor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Maps to the funding_history table:
 *
 * CREATE TABLE funding_history (
 *   id               INT AUTO_INCREMENT PRIMARY KEY,
 *   company_id       CHAR(36) NOT NULL REFERENCES companies(id),
 *   round            VARCHAR(100) NOT NULL,
 *   amount           DECIMAL(15,2) NOT NULL,
 *   date             DATE NOT NULL,
 *   INDEX(idx_fh_company)(company_id)
 * );
 */
@Entity
@Table(name = "funding_history",
        indexes = @Index(name = "idx_fh_company", columnList = "company_id"))
@Data
@SuperBuilder
@RequiredArgsConstructor
public class FundingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Many funding rounds belong to one company.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /**
     * Round name (e.g. "Series A", "Seed").
     */
    @Column(nullable = false, length = 100)
    private String round;

    /**
     *
     * Amount raised in this round.
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * Date when the round closed.
     */
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "funding_history_investors",
            joinColumns = @JoinColumn(name = "funding_history_id"),
            inverseJoinColumns = @JoinColumn(name = "investor_id")
    )
    private Set<Investor> investors = new HashSet<>();
    // ------------------------
    // Constructors
    // ------------------------


    public FundingHistory(Company company, String round, BigDecimal amount, LocalDate date) {
        this.company = company;
        this.round = round;
        this.amount = amount;
        this.date = date;
    }

    // ------------------------
    // Getters & Setters
    // ------------------------

}