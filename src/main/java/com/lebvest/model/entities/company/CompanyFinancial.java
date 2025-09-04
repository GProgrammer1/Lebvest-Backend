package com.lebvest.model.entities.company;

import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name="company_financials",
        indexes = {
                @Index(name = "idx_cf_company", columnList = "company_id")
        })
@RequiredArgsConstructor
@Data
@SuperBuilder
public class CompanyFinancial {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id",
    referencedColumnName = "id"
    )
    private Company company;

    @Column(nullable = false)
    private int year;

    @Column(name = "revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "expenses", nullable = false, precision = 15, scale = 2)
    private BigDecimal expenses = BigDecimal.ZERO;

    @Column(name = "profit", nullable = false, precision = 15, scale = 2)
    private BigDecimal profit = BigDecimal.ZERO;



}
