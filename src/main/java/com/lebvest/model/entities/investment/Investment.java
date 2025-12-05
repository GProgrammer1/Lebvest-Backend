package com.lebvest.model.entities.investment;

import com.lebvest.model.entities.company.Company;
import com.lebvest.model.enums.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 2.3) investments */
@Entity
@Table(name = "investments",
        indexes = {
                @Index(name = "idx_inv_company", columnList = "company_id"),
                @Index(name = "idx_inv_category", columnList = "category"),
        })
@RequiredArgsConstructor
@Data
@SuperBuilder
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Company company;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvestmentCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    private RiskLevel riskLevel;

    @Column(name = "expected_return", precision = 5, scale = 2, nullable = false)
    private BigDecimal expectedReturn;

    @Column(name = "min_investment", precision = 15, scale = 2, nullable = false)
    private BigDecimal minInvestment;

    @Column(name = "target_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal targetAmount;

    @Column(name = "raised_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal raisedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Location location;


    @Enumerated(EnumType.STRING)
    @Column(name = "investment_type", nullable = false)
    private InvestmentType investmentType;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "funding_stage")
    private String fundingStage;

    @Column(nullable = false)
    private LocalDate deadline;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvestmentStatus status = InvestmentStatus.PENDING_REVIEW;

    @OneToOne(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InvestmentAiPrediction aiPrediction;

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestmentHighlight> highlights = new ArrayList<>();

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestmentTeamMember> teamMembers = new ArrayList<>();

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestmentFinancial> financials = new ArrayList<>();

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestmentDocument> documents = new ArrayList<>();

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestmentUpdate> updates = new ArrayList<>();

    @OneToMany(mappedBy = "investment", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<InvestorInvestment> investorInvestments = new ArrayList<>();
}
