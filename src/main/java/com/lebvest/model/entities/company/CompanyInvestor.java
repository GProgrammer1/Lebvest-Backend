package com.lebvest.model.entities.company;

import com.lebvest.model.entities.investor.Investor;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name="company_investors",
        indexes = {
                @Index(name = "idx_ci_company", columnList = "company_id")
        })
@RequiredArgsConstructor
@Data
@SuperBuilder
public class CompanyInvestor {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "investor_id",
    nullable = false)
    private Investor investor;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)              // many investments → one company
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "invested_at", nullable = false)
    private LocalDate investedAt;




}
